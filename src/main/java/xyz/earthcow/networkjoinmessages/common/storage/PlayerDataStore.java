package xyz.earthcow.networkjoinmessages.common.storage;

import xyz.earthcow.networkjoinmessages.common.util.PlayerDataSnapshot;

import java.util.UUID;

public interface PlayerDataStore extends AutoCloseable {

    PlayerDataSnapshot getPreferences(UUID uuid);
    void savePreferences(UUID uuid, PlayerDataSnapshot preferences);

}
