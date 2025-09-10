package xyz.earthcow.networkjoinmessages.bungee;

import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.*;
import xyz.earthcow.networkjoinmessages.bungee.commands.FakeCommand;
import xyz.earthcow.networkjoinmessages.bungee.commands.ImportCommand;
import xyz.earthcow.networkjoinmessages.bungee.commands.ReloadCommand;
import xyz.earthcow.networkjoinmessages.bungee.commands.ToggleJoinCommand;
import xyz.earthcow.networkjoinmessages.bungee.listeners.PlayerListener;
import xyz.earthcow.networkjoinmessages.common.abstraction.*;
import xyz.earthcow.networkjoinmessages.common.Core;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BungeeMain extends Plugin implements CorePlugin {

    private static BungeeMain instance;
    private PlayerManager manager = new PlayerManager();

    private Core core;
    private CoreCommandSender console;

    private BungeeLogger bungeeLogger;
    private BungeeAudiences audiences;

    private PremiumVanish premiumVanish;

    @Override
    public void onEnable() {
        // Anonymous usage data via bStats (https://bstats.org/plugin/bungeecord/NetworkJoinMessages/26527)
        final int PLUGIN_ID = 26527;
        Metrics metrics = new Metrics(this, PLUGIN_ID);

        this.bungeeLogger = new BungeeLogger(getLogger());
        this.audiences = BungeeAudiences.create(this);
        this.core = new Core(this);
        this.console = new BungeeCommandSender(getProxy().getConsole());

        instance = this;

        getProxy()
            .getPluginManager()
            .registerListener(this, new PlayerListener());

        getProxy()
            .getPluginManager()
            .registerCommand(this, new ImportCommand());
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

    @Override
    public void disable() {
        getProxy().getPluginManager().unregisterCommands(this);
        getProxy().getPluginManager().unregisterListeners(this);
        getProxy().getScheduler().cancel(this);
    }

    public static BungeeMain getInstance() {
        return instance;
    }

    @Override
    public PlayerManager getPlayerManager(){
        return manager;
    }

    @Override
    public CorePlayer createPlayer(UUID uuid) {
        ProxiedPlayer proxiedPlayer = getProxy().getPlayer(uuid);
        return new BungeePlayer(proxiedPlayer);
    }

    @Override
    public CoreCommandSender getConsole() {
        return console;
    }

    public BungeeAudiences getAudiences() {
        return audiences;
    }

    @Override
    public Core getCore() {
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
        return getProxy().getPlayers().stream().map(proxiedPlayer -> getOrCreatePlayer(proxiedPlayer.getUniqueId())).collect(Collectors.toList());
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

    @Override
    public boolean isPluginLoaded(String pluginName) {
        return getProxy().getPluginManager().getPlugin(pluginName) != null;
    }
}
