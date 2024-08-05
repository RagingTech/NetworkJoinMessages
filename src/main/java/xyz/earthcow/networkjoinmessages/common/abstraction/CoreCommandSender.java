package xyz.earthcow.networkjoinmessages.common.abstraction;

public interface CoreCommandSender {
    String getName();
    void sendMessage(String message);
    boolean hasPermission(String permission);
}
