package xyz.earthcow.networkjoinmessages.common.events;

import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

/**
 * Called when a player swaps from one server to another on the network
 */
public class SwapServerEvent {

    private final CorePlayer player;
    private final String serverFrom;
    private final String serverTo;
    private final boolean isSilenced;
    private final String message;

    public SwapServerEvent(
        CorePlayer player,
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

    public CorePlayer getPlayer() {
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
