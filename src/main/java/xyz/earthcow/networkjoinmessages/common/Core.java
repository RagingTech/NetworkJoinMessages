package xyz.earthcow.networkjoinmessages.common;

import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.commands.CoreFakeCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreImportCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreReloadCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreToggleJoinCommand;
import xyz.earthcow.networkjoinmessages.common.modules.DiscordWebhookIntegration;
import xyz.earthcow.networkjoinmessages.common.util.H2PlayerJoinTracker;

public class Core {
    private static Core instance;

    public static Core getInstance() {
        return instance;
    }

    private final CorePlugin plugin;

    public CorePlugin getPlugin() {
        return plugin;
    }

    private final DiscordWebhookIntegration discordWebhookIntegration;
    private H2PlayerJoinTracker firstJoinTracker;

    public DiscordWebhookIntegration getDiscordWebhookIntegration() {
        return discordWebhookIntegration;
    }

    public final CoreImportCommand coreImportCommand = new CoreImportCommand();
    public final CoreFakeCommand coreFakeCommand = new CoreFakeCommand();
    public final CoreReloadCommand coreReloadCommand = new CoreReloadCommand();
    public final CoreToggleJoinCommand coreToggleJoinCommand = new CoreToggleJoinCommand();

    public Core(CorePlugin plugin) {
        this.plugin = plugin;

        instance = this;

        loadConfigs();
        discordWebhookIntegration = new DiscordWebhookIntegration();

        try {
            firstJoinTracker = new H2PlayerJoinTracker("./" + plugin.getDataFolder().getPath() + "/joined");
        } catch (Exception ex) {
            plugin.getCoreLogger().severe("Failed to load H2 first join tracker!");
        }

    }

    public void loadConfigs() {
        ConfigManager.setupConfigs(plugin);
        Storage.getInstance().setUpDefaultValuesFromConfig();
        if (discordWebhookIntegration != null) {
            discordWebhookIntegration.loadVariables();
        }
    }

    public H2PlayerJoinTracker getFirstJoinTracker() {
        return firstJoinTracker;
    }
}
