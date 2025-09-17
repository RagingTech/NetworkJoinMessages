package xyz.earthcow.networkjoinmessages.common.events;

/**
 * Called when a player quits the network
 */
public record NetworkQuitEvent(java.util.UUID playerUniqueId, String serverLeft, boolean isSilenced, String message,
                               String cleanMessage) {

}
