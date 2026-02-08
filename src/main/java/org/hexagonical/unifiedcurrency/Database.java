package org.hexagonical.unifiedcurrency;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class Database {

    static Path DATABASE_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("database.db");

    

    public static final String url = "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath();
}