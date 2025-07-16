package xyz.earthcow.networkjoinmessages.velocity.general;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;
import xyz.earthcow.networkjoinmessages.common.abstraction.*;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.*;
import xyz.earthcow.networkjoinmessages.velocity.commands.FakeCommand;
import xyz.earthcow.networkjoinmessages.velocity.commands.ImportCommand;
import xyz.earthcow.networkjoinmessages.velocity.commands.ReloadCommand;
import xyz.earthcow.networkjoinmessages.velocity.commands.ToggleJoinCommand;
import xyz.earthcow.networkjoinmessages.velocity.listeners.PlayerListener;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Plugin(
    id = "networkjoinmessages",
    name = "NetworkJoinMessages",
    version = "2.3.0-SNAPSHOT-4",
    url = "https://github.com/RagingTech/NetworkJoinMessages",
    description = "A plugin handling join, leave and switch messages for proxy servers.",
    authors = { "EarthCow" },
    dependencies = {
        @Dependency(id = "supervanish", optional = true),
        @Dependency(id = "premiumvanish", optional = true),
        @Dependency(id = "luckperms", optional = true),
        @Dependency(id = "papiproxybridge", optional = true),
        @Dependency(id = "miniplaceholders", optional = true)
    }
)
public class VelocityMain implements CorePlugin {

    private static VelocityMain instance;
    private final ProxyServer proxy;
    private final VelocityLogger velocityLogger;
    private final File dataFolder;
    private PremiumVanish premiumVanish;
    private NetworkJoinMessagesCore core;
    private VelocityCommandSender console;
    private final Metrics.Factory metricsFactory;

    @Inject
    public VelocityMain(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.proxy = proxy;
        this.velocityLogger = new VelocityLogger(logger);
        this.dataFolder = dataDirectory.toFile();
        this.metricsFactory = metricsFactory;

        instance = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Anonymous usage data via bStats (https://bstats.org/plugin/velocity/NetworkJoinMessages/26526)
        final int PLUGIN_ID = 26526;
        Metrics metrics = metricsFactory.make(this, PLUGIN_ID);

        this.core = new NetworkJoinMessagesCore(this);
        this.console = new VelocityCommandSender(proxy.getConsoleCommandSource());

        proxy.getEventManager().register(this, new PlayerListener());

        CommandManager commandManager = proxy.getCommandManager();
        commandManager.register(
            commandManager
                .metaBuilder("njoinimport")
                .plugin(this)
                .build(),
            new ImportCommand()
        );
        commandManager.register(
            commandManager
                .metaBuilder("fakemessage")
                .aliases("fm")
                .plugin(this)
                .build(),
            new FakeCommand()
        );
        commandManager.register(
            commandManager
                .metaBuilder("networkjoinreload")
                .aliases("njoinreload")
                .plugin(this)
                .build(),
            new ReloadCommand()
        );
        commandManager.register(
            commandManager
                .metaBuilder("togglejoinmessage")
                .aliases("njointoggle")
                .plugin(this)
                .build(),
            new ToggleJoinCommand()
        );

        if (proxy.getPluginManager().getPlugin("premiumvanish").isPresent()) {
            this.premiumVanish = new VelocityPremiumVanish();
            velocityLogger.info("Successfully hooked into PremiumVanish!");
        }
    }

    @Override
    public void fireEvent(Object event) {
        proxy.getEventManager().fireAndForget(event);
    }

    @Override
    public void runTaskLater(Runnable task, int timeInSecondsLater) {
        proxy.getScheduler().buildTask(this, task).delay(timeInSecondsLater, TimeUnit.SECONDS).schedule();
    }

    @Override
    public void runTaskAsync(Runnable task) {
        proxy.getScheduler().buildTask(this, task).schedule();
    }

    @Override
    public boolean isPluginLoaded(String pluginName) {
        return proxy.getPluginManager().isLoaded(pluginName.toLowerCase());
    }

    // Getters

    public static VelocityMain getInstance() {
        return instance;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    @Override
    public CoreLogger getCoreLogger() {
        return velocityLogger;
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    @Override
    public PremiumVanish getVanishAPI() {
        return premiumVanish;
    }

    @Override
    public ServerType getServerType() {
        return ServerType.VELOCITY;
    }

    @Override
    public NetworkJoinMessagesCore getCore() {
        return core;
    }

    @Override
    public CoreCommandSender getConsole() {
        return console;
    }

    @Override
    public List<CorePlayer> getAllPlayers() {
        return proxy.getAllPlayers().stream().map(VelocityPlayer::new).collect(Collectors.toList());
    }

    @Override
    public CoreBackendServer getServer(String serverName) {
        RegisteredServer registeredServer = proxy.getServer(serverName).orElse(null);
        if (registeredServer == null) return null;
        return new VelocityServer(registeredServer);
    }
}
