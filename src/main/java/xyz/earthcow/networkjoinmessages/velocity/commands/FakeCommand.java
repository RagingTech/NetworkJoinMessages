package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import xyz.earthcow.networkjoinmessages.velocity.general.Storage;
import xyz.earthcow.networkjoinmessages.velocity.general.VelocityMain;
import xyz.earthcow.networkjoinmessages.velocity.util.HexChat;
import xyz.earthcow.networkjoinmessages.velocity.util.MessageHandler;

import java.util.List;

public class FakeCommand implements SimpleCommand {

	@Override
	public void execute(SimpleCommand.Invocation invocation) {

		if (!(invocation.source() instanceof Player)) {
			return;
		}

		Player player = (Player) invocation.source();
		if(!player.hasPermission("networkjoinmessages.fakemessage")) {
			return;
		}
		String[] args = invocation.arguments();
		if(args.length < 1) {
			String msg =  VelocityMain.getInstance().getRootNode().node("Messages", "Commands", "Fakemessage", "NoArgument").getString(
					"&6Arguments:\n"
							+ "- &cfakejoin\n"
							+ "- &cfakequit\n"
							+ "- &cfakeswitch&6 (to) (from)\n");
			player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
			return;
		} else {
			if(args[0].equalsIgnoreCase("fakejoin") || args[0].equalsIgnoreCase("fj") ) {
				String message = MessageHandler.getInstance().formatJoinMessage(player);
				MessageHandler.getInstance().broadcastMessage(HexChat.translateHexCodes( message), "join", player);
				return;

			} else if(args[0].equalsIgnoreCase("fakequit")  || args[0].equalsIgnoreCase("fq")) {
				String message = MessageHandler.getInstance().formatQuitMessage(player);
				MessageHandler.getInstance().broadcastMessage(HexChat.translateHexCodes( message), "leave", player);
				return;

			} else if(args[0].equalsIgnoreCase("fakeswitch")  || args[0].equalsIgnoreCase("fs")) {
				if(args.length < 3) {
					String msg =  VelocityMain.getInstance().getRootNode().node("Messages", "Commands", "Fakemessage", "FakeSwitchNoArgument").getString(
							"&6Arguments:\n"
									+ "- &cfakejoin\n"
									+ "- &cfakequit\n"
									+ "- &cfakeswitch&6 (to) (from)\n"
									+ "&4Error: Please specify &cTO&4 and &cFROM");
					player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
					return;
				} else {
					String fromName = args[1];
					String toName = args[2];

					String message = MessageHandler.getInstance().formatSwitchMessage(player, fromName, toName);

					MessageHandler.getInstance().broadcastMessage(HexChat.translateHexCodes( message), "switch", fromName, toName);
					return;
				}
			} else if(args[0].equalsIgnoreCase("toggle")) {
				String msg = "";
				if(!player.hasPermission("networkjoinmessages.silent")){
					msg = VelocityMain.getInstance().getRootNode().node("Messages", "Commands", "Fakemessage", "ToggleSilentNoPerm").getString(
							"&cYou do not have the permission to join the server silently.");
					player.sendMessage(Component.text(HexChat.translateHexCodes(msg)));
					return;
				} else {
					Boolean state = !Storage.getInstance().getAdminMessageState(player);
					msg = VelocityMain.getInstance().getRootNode().node("Messages", "Commands", "Fakemessage", "ToggleSilent").getString(
							"&eYour SilentMode has now been set to &6<state>");
					msg = msg.replace("<state>", state+"");
					player.sendMessage(Component.text(HexChat.translateHexCodes(msg)));
					Storage.getInstance().setAdminMessageState(player,state);
					return;
				}
			}
		}

	}

	@Override
	public boolean hasPermission(final SimpleCommand.Invocation invocation) {
		return invocation.source().hasPermission("networkjoinmessages.fakemessage");
	}

	@Override
	public List<String> suggest(final Invocation invocation) {
		List<String> commandArguments = ImmutableList.of("fakejoin","fakequit","fakeswitch","fj","fq","fs","toggle");
		String[] args = invocation.arguments();
		switch (args.length) {
			case 1:
				return commandArguments;
			case 2:
				if(args[0].equalsIgnoreCase("fs") || args[0].equalsIgnoreCase("fakeswitch")) {
					return MessageHandler.getInstance().getServerNames();
				}
			case 3:
				if(args[0].equalsIgnoreCase("fs") || args[0].equalsIgnoreCase("fakeswitch")) {
					return MessageHandler.getInstance().getServerNames();
				}
			default:
				return ImmutableList.of(VelocityMain.getInstance().getRootNode().node("Messages", "Commands", "NoMoreArgumentsNeeded").getString("No more arguments needed."));
		}
	}

}
