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
import xyz.earthcow.networkjoinmessages.common.util.PlaceholderResolver;
import xyz.earthcow.networkjoinmessages.common.util.SpoofManager;

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
        DiscordWebhookBuilder webhookBuilder = new DiscordWebhookBuilder(plugin, config, messageFormatter, configManager.getDiscordConfig());
        DiscordIntegration discordIntegration = new DiscordIntegration(plugin, placeholderResolver, webhookBuilder, configManager.getDiscordConfig());

        // Spoof
        SpoofManager spoofManager = new SpoofManager(plugin, config, messageHandler, messageFormatter);

        this.customCharts = config.getCustomCharts();

        // Listeners
        this.corePlayerListener = new CorePlayerListener(
            plugin, config, stateStore, messageHandler, messageFormatter,
            receiverResolver, silenceChecker, leaveMessageCache, leaveJoinBuffer
        );

        // Commands
        this.coreImportCommand     = new CoreImportCommand(corePlayerListener.getPlayerJoinTracker());
        this.coreSpoofCommand      = new CoreSpoofCommand(config, stateStore, messageHandler, spoofManager);
        this.coreReloadCommand     = new CoreReloadCommand(plugin, configManager, config, placeholderResolver, messageHandler, leaveMessageCache, discordIntegration);
        this.coreToggleJoinCommand = new CoreToggleJoinCommand(config, stateStore, messageHandler);

        this.corePremiumVanishListener = premiumVanish == null ? null
            : new CorePremiumVanishListener(plugin.getCoreLogger(), config, spoofManager);
    }
}
