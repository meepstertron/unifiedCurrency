package org.hexagonical.unifiedcurrency;

public class Database {

    Path DATABASE_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("database.db");

    

    public static final String url = "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath();
}