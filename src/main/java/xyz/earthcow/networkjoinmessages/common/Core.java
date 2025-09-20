package xyz.earthcow.networkjoinmessages.common;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.commands.CoreImportCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreReloadCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreSpoofCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreToggleJoinCommand;
import xyz.earthcow.networkjoinmessages.common.modules.DiscordIntegration;
import xyz.earthcow.networkjoinmessages.common.util.Formatter;
import xyz.earthcow.networkjoinmessages.common.util.H2PlayerJoinTracker;

public class Core {
    private final CorePlugin plugin;

    private final Formatter formatter;
    private final Storage storage;
    private final MessageHandler messageHandler;

    private final DiscordIntegration discordIntegration;
    private H2PlayerJoinTracker firstJoinTracker;

    private final CoreImportCommand coreImportCommand;
    private final CoreSpoofCommand coreSpoofCommand;
    private final CoreReloadCommand coreReloadCommand;
    private final CoreToggleJoinCommand coreToggleJoinCommand;

    public Core(CorePlugin plugin, CoreLogger coreLogger) {
        this.plugin = plugin;

        ConfigManager configManager = new ConfigManager(plugin);

        this.storage = new Storage(plugin, configManager);
        this.formatter = new Formatter(plugin, storage);
        this.messageHandler = new MessageHandler(plugin, storage, formatter);
        this.discordIntegration = new DiscordIntegration(plugin, storage, formatter, messageHandler, configManager.getDiscordConfig());
        loadConfigs();

        try {
            firstJoinTracker = new H2PlayerJoinTracker(coreLogger, "./" + plugin.getDataFolder().getPath() + "/joined");
        } catch (Exception ex) {
            coreLogger.severe("Failed to load H2 first join tracker!");
            coreLogger.debug("Exception: " + ex);
        }

        this.coreImportCommand = new CoreImportCommand(this);
        this.coreSpoofCommand = new CoreSpoofCommand(storage, messageHandler, configManager.getPluginConfig());
        this.coreReloadCommand = new CoreReloadCommand(this, configManager.getPluginConfig());
        this.coreToggleJoinCommand = new CoreToggleJoinCommand(storage, messageHandler, configManager.getPluginConfig());

    }

    public void loadConfigs() {
        storage.setUpDefaultValuesFromConfig();
        discordIntegration.loadVariables();
    }

    public CorePlugin getPlugin() {
        return plugin;
    }

    public Formatter getFormatter() {
        return formatter;
    }

    public Storage getStorage() {
        return storage;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public H2PlayerJoinTracker getFirstJoinTracker() {
        return firstJoinTracker;
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
