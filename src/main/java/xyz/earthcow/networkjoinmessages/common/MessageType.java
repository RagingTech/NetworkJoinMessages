package xyz.earthcow.networkjoinmessages.common;

/**
 * Represents the kind of network event that produced a message.
 * Used throughout the broadcast and player-state layers to route
 * messages and select the appropriate suppression lists.
 */
public enum MessageType {
    FIRST_JOIN,
    JOIN,
    SWAP,
    LEAVE
}
