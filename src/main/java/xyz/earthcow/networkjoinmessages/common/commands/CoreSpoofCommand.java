package xyz.earthcow.networkjoinmessages.common.commands;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.player.PlayerStateStore;
import xyz.earthcow.networkjoinmessages.common.util.SpoofManager;

import java.util.List;

public class CoreSpoofCommand implements Command {

    private static final List<String> COMMAND_ARGS = ImmutableList.of("join", "leave", "swap", "toggle");

    private final PluginConfig config;
    private final PlayerStateStore stateStore;
    private final MessageHandler messageHandler;
    private final SpoofManager spoofManager;

    public CoreSpoofCommand(PluginConfig config, MessageHandler messageHandler, SpoofManager spoofManager) {
        // PlayerStateStore is accessed via the SpoofManager's toggle; pass stateStore separately
        // for the toggle subcommand
        this(config, null, messageHandler, spoofManager);
    }

    public CoreSpoofCommand(PluginConfig config, PlayerStateStore stateStore, MessageHandler messageHandler, SpoofManager spoofManager) {
        this.config = config;
        this.stateStore = stateStore;
        this.messageHandler = messageHandler;
        this.spoofManager = spoofManager;
    }

    @Override
    public void execute(CoreCommandSender sender, String[] args) {
        if (!(sender instanceof CorePlayer player)) {
            sender.sendMessage(Component.text("Only players can run this command!", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission("networkjoinmessages.spoof")) {
            messageHandler.sendMessage(player, config.getNoPermission());
            return;
        }
        if (args.length < 1) {
            messageHandler.sendMessage(player, config.getSpoofNoArgument());
            return;
        }

        switch (args[0].toLowerCase()) {
            case "join"  -> spoofManager.spoofJoin(player);
            case "leave" -> spoofManager.spoofLeave(player);
            case "swap"  -> {
                if (args.length < 3) {
                    messageHandler.sendMessage(player, config.getSpoofSwapNoArgument());
                    return;
                }
                spoofManager.spoofSwap(player, args[1], args[2]);
            }
            case "toggle" -> {
                if (!player.hasPermission("networkjoinmessages.silent")) {
                    messageHandler.sendMessage(player, config.getSpoofToggleSilentNoPerm());
                    return;
                }
                if (stateStore == null) return;
                boolean newState = !stateStore.getSilentState(player);
                stateStore.setSilentState(player, newState);
                messageHandler.sendMessage(player,
                    config.getSpoofToggleSilent()
                        .replaceAll("%state%|<state>", String.valueOf(newState))
                );
            }
        }
    }

    @Override
    public String getRequiredPermission() { return "networkjoinmessages.spoof"; }

    @Override
    public List<String> getTabCompletion(CoreCommandSender sender, String[] args) {
        return switch (args.length) {
            case 0, 1 -> COMMAND_ARGS;
            case 2, 3 -> args[0].equalsIgnoreCase("swap")
                ? config.getServerNames()
                : ImmutableList.of(config.getNoMoreArgumentsNeeded());
            default -> ImmutableList.of(config.getNoMoreArgumentsNeeded());
        };
    }
}
