package com.blackjack.database;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    private BasicDataSource dataSource;

    private DatabaseManager() {
        initializeDataSource();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDataSource() {
        try {
            Properties props = new Properties();
            InputStream input = getClass().getClassLoader()
                    .getResourceAsStream("database.properties");

            if (input == null) {
                logger.error("Unable to find database.properties");
                return;
            }

            props.load(input);

            dataSource = new BasicDataSource();
            dataSource.setDriverClassName(props.getProperty("db.driver"));
            dataSource.setUrl(props.getProperty("db.url"));
            dataSource.setUsername(props.getProperty("db.username"));
            dataSource.setPassword(props.getProperty("db.password"));

            // Setari pool conexiuni
            dataSource.setInitialSize(Integer.parseInt(
                    props.getProperty("db.pool.initialSize", "5")));
            dataSource.setMaxTotal(Integer.parseInt(
                    props.getProperty("db.pool.maxActive", "20")));
            dataSource.setMaxIdle(Integer.parseInt(
                    props.getProperty("db.pool.maxIdle", "10")));
            dataSource.setMinIdle(Integer.parseInt(
                    props.getProperty("db.pool.minIdle", "5")));

            // Validare conexiune
            dataSource.setValidationQuery("SELECT 1");
            dataSource.setTestOnBorrow(true);
            dataSource.setTestWhileIdle(true);

            logger.info("Database connection pool initialized successfully");

        } catch (IOException e) {
            logger.error("Failed to load database properties", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized");
        }
        return dataSource.getConnection();
    }

    public void closeDataSource() {
        if (dataSource != null) {
            try {
                dataSource.close();
                logger.info("Database connection pool closed");
            } catch (SQLException e) {
                logger.error("Error closing data source", e);
            }
        }
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            logger.error("Database connection test failed", e);
            return false;
        }
    }
}
