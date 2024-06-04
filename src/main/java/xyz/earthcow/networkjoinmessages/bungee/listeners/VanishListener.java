package xyz.earthcow.networkjoinmessages.bungee.listeners;

import de.myzelyam.api.vanish.BungeePlayerHideEvent;
import de.myzelyam.api.vanish.BungeePlayerShowEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import xyz.earthcow.networkjoinmessages.bungee.general.BungeeMain;
import xyz.earthcow.networkjoinmessages.bungee.general.Storage;

public class VanishListener implements Listener{

	@EventHandler
	public void playerHideEvent(BungeePlayerHideEvent e) {
		ProxiedPlayer player = e.getPlayer();
		//Main.getInstance().getLogger().info("playerHideEvent triggered");
		if(BungeeMain.getInstance().getConfig().getBoolean("OtherPlugins.PremiumVanish.ToggleFakemessageWhenVanishing",true)){
			if(player.hasPermission("bungeejoinmessages.silent")) {
				Storage.getInstance().setAdminMessageState(player, true);
				BungeeMain.getInstance().getLogger().info("PremiumVanish has toggled the MessageState of " + player.getDisplayName() + " to true");
			}
		}
	}
	
	@EventHandler
	public void playerShowEvent(BungeePlayerShowEvent e) {
		ProxiedPlayer player = e.getPlayer();
		BungeeMain.getInstance().getLogger().info("playerShowEvent triggered");
		if(BungeeMain.getInstance().getConfig().getBoolean("OtherPlugins.PremiumVanish.ToggleFakemessageWhenVanishing",true)){
			if(player.hasPermission("bungeejoinmessages.silent")) {
				Storage.getInstance().setAdminMessageState(player, false);
				BungeeMain.getInstance().getLogger().info("PremiumVanish has toggled the MessageState of " + player.getDisplayName() + " to false");
			}
		}
	}
}
