package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton pattern implementation for managing MySQL JDBC connections.
 * Reads optional environment variables (SMARTSTAY_DB_URL, SMARTSTAY_DB_USER,
 * SMARTSTAY_DB_PASSWORD) and falls back to standard local Ubuntu MySQL defaults.
 */
public final class DatabaseConnection {

    private static volatile DatabaseConnection instance;

    private final String url;
    private final String username;
    private final String password;

    private DatabaseConnection() {
        this.url = System.getenv().getOrDefault(
                "SMARTSTAY_DB_URL",
                "jdbc:mysql://localhost:3306/smartstay_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Colombo"
        );
        this.username = System.getenv().getOrDefault("SMARTSTAY_DB_USER", "root");
        this.password = System.getenv().getOrDefault("SMARTSTAY_DB_PASSWORD", "root");
    }

    /**
     * Thread-safe double-checked locking Singleton accessor.
     *
     * @return the single DatabaseConnection configuration instance.
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Opens and returns a fresh JDBC {@link Connection} to the SmartStay MySQL database.
     *
     * @return active JDBC Connection
     * @throws SQLException if the database is unreachable or credentials fail
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Utility check to test whether the configured MySQL database is reachable.
     *
     * @return true if a connection can be established, false otherwise
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException ex) {
            return false;
        }
    }
}
