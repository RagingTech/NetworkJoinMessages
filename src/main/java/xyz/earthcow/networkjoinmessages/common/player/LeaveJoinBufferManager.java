package xyz.earthcow.networkjoinmessages.common.player;

import org.jetbrains.annotations.NotNull;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages the leave-join buffer: a grace period after a player disconnects during which
 * a subsequent reconnect suppresses the leave message.
 *
 * <p>When a player disconnects, instead of broadcasting immediately this manager schedules
 * the leave broadcast after the configured delay. If the player reconnects within that window,
 * the scheduled task is cancelled and no leave message is sent.
 */
public final class LeaveJoinBufferManager {

    private final CorePlugin plugin;
    private final PluginConfig config;

    /** Maps player UUID -> pending leave broadcast task ID */
    private final Map<UUID, Integer> pendingLeaveTasks = new ConcurrentHashMap<>();

    public LeaveJoinBufferManager(CorePlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Returns true if buffering is disabled (duration == 0).
     * In that case, callers should broadcast leave messages immediately.
     */
    public boolean isDisabled() {
        return config.getLeaveJoinBufferDuration() <= 0;
    }

    /**
     * Schedules a buffered leave broadcast for the given player.
     * If the player reconnects before the delay elapses, call {@link #cancelIfPending}.
     *
     * @param player          the disconnecting player
     * @param leaveCallback   the action to run (broadcast the leave message) after the delay
     */
    public void scheduleLeave(@NotNull CorePlayer player, @NotNull Runnable leaveCallback) {
        UUID id = player.getUniqueId();
        int taskId = plugin.runTaskAsyncLater(() -> {
            leaveCallback.run();
            pendingLeaveTasks.remove(id);
        }, config.getLeaveJoinBufferDuration());
        pendingLeaveTasks.put(id, taskId);
        plugin.getCoreLogger().debug("Leave-join buffer started for " + player.getName());
    }

    /**
     * Cancels a pending buffered leave task for the given player if one exists.
     *
     * @return true if a pending task was found and cancelled (i.e., player rejoined in time)
     */
    public boolean cancelIfPending(@NotNull CorePlayer player) {
        Integer taskId = pendingLeaveTasks.remove(player.getUniqueId());
        if (taskId == null) return false;
        plugin.cancelTask(taskId);
        plugin.getCoreLogger().debug("Leave-join buffer cancelled for " + player.getName() + " (rejoined in time)");
        return true;
    }

    /** Returns true if a leave broadcast is pending for the given player. */
    public boolean isPending(@NotNull CorePlayer player) {
        return pendingLeaveTasks.containsKey(player.getUniqueId());
    }
}
