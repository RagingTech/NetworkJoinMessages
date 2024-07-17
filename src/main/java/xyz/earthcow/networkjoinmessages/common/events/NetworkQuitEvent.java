package xyz.earthcow.networkjoinmessages.common.events;

import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

/**
 * Called when a player quits the network
 */
public class NetworkQuitEvent {

    private final CorePlayer player;
    private final String serverLeft;
    private final boolean isSilenced;
    private final String message;

    public NetworkQuitEvent(
        CorePlayer player,
        String serverLeft,
        boolean isSilenced,
        String message
    ) {
        this.player = player;
        this.serverLeft = serverLeft;
        this.isSilenced = isSilenced;
        this.message = message;
    }

    public CorePlayer getPlayer() {
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
