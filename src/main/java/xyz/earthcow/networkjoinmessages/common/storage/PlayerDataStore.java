package xyz.earthcow.networkjoinmessages.common.storage;

import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.util.PlayerDataSnapshot;

import java.util.UUID;

public interface PlayerDataStore extends AutoCloseable {

    /**
     * Returns the stored snapshot for the given UUID, or {@code null} if no
     * record exists yet or the store is unreachable.
     */
    @Nullable
    PlayerDataSnapshot getData(UUID playerUuid);

    /**
     * Persists the given snapshot for the given UUID. Safe to call multiple
     * times — implementations must upsert, not insert-only.
     */
    void saveData(UUID playerUuid, PlayerDataSnapshot data);

    /**
     * Looks up a UUID by the player's last known username (case-insensitive).
     * Returns {@code null} if no match is found or the store is unreachable.
     *
     * <p>Used to resolve offline players in {@code /njointoggle <player>}.
     */
    @Nullable
    UUID resolveUuid(String playerName);

    /** No-op default so callers don't need to handle checked exceptions for backends that don't need closing. */
    @Override
    default void close() throws Exception {}

}
