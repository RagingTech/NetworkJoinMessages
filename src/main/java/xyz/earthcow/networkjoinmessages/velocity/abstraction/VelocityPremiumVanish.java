package xyz.earthcow.networkjoinmessages.velocity.abstraction;

import xyz.earthcow.networkjoinmessages.common.abstraction.PremiumVanish;

import java.util.List;
import java.util.UUID;

public class VelocityPremiumVanish implements PremiumVanish {

    public VelocityPremiumVanish() {

    }

    @Override
    public List<UUID> getInvisiblePlayers() {
        return List.of();
    }

    @Override
    public boolean isVanished(UUID uuid) {
        return false;
    }

}
