package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import xyz.earthcow.networkjoinmessages.common.general.Core;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityCommandSender;
import xyz.earthcow.networkjoinmessages.velocity.general.VelocityMain;

public class ReloadCommand implements SimpleCommand {

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        Core.getInstance().coreReloadCommand.execute(
            invocation.source() instanceof Player ?
                // If the CommandSource is a Player
                VelocityMain.getInstance().getOrCreatePlayer(((Player) invocation.source()).getUniqueId())
                :
                // If the CommandSource is not a Player
                new VelocityCommandSender(invocation.source()),
            invocation.arguments());
    }

    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation
            .source()
            .hasPermission(Core.getInstance().coreReloadCommand.getRequiredPermission());
    }
}
