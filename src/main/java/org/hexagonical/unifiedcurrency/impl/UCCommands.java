package org.hexagonical.unifiedcurrency.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.hexagonical.unifiedcurrency.Unifiedcurrency;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.hexagonical.unifiedcurrency.impl.UCHelpers.*;


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

        context.getSource().sendFeedback(() -> Text.literal("Your Balance is: " + balance + Config.get("currency_symbol", "$")), false);
        return 1;
    }

    // /uc balance get <player> command
    public static int getOtherBalanceCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerConfigEntry player = GameProfileArgumentType.getProfileArgument(context, "player").iterator().next();
        String playerName = player.name();


        double balance = getBalance(player.id().toString(), false, true);


        context.getSource().sendFeedback(() -> Text.literal(playerName + "'s Balance is: " + balance + Config.get("currency_symbol", "$")), false);
        return 1;
    }

    public static int addBalanceCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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

        context.getSource().sendFeedback(() -> Text.literal("Added " + amount + Config.get("currency_symbol","$") + " to " + playerName + "'s balance"), false);
        return 1;
    }

    public static int setBalanceCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerConfigEntry player = GameProfileArgumentType.getProfileArgument(context, "player").iterator().next();
        String playerName = player.name();
        float amount = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(context, "amount");
        try {
            setBalance(player, (double) amount);
        } catch (SQLException e) {
            return 0;
        }


        context.getSource().sendFeedback(() -> Text.literal("Set " + playerName + "'s balance to " + amount + Config.get("currency_symbol", "$")), false);
        return 1;

    }



    public static int reloadCommand(CommandContext<ServerCommandSource> context) {
        Config.load();
        context.getSource().sendFeedback(() -> Text.literal("Reloaded!").formatted(Formatting.GREEN), false);
        return 1;
    }

    public static int recalculateBalancesCommand(CommandContext<ServerCommandSource> context) {

        CompletableFuture.runAsync(UCHelpers::recalcBalances);

        return 1;
    }

    public static int getTransactionsCommand(CommandContext<ServerCommandSource> context) {

        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("This command can only be run by a player."));
            return 0;
        }
        List<Database.Transaction> transactions;
        int maxTransactions;
        try {
             transactions = getTransactions(player.getUuidAsString(), 10);

            maxTransactions = Database.countTransactions(context.getSource().getPlayer().getUuidAsString());

        } catch (SQLException e) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String preMessage = String.format("<gray>╔ Transaction Log for <white><u>%s</u></white> as of <blue>{%s}</blue>",
                context.getSource().getName(), now.format(formatter));

        String afterMessage = String.format(
                "<gray>╚ Showing %s/%s Transactions, 10 Per page</gray>",
                transactions.toArray().length, maxTransactions

        );

        Audience audience = Unifiedcurrency.adventure.audience(player);

        audience.sendMessage(MiniMessage.miniMessage().deserialize(preMessage));


        for (Database.Transaction tx : transactions) {
            sendTransactionMessage(player, tx);
        }

        audience.sendMessage(MiniMessage.miniMessage().deserialize(afterMessage));

        return 1;
    }

    public static int payPlayerCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity author = context.getSource().getPlayer();
        if (author == null) {
            context.getSource().sendError(Text.literal("This command can only be run by a player."));
            return 0;
        }

        if (Config.get("allow_player_transactions", true)) {

        }

        PlayerConfigEntry player = GameProfileArgumentType.getProfileArgument(context, "player").iterator().next();
        String playerName = player.name();
        double amount = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(context, "amount");

        if (amount <= 0) {
            context.getSource().sendError(Text.literal("Invalid number"));
            return 1;
        }

        ServerCommandSource source = context.getSource();

        String recipientUuid = player.id().toString();
        String authorUuid = author.getUuidAsString();

        CompletableFuture.runAsync(() -> {
            try {
                double authorBalance = getBalance(authorUuid, true, true);
                if (authorBalance >= amount) {
                    addPlayerTransaction(recipientUuid, authorUuid, amount);
                    source.getServer().execute(() ->
                            source.sendFeedback(() -> Text.literal("Sent " + amount + Config.get("currency_symbol", "$") +" to " + playerName), false)
                    );
                } else {
                    source.getServer().execute(() ->
                            source.sendError(Text.literal("You do not have enough money!"))
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                source.getServer().execute(() ->
                        source.sendError(Text.literal("Payment failed: " + e.getMessage()))
                );
            }
        });

        return 1;
    }



    public static void sendTransactionMessage(ServerPlayerEntity player, Database.Transaction tx) {

        String typeSign = tx.isDebit(player.getUuidAsString()) ? "<red>-</red>" : "<green>+</green>";

        // for strikethru if its invalid
        String startSt = !tx.isValid() ? "<st>" : "";
        String endSt = !tx.isValid() ? "</st>" : "";

        String commandWord = tx.isDebit(player.getUuidAsString()) ? "Sent" : "Recieved";
        String targetWord = tx.isDebit(player.getUuidAsString())  ? "to" : "from";

        String username = tx.isServer() ? "server" : tx.isDebit(player.getUuidAsString()) ? tx.getRecipientName() : tx.getAuthorName();

        String usernameWord = username.equals("server") ? "<gold>Server</gold>" : "<u>"+username+"</u>";

        String cashWord = "<dark_green>"+tx.getAmount()+Config.get("currency_symbol", Config.get("currency_symbol", "$"))+"</dark_green>";

        String invalidFlag = tx.isValid() ? "" : "<red>[INVALID]</red>";

        String message = String.format(
                "<gray>╠ %s[</gray>%s<gray>] {%s}</gray> %s %s %s %s %s %s",
                startSt, typeSign, tx.getTimestamp(), commandWord,cashWord, targetWord, usernameWord,endSt, invalidFlag
        );

        Audience audience = Unifiedcurrency.adventure.audience(player);
        audience.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }


}


