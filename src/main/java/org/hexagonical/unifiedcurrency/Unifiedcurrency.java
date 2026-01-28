package org.hexagonical.unifiedcurrency;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.CommandManager;


import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Logger;

public class Unifiedcurrency implements ModInitializer {

    Logger logger = Logger.getLogger("unifiedcurrency");
    public static final String MOD_ID = "unifiedcurrency";
     Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(MOD_ID + ".yml");
    @Override
    public void onInitialize() {


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

        String url = "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath();
        try {
            Connection conn = DriverManager.getConnection(url);
        } catch (Exception e) {
            logger.severe("Failed to connect to the database: " + e.getMessage());
        }

        logger.info("Creating Tables");
        



        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("uc")
                            .executes(UCCommands::rootCommand)
                            .then(CommandManager.literal("balance")
                                    .executes(UCCommands::getBalanceCommand)
                                    .then(CommandManager.literal("get")
                                            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                    .executes(UCCommands::getOtherBalanceCommand)))
                                    .then(CommandManager.literal("add")
                                            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile()))
                                                    .then(CommandManager.argument("amount", FloatArgumentType.floatArg())
                                                            .executes(UCCommands::addBalanceCommand))))

                                    .then(CommandManager.literal("set")
                                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())))
                                                    .then(CommandManager.argument("amount", FloatArgumentType.floatArg())
                                                            .executes(UCCommands::setBalanceCommand)))
            );
        });
        logger.info("Unified Currency Mod Initialized :D");

    }
}
