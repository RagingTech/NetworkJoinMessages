package xyz.earthcow.networkjoinmessages.common.events;

import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

/**
 * Called when a player leaves the network
 */
public record NetworkLeaveEvent(CorePlayer player, String serverName, String serverDisplayName, boolean isSilenced,
                                String message, String cleanMessage) {

}
