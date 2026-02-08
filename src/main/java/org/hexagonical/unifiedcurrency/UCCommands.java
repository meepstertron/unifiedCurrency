package org.hexagonical.unifiedcurrency;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class UCCommands {

    // Root command
    public static int rootCommand(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> Text.literal("Running Unified Currency"), false);
        return 1;
    }

    // /uc balance command
    public static int getBalanceCommand(CommandContext<ServerCommandSource> context) {
        float balance = 0.0f; // Placeholder
        //TODO: actually get the balance of the player

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


        } catch (SQLException e) {
            Unifiedcurrency.logger.severe("Couldnt add balance: " + e);
        }
    }

    private static void setBalance(PlayerConfigEntry player, Double ammount) throws SQLException {
        String transactionsql = "INSERT INTO transactions (author, recipient, change) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(Database.url)) {
            PreparedStatement stmt = conn.prepareStatement(transactionsql);

            stmt.setString(1, "server");
            stmt.setString(2, player.id().toString());
            stmt.setDouble(3, ammount);
            stmt.executeUpdate();


        } catch (SQLException e) {
            Unifiedcurrency.logger.severe("Couldnt set balance: " + e);
        }
    }

    private static double calculateBalance(String uuid) {
        String balanceSql = """
                    SELECT
                        COALESCE(SUM(CASE WHEN author = ? THEN -change ELSE 0 END), 0) +
                        COALESCE(SUM(CASE WHEN recipient = ? THEN change ELSE 0 END), 0) 
                    AS balance
                    FROM transactions
                    WHERE author = ? OR recipient = ?;
                """;


        try (Connection conn = DriverManager.getConnection(Database.url)) {
            PreparedStatement stmt = conn.prepareStatement(balanceSql);

        return 0f;
    } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}


