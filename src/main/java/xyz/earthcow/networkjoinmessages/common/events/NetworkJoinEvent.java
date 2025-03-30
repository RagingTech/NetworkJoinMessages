package xyz.earthcow.networkjoinmessages.common.events;

import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

/**
 * Called when a player joins the network and all respective checks have passed
 */
public class NetworkJoinEvent {

    private final CorePlayer player;
    private final String serverJoined;
    private final boolean isSilenced;
    private final String message;
    private final String cleanMessage;

    public NetworkJoinEvent(
        CorePlayer player,
        String serverJoined,
        boolean isSilenced,
        String message,
        String cleanMessage
    ) {
        this.player = player;
        this.serverJoined = serverJoined;
        this.isSilenced = isSilenced;
        this.message = message;
        this.cleanMessage = cleanMessage;
    }

    public CorePlayer getPlayer() {
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

    public String getCleanMessage() {
        return cleanMessage;
    }
}
