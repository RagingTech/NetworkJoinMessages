package xyz.earthcow.networkjoinmessages.common.util;

import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;

import java.sql.*;
import java.util.Enumeration;
import java.util.UUID;

public class H2PlayerJoinTracker implements AutoCloseable {
    private final String dbPath;
    private Connection connection;

    public H2PlayerJoinTracker(String dbPath) throws SQLException {
        registerDriverIfNeeded();

        this.dbPath = dbPath;
        setUpConnection();
    }

    private void setUpConnection() throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:h2:" + dbPath);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS players_joined (" +
                "player_uuid VARCHAR(36) PRIMARY KEY, " +
                "player_name VARCHAR(64) NOT NULL)");
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    public boolean isConnectionInvalid() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                setUpConnection();
            }
            return false;
        } catch (SQLException e) {
            NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().severe(
                "Cannot access joined database! Does the file exist?");
            return true;
        }
    }

    public boolean hasJoined(UUID playerUuid) {
        if (isConnectionInvalid()) {
            return false;
        }
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT 1 FROM players_joined WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().severe(
                "SQL Failure: Failed to determine if " + playerUuid + " has joined before");
            return false;
        }
    }

    public void markAsJoined(UUID playerUuid, String playerName) {
        if (isConnectionInvalid()) {
            return;
        }
        try (PreparedStatement ps = connection.prepareStatement(
            "MERGE INTO players_joined (player_uuid, player_name) KEY(player_uuid) VALUES (?, ?)")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().severe(
                "SQL Failure: Failed to mark player joined for player " + playerName);
        }
    }

    private static void registerDriverIfNeeded() {
        boolean alreadyRegistered = false;
        try {
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                if (drivers.nextElement().getClass().getName().equals("org.h2.Driver")) {
                    alreadyRegistered = true;
                    break;
                }
            }

            if (!alreadyRegistered) {
                Class<?> clazz = Class.forName("org.h2.Driver", true, H2PlayerJoinTracker.class.getClassLoader());
                Driver realDriver = (Driver) clazz.getDeclaredConstructor().newInstance();
                DriverManager.registerDriver(new DriverShim(realDriver));
            }
        } catch (Exception e) {
            NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().severe(
                "Failed to manually register H2 JDBC driver");
        }
    }
}
