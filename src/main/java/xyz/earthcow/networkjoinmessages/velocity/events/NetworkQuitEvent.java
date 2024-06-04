package xyz.earthcow.networkjoinmessages.velocity.events;

import com.velocitypowered.api.proxy.Player;

/**
 * Called when a player quits the network
 */
public class NetworkQuitEvent {

    private final Player player;
    private final String serverLeft;
    private final boolean isSilenced;
    private final String message;

    public NetworkQuitEvent(Player player, String serverLeft, boolean isSilenced, String message) {
        this.player = player;
        this.serverLeft = serverLeft;
        this.isSilenced = isSilenced;
        this.message = message;
    }

    public Player getPlayer() {
        return player;
    }

    public String getServerLeft() {
        return serverLeft;
    }

    public boolean isSilenced() {
        return isSilenced;
    }

    public String getMessage() {
        return message;
    }
}
