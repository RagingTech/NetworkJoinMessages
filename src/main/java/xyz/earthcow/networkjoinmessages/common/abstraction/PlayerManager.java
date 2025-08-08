package xyz.earthcow.networkjoinmessages.common.abstraction;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {
    private final Map<UUID, CorePlayer> players = new ConcurrentHashMap<>();

    @Nullable
    public CorePlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    @Nullable
    public CorePlayer getPlayer(String name) {
        return players.values().stream()
            .filter(player -> player.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    public void addPlayer(CorePlayer player) {
        players.put(player.getUniqueId(), player);
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }
}
