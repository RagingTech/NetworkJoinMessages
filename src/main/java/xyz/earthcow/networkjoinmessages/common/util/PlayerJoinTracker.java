package xyz.earthcow.networkjoinmessages.common.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Abstraction over first-join storage backends.
 * Implementations must be thread-safe.
 */
public interface PlayerJoinTracker extends AutoCloseable {

    /**
     * Returns {@code true} if the player has previously been recorded as having joined.
     */
    boolean hasJoined(UUID playerUuid);

    /**
     * Records that a player has joined. Safe to call multiple times for the same UUID.
     */
    void markAsJoined(UUID playerUuid, String playerName);

    /**
     * Imports all entries from a vanilla {@code usercache.json} file into the join-tracker database.
     *
     * @param userCacheStr path to the usercache.json file
     * @return true if the import succeeded, false otherwise
     */
    default boolean addUsersFromUserCache(String userCacheStr) {
        Path userCachePath = Paths.get(userCacheStr);
        if (!Files.exists(userCachePath)) {
            return false;
        }
        try {
            String json = Files.readString(userCachePath);
            JsonArray entries = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement entry : entries) {
                JsonObject obj = entry.getAsJsonObject();
                String username = obj.get("name").getAsString();
                UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
                markAsJoined(uuid, username);
            }
            return true;
        } catch (IOException | IllegalStateException | IllegalArgumentException e) {
            return false;
        }
    }

    /** No-op default so callers don't need to handle checked exceptions for backends that don't need closing. */
    @Override
    default void close() throws Exception {}
}
