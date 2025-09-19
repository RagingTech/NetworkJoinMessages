package xyz.earthcow.networkjoinmessages.common.events;

import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

/**
 * Called when a player joins the network and all respective checks have passed
 */
public record NetworkJoinEvent(CorePlayer player, String serverName, String serverDisplayName, boolean isSilenced,
                               boolean isFirstJoin, String message, String cleanMessage) {

}
