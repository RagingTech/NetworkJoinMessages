package tv.tirco.bungeejoin.events;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

/**
 * Called when a player joins the network and all respective checks have passed
 */
public class NetworkJoinEvent extends Event{
	
	private final ProxiedPlayer player;
	private final String serverJoined;
	private final boolean isSilenced;
	private final String message;
	
	public NetworkJoinEvent(ProxiedPlayer player, String serverJoined, boolean isSilenced, String message) {
		this.player = player;
		this.serverJoined = serverJoined;
		this.isSilenced = isSilenced;
		this.message = message;
	}
	
	public ProxiedPlayer getPlayer() {
		return player;
	}
	
	public String getServerJoined() {
		return serverJoined;
	}
	
	public boolean isSilenced() {
		return isSilenced;
	}
	
	public String getMessage() {
		return message;
	}
}
