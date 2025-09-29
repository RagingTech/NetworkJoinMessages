package xyz.earthcow.networkjoinmessages.common.abstraction;

import net.kyori.adventure.text.Component;

public interface CoreCommandSender {
    String getName();
    void sendMessage(Component component);
    boolean hasPermission(String permission);
}
