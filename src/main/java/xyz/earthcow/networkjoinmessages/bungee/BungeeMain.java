package xyz.earthcow.networkjoinmessages.bungee;

import lombok.Getter;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.CustomChart;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.*;
import xyz.earthcow.networkjoinmessages.bungee.commands.ImportCommand;
import xyz.earthcow.networkjoinmessages.bungee.commands.ReloadCommand;
import xyz.earthcow.networkjoinmessages.bungee.commands.SpoofCommand;
import xyz.earthcow.networkjoinmessages.bungee.commands.ToggleCommand;
import xyz.earthcow.networkjoinmessages.bungee.events.BungeeNetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.bungee.events.BungeeNetworkLeaveEvent;
import xyz.earthcow.networkjoinmessages.bungee.events.BungeeSwapServerEvent;
import xyz.earthcow.networkjoinmessages.bungee.listeners.BungeeDiscordListener;
import xyz.earthcow.networkjoinmessages.bungee.listeners.PlayerListener;
import xyz.earthcow.networkjoinmessages.common.Core;
import xyz.earthcow.networkjoinmessages.common.abstraction.*;
import xyz.earthcow.networkjoinmessages.common.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.common.events.NetworkLeaveEvent;
import xyz.earthcow.networkjoinmessages.common.events.SwapServerEvent;
import xyz.earthcow.networkjoinmessages.common.modules.DiscordIntegration;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BungeeMain extends Plugin implements CorePlugin {

    @Getter
    private static BungeeMain instance;
    private final PlayerManager manager = new PlayerManager();

    private Core core;
    private CoreCommandSender console;

    private BungeeLogger bungeeLogger;
    @Getter
    private BungeeAudiences audiences;

    private PremiumVanish premiumVanish;

    private BungeeDiscordListener bungeeDiscordListener = null;

    @Override
    public void onEnable() {
        // Anonymous usage data via bStats (https://bstats.org/plugin/bungeecord/NetworkJoinMessages/26527)
        final int PLUGIN_ID = 26527;
        Metrics metrics = new Metrics(this, PLUGIN_ID);

        this.audiences = BungeeAudiences.create(this);
        this.bungeeLogger = new BungeeLogger(getLogger());
        this.console = new BungeeCommandSender(getProxy().getConsole());

        this.core = new Core(this);

        instance = this;

        getProxy()
            .getPluginManager()
            .registerListener(this, new PlayerListener(core.getCorePlayerListener()));

        getProxy()
            .getPluginManager()
            .registerCommand(this, new ImportCommand(core.getCoreImportCommand()));
        getProxy()
            .getPluginManager()
            .registerCommand(this, new SpoofCommand(core.getCoreSpoofCommand()));
        getProxy()
            .getPluginManager()
            .registerCommand(this, new ReloadCommand(core.getCoreReloadCommand()));
        getProxy()
            .getPluginManager()
            .registerCommand(this, new ToggleCommand(core.getCoreToggleJoinCommand()));

        if (getProxy().getPluginManager().getPlugin("PremiumVanish") != null) {
            this.premiumVanish = new BungeePremiumVanish();
            bungeeLogger.info("Successfully hooked into PremiumVanish!");
        }

        for (CustomChart chart : core.getCustomCharts()) {
            metrics.addCustomChart(chart);
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
        } else if (event instanceof NetworkJoinEvent) {
            getProxy().getPluginManager().callEvent(new BungeeNetworkJoinEvent((NetworkJoinEvent) event));
        } else if (event instanceof NetworkLeaveEvent) {
            getProxy().getPluginManager().callEvent(new BungeeNetworkLeaveEvent((NetworkLeaveEvent) event));
        } else if (event instanceof SwapServerEvent) {
            getProxy().getPluginManager().callEvent(new BungeeSwapServerEvent((SwapServerEvent) event));
        }
    }

    @Override
    public void registerDiscordListener(DiscordIntegration discordIntegration) {
        if (bungeeDiscordListener != null) return;
        bungeeDiscordListener = new BungeeDiscordListener(discordIntegration);
        getProxy()
                .getPluginManager()
                .registerListener(this, bungeeDiscordListener);
    }

    @Override
    public void unregisterDiscordListener() {
        if (bungeeDiscordListener == null) return;
        getProxy()
                .getPluginManager()
                .unregisterListener(bungeeDiscordListener);
    }

    @Override
    public PremiumVanish getVanishAPI() {
        return premiumVanish;
    }

    @Override
    public void cancelTask(int taskId) {
        getProxy().getScheduler().cancel(taskId);
    }

    @Override
    public int runTaskRepeatedly(Runnable task, int timeInSecondsLater) {
        return getProxy().getScheduler().schedule(this, task, timeInSecondsLater, timeInSecondsLater, TimeUnit.SECONDS).getId();
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
