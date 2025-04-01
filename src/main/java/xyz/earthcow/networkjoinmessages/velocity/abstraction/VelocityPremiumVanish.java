package xyz.earthcow.networkjoinmessages.velocity.abstraction;

import com.velocitypowered.api.proxy.Player;
import de.myzelyam.api.vanish.VelocityVanishAPI;
import xyz.earthcow.networkjoinmessages.common.abstraction.PremiumVanish;
import xyz.earthcow.networkjoinmessages.velocity.general.VelocityMain;

import java.util.List;
import java.util.UUID;

public class VelocityPremiumVanish implements PremiumVanish {

    @Override
    public List<UUID> getInvisiblePlayers() {
        return VelocityVanishAPI.getInvisiblePlayers();
    }

    @Override
    public boolean isVanished(UUID uuid) {
        Player player = VelocityMain.getInstance().getProxy().getPlayer(uuid).orElse(null);
        if (player == null) {
            return false;
        }
        return VelocityVanishAPI.isInvisible(player);
    }

}
