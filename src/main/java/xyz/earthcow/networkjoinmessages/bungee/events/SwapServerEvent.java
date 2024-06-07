package xyz.earthcow.networkjoinmessages.bungee.events;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

/**
 * Called when a player swaps from one server to another on the network
 */
public class SwapServerEvent extends Event {

    private final ProxiedPlayer player;
    private final String serverFrom;
    private final String serverTo;
    private final boolean isSilenced;
    private final String message;

    public SwapServerEvent(
        ProxiedPlayer player,
        String serverFrom,
        String serverTo,
        boolean isSilenced,
        String message
    ) {
        this.player = player;
        this.serverFrom = serverFrom;
        this.serverTo = serverTo;
        this.isSilenced = isSilenced;
        this.message = message;
    }

    public ProxiedPlayer getPlayer() {
        return player;
    }

    public String getServerFrom() {
        return serverFrom;
    }

    public String getServerTo() {
        return serverTo;
    }

    public boolean isSilenced() {
        return isSilenced;
    }

    public String getMessage() {
        return message;
    }
}
