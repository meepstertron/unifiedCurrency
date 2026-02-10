package org.hexagonical.unifiedcurrency;

import net.fabricmc.loader.api.FabricLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

public class Config {

    private static final Logger logger = Logger.getLogger("unifiedcurrency");
    private static Map<String, Object> data;

    public static void load() {

        Path configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(Unifiedcurrency.MOD_ID + ".yml");


        try (InputStream in = Files.newInputStream(configPath)) {
            Yaml yaml = new Yaml();
            data = yaml.load(in);
            logger.info("Config loaded successfully from: " + configPath);
        } catch (IOException e) {
            logger.severe("Failed to load config file: " + e.getMessage());
            throw new RuntimeException("Could not load config file", e);
        }
    }

    public static Object get(String key) {
        String[] parts = key.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (!(current instanceof Map)) return null;
            current = ((Map<String, Object>) current).get(part);
            if (current == null) return null;
        }

        return current;
    }

    public static Map<String, Object> getData() {
        return data;
    }
}
