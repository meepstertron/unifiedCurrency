package org.hexagonical.unifiedcurrency.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.hexagonical.unifiedcurrency.Unifiedcurrency;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public class UCCommands {

    static Gson gson = new Gson();

    // Root command
    public static int rootCommand(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> Text.literal("Running Unified Currency"), false);
        return 1;
    }

    // /uc balance command
    public static int getBalanceCommand(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("This command can only be run by a player."));
            return 0;
        }



        System.out.println("UUID used in query: " + player.getUuidAsString());


        double balance = getBalance(player.getUuidAsString(), false, true);

        context.getSource().sendFeedback(() -> Text.literal("Your Balance is: " + balance + "$"), false);
        return 1;
    }

    // /uc balance get <player> command
    public static int getOtherBalanceCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        float balance = 0.0f; // Placeholder
        PlayerConfigEntry player = GameProfileArgumentType.getProfileArgument(context, "player").iterator().next();
        String playerName = player.name();
        context.getSource().sendFeedback(() -> Text.literal(playerName + "'s Balance is: " + balance + "$"), false);
        return 1;
    }

    public static int addBalanceCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Placeholder for add balance logic
        PlayerConfigEntry player = GameProfileArgumentType.getProfileArgument(context, "player").iterator().next();
        String playerName = player.name();
        float amount = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(context, "amount");

        CompletableFuture.runAsync(() -> {
            try {
                addBalance(player, (double) amount);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        context.getSource().sendFeedback(() -> Text.literal("Added " + amount + "$" + " to " + playerName + "'s balance"), false);
        return 1;
    }

    public static int setBalanceCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Placeholder for set balance logic
        PlayerConfigEntry player = GameProfileArgumentType.getProfileArgument(context, "player").iterator().next();
        String playerName = player.name();
        float amount = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(context, "amount");
        try {
            setBalance(player, (double) amount);
        } catch (SQLException e) {
            return 0;
        }


        context.getSource().sendFeedback(() -> Text.literal("Set " + playerName + "'s balance to " + amount + "$"), false);
        return 1;

    }

    private static void addBalance(PlayerConfigEntry player, Double ammount) throws SQLException {
        String transactionsql = "INSERT INTO transactions (author, recipient, change) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(Database.url)) {
            PreparedStatement stmt = conn.prepareStatement(transactionsql);

            stmt.setString(1, "server");
            stmt.setString(2, player.id().toString());
            stmt.setDouble(3, ammount);
            stmt.executeUpdate();


            getBalance(player.id().toString(), true, false  );
        } catch (SQLException e) {
            Unifiedcurrency.logger.severe("Couldnt add balance: " + e);

        }
    }

    public static int reloadCommand(CommandContext<ServerCommandSource> context) {
        Config.load();
        context.getSource().sendFeedback(() -> Text.literal("Reloaded!").formatted(Formatting.GREEN), false);
        return 1;
    }

    public static int recalculateBalancesCommand(CommandContext<ServerCommandSource> context) {

        CompletableFuture.runAsync(UCCommands::recalcBalances);

        return 1;
    }

    public static int getTransactionsCommand(CommandContext<ServerCommandSource> context) {

        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("This command can only be run by a player."));
            return 0;
        }
        List<Database.Transaction> transactions;
        try {
             transactions = getTransactions(player.getUuidAsString(), 10);
        } catch (SQLException e) {
            return 0;
        }

        player.sendMessage("");
        for (Database.Transaction tx : transactions) {

        }


        return 1;
    }

    private static List<Database.Transaction> getTransactions(String uuid, int limit) throws SQLException {
        List<Database.Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE author = ? OR recipient = ? ORDER BY timestamp DESC LIMIT ?";

        try (Connection conn = DriverManager.getConnection(Database.url)) {
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setString(1, uuid);
            stmt.setString(2, uuid);
            stmt.setInt(3, limit);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                transactions.add(new Database.Transaction(
                        rs.getInt("id"),
                        rs.getString("author"),
                        rs.getString("recipient"),
                        rs.getDouble("change"),
                        rs.getString("timestamp"),
                        rs.getBoolean("valid")



                ));
            }
        }

        return transactions;
    }

    private static void setBalance(PlayerConfigEntry player, Double ammount) throws SQLException {
        String transactionsql = "INSERT INTO transactions (author, recipient, change) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(Database.url)) {
            PreparedStatement stmt = conn.prepareStatement(transactionsql);

            double change = ammount-getBalance(player.id().toString(), false, false  );

            stmt.setString(1, "server");
            stmt.setString(2, player.id().toString());
            stmt.setDouble(3, change);
            stmt.executeUpdate();

            getBalance(player.id().toString(), true, false  );
        } catch (SQLException e) {
            Unifiedcurrency.logger.severe("Couldnt set balance: " + e);
        }


    }


    private static void recalcBalances() {
        String sql = "SELECT * FROM players";
        List<String> players = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(Database.url)) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String uuid = rs.getString("uuid");
                players.add(uuid);
            };
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        for (String player : players) {
            getBalance(player, true, false);
        }


    }

    public void sendTransactionMessage(ServerPlayerEntity player, Database.Transaction tx) {
        String typeColor = tx.isDebit(player.getUuidAsString()) ? "<red>" : "<green>";
        String typeSign = tx.isDebit(player.getUuidAsString()) ? "-" : "+";

        // for strikethru if its invalid
        String startSt = !tx.isValid() ? "<st>" : "";
        String endSt = !tx.isValid() ? "</st>" : "";

        String message = String.format(
                "<gray>╠ </gray>%s"
        );

    }

    private static double getBalance(String uuid, Boolean updateCache, Boolean useCache) {
        String balanceSql;
        if (!useCache) {
            balanceSql = """
                        SELECT
                            COALESCE(SUM(CASE WHEN author = ? THEN -change ELSE 0 END), 0) +
                            COALESCE(SUM(CASE WHEN recipient = ? THEN change ELSE 0 END), 0)
                        AS balance
                        FROM transactions
                        WHERE (author = ? OR recipient = ?) AND valid = 1;
                    """;
        } else {
            balanceSql = "SELECT currency FROM players WHERE uuid = ?";
        }

        try (Connection conn = DriverManager.getConnection(Database.url)) {
            PreparedStatement stmt = conn.prepareStatement(balanceSql);

            stmt.setString(1, uuid);
            if (!useCache) {
                stmt.setString(2, uuid);
                stmt.setString(3, uuid);
                stmt.setString(4, uuid);
            }
            ResultSet rs = stmt.executeQuery();
            double balance;
            if (!useCache){
                balance = rs.next() ? rs.getDouble("balance") : 0.0;
            } else {
                if (rs.next()) {
                    JsonObject obj = gson.fromJson(rs.getString("currency"), JsonObject.class);
                    System.out.println("Json Object: "+ obj);
                    balance = obj.get("main").getAsDouble();
                } else {
                    balance = 0.0;
                }

            }

            if (updateCache && !useCache) {
                String updatesql = "UPDATE players SET currency = ? WHERE uuid = ?";
                PreparedStatement stmt1 = conn.prepareStatement(updatesql);

                String newCurrencyJSON = "{\"main\":" +balance+"}";

                stmt1.setString(1, newCurrencyJSON);
                stmt1.setString(2, uuid);
                stmt1.executeUpdate();


            }
        return balance;
    } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}


