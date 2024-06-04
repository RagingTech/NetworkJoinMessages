package xyz.earthcow.networkjoinmessages.velocity.events;

import com.velocitypowered.api.proxy.Player;

/**
 * Called when a player joins the network and all respective checks have passed
 */
public class NetworkJoinEvent {
	
	private final Player player;
	private final String serverJoined;
	private final boolean isSilenced;
	private final String message;
	
	public NetworkJoinEvent(Player player, String serverJoined, boolean isSilenced, String message) {
		this.player = player;
		this.serverJoined = serverJoined;
		this.isSilenced = isSilenced;
		this.message = message;
	}
	
	public Player getPlayer() {
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
