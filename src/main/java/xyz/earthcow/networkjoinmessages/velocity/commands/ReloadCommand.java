package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import xyz.earthcow.networkjoinmessages.velocity.general.VelocityMain;
import xyz.earthcow.networkjoinmessages.velocity.util.HexChat;

public class ReloadCommand implements SimpleCommand {

    @Override
    public void execute(SimpleCommand.Invocation invocation) {

        if (invocation.source().hasPermission("networkjoinmessages.reload")) {
            VelocityMain.getInstance().reloadConfig();
            VelocityMain.getInstance().getDiscordWebhookIntegration().loadConfig();
            String msg = VelocityMain.getInstance().getRootNode().node("Messages", "Commands", "Reload", "ConfigReloaded").getString("Config Reloaded!");
            invocation.source().sendMessage(Component.text(HexChat.translateHexCodes(msg)));
        }

    }

    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("networkjoinmessages.exclusions");
    }

}
