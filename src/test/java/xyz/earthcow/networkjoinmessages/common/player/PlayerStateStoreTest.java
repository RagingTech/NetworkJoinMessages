package xyz.earthcow.networkjoinmessages.common.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import xyz.earthcow.networkjoinmessages.common.MessageType;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.storage.PlayerDataStore;
import xyz.earthcow.networkjoinmessages.common.util.PlayerDataSnapshot;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlayerStateStoreTest {

    @Mock private CorePlugin      plugin;
    @Mock private PluginConfig    config;
    @Mock private PlayerDataStore store;
    @Mock private CorePlayer      player;

    private final UUID playerUuid = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getName()).thenReturn("TestPlayer");

        // Default: all "ignore by default" flags OFF, silent default OFF
        when(config.isIgnoreJoinByDefault()).thenReturn(false);
        when(config.isIgnoreSwapByDefault()).thenReturn(false);
        when(config.isIgnoreLeaveByDefault()).thenReturn(false);
        when(config.isSilentJoinDefaultState()).thenReturn(false);

        // Async tasks: execute synchronously for testing
        doAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; })
            .when(plugin).runTaskAsync(any());
    }

    // -----------------------------------------------------------------------
    // loadData -- new player (null snapshot from store)
    // -----------------------------------------------------------------------

    @Test
    void loadData_newPlayer_storeReturnsNull_nothingAddedToSuppression() {
        when(store.getData(playerUuid)).thenReturn(null);
        when(config.isIgnoreJoinByDefault()).thenReturn(false);
        when(config.isIgnoreSwapByDefault()).thenReturn(false);
        when(config.isIgnoreLeaveByDefault()).thenReturn(false);

        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.loadData(playerUuid, "TestPlayer");

        // Messages should NOT be suppressed
        assertFalse(stateStore.getSuppressedPlayers(MessageType.JOIN).contains(playerUuid));
        assertFalse(stateStore.getSuppressedPlayers(MessageType.SWAP).contains(playerUuid));
        assertFalse(stateStore.getSuppressedPlayers(MessageType.LEAVE).contains(playerUuid));
    }

    @Test
    void loadData_newPlayer_ignoreJoinByDefault_suppressesJoin() {
        when(store.getData(playerUuid)).thenReturn(null);
        when(config.isIgnoreJoinByDefault()).thenReturn(true);

        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.loadData(playerUuid, "TestPlayer");

        assertTrue(stateStore.getSuppressedPlayers(MessageType.JOIN).contains(playerUuid));
    }

    @Test
    void loadData_newPlayer_ignoreSwapByDefault_suppressesSwap() {
        when(store.getData(playerUuid)).thenReturn(null);
        when(config.isIgnoreSwapByDefault()).thenReturn(true);

        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.loadData(playerUuid, "TestPlayer");

        assertTrue(stateStore.getSuppressedPlayers(MessageType.SWAP).contains(playerUuid));
    }

    @Test
    void loadData_newPlayer_ignoreLeaveByDefault_suppressesLeave() {
        when(store.getData(playerUuid)).thenReturn(null);
        when(config.isIgnoreLeaveByDefault()).thenReturn(true);

        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.loadData(playerUuid, "TestPlayer");

        assertTrue(stateStore.getSuppressedPlayers(MessageType.LEAVE).contains(playerUuid));
    }

    @Test
    void loadData_newPlayer_savesInitialRecord() {
        when(store.getData(playerUuid)).thenReturn(null);

        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.loadData(playerUuid, "TestPlayer");

        verify(store).saveData(eq(playerUuid), any(PlayerDataSnapshot.class));
    }

    // -----------------------------------------------------------------------
    // loadData -- returning player with stored snapshot
    // -----------------------------------------------------------------------

    @Test
    void loadData_returningPlayer_silentStateRestored() {
        PlayerDataSnapshot snapshot = new PlayerDataSnapshot("TestPlayer", true, null, null, null);
        when(store.getData(playerUuid)).thenReturn(snapshot);

        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.loadData(playerUuid, "TestPlayer");

        // Grant the permission so getSilentState checks the map
        when(player.hasPermission("networkjoinmessages.silent")).thenReturn(true);
        assertTrue(stateStore.getSilentState(player));
    }

    @Test
    void loadData_returningPlayer_nullSilentState_usesDefault() {
        PlayerDataSnapshot snapshot = new PlayerDataSnapshot("TestPlayer", null, null, null, null);
        when(store.getData(playerUuid)).thenReturn(snapshot);
        when(config.isSilentJoinDefaultState()).thenReturn(false);

        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.loadData(playerUuid, "TestPlayer");

        when(player.hasPermission("networkjoinmessages.silent")).thenReturn(true);
        assertFalse(stateStore.getSilentState(player));
    }

    @Test
    void loadData_returningPlayer_explicitIgnoreJoinTrue_suppressesJoin() {
        PlayerDataSnapshot snapshot = new PlayerDataSnapshot("TestPlayer", null, true, null, null);
        when(store.getData(playerUuid)).thenReturn(snapshot);

        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.loadData(playerUuid, "TestPlayer");

        assertTrue(stateStore.getSuppressedPlayers(MessageType.JOIN).contains(playerUuid));
    }

    @Test
    void loadData_returningPlayer_explicitIgnoreJoinFalse_doesNotSuppressEvenWithDefault() {
        when(config.isIgnoreJoinByDefault()).thenReturn(true); // default says suppress
        PlayerDataSnapshot snapshot = new PlayerDataSnapshot("TestPlayer", null, false, null, null);
        when(store.getData(playerUuid)).thenReturn(snapshot);

        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.loadData(playerUuid, "TestPlayer");

        // Explicit false overrides the default
        assertFalse(stateStore.getSuppressedPlayers(MessageType.JOIN).contains(playerUuid));
    }

    // -----------------------------------------------------------------------
    // getSilentState -- permission guard
    // -----------------------------------------------------------------------

    @Test
    void getSilentState_playerLacksPermission_returnsFalseRegardlessOfStoredState() {
        when(player.hasPermission("networkjoinmessages.silent")).thenReturn(false);

        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        // Even if the map had true stored for this UUID, no permission => false
        assertFalse(stateStore.getSilentState(player));
    }

    @Test
    void getSilentState_playerHasPermission_firstAccessUsesDefault() {
        when(player.hasPermission("networkjoinmessages.silent")).thenReturn(true);
        when(config.isSilentJoinDefaultState()).thenReturn(true);

        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        assertTrue(stateStore.getSilentState(player));
    }

    // -----------------------------------------------------------------------
    // setSilentState
    // -----------------------------------------------------------------------

    @Test
    void setSilentState_updatesStateAndPersists() {
        when(player.hasPermission("networkjoinmessages.silent")).thenReturn(true);

        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.setSilentState(player, true);

        assertTrue(stateStore.getSilentState(player));
        verify(store).saveData(eq(playerUuid), any(PlayerDataSnapshot.class));
    }

    @Test
    void setSilentState_falseUpdatesAndPersists() {
        when(player.hasPermission("networkjoinmessages.silent")).thenReturn(true);

        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.setSilentState(player, true);
        stateStore.setSilentState(player, false);

        assertFalse(stateStore.getSilentState(player));
    }

    // -----------------------------------------------------------------------
    // setSendMessageState -- by string type
    // -----------------------------------------------------------------------

    @Test
    void setSendMessageState_allFalse_suppressesAllTypes() {
        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.setSendMessageState("all", player, false);

        assertTrue(stateStore.getSuppressedPlayers(MessageType.JOIN).contains(playerUuid));
        assertTrue(stateStore.getSuppressedPlayers(MessageType.SWAP).contains(playerUuid));
        assertTrue(stateStore.getSuppressedPlayers(MessageType.LEAVE).contains(playerUuid));
    }

    @Test
    void setSendMessageState_allTrue_removesAllSuppression() {
        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.setSendMessageState("all", player, false); // first suppress
        stateStore.setSendMessageState("all", player, true);  // then re-enable

        assertFalse(stateStore.getSuppressedPlayers(MessageType.JOIN).contains(playerUuid));
        assertFalse(stateStore.getSuppressedPlayers(MessageType.SWAP).contains(playerUuid));
        assertFalse(stateStore.getSuppressedPlayers(MessageType.LEAVE).contains(playerUuid));
    }

    @Test
    void setSendMessageState_joinOnly_onlyJoinSuppressed() {
        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.setSendMessageState("join", player, false);

        assertTrue(stateStore.getSuppressedPlayers(MessageType.JOIN).contains(playerUuid));
        assertFalse(stateStore.getSuppressedPlayers(MessageType.SWAP).contains(playerUuid));
        assertFalse(stateStore.getSuppressedPlayers(MessageType.LEAVE).contains(playerUuid));
    }

    @Test
    void setSendMessageState_leaveOnly_onlyLeaveSuppressed() {
        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.setSendMessageState("leave", player, false);

        assertFalse(stateStore.getSuppressedPlayers(MessageType.JOIN).contains(playerUuid));
        assertFalse(stateStore.getSuppressedPlayers(MessageType.SWAP).contains(playerUuid));
        assertTrue(stateStore.getSuppressedPlayers(MessageType.LEAVE).contains(playerUuid));
    }

    @Test
    void setSendMessageState_swapOnly_onlySwapSuppressed() {
        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.setSendMessageState("swap", player, false);

        assertFalse(stateStore.getSuppressedPlayers(MessageType.JOIN).contains(playerUuid));
        assertTrue(stateStore.getSuppressedPlayers(MessageType.SWAP).contains(playerUuid));
        assertFalse(stateStore.getSuppressedPlayers(MessageType.LEAVE).contains(playerUuid));
    }

    @Test
    void setSendMessageState_persistsToStore() {
        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.setSendMessageState("join", player, false);

        verify(store).saveData(eq(playerUuid), any(PlayerDataSnapshot.class));
    }

    // -----------------------------------------------------------------------
    // getSuppressedPlayers -- FIRST_JOIN maps to JOIN set
    // -----------------------------------------------------------------------

    @Test
    void getSuppressedPlayers_firstJoinMapsToJoinSet() {
        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.setSendMessageState("join", player, false);

        Set<UUID> joinSuppressed      = stateStore.getSuppressedPlayers(MessageType.JOIN);
        Set<UUID> firstJoinSuppressed = stateStore.getSuppressedPlayers(MessageType.FIRST_JOIN);
        assertSame(joinSuppressed, firstJoinSuppressed,
            "FIRST_JOIN should return the same set as JOIN");
    }

    // -----------------------------------------------------------------------
    // getFrom / setFrom
    // -----------------------------------------------------------------------

    @Test
    void setFrom_storesServerName() {
        // This is only added so that player.getCurrentServer().getName() doesn't throw a NPE
        // The only time player#getCurrentServer can possibly be null is the brief time between when they initially
        // connect to when the server connected event is fired and CorePlayerListener#handleJoin is run
        var mockServer = mock(xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer.class);
        when(player.getCurrentServer()).thenReturn(mockServer);
        when(mockServer.getName()).thenReturn("");

        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        stateStore.setFrom(player, "lobby");
        assertEquals("lobby", stateStore.getFrom(player));
    }

    @Test
    void getFrom_noEntryStored_fallsBackToCurrentServer() {
        var mockServer = mock(xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer.class);
        when(player.getCurrentServer()).thenReturn(mockServer);
        when(mockServer.getName()).thenReturn("survival");

        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, store);
        assertEquals("survival", stateStore.getFrom(player));
    }

    // -----------------------------------------------------------------------
    // null store -- no NPE
    // -----------------------------------------------------------------------

    @Test
    void loadData_nullStore_doesNotThrow() {
        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, null);
        assertDoesNotThrow(() -> stateStore.loadData(playerUuid, "TestPlayer"));
    }

    @Test
    void setSendMessageState_nullStore_doesNotThrow() {
        PlayerStateStore stateStore = new PlayerStateStore(plugin, config, null);
        assertDoesNotThrow(() -> stateStore.setSendMessageState("join", player, false));
    }
}
