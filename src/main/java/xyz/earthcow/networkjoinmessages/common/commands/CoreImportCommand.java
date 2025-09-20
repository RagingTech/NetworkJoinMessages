package xyz.earthcow.networkjoinmessages.common.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.util.H2PlayerJoinTracker;

import java.util.List;

public class CoreImportCommand implements Command {

    private final H2PlayerJoinTracker playerJoinTracker;

    public CoreImportCommand(H2PlayerJoinTracker playerJoinTracker) {
        this.playerJoinTracker = playerJoinTracker;
    }

    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (args.length < 1) {
            coreCommandSender.sendMessage(Component.text("You must specify the path(s) to the usercache.json file(s)", NamedTextColor.RED));
            return;
        }

        for (String arg : args) {
            if (!playerJoinTracker.addUsersFromUserCache(arg)) {
                coreCommandSender.sendMessage(Component.text("Failed to import users from " + arg, NamedTextColor.RED));
            } else {
                coreCommandSender.sendMessage(Component.text("Successfully imported users from " + arg, NamedTextColor.GREEN));
            }
        }

    }

    @Override
    public String getRequiredPermission() {
        return "networkjoinmessages.import";
    }

    @Override
    public List<String> getTabCompletion(CoreCommandSender coreCommandSender, String[] args) {
        return List.of("path/to/usercache.json");
    }


}
