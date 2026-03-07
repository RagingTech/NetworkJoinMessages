package xyz.earthcow.networkjoinmessages.common.commands;

import com.google.common.collect.ImmutableList;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.player.PlayerStateStore;

import java.util.List;
import java.util.UUID;

public class CoreToggleJoinCommand implements Command {

    private static final List<String> COMMAND_ARGS = ImmutableList.of("join", "leave", "swap", "all");

    private final PluginConfig config;
    private final PlayerStateStore stateStore;
    private final MessageHandler messageHandler;
    private final CorePlugin plugin;

    public CoreToggleJoinCommand(PluginConfig config, PlayerStateStore stateStore,
                                 MessageHandler messageHandler, CorePlugin plugin) {
        this.config = config;
        this.stateStore = stateStore;
        this.messageHandler = messageHandler;
        this.plugin = plugin;
    }

    @Override
    public void execute(CoreCommandSender sender, String[] args) {
        // --- Determine whether this is a self-toggle or a targeted toggle ---
        final boolean isTargeted = args.length >= 3;

        // Permission check: targeted toggle requires the .others node
        final String requiredPermission = isTargeted
            ? "networkjoinmessages.toggle.others"
            : "networkjoinmessages.toggle";

        if (!sender.hasPermission(requiredPermission)) {
            messageHandler.sendMessage(sender, config.getNoPermission());
            return;
        }

        // Console with no player argument is not valid
        if (!isTargeted && !(sender instanceof CorePlayer)) {
            messageHandler.sendMessage(sender, config.getToggleJoinMustSpecifyPlayer());
            return;
        }

        // Validate positional args
        if (args.length < 1) {
            messageHandler.sendMessage(sender, config.getToggleJoinMissingFirstArg());
            return;
        }
        if (args.length < 2) {
            messageHandler.sendMessage(sender, config.getToggleJoinMissingState());
            return;
        }

        final String mode = args[0].toLowerCase();
        if (!COMMAND_ARGS.contains(mode)) {
            messageHandler.sendMessage(sender, config.getToggleJoinMissingFirstArg());
            return;
        }

        final boolean state = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");

        if (isTargeted) {
            final String targetName = args[2];
            final CorePlayer onlineTarget = plugin.getPlayerManager().getPlayer(targetName);

            if (onlineTarget == null) {
                messageHandler.sendMessage(sender,
                    config.getToggleJoinTargetNotFound()
                        .replace("%player%", targetName));
                return;
            }

            final UUID targetUuid = onlineTarget.getUniqueId();
            stateStore.setSendMessageState(mode, targetUuid, onlineTarget.getName(), state);

            messageHandler.sendMessage(sender,
                config.getToggleJoinConfirmationOther()
                    .replace("%player%", onlineTarget.getName())
                    .replace("%mode%",     mode)
                    .replace("%state%",   String.valueOf(state)));

        } else {
            final CorePlayer player = (CorePlayer) sender;
            stateStore.setSendMessageState(mode, player, state);

            // <mode> and <state> are only kept for legacy support
            messageHandler.sendMessage(player,
                config.getToggleJoinConfirmation()
                    .replaceAll("<mode>|%mode%",   mode)
                    .replaceAll("<state>|%state%", String.valueOf(state)));
        }
    }

    @Override
    public String getRequiredPermission() { return "networkjoinmessages.toggle"; }

    @Override
    public List<String> getTabCompletion(CoreCommandSender sender, String[] args) {
        return switch (args.length) {
            case 0, 1 -> COMMAND_ARGS;
            case 2    -> ImmutableList.of("on", "off");
            case 3    -> {
                if (!sender.hasPermission("networkjoinmessages.toggle.others")) {
                    yield ImmutableList.of();
                }
                yield plugin.getAllPlayers().stream()
                    .map(CorePlayer::getName)
                    .collect(ImmutableList.toImmutableList());
            }
            default   -> ImmutableList.of(config.getNoMoreArgumentsNeeded());
        };
    }
}
