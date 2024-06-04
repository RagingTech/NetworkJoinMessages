package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import xyz.earthcow.networkjoinmessages.velocity.general.Storage;
import xyz.earthcow.networkjoinmessages.velocity.general.VelocityMain;
import xyz.earthcow.networkjoinmessages.velocity.util.HexChat;

import java.util.List;

public class ToggleJoinCommand implements SimpleCommand {

	List<String> commandArguments = ImmutableList.of("join", "leave", "quit", "switch", "all");

	@Override
	public void execute(SimpleCommand.Invocation invocation) {

		if (invocation.source() instanceof Player) {
			Player player = (Player) invocation.source();
			if (!player.hasPermission("networkjoinmessages.togglemessage")) {
				String msg = VelocityMain.getInstance().getRootNode().node("Messages", "Commands", "NoPermission").getString(
						"&cYou do not have the permission to use this command.");
				player.sendMessage(Component.text(HexChat.translateHexCodes(msg)));
				return;
			}

			String[] args = invocation.arguments();
			if (args.length < 1) {
				String msg = VelocityMain.getInstance().getRootNode().node("Messages", "Commands", "ToggleJoin", "MissingFirstArgument").getString(
						"&6Please specify which messages you would like to disable/enable.\n"
								+ "&6Valid arguments are:&f join, leave, switch, all");
				player.sendMessage(Component.text(HexChat.translateHexCodes(msg)));
				return;
			}

			if (args.length < 2) {
				String msg = VelocityMain.getInstance().getRootNode().node("Messages", "Commands", "ToggleJoin", "MissingState").getString(
						"&6Please specify which state you would like to set the message to.\n"
								+ "&6Valid arguments are: &aon &7/ &coff &6or &atrue &7/ &cfalse&f.");
				player.sendMessage(Component.text(HexChat.translateHexCodes(msg)));
			} else {
				boolean state = false;
				if (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true")) {
					state = true;
				}
				if(commandArguments.contains(args[0].toLowerCase())) {
					Storage.getInstance().setSendMessageState(args[0], player.getUniqueId(), state);
					String msg = VelocityMain.getInstance().getRootNode().node("Messages", "Commands", "ToggleJoin", "Confirmation").getString(
							"&6Receive messages for &f<mode>&6 has been set to &f<state>\n&6This will last until the network reboots.");
					msg = msg.replace("<mode>", args[0]);
					msg = msg.replace("<state>", state + "");
					player.sendMessage(Component.text(HexChat.translateHexCodes(msg)));
					return;
				} else {
					//Triggered if commandArguments does not contain the argument used.
					String msg = VelocityMain.getInstance().getRootNode().node("Messages", "Commands", "ToggleJoin", "MissingFirstArgument").getString(
							"&6Please specify which messages you would like to disable/enable.\n"
									+ "&6Valid arguments are:&f join, leave, switch, all");
					player.sendMessage(Component.text(HexChat.translateHexCodes(msg)));
					return;
				}
			}
		}

	}

	@Override
	public boolean hasPermission(final SimpleCommand.Invocation invocation) {
		return invocation.source().hasPermission("networkjoinmessages.togglemessage");
	}

	@Override
	public List<String> suggest(final Invocation invocation) {
		switch (invocation.arguments().length) {
			case 1:
				return commandArguments;
			case 2:
				return ImmutableList.of("on","off");
			default:
				return ImmutableList.of("No more arguments needed.");
		}
	}

}
