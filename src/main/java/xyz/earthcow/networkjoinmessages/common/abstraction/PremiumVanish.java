package xyz.earthcow.networkjoinmessages.common.abstraction;

import java.util.List;
import java.util.UUID;

public interface PremiumVanish {

    List<UUID> getInvisiblePlayers();

    boolean isVanished(UUID uuid);

}
