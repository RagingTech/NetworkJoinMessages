package xyz.earthcow.networkjoinmessages.common;

import lombok.Getter;
import org.bstats.charts.CustomChart;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.abstraction.PremiumVanish;
import xyz.earthcow.networkjoinmessages.common.broadcast.MessageFormatter;
import xyz.earthcow.networkjoinmessages.common.broadcast.ReceiverResolver;
import xyz.earthcow.networkjoinmessages.common.commands.*;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.listeners.CorePlayerListener;
import xyz.earthcow.networkjoinmessages.common.listeners.CorePremiumVanishListener;
import xyz.earthcow.networkjoinmessages.common.modules.DiscordIntegration;
import xyz.earthcow.networkjoinmessages.common.modules.DiscordWebhookBuilder;
import xyz.earthcow.networkjoinmessages.common.modules.SayanVanishHook;
import xyz.earthcow.networkjoinmessages.common.player.*;
import xyz.earthcow.networkjoinmessages.common.storage.H2PlayerJoinTracker;
import xyz.earthcow.networkjoinmessages.common.storage.PlayerJoinTracker;
import xyz.earthcow.networkjoinmessages.common.storage.SQLPlayerJoinTracker;
import xyz.earthcow.networkjoinmessages.common.storage.TextPlayerJoinTracker;
import xyz.earthcow.networkjoinmessages.common.util.*;

import java.util.Collection;

/**
 * Composition root. Constructs and wires all plugin components together.
 * The only class allowed to know about all components simultaneously.
 */
@Getter
public class Core {

    private final CorePlugin plugin;
    private final Collection<CustomChart> customCharts;

    private final CorePlayerListener corePlayerListener;
    private final CoreImportCommand coreImportCommand;
    private final CoreSpoofCommand coreSpoofCommand;
    private final CoreReloadCommand coreReloadCommand;
    private final CoreToggleJoinCommand coreToggleJoinCommand;
    private final CorePremiumVanishListener corePremiumVanishListener;

    public Core(CorePlugin plugin, PremiumVanish premiumVanish) {
        this.plugin = plugin;

        // Infrastructure
        ConfigManager configManager = new ConfigManager(plugin);
        PluginConfig config = new PluginConfig(plugin, configManager);

        // Optional third-party hooks
        SayanVanishHook sayanVanishHook = null;
        if (plugin.isPluginLoaded("SayanVanish")) {
            sayanVanishHook = new SayanVanishHook();
            plugin.getCoreLogger().info("Successfully hooked into SayanVanish!");
        }

        // Core data / state
        PlayerStateStore stateStore = new PlayerStateStore(config);

        // Placeholder resolution
        PlaceholderResolver placeholderResolver = new PlaceholderResolver(plugin, config);

        // Message building
        MessageFormatter messageFormatter = new MessageFormatter(plugin, config, sayanVanishHook);
        ReceiverResolver receiverResolver  = new ReceiverResolver(plugin, config);
        MessageHandler   messageHandler    = new MessageHandler(plugin, config, stateStore, placeholderResolver, receiverResolver);

        // Player event helpers
        SilenceChecker       silenceChecker  = new SilenceChecker(plugin, config, stateStore, sayanVanishHook, premiumVanish);
        LeaveMessageCache    leaveMessageCache = new LeaveMessageCache(plugin, config, messageFormatter, placeholderResolver);
        LeaveJoinBufferManager leaveJoinBuffer = new LeaveJoinBufferManager(plugin, config);

        // Discord integration
        DiscordWebhookBuilder webhookBuilder = new DiscordWebhookBuilder(plugin, configManager.getDiscordConfig());
        DiscordIntegration discordIntegration = new DiscordIntegration(plugin, placeholderResolver, messageFormatter, webhookBuilder, configManager.getDiscordConfig());

        // First-join tracker — backend selected from config (nullable; callers guard against null)
        PlayerJoinTracker firstJoinTracker = null;
        try {
            String storageType = config.getStorageType();
            if ("TEXT".equalsIgnoreCase(storageType)) {
                firstJoinTracker = new TextPlayerJoinTracker(
                    plugin.getCoreLogger(),
                    plugin.getDataFolder().toPath().resolve("joined.txt")
                );
                plugin.getCoreLogger().info("Using TEXT storage for first-join tracking (joined.txt).");
            } else if ("SQL".equalsIgnoreCase(storageType)) {
                firstJoinTracker = new SQLPlayerJoinTracker(
                    plugin.getCoreLogger(),
                    config.buildSqlConfig(),
                    plugin.getDataFolder().toPath()
                );
                plugin.getCoreLogger().info("Using SQL storage for first-join tracking.");
            } else {
                firstJoinTracker = new H2PlayerJoinTracker(
                    plugin.getCoreLogger(),
                    plugin.getDataFolder().toPath().resolve("joined").toAbsolutePath().toString()
                );
                plugin.getCoreLogger().info("Using H2 storage for first-join tracking.");
            }
        } catch (SQLDriverLoader.DriverLoadException ex) {
            plugin.getCoreLogger().severe("Failed to download/load the SQL driver — first-join tracking is disabled. " +
                "Check your internet connection or place the driver JAR manually in the plugins/NetworkJoinMessages/drivers/ folder.");
            plugin.getCoreLogger().debug("Exception: " + ex);
        } catch (Exception ex) {
            plugin.getCoreLogger().severe("Failed to load first-join tracker! First-join messages will be unavailable.");
            plugin.getCoreLogger().debug("Exception: " + ex);
        }

        // Spoof
        SpoofManager spoofManager = new SpoofManager(plugin, config, messageHandler, messageFormatter, placeholderResolver);

        this.customCharts = config.getCustomCharts();

        // Listeners
        this.corePlayerListener = new CorePlayerListener(
            plugin, config, stateStore, messageHandler, messageFormatter,
            receiverResolver, silenceChecker, leaveMessageCache, leaveJoinBuffer,
            placeholderResolver, firstJoinTracker
        );

        // Commands
        this.coreImportCommand     = new CoreImportCommand(firstJoinTracker);
        this.coreSpoofCommand      = new CoreSpoofCommand(config, stateStore, messageHandler, spoofManager);
        this.coreReloadCommand     = new CoreReloadCommand(configManager, config, placeholderResolver, messageHandler, leaveMessageCache, discordIntegration);
        this.coreToggleJoinCommand = new CoreToggleJoinCommand(config, stateStore, messageHandler);

        this.corePremiumVanishListener = premiumVanish == null ? null
            : new CorePremiumVanishListener(plugin.getCoreLogger(), config, spoofManager);
    }
}
