package xyz.earthcow.networkjoinmessages.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import lombok.Getter;
import org.bstats.charts.CustomChart;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;
import xyz.earthcow.networkjoinmessages.common.Core;
import xyz.earthcow.networkjoinmessages.common.BuildConstants;
import xyz.earthcow.networkjoinmessages.common.abstraction.*;
import xyz.earthcow.networkjoinmessages.common.modules.DiscordIntegration;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.*;
import xyz.earthcow.networkjoinmessages.velocity.commands.ImportCommand;
import xyz.earthcow.networkjoinmessages.velocity.commands.ReloadCommand;
import xyz.earthcow.networkjoinmessages.velocity.commands.SpoofCommand;
import xyz.earthcow.networkjoinmessages.velocity.commands.ToggleJoinCommand;
import xyz.earthcow.networkjoinmessages.velocity.listeners.PlayerListener;
import xyz.earthcow.networkjoinmessages.velocity.listeners.VelocityDiscordListener;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Plugin(
    id = "networkjoinmessages",
    name = "NetworkJoinMessages",
    version = BuildConstants.VERSION,
    url = "https://github.com/RagingTech/NetworkJoinMessages",
    description = "A plugin handling join, leave and swap messages for proxy servers.",
    authors = { "EarthCow" },
    dependencies = {
        @Dependency(id = "supervanish", optional = true),
        @Dependency(id = "premiumvanish", optional = true),
        @Dependency(id = "luckperms", optional = true),
        @Dependency(id = "papiproxybridge", optional = true),
        @Dependency(id = "miniplaceholders", optional = true),
        @Dependency(id = "limboapi", optional = true),
        @Dependency(id = "sayanvanish", optional = true)
    }
)
public class VelocityMain implements CorePlugin {

    @Getter
    private static VelocityMain instance;
    private final PlayerManager manager = new PlayerManager();
    @Getter
    private final ProxyServer proxy;
    private final Logger logger;
    private final File dataFolder;
    private PremiumVanish premiumVanish;
    private Core core;
    private VelocityLogger velocityLogger;
    private VelocityCommandSender console;
    private final Metrics.Factory metricsFactory;
    private boolean isLimboAPIAvailable = false;

    private VelocityDiscordListener velocityDiscordListener = null;

    private final List<ScheduledTask> tasks = new ArrayList<>();

    @Inject
    public VelocityMain(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataFolder = dataDirectory.toFile();
        this.metricsFactory = metricsFactory;

        instance = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Anonymous usage data via bStats (https://bstats.org/plugin/velocity/NetworkJoinMessages/26526)
        final int PLUGIN_ID = 26526;
        Metrics metrics = metricsFactory.make(this, PLUGIN_ID);

        this.velocityLogger = new VelocityLogger(logger);
        this.console = new VelocityCommandSender(proxy.getConsoleCommandSource());

        this.core = new Core(this);

        proxy.getEventManager().register(this, new PlayerListener(core.getCorePlayerListener()));

        registerCommands();

        if (proxy.getPluginManager().getPlugin("premiumvanish").isPresent()) {
            this.premiumVanish = new VelocityPremiumVanish();
            velocityLogger.info("Successfully hooked into PremiumVanish!");
        }

        if (isPluginLoaded("limboapi")) {
            this.isLimboAPIAvailable = true;
            velocityLogger.info("Successfully hooked into LimboAPI!");
        }

        for (CustomChart chart : core.getCustomCharts()) {
            metrics.addCustomChart(chart);
        }
    }

    private void registerCommands() {
        CommandManager commandManager = proxy.getCommandManager();
        commandManager.register(
            commandManager
                .metaBuilder("njoinimport")
                .plugin(this)
                .build(),
            new ImportCommand(core.getCoreImportCommand())
        );
        commandManager.register(
            commandManager
                .metaBuilder("njoinspoof")
                .plugin(this)
                .build(),
            new SpoofCommand(core.getCoreSpoofCommand())
        );
        commandManager.register(
            commandManager
                .metaBuilder("njoinreload")
                .plugin(this)
                .build(),
            new ReloadCommand(core.getCoreReloadCommand())
        );
        commandManager.register(
            commandManager
                .metaBuilder("njointoggle")
                .plugin(this)
                .build(),
            new ToggleJoinCommand(core.getCoreToggleJoinCommand())
        );
    }

    @Override
    public void disable() {
        proxy.getEventManager().unregisterListeners(this);
        proxy.getCommandManager().unregister(proxy.getCommandManager().getCommandMeta("njoinimport"));
        proxy.getCommandManager().unregister(proxy.getCommandManager().getCommandMeta("njoinspoof"));
        proxy.getCommandManager().unregister(proxy.getCommandManager().getCommandMeta("njoinreload"));
        proxy.getCommandManager().unregister(proxy.getCommandManager().getCommandMeta("njointoggle"));
    }

    @Override
    public void fireEvent(Object event) {
        proxy.getEventManager().fireAndForget(event);
    }

    @Override
    public void registerDiscordListener(DiscordIntegration discordIntegration) {
        if (velocityDiscordListener != null) return;
        velocityDiscordListener = new VelocityDiscordListener(discordIntegration);
        proxy.getEventManager().register(this, velocityDiscordListener);
    }

    @Override
    public void unregisterDiscordListener() {
        if (velocityDiscordListener == null) return;
        proxy.getEventManager().unregisterListener(this, velocityDiscordListener);
    }

    @Override
    public void cancelTask(int taskId) {
        try {
            tasks.get(taskId).cancel();
        } catch (IndexOutOfBoundsException ignored) {
        }
    }

    @Override
    public int runTaskRepeatedly(Runnable task, int timeInSecondsLater) {
        tasks.add(
            proxy.getScheduler().buildTask(this, scheduledTask -> {
                task.run();
                tasks.remove(scheduledTask);
            }).delay(timeInSecondsLater, TimeUnit.SECONDS).repeat(timeInSecondsLater, TimeUnit.SECONDS).schedule()
        );
        return tasks.size() - 1;
    }

    @Override
    public void runTaskAsync(Runnable task) {
        proxy.getScheduler().buildTask(this, task).schedule();
    }

    @Override
    public boolean isPluginLoaded(String pluginName) {
        return proxy.getPluginManager().isLoaded(pluginName.toLowerCase());
    }

    @Override
    public CorePlayer createPlayer(UUID uuid) {
        Optional<Player> player = proxy.getPlayer(uuid);
        return player.map(VelocityPlayer::new).orElse(null);
    }

    // Getters

    @Override
    public PlayerManager getPlayerManager(){
        return manager;
    }

    public boolean getIsLimboAPIAvailable() {
        return isLimboAPIAvailable;
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
    public Core getCore() {
        return core;
    }

    @Override
    public CoreCommandSender getConsole() {
        return console;
    }

    @Override
    public List<CorePlayer> getAllPlayers() {
        return proxy.getAllPlayers().stream().map(player -> getOrCreatePlayer(player.getUniqueId())).collect(Collectors.toList());
    }

    @Override
    public CoreBackendServer getServer(String serverName) {
        RegisteredServer registeredServer = proxy.getServer(serverName).orElse(null);
        if (registeredServer == null) return null;
        return new VelocityServer(registeredServer);
    }
}
