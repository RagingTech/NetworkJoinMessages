package xyz.earthcow.networkjoinmessages.common.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.util.PlayerDataSnapshot;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class H2PlayerDataStoreTest {

    private File tempDbFile;
    private H2PlayerDataStore store;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = Files.createTempFile("testplayerdata", ".mv.db").toFile();
        CoreLogger logger = Mockito.mock(CoreLogger.class);
        store = new H2PlayerDataStore(logger, tempDbFile.getAbsolutePath());
    }

    @AfterEach
    void tearDown() throws Exception {
        store.close();
        if (tempDbFile.exists()) tempDbFile.delete();
    }

    // --- getData ---

    @Test
    void testGetDataUnknownUUIDReturnsNull() {
        assertNull(store.getData(UUID.randomUUID()),
            "Unknown UUID should return null");
    }

    @Test
    void testGetDataAfterSaveReturnsSnapshot() {
        UUID uuid = UUID.randomUUID();
        PlayerDataSnapshot snapshot = new PlayerDataSnapshot("Notch", true, false, null, null);
        store.saveData(uuid, snapshot);

        PlayerDataSnapshot result = store.getData(uuid);
        assertNotNull(result);
        assertEquals("Notch",      result.playerName());
        assertEquals(true,         result.silentState());
        assertEquals(false,        result.ignoreJoin());
        assertNull(result.ignoreSwap(),  "ignoreSwap should round-trip as null");
        assertNull(result.ignoreLeave(), "ignoreLeave should round-trip as null");
    }

    @Test
    void testGetDataDistinguishesNullFromFalse() {
        UUID uuid = UUID.randomUUID();
        // Store an explicit false — must not come back as null
        store.saveData(uuid, new PlayerDataSnapshot("jeb_", null, false, false, false));

        PlayerDataSnapshot result = store.getData(uuid);
        assertNotNull(result);
        assertNull(result.silentState(), "silentState should be null");
        assertEquals(false, result.ignoreJoin(),  "explicit false must not be lost");
        assertEquals(false, result.ignoreSwap(),  "explicit false must not be lost");
        assertEquals(false, result.ignoreLeave(), "explicit false must not be lost");
    }

    // --- saveData ---

    @Test
    void testSaveDataIsIdempotent() {
        UUID uuid = UUID.randomUUID();
        store.saveData(uuid, new PlayerDataSnapshot("Player1", null, null, null, null));
        store.saveData(uuid, new PlayerDataSnapshot("Player1", null, null, null, null));
        assertNotNull(store.getData(uuid), "Repeated save should not throw or corrupt data");
    }

    @Test
    void testSaveDataOverwritesExistingRow() {
        UUID uuid = UUID.randomUUID();
        store.saveData(uuid, new PlayerDataSnapshot("Player2", false, true, null, null));
        store.saveData(uuid, new PlayerDataSnapshot("Player2", true, false, null, null));

        PlayerDataSnapshot result = store.getData(uuid);
        assertNotNull(result);
        assertEquals(true,  result.silentState(), "silentState should reflect latest save");
        assertEquals(false, result.ignoreJoin(),  "ignoreJoin should reflect latest save");
    }

    @Test
    void testSaveDataUpdatesPlayerName() {
        UUID uuid = UUID.randomUUID();
        store.saveData(uuid, new PlayerDataSnapshot("OldName", null, null, null, null));
        store.saveData(uuid, new PlayerDataSnapshot("NewName", null, null, null, null));

        assertEquals("NewName", store.getData(uuid).playerName(),
            "Player name should be updated on upsert");
    }

    @Test
    void testMultiplePlayersAreIndependent() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        store.saveData(uuid1, new PlayerDataSnapshot("Alpha", true,  null, null, null));
        store.saveData(uuid2, new PlayerDataSnapshot("Beta",  false, null, null, null));

        assertEquals(true,  store.getData(uuid1).silentState());
        assertEquals(false, store.getData(uuid2).silentState());
    }

    // --- resolveUuid ---

    @Test
    void testResolveUuidUnknownNameReturnsNull() {
        assertNull(store.resolveUuid("nobody"),
            "Unknown player name should return null");
    }

    @Test
    void testResolveUuidExactMatch() {
        UUID uuid = UUID.randomUUID();
        store.saveData(uuid, new PlayerDataSnapshot("Notch", null, null, null, null));

        assertEquals(uuid, store.resolveUuid("Notch"),
            "Exact name match should return correct UUID");
    }

    @Test
    void testResolveUuidCaseInsensitive() {
        UUID uuid = UUID.randomUUID();
        store.saveData(uuid, new PlayerDataSnapshot("Notch", null, null, null, null));

        assertEquals(uuid, store.resolveUuid("notch"),
            "Name lookup should be case-insensitive");
        assertEquals(uuid, store.resolveUuid("NOTCH"),
            "Name lookup should be case-insensitive");
    }

    @Test
    void testResolveUuidReturnsLatestAfterNameUpdate() {
        UUID uuid = UUID.randomUUID();
        store.saveData(uuid, new PlayerDataSnapshot("OldName", null, null, null, null));
        store.saveData(uuid, new PlayerDataSnapshot("NewName", null, null, null, null));

        assertNull(store.resolveUuid("OldName"),
            "Old name should no longer resolve after update");
        assertEquals(uuid, store.resolveUuid("NewName"),
            "New name should resolve correctly");
    }
}
