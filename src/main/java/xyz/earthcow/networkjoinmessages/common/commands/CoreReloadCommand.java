package xyz.earthcow.networkjoinmessages.common.commands;

import dev.dejvokep.boostedyaml.YamlDocument;
import xyz.earthcow.networkjoinmessages.common.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.Core;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;

import java.util.List;

public class CoreReloadCommand implements Command {

    private final Core core;
    private final YamlDocument config;

    public CoreReloadCommand(Core core, YamlDocument pluginConfig) {
        this.core = core;
        this.config = pluginConfig;
    }

    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (coreCommandSender.hasPermission("networkjoinmessages.reload")) {
            core.loadConfigs();
            core.getMessageHandler().sendMessage(coreCommandSender,
                config.getString("Messages.Commands.Reload.ConfigReloaded")
            );
        }
    }

    @Override
    public String getRequiredPermission() {
        return "networkjoinmessages.reload";
    }

    @Override
    public List<String> getTabCompletion(CoreCommandSender coreCommandSender, String[] args) {
        return null;
    }
}