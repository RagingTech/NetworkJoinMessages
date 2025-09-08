package xyz.earthcow.networkjoinmessages.common.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xyz.earthcow.networkjoinmessages.common.general.Core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        this.connection = DriverManager.getConnection("jdbc:h2:file:" + dbPath);

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

    public synchronized boolean isConnectionInvalid() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                setUpConnection();
            }
            return false;
        } catch (SQLException e) {
            Core.getInstance().getPlugin().getCoreLogger().severe(
                "Cannot access joined database! Does the file exist?");
            return true;
        }
    }

    public synchronized boolean hasJoined(UUID playerUuid) {
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
            Core.getInstance().getPlugin().getCoreLogger().severe(
                "SQL Failure: Failed to determine if " + playerUuid + " has joined before");
            return false;
        }
    }

    public synchronized void markAsJoined(UUID playerUuid, String playerName) {
        if (isConnectionInvalid()) {
            return;
        }
        try (PreparedStatement ps = connection.prepareStatement(
            "MERGE INTO players_joined (player_uuid, player_name) KEY(player_uuid) VALUES (?, ?)")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            Core.getInstance().getPlugin().getCoreLogger().severe(
                "SQL Failure: Failed to mark player joined for player " + playerName);
        }
    }

    public boolean addUsersFromUserCache(String userCacheStr) {
        Path userCachePath = Paths.get(userCacheStr);
        if (!Files.exists(userCachePath)) {
            return false;
        }

        try {
            String json = new String(Files.readAllBytes(userCachePath));
            JsonArray entries = JsonParser.parseString(json).getAsJsonArray();

            for (JsonElement entry : entries) {
                JsonObject obj = entry.getAsJsonObject();
                String username = obj.get("name").getAsString();
                UUID uuid = UUID.fromString(obj.get("uuid").getAsString());

                markAsJoined(uuid, username);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
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
            Core.getInstance().getPlugin().getCoreLogger().severe(
                "Failed to manually register H2 JDBC driver");
        }
    }
}
