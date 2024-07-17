package xyz.earthcow.networkjoinmessages.bungee.general;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Plugin;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeeLogger;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeePlayer;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeeServer;
import xyz.earthcow.networkjoinmessages.bungee.commands.FakeCommand;
import xyz.earthcow.networkjoinmessages.bungee.commands.ReloadCommand;
import xyz.earthcow.networkjoinmessages.bungee.commands.ToggleJoinCommand;
import xyz.earthcow.networkjoinmessages.bungee.listeners.PlayerListener;
import xyz.earthcow.networkjoinmessages.common.abstraction.*;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BungeeMain extends Plugin implements CorePlugin {

    private static BungeeMain instance;
    private NetworkJoinMessagesCore core;

    private BungeeLogger bungeeLogger;

    @Override
    public void onEnable() {
        this.bungeeLogger = new BungeeLogger(getLogger());
        this.core = new NetworkJoinMessagesCore(this);

        instance = this;

        getProxy()
            .getPluginManager()
            .registerListener(this, new PlayerListener());

        ProxyServer.getInstance()
            .getPluginManager()
            .registerCommand(this, new FakeCommand());
        ProxyServer.getInstance()
            .getPluginManager()
            .registerCommand(this, new ReloadCommand());
        ProxyServer.getInstance()
            .getPluginManager()
            .registerCommand(this, new ToggleJoinCommand());

    }

    @Override
    public void onDisable() {

    }

    public static BungeeMain getInstance() {
        return instance;
    }

    @Override
    public NetworkJoinMessagesCore getCore() {
        return core;
    }

    @Override
    public ServerType getServerType() {
        return ServerType.BUNGEE;
    }

    @Override
    public CoreLogger getCoreLogger() {
        return bungeeLogger;
    }

    @Override
    public List<CorePlayer> getAllPlayers() {
        return getProxy().getPlayers().stream().map(BungeePlayer::new).collect(Collectors.toList());
    }

    @Override
    public CoreBackendServer getServer(String serverName) {
        return new BungeeServer(getProxy().getServerInfo(serverName));
    }

    @Override
    public void fireEvent(Object event) {
        if (event instanceof Event) {
            getProxy().getPluginManager().callEvent((Event) event);
        }
    }

    @Override
    public boolean getVanishAPI() {
        return false;
    }

    @Override
    public void runTaskLater(Runnable task, int timeInSecondsLater) {
        getProxy().getScheduler().schedule(this, task, timeInSecondsLater, TimeUnit.SECONDS);
    }

    @Override
    public void runTaskAsync(Runnable task) {
        getProxy().getScheduler().runAsync(this, task);
    }
}
