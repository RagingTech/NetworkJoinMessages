package xyz.earthcow.networkjoinmessages.common.storage;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.util.DriverShim;

import java.sql.*;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Tracks which players have ever joined the network using an embedded H2 database.
 */
public class H2PlayerJoinTracker implements PlayerJoinTracker {

    private static final String CREATE_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS players_joined (" +
        "  player_uuid VARCHAR(36) PRIMARY KEY, " +
        "  player_name VARCHAR(64) NOT NULL" +
        ")";

    private static final String SELECT_SQL =
        "SELECT 1 FROM players_joined WHERE player_uuid = ?";

    private static final String UPSERT_SQL =
        "MERGE INTO players_joined (player_uuid, player_name) KEY(player_uuid) VALUES (?, ?)";

    private final CoreLogger logger;
    private final String dbPath;
    private Connection connection;

    public H2PlayerJoinTracker(CoreLogger logger, String dbPath) throws SQLException {
        this.logger = logger;
        this.dbPath = dbPath;
        registerDriverIfNeeded();
        setUpConnection();
    }

    private void setUpConnection() throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:h2:file:" + dbPath);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Checks whether the connection is valid, attempting to reconnect if needed.
     *
     * @return true if the connection is unusable, false if it is valid
     */
    public synchronized boolean isConnectionInvalid() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                setUpConnection();
            }
            return false;
        } catch (SQLException e) {
            logger.severe("Cannot access joined database at '" + dbPath + "'. Does the file exist?");
            return true;
        }
    }

    /**
     * Returns true if the given player UUID has previously been recorded as joined.
     */
    public synchronized boolean hasJoined(UUID playerUuid) {
        if (isConnectionInvalid()) return false;
        try (PreparedStatement ps = connection.prepareStatement(SELECT_SQL)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.severe("SQL failure: Could not query join status for UUID " + playerUuid);
            return false;
        }
    }

    /**
     * Records that a player has joined. Safe to call multiple times for the same UUID (upsert).
     */
    public synchronized void markAsJoined(UUID playerUuid, String playerName) {
        if (isConnectionInvalid()) return;
        try (PreparedStatement ps = connection.prepareStatement(UPSERT_SQL)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("SQL failure: Could not mark player '" + playerName + "' (" + playerUuid + ") as joined");
        }
    }

    /**
     * Registers the H2 JDBC driver via a {@link DriverShim} if it has not already been registered.
     * This is necessary to avoid issues when the H2 driver class is loaded by a non-system classloader.
     */
    private void registerDriverIfNeeded() {
        try {
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                if (drivers.nextElement().getClass().getName().equals("org.h2.Driver")) {
                    return; // already registered
                }
            }
            Class<?> clazz = Class.forName("org.h2.Driver", true, H2PlayerJoinTracker.class.getClassLoader());
            Driver realDriver = (Driver) clazz.getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(new DriverShim(realDriver));
        } catch (Exception e) {
            logger.severe("Failed to manually register the H2 JDBC driver. First-join tracking will be unavailable.");
        }
    }
}
