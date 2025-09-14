package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import xyz.earthcow.networkjoinmessages.common.commands.CoreReloadCommand;
import xyz.earthcow.networkjoinmessages.velocity.VelocityMain;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityCommandSender;

public class ReloadCommand implements SimpleCommand {

    private final CoreReloadCommand coreReloadCommand;

    public ReloadCommand(CoreReloadCommand coreReloadCommand) {
        this.coreReloadCommand = coreReloadCommand;
    }

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        coreReloadCommand.execute(
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
            .hasPermission(coreReloadCommand.getRequiredPermission());
    }
}
