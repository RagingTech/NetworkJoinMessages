package xyz.earthcow.networkjoinmessages.common.commands;

import xyz.earthcow.networkjoinmessages.common.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.Core;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;

import java.util.List;

public class CoreReloadCommand implements Command {

    private final Core core;

    public CoreReloadCommand(Core core) {
        this.core = core;
    }

    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (coreCommandSender.hasPermission("networkjoinmessages.reload")) {
            core.loadConfigs();
            core.getMessageHandler().sendMessage(coreCommandSender,
                ConfigManager.getPluginConfig().getString("Messages.Commands.Reload.ConfigReloaded")
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