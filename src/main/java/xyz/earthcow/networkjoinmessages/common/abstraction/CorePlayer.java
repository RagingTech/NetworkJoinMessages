package xyz.earthcow.networkjoinmessages.common.abstraction;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter @Setter
public abstract class CorePlayer implements CoreCommandSender {
    // Fields
    private volatile boolean connected = false;
    private CoreBackendServer lastKnownConnectedServer;
    private volatile boolean disconnecting = false;
    private String cachedLeaveMessage;
    private Audience audience;
    private boolean premiumVanishHidden = false;
    private int premiumVanishUseLevel = 0;
    private int premiumVanishSeeLevel = 0;

    public CorePlayer(CoreBackendServer lastKnownConnectedServer, Audience audience) {
        this.lastKnownConnectedServer = lastKnownConnectedServer;
        this.audience = audience;
    }

    // Abstract
    @NotNull
    public abstract UUID getUniqueId();
    public abstract int getConnectionIdentity();
    @Nullable
    public abstract CoreBackendServer getCurrentServer();
    public abstract boolean isInLimbo();
}
