package org.hexagonical.unifiedcurrency;

import com.mojang.brigadier.arguments.FloatArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;


import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.hexagonical.unifiedcurrency.impl.Config;
import org.hexagonical.unifiedcurrency.impl.Database;
import org.hexagonical.unifiedcurrency.impl.UCCommands;
import org.jetbrains.annotations.NotNull;


import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class Unifiedcurrency implements ModInitializer {

    public static Logger logger = Logger.getLogger("unifiedcurrency");
    public static final String MOD_ID = "unifiedcurrency";

    Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(MOD_ID + ".yml");
    public static MinecraftServerAudiences adventure;

    @Override
    public void onInitialize() {

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            adventure = MinecraftServerAudiences.of(server);
        });

        // Clean up when the server stops to prevent memory leaks
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            adventure = null;
        });



        if (Files.notExists(CONFIG_PATH)) {
            try {
                InputStream in = Unifiedcurrency.class
                        .getClassLoader()
                        .getResourceAsStream("defaults/config.yml");

                if (in == null) {
                    throw new IllegalStateException("Default config file not found in resources. Ya fucked up! Uh make a issue on github or smth. (if persistent, try copying the config manually?)");
                }
                Files.copy(in, CONFIG_PATH);
            } catch (Exception e) {
                logger.severe("Failed to create config file: " + e.getMessage());
            }


        }

        Config.load();




        logger.info("Opening database...");
        Path DATABASE_PATH = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("database.db");

        if (Files.notExists(DATABASE_PATH)) {
            try {
                Files.createFile(DATABASE_PATH);
                logger.info("Database file created at: " + DATABASE_PATH);
            } catch (Exception e) {
                logger.severe("Failed to create database file: " + e.getMessage());
            }
        }

        logger.info("Creating Tables");

        String url = "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath();
        try {
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();

            stmt.execute("PRAGMA foreign_keys = ON;");

            logger.info("Connected to db");

            String createPlayerTable = """
                    CREATE TABLE IF NOT EXISTS players (
                        uuid TEXT PRIMARY KEY,
                        username TEXT NOT NULL,
                        currency TEXT DEFAULT '{}',
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

            String createTransactionsTable = """
                    CREATE TABLE IF NOT EXISTS transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        author TEXT NOT NULL,
                        recipient TEXT NOT NULL,
                        change DOUBLE NOT NULL,
                        timestamp TEXT DEFAULT CURRENT_TIMESTAMP,
                        valid BOOLEAN DEFAULT true,
                        FOREIGN KEY(author) REFERENCES players(uuid),
                        FOREIGN KEY(recipient) REFERENCES players(uuid)
                    )
                    CREATE INDEX IF NOT EXISTS idx_sender ON transactions(author);
                    CREATE INDEX IF NOT EXISTS idx_receiver ON transactions(receiver);
                    """;
            stmt.execute(createPlayerTable);
            stmt.execute(createTransactionsTable);

        } catch (Exception e) {
            logger.severe("Failed to connect to the database: " + e.getMessage());
        }


        



        logger.info("registering commands");
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("uc")
                            .executes(UCCommands::rootCommand)
                            .then(CommandManager.literal("reload")
                                    .executes(UCCommands::reloadCommand)
                                    .requires(Permissions.require("unifiedcurrency.command.reload")))
                            .then(CommandManager.literal("recalculatebalannces")
                                    .executes(UCCommands::recalculateBalancesCommand)
                                    .requires(Permissions.require("unifiedcurrency.command.recalculatebalances")))

                            .then(CommandManager.literal("balance")
                                    .executes(UCCommands::getBalanceCommand)

                                    // get
                                    .then(CommandManager.literal("get")
                                            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                    .executes(UCCommands::getOtherBalanceCommand)))
                                    // add
                                    .then(CommandManager.literal("add")
                                            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                    .then(CommandManager.argument("amount", FloatArgumentType.floatArg())
                                                            .executes(UCCommands::addBalanceCommand))))
                                    // set
                                    .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                .then(CommandManager.argument("amount", FloatArgumentType.floatArg())
                                                        .executes(UCCommands::setBalanceCommand)))))
                            .then(CommandManager.literal("transactions")
                                    .executes(UCCommands::getTransactionsCommand))

            );
            dispatcher.register(CommandManager.literal("pay")
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                            .then(CommandManager.argument("amount", FloatArgumentType.floatArg())
                                    .executes(UCCommands::payPlayerCommand)))

            );
        });

        logger.info("registering events");

        ServerPlayerEvents.JOIN.register((this::onPlayerJoin));

        logger.info("Unified Currency Mod Initialized :D");

    }

    private void onPlayerJoin(ServerPlayerEntity player) {
        if (Database.isUserInDB(player.getUuidAsString()))
            CompletableFuture.runAsync(() -> initPlayerOnDB(player));
    }

    private void initPlayerOnDB(ServerPlayerEntity player) {
        String playersql = "INSERT OR IGNORE INTO players (uuid, username, currency) VALUES (?, ?, ?)";
        String transactionsql = "INSERT OR IGNORE INTO transactions (author, recipient, change) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(Database.url)) {
            PreparedStatement stmt = conn.prepareStatement(playersql);
            PreparedStatement stmt2 = conn.prepareStatement(transactionsql);

            stmt.setString(1, player.getUuidAsString());
            stmt.setString(2, player.getName().getString());
            stmt.setString(3, "{'main':" +Config.get("starter_currency")+"}" );
            stmt.executeUpdate();


            stmt2.setString(1, "server");
            stmt2.setString(2, player.getUuidAsString());
            stmt2.setDouble(3, 100.00);
            stmt2.executeUpdate();

        } catch (SQLException e) {
            logger.severe("Failed to add player to db: " + e);
        }
    }

}
