package org.criticizer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private static final Properties properties = new Properties();
    private static final String CONFIG_FILE = "application.properties";

    static {
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                log.error("FATAL: Configuration file '{}' not found in classpath.", CONFIG_FILE);
                throw new RuntimeException("Configuration file '" + CONFIG_FILE + "' not found.");
            }
            properties.load(is);
            log.info("Configuration from '{}' loaded successfully.", CONFIG_FILE);
        } catch (IOException e) {
            log.error("FATAL: Could not load configuration file '{}'.", CONFIG_FILE, e);
            throw new RuntimeException("Could not load configuration file.", e);
        }
    }

    public static String getDbUrl() {
        return properties.getProperty("db.url");
    }

    public static String getDbUser() {
        return properties.getProperty("db.user");
    }

    public static String getDbPassword() {
        return properties.getProperty("db.password");
    }
}