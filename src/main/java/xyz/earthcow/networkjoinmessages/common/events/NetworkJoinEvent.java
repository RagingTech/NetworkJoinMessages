package xyz.earthcow.networkjoinmessages.common.events;

/**
 * Called when a player joins the network and all respective checks have passed
 */
public record NetworkJoinEvent(java.util.UUID playerUniqueId, String serverJoined, boolean isSilenced, boolean isFirstJoin,
                               String message, String cleanMessage) {

}
