package xyz.earthcow.networkjoinmessages.common.abstraction;

import java.util.List;

public interface CoreBackendServer {
    String getName();
    List<CorePlayer> getPlayersConnected();
}
