package xyz.earthcow.networkjoinmessages.common.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class H2PlayerJoinTrackerTest {

    private File tempDbFile;
    private H2PlayerJoinTracker tracker;

    @BeforeEach
    void setup() throws Exception {
        tempDbFile = Files.createTempFile("join-tracker-h2-test", ".mv.db").toFile();
        CoreLogger logger = Mockito.mock(CoreLogger.class);
        tracker = new H2PlayerJoinTracker(logger, tempDbFile.getAbsolutePath());
    }

    @AfterEach
    void tearDown() throws Exception {
        tracker.close();
        if (tempDbFile.exists()) tempDbFile.delete();
    }

    // -----------------------------------------------------------------------
    // hasJoined -- baseline
    // -----------------------------------------------------------------------

    @Test
    void hasJoined_unknownUuid_returnsFalse() {
        assertFalse(tracker.hasJoined(UUID.randomUUID()));
    }

    @Test
    void hasJoined_afterMarkAsJoined_returnsTrue() {
        UUID uuid = UUID.randomUUID();
        tracker.markAsJoined(uuid, "Notch");
        assertTrue(tracker.hasJoined(uuid));
    }

    // -----------------------------------------------------------------------
    // markAsJoined -- idempotency (MERGE semantics)
    // -----------------------------------------------------------------------

    @Test
    void markAsJoined_calledTwice_noException() {
        UUID uuid = UUID.randomUUID();
        assertDoesNotThrow(() -> {
            tracker.markAsJoined(uuid, "Notch");
            tracker.markAsJoined(uuid, "Notch");
        });
    }

    @Test
    void markAsJoined_calledTwiceSameUuid_appearsOnceInExport() {
        UUID uuid = UUID.randomUUID();
        tracker.markAsJoined(uuid, "Notch");
        tracker.markAsJoined(uuid, "NotchRenamed");

        Map<UUID, String> exported = tracker.exportAll();
        assertEquals(1, exported.size(), "MERGE must not create duplicate rows");
    }

    @Test
    void markAsJoined_calledTwiceNewName_nameIsUpdated() {
        UUID uuid = UUID.randomUUID();
        tracker.markAsJoined(uuid, "OldName");
        tracker.markAsJoined(uuid, "NewName");

        assertEquals("NewName", tracker.exportAll().get(uuid),
            "MERGE should update the player name on second call");
    }

    // -----------------------------------------------------------------------
    // markAsJoined -- multiple distinct players
    // -----------------------------------------------------------------------

    @Test
    void markAsJoined_multipleDistinctPlayers_allTracked() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        tracker.markAsJoined(uuid1, "Alpha");
        tracker.markAsJoined(uuid2, "Beta");
        tracker.markAsJoined(uuid3, "Gamma");

        assertTrue(tracker.hasJoined(uuid1));
        assertTrue(tracker.hasJoined(uuid2));
        assertTrue(tracker.hasJoined(uuid3));
    }

    // -----------------------------------------------------------------------
    // exportAll
    // -----------------------------------------------------------------------

    @Test
    void exportAll_emptyDatabase_returnsEmptyMap() {
        assertTrue(tracker.exportAll().isEmpty());
    }

    @Test
    void exportAll_returnsAllRegisteredPlayers() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        tracker.markAsJoined(uuid1, "Alpha");
        tracker.markAsJoined(uuid2, "Beta");

        Map<UUID, String> exported = tracker.exportAll();
        assertEquals(2, exported.size());
        assertEquals("Alpha", exported.get(uuid1));
        assertEquals("Beta",  exported.get(uuid2));
    }

    @Test
    void exportAll_capturesPlayerNames() {
        UUID uuid = UUID.randomUUID();
        tracker.markAsJoined(uuid, "Notch");

        assertEquals("Notch", tracker.exportAll().get(uuid));
    }

    // -----------------------------------------------------------------------
    // UUID string representation round-trip
    // -----------------------------------------------------------------------

    @Test
    void uuidRoundTrip_dashFormattedUuid_roundTripsCorrectly() {
        UUID uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        tracker.markAsJoined(uuid, "Notch");
        assertTrue(tracker.hasJoined(uuid));
        assertEquals("Notch", tracker.exportAll().get(uuid));
    }

    // -----------------------------------------------------------------------
    // Thread safety -- concurrent writes
    // -----------------------------------------------------------------------

    @Test
    void markAsJoined_concurrentCalls_noDataCorruption() throws Exception {
        int threadCount = 20;
        UUID[] uuids = new UUID[threadCount];
        for (int i = 0; i < threadCount; i++) uuids[i] = UUID.randomUUID();

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> tracker.markAsJoined(uuids[idx], "Player" + idx));
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        for (UUID uuid : uuids) {
            assertTrue(tracker.hasJoined(uuid),
                "All UUIDs should be tracked after concurrent inserts");
        }
    }

    // -----------------------------------------------------------------------
    // Large batch
    // -----------------------------------------------------------------------

    @Test
    void markAsJoined_largeNumberOfPlayers_allTracked() {
        int count = 200;
        UUID[] uuids = new UUID[count];
        for (int i = 0; i < count; i++) {
            uuids[i] = UUID.randomUUID();
            tracker.markAsJoined(uuids[i], "Player" + i);
        }
        assertEquals(count, tracker.exportAll().size());
        for (UUID uuid : uuids) {
            assertTrue(tracker.hasJoined(uuid));
        }
    }
}
