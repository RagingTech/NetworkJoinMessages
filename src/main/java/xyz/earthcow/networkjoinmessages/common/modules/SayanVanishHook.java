package xyz.earthcow.networkjoinmessages.common.modules;

import org.sayandev.sayanvanish.api.SayanVanishAPI;
import org.sayandev.sayanvanish.api.User;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public class SayanVanishHook {

    private final SayanVanishAPI<User> sayanVanishAPI;

    public SayanVanishHook() {
        this.sayanVanishAPI = SayanVanishAPI.getInstance();
    }

    public boolean isVanished(CorePlayer player) {
        return sayanVanishAPI.isVanished(player.getUniqueId());
    }

    public Collection<UUID> getVanishedPlayers() {
        return sayanVanishAPI.getVanishedUsers().stream().map(User::getUniqueId).collect(Collectors.toSet());
    }

}
