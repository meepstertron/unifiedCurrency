package org.hexagonical.unifiedcurrency.impl;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.sql.*;

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
    

    public static final String url = "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath();
}