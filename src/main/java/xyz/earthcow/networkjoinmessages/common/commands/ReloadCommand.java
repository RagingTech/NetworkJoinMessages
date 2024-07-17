package xyz.earthcow.networkjoinmessages.common.commands;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.general.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;
import xyz.earthcow.networkjoinmessages.common.util.HexChat;

import java.util.List;

public class ReloadCommand implements Command {

    @Override
    public void execute(Invocation invocation) {

    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation
            .source()
            .hasPermission();
    }

    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (coreCommandSender.hasPermission("networkjoinmessages.reload")) {
            NetworkJoinMessagesCore.getInstance().loadConfig();
            String msg = ConfigManager.getPluginConfig().getString("Messages.Commands.Reload.ConfigReloaded");
            coreCommandSender.sendMessage(HexChat.translateHexCodes(msg));
        }
    }

    @Override
    public String getRequiredPermssion() {
        return "networkjoinmessages.exclusions";
    }

    @Override
    public List<String> getTabCompletion(CoreCommandSender coreCommandSender, String[] args) {
        return null;
    }
}
