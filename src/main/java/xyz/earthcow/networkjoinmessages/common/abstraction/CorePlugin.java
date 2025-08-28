package xyz.earthcow.networkjoinmessages.common.abstraction;

import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;

import java.io.File;
import java.util.List;
import java.util.UUID;

public interface CorePlugin {
    NetworkJoinMessagesCore getCore();

    void disable();

    ServerType getServerType();
    File getDataFolder();
    CoreLogger getCoreLogger();

    List<CorePlayer> getAllPlayers();
    CoreBackendServer getServer(String serverName);

    void fireEvent(Object event);

    PremiumVanish getVanishAPI();

    void runTaskLater(Runnable task, int timeInSecondsLater);
    void runTaskAsync(Runnable task);

    boolean isPluginLoaded(String pluginName);

    CoreCommandSender getConsole();

    PlayerManager getPlayerManager();

    default CorePlayer getOrPutPlayer(CorePlayer player) {
        PlayerManager manager = getPlayerManager();
        CorePlayer managerPlayer = manager.getPlayer(player.getUniqueId());
        if (managerPlayer == null) {
            manager.addPlayer(player);
            return player;
        }
        return managerPlayer;
    }

    default CorePlayer getOrCreatePlayer(UUID uuid) {
        PlayerManager manager = getPlayerManager();
        CorePlayer player = manager.getPlayer(uuid);
        if (player == null) {
            player = createPlayer(uuid);
            manager.addPlayer(player);
        }
        return player;
    }

    CorePlayer createPlayer(UUID uuid);
}
