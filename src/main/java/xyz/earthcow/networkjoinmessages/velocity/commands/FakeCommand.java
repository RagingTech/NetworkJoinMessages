package xyz.earthcow.networkjoinmessages.bungee.commands;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import xyz.earthcow.networkjoinmessages.bungee.general.Main;
import xyz.earthcow.networkjoinmessages.bungee.general.Storage;
import xyz.earthcow.networkjoinmessages.bungee.util.HexChat;
import xyz.earthcow.networkjoinmessages.bungee.util.MessageHandler;

public class FakeCommand extends Command implements TabExecutor{

	//Personal note: I hate the way commands are set up with bungee.
    public FakeCommand() {
        super("fakemessage","bungeejoinmessages.fakemessage","fm");
   }
    
    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if(!player.hasPermission("bungeejoinmessages.fakemessage")) {
            	return;
            }
            if(args.length < 1) {
            	String msg =  Main.getInstance().getConfig().getString("Messages.Commands.Fakemessage.NoArgument",     			
            			"&6Arguments:\n"
            			+ "- &cfakejoin\n"
            			+ "- &cfakequit\n"
            			+ "- &cfakeswitch&6 (to) (from)\n");
            	TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', msg));
            	player.sendMessage(message);
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
                    	String msg =  Main.getInstance().getConfig().getString("Messages.Commands.Fakemessage.FakeSwitchNoArgument",     					
                    			"&6Arguments:\n"
                    			+ "- &cfakejoin\n"
                    			+ "- &cfakequit\n"
                    			+ "- &cfakeswitch&6 (to) (from)\n"
                    			+ "&4Error: Please specify &cTO&4 and &cFROM");
                    	TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', msg));
                    	player.sendMessage(message);
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
            		if(!player.hasPermission("bungeejoinmessages.silent")){
            			msg = Main.getInstance().getConfig().getString("Messages.Commands.Fakemessage.ToggleSilentNoPerm", 
            					"&cYou do not have the permission to join the server silently.");
            			TextComponent message = new TextComponent(HexChat.translateHexCodes( msg));
                    	player.sendMessage(message);
                    	return;
            		} else {
            			Boolean state = !Storage.getInstance().getAdminMessageState(player);
            			msg = Main.getInstance().getConfig().getString("Messages.Commands.Fakemessage.ToggleSilent", 
            					"&eYour SilentMode has now been set to &6<state>");
            			msg = msg.replace("<state>", state+"");
            			TextComponent message = new TextComponent(HexChat.translateHexCodes( msg));
                    	player.sendMessage(message);
            			Storage.getInstance().setAdminMessageState(player,state);
            			return;
            		}
            	}
            }
        }
    }

	@Override
	public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
		List<String> commandArguments = ImmutableList.of("fakejoin","fakequit","fakeswitch","fj","fq","fs","toggle");
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
			return ImmutableList.of(Main.getInstance().getConfig().getString("Messages.Commands.NoMoreArgumentsNeeded","No more arguments needed."));
    	}
	}

}
