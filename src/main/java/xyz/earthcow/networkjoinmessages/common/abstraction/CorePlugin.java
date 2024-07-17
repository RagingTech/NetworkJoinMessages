package xyz.earthcow.networkjoinmessages.common.abstraction;

import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;

import java.io.File;
import java.util.List;

public interface CorePlugin {
    NetworkJoinMessagesCore getCore();

    ServerType getServerType();
    File getDataFolder();
    CoreLogger getLogger();

    List<CorePlayer> getAllPlayers();
    CoreBackendServer getServer(String serverName);

    void fireEvent(Object event);

    boolean getVanishAPI();

    void runTaskLater(Runnable task, int timeInSecondsLater);
    void runTaskAsync(Runnable task);
}
