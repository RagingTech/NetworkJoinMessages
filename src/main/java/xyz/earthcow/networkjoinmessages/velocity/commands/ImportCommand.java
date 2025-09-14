package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import xyz.earthcow.networkjoinmessages.common.commands.CoreImportCommand;
import xyz.earthcow.networkjoinmessages.velocity.VelocityMain;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityCommandSender;

public class ImportCommand implements SimpleCommand {

    private final CoreImportCommand coreImportCommand;

    public ImportCommand(CoreImportCommand coreImportCommand) {
        this.coreImportCommand = coreImportCommand;
    }

    @Override
    public void execute(Invocation invocation) {
        coreImportCommand.execute(
            invocation.source() instanceof Player ?
                // If the CommandSource is a Player
                VelocityMain.getInstance().getOrCreatePlayer(((Player) invocation.source()).getUniqueId())
                :
                // If the CommandSource is not a Player
                new VelocityCommandSender(invocation.source()),
            invocation.arguments());
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation
            .source()
            .hasPermission(coreImportCommand.getRequiredPermission());
    }
}
