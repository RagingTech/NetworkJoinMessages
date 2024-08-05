package xyz.earthcow.networkjoinmessages.common.commands;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;

import java.util.List;

public interface Command {

    void execute(CoreCommandSender coreCommandSender, String[] args);

    String getRequiredPermssion();

    List<String> getTabCompletion(CoreCommandSender coreCommandSender, String[] args);

}
