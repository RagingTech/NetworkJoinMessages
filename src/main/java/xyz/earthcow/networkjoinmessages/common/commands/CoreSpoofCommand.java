package xyz.earthcow.networkjoinmessages.common.commands;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.Storage;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.util.SpoofManager;

import java.util.List;

public class CoreSpoofCommand implements Command {

    private final List<String> COMMAND_ARGS = ImmutableList.of(
        "join", "leave", "swap", "toggle"
    );

    private final Storage storage;
    private final MessageHandler messageHandler;
    private final SpoofManager spoofManager;

    public CoreSpoofCommand(Storage storage, MessageHandler messageHandler, SpoofManager spoofManager) {
        this.storage = storage;
        this.messageHandler = messageHandler;
        this.spoofManager = spoofManager;
    }

    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (!(coreCommandSender instanceof CorePlayer player)) {
            coreCommandSender.sendMessage(Component.text("Only players can run this command!", NamedTextColor.RED));
            return;
        }

        if (!player.hasPermission("networkjoinmessages.spoof")) {
            messageHandler.sendMessage(
                player,
                storage.getNoPermission()
            );
            return;
        }

        if (args.length < 1) {
            messageHandler.sendMessage(
                player,
                storage.getSpoofNoArgument()
            );
            return;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                spoofManager.spoofJoin(player);
                return;
            case "leave":
                spoofManager.spoofLeave(player);
                return;
            case "swap":
                if (args.length < 3) {
                    messageHandler.sendMessage(
                        player,
                        storage.getSpoofSwapNoArgument()
                    );
                    return;
                }
                String from = args[1];
                String to = args[2];

                spoofManager.spoofSwap(player, from, to);
                return;
            case "toggle":
                if (!player.hasPermission("networkjoinmessages.silent")) {
                    messageHandler.sendMessage(
                        player,
                        storage.getSpoofToggleSilentNoPerm()
                    );
                    return;
                }
                boolean state = !storage.getSilentMessageState(player);
                storage.setSilentMessageState(player, state);
                messageHandler.sendMessage(
                    player,
                    storage.getSpoofToggleSilent()
                        .replaceAll("%state%", String.valueOf(state))
                        .replaceAll("<state>", String.valueOf(state))
                );
        }
    }

    @Override
    public String getRequiredPermission() {
        return "networkjoinmessages.spoof";
    }

    @Override
    public List<String> getTabCompletion(CoreCommandSender coreCommandSender, String[] args) {
        switch (args.length) {
            case 0:
            case 1:
                return COMMAND_ARGS;
            case 2:
            case 3:
                if (args[0].equalsIgnoreCase("swap")) {
                    return storage.getServerNames();
                } else {
                    return ImmutableList.of(
                        storage.getNoMoreArgumentsNeeded()
                    );
                }
            default:
                return ImmutableList.of(
                    storage.getNoMoreArgumentsNeeded()
                );
        }
    }
}