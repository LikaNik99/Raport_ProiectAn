package md.uzina.server;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DBManager {
    private static HikariDataSource ds;

    public static void init() throws SQLException {
        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("DB_PORT", "5432");
        String db = System.getenv().getOrDefault("DB_NAME", "logare_db");
        String user = System.getenv().getOrDefault("DB_USER", "postgres");
        String pass = System.getenv().getOrDefault("DB_PASS", "postgres");

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        
        System.out.println("===========================================");
        System.out.println("DB Connection Config:");
        System.out.println("  Host: " + host);
        System.out.println("  Port: " + port);
        System.out.println("  Database: " + db);
        System.out.println("  User: " + user);
        System.out.println("  JDBC URL: " + jdbcUrl);
        System.out.println("===========================================");

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(10);
        ds = new HikariDataSource(cfg);

        // test
        try (Connection c = ds.getConnection()) {
            // ok
        }
    }

    public static Connection getConnection() throws SQLException {
        if (ds == null) {
            throw new SQLException("DataSource not initialized. Call DBManager.init() before using connections.");
        }
        return ds.getConnection();
    }

    public static void close() {
        if (ds != null) ds.close();
    }
}
