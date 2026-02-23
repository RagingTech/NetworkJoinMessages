package xyz.earthcow.networkjoinmessages.common.commands;

import xyz.earthcow.networkjoinmessages.common.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.modules.DiscordIntegration;
import xyz.earthcow.networkjoinmessages.common.player.LeaveMessageCache;
import xyz.earthcow.networkjoinmessages.common.util.PlaceholderResolver;

import java.util.List;

public class CoreReloadCommand implements Command {

    private final CorePlugin plugin;
    private final ConfigManager configManager;
    private final PluginConfig config;
    private final PlaceholderResolver placeholderResolver;
    private final MessageHandler messageHandler;
    private final LeaveMessageCache leaveMessageCache;
    private final DiscordIntegration discordIntegration;

    public CoreReloadCommand(
            CorePlugin plugin,
            ConfigManager configManager,
            PluginConfig config,
            PlaceholderResolver placeholderResolver,
            MessageHandler messageHandler,
            LeaveMessageCache leaveMessageCache,
            DiscordIntegration discordIntegration
    ) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.config = config;
        this.placeholderResolver = placeholderResolver;
        this.messageHandler = messageHandler;
        this.leaveMessageCache = leaveMessageCache;
        this.discordIntegration = discordIntegration;
    }

    @Override
    public void execute(CoreCommandSender sender, String[] args) {
        if (!sender.hasPermission("networkjoinmessages.reload")) return;

        configManager.reload();
        config.reload();
        placeholderResolver.setPPBRequestTimeout(config.getPPBRequestTimeout());
        discordIntegration.loadConfig();
        leaveMessageCache.initForAllPlayers();

        plugin.runTaskAsync(() -> {
            plugin.getAllPlayers().forEach(leaveMessageCache::refresh);
            messageHandler.sendMessage(sender, config.getReloadConfirmation());
        });
    }

    @Override
    public String getRequiredPermission() {
        return "networkjoinmessages.reload";
    }

    @Override
    public List<String> getTabCompletion(CoreCommandSender sender, String[] args) {
        return null;
    }
}
