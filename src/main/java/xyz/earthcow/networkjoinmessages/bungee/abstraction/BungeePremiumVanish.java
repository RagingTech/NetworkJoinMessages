package xyz.earthcow.networkjoinmessages.bungee.abstraction;

import de.myzelyam.api.vanish.BungeeVanishAPI;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import xyz.earthcow.networkjoinmessages.bungee.general.BungeeMain;
import xyz.earthcow.networkjoinmessages.common.abstraction.PremiumVanish;

import java.util.List;
import java.util.UUID;

public class BungeePremiumVanish implements PremiumVanish {

    @Override
    public List<UUID> getInvisiblePlayers() {
        return BungeeVanishAPI.getInvisiblePlayers();
    }

    @Override
    public boolean isVanished(UUID uuid) {
        ProxiedPlayer player = BungeeMain.getInstance().getProxy().getPlayer(uuid);
        if (player == null) {
            return false;
        }
        return BungeeVanishAPI.isInvisible(player);
    }

}
