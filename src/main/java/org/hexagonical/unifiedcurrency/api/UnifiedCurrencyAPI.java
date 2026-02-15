package org.hexagonical.unifiedcurrency.api;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Public API for accessing Unified Currency.
 * Other Mods/Integrations should ONLY use this class!
 */
public class UnifiedCurrencyAPI {

    /**
     * Returns the cached balance of a player
     *
     * @param player The player whose balance should be fetched
     * @return The player's Balance as a double
     */
    public static double getBalance(ServerPlayerEntity player) {

        return 0.0;
    }

    /**
     * Add to a player's balance from the "server" account
     * For transactions appendPlayerTransaction() is recomended
     *
     * @param player The target Player recieving the currency
     * @param amount Money to add in a float
     */
    public static void addBalance(ServerPlayerEntity player, double amount) {

    }

    /**
     * Set a Player's Balance
     *
     * @param player The target player's currency being overwritten
     * @param amount How much in a Float
     */
    public static void setBalance(ServerPlayerEntity player, double amount) {

    }

    /**
     * Append a Player->Player transaction.
     * use this if you want to transfer inbetween players
     * Make sure to recalculate balances of said player
     *
     * @param sender Origin Player
     * @param receiver Player Receiving
     * @param amount How much
     * @param check_balance If to check the balance before appending
     */
    public static void appendPlayerTransaction(ServerPlayerEntity sender, ServerPlayerEntity receiver, double amount, boolean check_balance ) {

    }

    /**
     * Append a Server->Player transaction
     * only used in unique cases use setBalance or addBalance instead.
     *
     * @param receiver Player receiving the currency
     * @param amount How much
     */
    public static void appendServerTransaction(ServerPlayerEntity receiver, double amount ) {

    }

    /**
     * Recalculate one player's balance from their transaction history and write it to their cache
     *
     * @param player Player to be recalculated
     */
    public static void recalculatePlayerBalance(ServerPlayerEntity player) {

    }

    /**
     * DO NOT call this if you want just one player
     * it is very resource intensive and shouldnt really be used unless most of the playerbase has been modified
     */
    public static void recalculateAllPlayers() {

    }
}
