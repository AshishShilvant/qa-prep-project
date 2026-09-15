package com.qaprep.framework.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens JDBC connections to the test database using connection details from
 * environment variables, falling back to sensible localhost defaults for local
 * runs. In CI, these are supplied by the MySQL service container defined in
 * ci.yml, so the same test code runs unchanged locally and on GitHub Actions.
 */
public final class DbConnectionManager {

    private static final String HOST = envOrDefault("DB_HOST", "127.0.0.1");
    private static final String PORT = envOrDefault("DB_PORT", "3306");
    private static final String DATABASE = envOrDefault("DB_NAME", "qaprep");
    private static final String USER = envOrDefault("DB_USER", "root");
    private static final String PASSWORD = envOrDefault("DB_PASSWORD", "root");

    private DbConnectionManager() {
    }

    /**
     * Opens a new connection to the test database. Callers own the connection's
     * lifecycle and are responsible for closing it (try-with-resources or an
     * explicit close in @AfterClass).
     */
    public static Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return DriverManager.getConnection(url, USER, PASSWORD);
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
