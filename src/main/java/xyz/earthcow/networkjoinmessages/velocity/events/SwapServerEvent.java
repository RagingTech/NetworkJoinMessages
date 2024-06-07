package xyz.earthcow.networkjoinmessages.velocity.events;

import com.velocitypowered.api.proxy.Player;

/**
 * Called when a player swaps from one server to another on the network
 */
public class SwapServerEvent {

    private final Player player;
    private final String serverFrom;
    private final String serverTo;
    private final boolean isSilenced;
    private final String message;

    public SwapServerEvent(
        Player player,
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

    public Player getPlayer() {
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
