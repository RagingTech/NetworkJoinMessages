package xyz.earthcow.networkjoinmessages.bungee.general;

import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Plugin;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeeLogger;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeePlayer;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeePremiumVanish;
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
    private BungeeAudiences audiences;

    private PremiumVanish premiumVanish;

    @Override
    public void onEnable() {
        this.bungeeLogger = new BungeeLogger(getLogger());
        this.audiences = BungeeAudiences.create(this);
        this.core = new NetworkJoinMessagesCore(this);

        instance = this;

        getProxy()
            .getPluginManager()
            .registerListener(this, new PlayerListener());

        getProxy()
            .getPluginManager()
            .registerCommand(this, new FakeCommand());
        getProxy()
            .getPluginManager()
            .registerCommand(this, new ReloadCommand());
        getProxy()
            .getPluginManager()
            .registerCommand(this, new ToggleJoinCommand());

        if (getProxy().getPluginManager().getPlugin("PremiumVanish") != null) {
            this.premiumVanish = new BungeePremiumVanish();
            bungeeLogger.info("Successfully hooked into PremiumVanish!");
        }

    }

    @Override
    public void onDisable() {

    }

    public static BungeeMain getInstance() {
        return instance;
    }

    public BungeeAudiences getAudiences() {
        return audiences;
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
        ServerInfo serverInfo = getProxy().getServerInfo(serverName);
        if (serverInfo == null) return null;
        return new BungeeServer(serverInfo);
    }

    @Override
    public void fireEvent(Object event) {
        if (event instanceof Event) {
            getProxy().getPluginManager().callEvent((Event) event);
        }
    }

    @Override
    public PremiumVanish getVanishAPI() {
        return premiumVanish;
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
