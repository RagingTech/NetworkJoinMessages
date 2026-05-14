package xyz.earthcow.networkjoinmessages.common.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeaveJoinBufferManagerTest {

    @Mock private CorePlugin   plugin;
    @Mock private CoreLogger   logger;
    @Mock private PluginConfig config;
    @Mock private CorePlayer   player;

    private final UUID playerUuid = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(plugin.getCoreLogger()).thenReturn(logger);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getName()).thenReturn("TestPlayer");
    }

    // -----------------------------------------------------------------------
    // isDisabled
    // -----------------------------------------------------------------------

    @Test
    void isDisabled_zeroDuration_returnsTrue() {
        when(config.getLeaveJoinBufferDuration()).thenReturn(0);
        LeaveJoinBufferManager mgr = new LeaveJoinBufferManager(plugin, config);
        assertTrue(mgr.isDisabled());
    }

    @Test
    void isDisabled_positiveDuration_returnsFalse() {
        when(config.getLeaveJoinBufferDuration()).thenReturn(5000);
        LeaveJoinBufferManager mgr = new LeaveJoinBufferManager(plugin, config);
        assertFalse(mgr.isDisabled());
    }

    @Test
    void isDisabled_negativeDuration_returnsTrue() {
        when(config.getLeaveJoinBufferDuration()).thenReturn(-1);
        LeaveJoinBufferManager mgr = new LeaveJoinBufferManager(plugin, config);
        assertTrue(mgr.isDisabled());
    }

    // -----------------------------------------------------------------------
    // isPending -- before any schedule
    // -----------------------------------------------------------------------

    @Test
    void isPending_noTaskScheduled_returnsFalse() {
        when(config.getLeaveJoinBufferDuration()).thenReturn(5000);
        LeaveJoinBufferManager mgr = new LeaveJoinBufferManager(plugin, config);
        assertFalse(mgr.isPending(player));
    }

    // -----------------------------------------------------------------------
    // scheduleLeave
    // -----------------------------------------------------------------------

    @Test
    void scheduleLeave_schedulesAsyncTask() {
        when(config.getLeaveJoinBufferDuration()).thenReturn(5000);
        when(plugin.runTaskAsyncLater(any(), eq(5000))).thenReturn(42);

        LeaveJoinBufferManager mgr = new LeaveJoinBufferManager(plugin, config);
        mgr.scheduleLeave(player, () -> {});

        verify(plugin).runTaskAsyncLater(any(), eq(5000));
    }

    @Test
    void scheduleLeave_afterSchedule_isPendingReturnsTrue() {
        when(config.getLeaveJoinBufferDuration()).thenReturn(5000);
        when(plugin.runTaskAsyncLater(any(), anyInt())).thenReturn(42);

        LeaveJoinBufferManager mgr = new LeaveJoinBufferManager(plugin, config);
        mgr.scheduleLeave(player, () -> {});

        assertTrue(mgr.isPending(player));
    }

    @Test
    void scheduleLeave_whenTaskFires_callbackIsInvoked() {
        when(config.getLeaveJoinBufferDuration()).thenReturn(5000);

        // Capture the Runnable so we can fire it manually
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(plugin.runTaskAsyncLater(runnableCaptor.capture(), anyInt())).thenReturn(42);

        AtomicBoolean callbackFired = new AtomicBoolean(false);
        LeaveJoinBufferManager mgr = new LeaveJoinBufferManager(plugin, config);
        mgr.scheduleLeave(player, () -> callbackFired.set(true));

        // Simulate the scheduler firing
        runnableCaptor.getValue().run();

        assertTrue(callbackFired.get(), "Leave callback should have been invoked");
    }

    @Test
    void scheduleLeave_whenTaskFires_pendingEntryIsRemoved() {
        when(config.getLeaveJoinBufferDuration()).thenReturn(5000);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(plugin.runTaskAsyncLater(runnableCaptor.capture(), anyInt())).thenReturn(42);

        LeaveJoinBufferManager mgr = new LeaveJoinBufferManager(plugin, config);
        mgr.scheduleLeave(player, () -> {});

        runnableCaptor.getValue().run();

        assertFalse(mgr.isPending(player),
            "Pending entry should be removed after the task fires");
    }

    // -----------------------------------------------------------------------
    // cancelIfPending
    // -----------------------------------------------------------------------

    @Test
    void cancelIfPending_noPendingTask_returnsFalse() {
        when(config.getLeaveJoinBufferDuration()).thenReturn(5000);
        LeaveJoinBufferManager mgr = new LeaveJoinBufferManager(plugin, config);
        assertFalse(mgr.cancelIfPending(player));
    }

    @Test
    void cancelIfPending_hasPendingTask_returnsTrueAndCancels() {
        when(config.getLeaveJoinBufferDuration()).thenReturn(5000);
        when(plugin.runTaskAsyncLater(any(), anyInt())).thenReturn(99);

        LeaveJoinBufferManager mgr = new LeaveJoinBufferManager(plugin, config);
        mgr.scheduleLeave(player, () -> {});

        boolean result = mgr.cancelIfPending(player);

        assertTrue(result);
        verify(plugin).cancelTask(99);
    }

    @Test
    void cancelIfPending_hasPendingTask_removesPendingEntry() {
        when(config.getLeaveJoinBufferDuration()).thenReturn(5000);
        when(plugin.runTaskAsyncLater(any(), anyInt())).thenReturn(99);

        LeaveJoinBufferManager mgr = new LeaveJoinBufferManager(plugin, config);
        mgr.scheduleLeave(player, () -> {});
        mgr.cancelIfPending(player);

        assertFalse(mgr.isPending(player));
    }

    @Test
    void cancelIfPending_calledTwice_secondCallReturnsFalse() {
        when(config.getLeaveJoinBufferDuration()).thenReturn(5000);
        when(plugin.runTaskAsyncLater(any(), anyInt())).thenReturn(99);

        LeaveJoinBufferManager mgr = new LeaveJoinBufferManager(plugin, config);
        mgr.scheduleLeave(player, () -> {});

        assertTrue(mgr.cancelIfPending(player));
        assertFalse(mgr.cancelIfPending(player), "Second cancel on same player should return false");
    }

    // -----------------------------------------------------------------------
    // Multiple players -- independence
    // -----------------------------------------------------------------------

    @Test
    void scheduleLeave_multiplePlayers_areTrackedIndependently() {
        CorePlayer player2 = mock(CorePlayer.class);
        UUID uuid2 = UUID.randomUUID();
        when(player2.getUniqueId()).thenReturn(uuid2);
        when(player2.getName()).thenReturn("OtherPlayer");

        when(config.getLeaveJoinBufferDuration()).thenReturn(5000);
        when(plugin.runTaskAsyncLater(any(), anyInt())).thenReturn(1, 2);

        LeaveJoinBufferManager mgr = new LeaveJoinBufferManager(plugin, config);
        mgr.scheduleLeave(player,  () -> {});
        mgr.scheduleLeave(player2, () -> {});

        assertTrue(mgr.isPending(player));
        assertTrue(mgr.isPending(player2));

        mgr.cancelIfPending(player);

        assertFalse(mgr.isPending(player));
        assertTrue(mgr.isPending(player2), "Cancelling player1 must not affect player2");
    }

    // -----------------------------------------------------------------------
    // scheduleLeave -- second call for same player overwrites first
    // -----------------------------------------------------------------------

    @Test
    void scheduleLeave_calledTwiceForSamePlayer_overwritesTaskId() {
        when(config.getLeaveJoinBufferDuration()).thenReturn(5000);
        when(plugin.runTaskAsyncLater(any(), anyInt())).thenReturn(10, 20);

        LeaveJoinBufferManager mgr = new LeaveJoinBufferManager(plugin, config);
        mgr.scheduleLeave(player, () -> {});
        mgr.scheduleLeave(player, () -> {}); // second schedule

        // cancelIfPending should cancel the LAST registered task (20)
        mgr.cancelIfPending(player);
        verify(plugin).cancelTask(20);
    }
}
