package org.hexagonical.unifiedcurrency;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.sql.*;

public class Database {

    static Path DATABASE_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("database.db");


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