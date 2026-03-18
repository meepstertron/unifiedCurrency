package org.hexagonical.unifiedcurrency.api;

import net.minecraft.server.network.ServerPlayerEntity;
import org.hexagonical.unifiedcurrency.Unifiedcurrency;
import org.hexagonical.unifiedcurrency.impl.Database;
import org.hexagonical.unifiedcurrency.impl.UCHelpers;

import java.sql.SQLException;
import java.util.List;


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
        return UCHelpers.getBalance(player.getUuidAsString(), false, true);
    }

    /**
     * Calculate a Player's balance from the Transaction log and ignore cache!
     *
     * @param player Whose balance should be calculated
     * @return Player's balance as a double
     */
    public static double getCalculatedBalance(ServerPlayerEntity player) {
        return UCHelpers.getBalance(player.getUuidAsString(), true, false);
    }


    /**
     * Add to a player's balance from the "server" account
     * For transactions appendPlayerTransaction() is recomended
     *
     * @param player The target Player recieving the currency
     * @param amount Money to add in a float
     * @exception SQLException When something doesnt go right with the database
     */
    public static void addBalance(ServerPlayerEntity player, double amount) throws SQLException {
        UCHelpers.addBalance(player.getPlayerConfigEntry(), amount);
    }

    /**
     * Set a Player's Balance
     *
     * @param player The target player's currency being overwritten
     * @param amount How much in a Float
     */
    public static void setBalance(ServerPlayerEntity player, double amount) throws SQLException {
        UCHelpers.setBalance(player.getPlayerConfigEntry(), amount);
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
    public static void appendPlayerTransaction(ServerPlayerEntity sender, ServerPlayerEntity receiver, double amount, boolean check_balance ) throws UCHelpers.InsufficientFundsException {

        if (check_balance) {
            if  (UCHelpers.getBalance(sender.getUuidAsString(), false, false) < amount) {
                throw new UCHelpers.InsufficientFundsException("Player does not have enough currency");
            }
        }

        UCHelpers.addPlayerTransaction(receiver.getUuidAsString(), sender.getUuidAsString(), amount);
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
        UCHelpers.getBalance(player.getUuidAsString(), true, false);
    }

    /**
     * DO NOT call this if you want just one player
     * it is very resource intensive and shouldn't really be used unless most of the playerbase has been modified
     * use recalculatePlayerBalance() or getCalculatedBalance() for a singular player.
     */
    public static void recalculateAllPlayers() {
        UCHelpers.recalcBalances();
    }

    public static boolean isPlayerInDatabase(ServerPlayerEntity player) {

        return UCHelpers.isUserInDB(player.getUuidAsString());
    }

    /**
     * Get a Player's latest Transaction history
     * @param player
     * @param limit Limit of how many entries to fetch
     * @return
     * @throws SQLException
     */
    public static List<Database.Transaction> getTransactions(ServerPlayerEntity player, int limit) throws SQLException {
        return UCHelpers.getTransactions(player.getUuidAsString(), limit);
    }
}
