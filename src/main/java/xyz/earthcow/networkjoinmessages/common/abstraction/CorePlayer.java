package xyz.earthcow.networkjoinmessages.common.abstraction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface CorePlayer extends CoreCommandSender {
    @NotNull
    UUID getUniqueId();
    @Nullable
    CoreBackendServer getCurrentServer();
    @Nullable
    CoreBackendServer getLastKnownConnectedServer();
    void setLastKnownConnectedServer(CoreBackendServer server);
}
