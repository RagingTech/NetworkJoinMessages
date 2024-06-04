package xyz.earthcow.networkjoinmessages.bungee.events;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

/**
 * Called when a player quits the network
 */
public class NetworkQuitEvent extends Event {

    private final ProxiedPlayer player;
    private final String serverLeft;
    private final boolean isSilenced;
    private final String message;

    public NetworkQuitEvent(
        ProxiedPlayer player,
        String serverLeft,
        boolean isSilenced,
        String message
    ) {
        this.player = player;
        this.serverLeft = serverLeft;
        this.isSilenced = isSilenced;
        this.message = message;
    }

    public ProxiedPlayer getPlayer() {
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
