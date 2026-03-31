package org.hexagonical.unifiedcurrency.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.server.PlayerConfigEntry;
import org.hexagonical.unifiedcurrency.Unifiedcurrency;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class UCHelpers {
    static Gson gson = new Gson();
    public static void addBalance(PlayerConfigEntry player, Double ammount) throws SQLException {
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

    public static void setBalance(PlayerConfigEntry player, Double ammount) throws SQLException {
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

    public static double getBalance(String uuid, Boolean updateCache, Boolean useCache) {
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
    public static List<Database.Transaction> getTransactions(String uuid, int limit) throws SQLException {
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

    public static boolean isUserInDB(String uuid) {
        String sql = "SELECT * FROM players WHERE uuid = ?";


        try (Connection conn = DriverManager.getConnection(Database.url)) {
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setString(1, uuid);

            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void recalcBalances() {
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

    public static void addPlayerTransaction(String recipient, String author, Double amount) {
        String sql = "INSERT INTO transactions (author, recipient, change) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(Database.url)){
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setString(1, author);
            stmt.setString(2, recipient);
            stmt.setDouble(3, amount);

            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }



    }

    public static class InsufficientFundsException extends Exception {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }



}
