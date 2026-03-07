package xyz.earthcow.networkjoinmessages.common.commands;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.player.PlayerStateStore;

import java.util.List;

public class CoreToggleJoinCommand implements Command {

    private static final List<String> COMMAND_ARGS = ImmutableList.of("join", "leave", "swap", "all");

    private final PluginConfig config;
    private final PlayerStateStore stateStore;
    private final MessageHandler messageHandler;

    public CoreToggleJoinCommand(PluginConfig config, PlayerStateStore stateStore, MessageHandler messageHandler) {
        this.config = config;
        this.stateStore = stateStore;
        this.messageHandler = messageHandler;
    }

    @Override
    public void execute(CoreCommandSender sender, String[] args) {
        if (!(sender instanceof CorePlayer player)) {
            sender.sendMessage(Component.text("Only players can run this command!", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission("networkjoinmessages.toggle")) {
            messageHandler.sendMessage(player, config.getNoPermission());
            return;
        }
        if (args.length < 1) {
            messageHandler.sendMessage(player, config.getToggleJoinMissingFirstArg());
            return;
        }
        if (args.length < 2) {
            messageHandler.sendMessage(player, config.getToggleJoinMissingState());
            return;
        }

        String mode = args[0].toLowerCase();
        if (!COMMAND_ARGS.contains(mode)) {
            messageHandler.sendMessage(player, config.getToggleJoinMissingFirstArg());
            return;
        }

        boolean state = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
        stateStore.setSendMessageState(mode, player, state);

        messageHandler.sendMessage(player,
            config.getToggleJoinConfirmation()
                .replaceAll("<mode>|%mode%",   mode)
                .replaceAll("<state>|%state%", String.valueOf(state))
        );
    }

    @Override
    public String getRequiredPermission() { return "networkjoinmessages.toggle"; }

    @Override
    public List<String> getTabCompletion(CoreCommandSender sender, String[] args) {
        return switch (args.length) {
            case 0, 1 -> COMMAND_ARGS;
            case 2    -> ImmutableList.of("on", "off");
            default   -> ImmutableList.of(config.getNoMoreArgumentsNeeded());
        };
    }
}
