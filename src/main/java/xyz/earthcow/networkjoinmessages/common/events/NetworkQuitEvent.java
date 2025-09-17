package xyz.earthcow.networkjoinmessages.common.events;

import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

/**
 * Called when a player quits the network
 */
public record NetworkQuitEvent(CorePlayer player, String serverLeft, boolean isSilenced, String message,
                               String cleanMessage) {

}
