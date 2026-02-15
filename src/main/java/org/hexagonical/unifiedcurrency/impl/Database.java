package org.hexagonical.unifiedcurrency.impl;

import net.fabricmc.loader.api.FabricLoader;
import org.hexagonical.unifiedcurrency.Unifiedcurrency;

import java.nio.file.Path;
import java.sql.*;
import java.util.Objects;

public class Database {

    static Path DATABASE_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("database.db");

    public static class Transaction {
        private final int id;
        private final String author;
        private final String reciever;
        private final double amount;
        private final String timestamp;
        private final boolean valid;

        public Transaction(int id, String author, String reciever, double amount, String timestamp, boolean valid) {
            this.id = id;
            this.author = author;
            this.reciever = reciever;
            this.amount = amount;
            this.timestamp = timestamp;
            this.valid = valid;
        }

        public boolean isDebit(String uuid) {
            return this.author.equals(uuid);
        }

        public boolean isValid() {
            return this.valid;
        }

        public String getTimestamp() {
            return this.timestamp;
        }

        public String getAuthorUUID() {
            return this.author;
        }

        public String getAuthorName() {
            // Query database for username
            String sql = "SELECT username FROM players WHERE uuid = ?";

            try (Connection conn = DriverManager.getConnection(Database.url)) {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, this.author);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getString("username");
                } else {
                    // Fallback if player not found in database
                    return "Unknown (" + this.author + ")";
                }
            } catch (SQLException e) {
                Unifiedcurrency.logger.warning("Failed to get author name for UUID " + this.author + ": " + e.getMessage());
                return "Unknown (" + this.author + ")";
            }
        }

        public String getRecipientUUID() {
            return this.reciever;
        }

        public String getRecipientName() {

            if (Objects.equals(this.reciever, "server")) {
                return "Server";
            }

            // Query database for username
            String sql = "SELECT username FROM players WHERE uuid = ?";

            try (Connection conn = DriverManager.getConnection(Database.url)) {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, this.reciever);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getString("username");
                } else {
                    // Fallback if player not found in database
                    return "Unknown (" + this.reciever + ")";
                }
            } catch (SQLException e) {
                Unifiedcurrency.logger.warning("Failed to get recipient name for UUID " + this.reciever + ": " + e.getMessage());
                return "Unknown (" + this.reciever + ")";
            }
        }

        public double getAmount() {
            return this.amount;
        }

        public int getId() {
            return this.id;
        }

        public boolean isServer() {
            return author.equalsIgnoreCase("server") || reciever.equalsIgnoreCase("server");
        }

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

    public static int countTransactions(String uuid) throws SQLException {
        String sql = "SELECT COUNT(*) FROM transactions WHERE author = ? OR recipient = ?";

        try (Connection conn = DriverManager.getConnection(Database.url)) {
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setString(1, uuid);
            stmt.setString(2, uuid);

            ResultSet rs = stmt.executeQuery();
            return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }


        return 0;
    }
    

    public static final String url = "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath();
}