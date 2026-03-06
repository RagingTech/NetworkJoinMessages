package xyz.earthcow.networkjoinmessages.common.storage;

import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.util.DriverShim;
import xyz.earthcow.networkjoinmessages.common.util.PlayerDataSnapshot;

import java.sql.*;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Persists per-player preferences and identity using an embedded H2 database.
 *
 * <p>The database file is created in the plugin's data folder at the path
 * supplied to the constructor. A single table {@code players} stores one row
 * per UUID; all preference columns are nullable so that {@code null} can be
 * distinguished from an explicit {@code false}.
 *
 * <p>All public methods are {@code synchronized} for thread safety, consistent
 * with the project's other H2-backed stores.
 */
public class H2PlayerDataStore implements PlayerDataStore {

    private static final String CREATE_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS players (" +
            "player_uuid  VARCHAR(36) PRIMARY KEY, " +
            "player_name  VARCHAR(16) NOT NULL, "    +
            "silent_state BOOLEAN NULL, "            +
            "ignore_join  BOOLEAN NULL, "            +
            "ignore_swap  BOOLEAN NULL, "            +
            "ignore_leave BOOLEAN NULL"              +
        ")";

    private static final String SELECT_SQL =
        "SELECT * FROM players WHERE player_uuid = ?";

    private static final String RESOLVE_SQL =
        "SELECT player_uuid FROM players WHERE player_name = ?";

    // MERGE upserts the identity columns on first insert, then UPDATE sets all
    // preference columns so an existing row is fully overwritten on save.
    private static final String UPSERT_SQL =
        "MERGE INTO players (player_uuid, player_name, silent_state, ignore_join, ignore_swap, ignore_leave) " +
            "KEY(player_uuid) VALUES (?, ?, ?, ?, ?, ?)";

    private final CoreLogger logger;
    private final String dbPath;
    private Connection connection;

    public H2PlayerDataStore(CoreLogger logger, String dbPath) throws SQLException {
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
     * @return {@code true} if the connection is unusable, {@code false} if it is valid
     */
    public synchronized boolean isConnectionInvalid() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                setUpConnection();
            }
            return false;
        } catch (SQLException e) {
            logger.severe("Cannot access player data store at '" + dbPath + "'. Does the file exist?");
            return true;
        }
    }

    @Override
    @Nullable
    public synchronized PlayerDataSnapshot getData(UUID playerUuid) {
        if (isConnectionInvalid()) return null;
        try (PreparedStatement ps = connection.prepareStatement(SELECT_SQL)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerDataSnapshot(
                        rs.getString("player_name"),
                        rs.getObject("silent_state", Boolean.class),
                        rs.getObject("ignore_join",  Boolean.class),
                        rs.getObject("ignore_swap",  Boolean.class),
                        rs.getObject("ignore_leave", Boolean.class)
                    );
                }
            }
        } catch (SQLException e) {
            logger.severe("SQL failure: Could not query player data for UUID " + playerUuid + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized void saveData(UUID playerUuid, PlayerDataSnapshot data) {
        if (isConnectionInvalid()) return;
        try (PreparedStatement ps = connection.prepareStatement(UPSERT_SQL)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, data.playerName());
            ps.setObject(3, data.silentState(), Types.BOOLEAN);
            ps.setObject(4, data.ignoreJoin(),  Types.BOOLEAN);
            ps.setObject(5, data.ignoreSwap(),  Types.BOOLEAN);
            ps.setObject(6, data.ignoreLeave(), Types.BOOLEAN);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("SQL failure: Could not save player data for '" + data.playerName() + "' (" + playerUuid + "): " + e.getMessage());
        }
    }

    @Override
    @Nullable
    public synchronized UUID resolveUuid(String playerName) {
        if (isConnectionInvalid()) return null;
        try (PreparedStatement ps = connection.prepareStatement(RESOLVE_SQL)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("player_uuid"));
                }
            }
        } catch (SQLException e) {
            logger.severe("SQL failure: Could not resolve UUID for player name '" + playerName + "': " + e.getMessage());
        }
        return null;
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
            Class<?> clazz = Class.forName("org.h2.Driver", true, H2PlayerDataStore.class.getClassLoader());
            Driver realDriver = (Driver) clazz.getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(new DriverShim(realDriver));
        } catch (Exception e) {
            logger.severe("Failed to manually register the H2 JDBC driver. Player data persistence will be unavailable.");
        }
    }
}
