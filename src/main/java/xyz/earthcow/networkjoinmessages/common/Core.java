package xyz.earthcow.networkjoinmessages.common;

import org.bstats.charts.CustomChart;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.commands.CoreImportCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreReloadCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreSpoofCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreToggleJoinCommand;
import xyz.earthcow.networkjoinmessages.common.listeners.CorePlayerListener;
import xyz.earthcow.networkjoinmessages.common.modules.DiscordIntegration;
import xyz.earthcow.networkjoinmessages.common.modules.SayanVanishHook;
import xyz.earthcow.networkjoinmessages.common.util.Formatter;

import java.util.Collection;

public class Core {
    private final CorePlugin plugin;

    private final Collection<CustomChart> customCharts;

    private final CorePlayerListener corePlayerListener;
    private final CoreImportCommand coreImportCommand;
    private final CoreSpoofCommand coreSpoofCommand;
    private final CoreReloadCommand coreReloadCommand;
    private final CoreToggleJoinCommand coreToggleJoinCommand;

    public Core(CorePlugin plugin) {
        this.plugin = plugin;

        SayanVanishHook sayanVanishHook;
        if (plugin.isPluginLoaded("SayanVanish")) {
            sayanVanishHook = new SayanVanishHook();
        } else {
            sayanVanishHook = null;
        }

        ConfigManager configManager = new ConfigManager(plugin);

        Storage storage = new Storage(plugin, configManager);
        Formatter formatter = new Formatter(plugin, storage);
        MessageHandler messageHandler = new MessageHandler(plugin, storage, formatter, sayanVanishHook);
        DiscordIntegration discordIntegration = new DiscordIntegration(plugin, storage, formatter, messageHandler, configManager.getDiscordConfig());

        this.customCharts = storage.getCustomCharts();

        this.corePlayerListener = new CorePlayerListener(plugin, storage, messageHandler, sayanVanishHook);

        this.coreImportCommand = new CoreImportCommand(corePlayerListener.getPlayerJoinTracker());
        this.coreSpoofCommand = new CoreSpoofCommand(plugin, storage, messageHandler);
        this.coreReloadCommand = new CoreReloadCommand(configManager, storage, discordIntegration, messageHandler);
        this.coreToggleJoinCommand = new CoreToggleJoinCommand(storage, messageHandler);

    }

    public CorePlugin getPlugin() {
        return plugin;
    }

    public Collection<CustomChart> getCustomCharts() {
        return customCharts;
    }

    public CorePlayerListener getCorePlayerListener() {
        return corePlayerListener;
    }

    public CoreImportCommand getCoreImportCommand() {
        return coreImportCommand;
    }

    public CoreSpoofCommand getCoreSpoofCommand() {
        return coreSpoofCommand;
    }

    public CoreReloadCommand getCoreReloadCommand() {
        return coreReloadCommand;
    }

    public CoreToggleJoinCommand getCoreToggleCommand() {
        return coreToggleJoinCommand;
    }
}
