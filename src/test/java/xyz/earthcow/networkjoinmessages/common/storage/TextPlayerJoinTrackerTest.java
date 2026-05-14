package xyz.earthcow.networkjoinmessages.common.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TextPlayerJoinTrackerTest {

    private Path tempFile;
    private CoreLogger logger;

    @BeforeEach
    void setup() throws Exception {
        tempFile = Files.createTempFile("join-tracker-test", ".txt");
        // Remove file so the tracker creates its own header
        Files.deleteIfExists(tempFile);
        logger = Mockito.mock(CoreLogger.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(tempFile);
    }

    // -----------------------------------------------------------------------
    // Initialization -- file creation
    // -----------------------------------------------------------------------

    @Test
    void init_fileDoesNotExist_createsFile() throws Exception {
        new TextPlayerJoinTracker(logger, tempFile);
        assertTrue(Files.exists(tempFile), "Tracker should create the file if absent");
    }

    @Test
    void init_existingFile_doesNotThrow() throws Exception {
        Files.writeString(tempFile, "# Comment\n");
        assertDoesNotThrow(() -> new TextPlayerJoinTracker(logger, tempFile));
    }

    // -----------------------------------------------------------------------
    // hasJoined -- fresh tracker
    // -----------------------------------------------------------------------

    @Test
    void hasJoined_unknownUuid_returnsFalse() throws Exception {
        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
        assertFalse(tracker.hasJoined(UUID.randomUUID()));
    }

    @Test
    void hasJoined_afterMarkAsJoined_returnsTrue() throws Exception {
        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
        UUID uuid = UUID.randomUUID();
        tracker.markAsJoined(uuid, "Notch");
        assertTrue(tracker.hasJoined(uuid));
    }

    // -----------------------------------------------------------------------
    // markAsJoined -- idempotency
    // -----------------------------------------------------------------------

    @Test
    void markAsJoined_calledTwice_noException() throws Exception {
        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
        UUID uuid = UUID.randomUUID();
        assertDoesNotThrow(() -> {
            tracker.markAsJoined(uuid, "Notch");
            tracker.markAsJoined(uuid, "Notch");
        });
    }

    @Test
    void markAsJoined_calledTwice_appearsOnceInExport() throws Exception {
        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
        UUID uuid = UUID.randomUUID();
        tracker.markAsJoined(uuid, "Notch");
        tracker.markAsJoined(uuid, "Notch"); // duplicate call
        assertEquals(1, tracker.exportAll().size());
    }

    // -----------------------------------------------------------------------
    // markAsJoined -- multiple players
    // -----------------------------------------------------------------------

    @Test
    void markAsJoined_multipleDistinctPlayers_allTracked() throws Exception {
        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        tracker.markAsJoined(uuid1, "Alpha");
        tracker.markAsJoined(uuid2, "Beta");

        assertTrue(tracker.hasJoined(uuid1));
        assertTrue(tracker.hasJoined(uuid2));
    }

    // -----------------------------------------------------------------------
    // Persistence -- reload from file
    // -----------------------------------------------------------------------

    @Test
    void markAsJoined_persistsToFile() throws Exception {
        UUID uuid = UUID.randomUUID();
        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
        tracker.markAsJoined(uuid, "Notch");

        // Read a fresh tracker from the same file
        TextPlayerJoinTracker reloaded = new TextPlayerJoinTracker(logger, tempFile);
        assertTrue(reloaded.hasJoined(uuid),
            "UUID should be persisted and visible after reload");
    }

    @Test
    void markAsJoined_multipleEntries_allPersist() throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
        tracker.markAsJoined(uuid1, "Alpha");
        tracker.markAsJoined(uuid2, "Beta");

        TextPlayerJoinTracker reloaded = new TextPlayerJoinTracker(logger, tempFile);
        assertTrue(reloaded.hasJoined(uuid1));
        assertTrue(reloaded.hasJoined(uuid2));
    }

    // -----------------------------------------------------------------------
    // File format -- reading existing file content
    // -----------------------------------------------------------------------

    @Test
    void init_parsesExistingUuidWithName() throws Exception {
        UUID uuid = UUID.randomUUID();
        Files.writeString(tempFile, "# Header comment\n" + uuid + ":Notch\n");

        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
        assertTrue(tracker.hasJoined(uuid));
    }

    @Test
    void init_parsesExistingUuidWithoutName() throws Exception {
        UUID uuid = UUID.randomUUID();
        Files.writeString(tempFile, uuid + "\n");

        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
        assertTrue(tracker.hasJoined(uuid));
    }

    @Test
    void init_skipsCommentLines() throws Exception {
        Files.writeString(tempFile, "# This is a comment\n# Another comment\n");
        // Should not throw or produce any entries
        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
        assertTrue(tracker.exportAll().isEmpty());
    }

    @Test
    void init_skipsBlankLines() throws Exception {
        UUID uuid = UUID.randomUUID();
        Files.writeString(tempFile, "\n\n" + uuid + ":Player\n\n");
        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
        assertEquals(1, tracker.exportAll().size());
    }

    @Test
    void init_skipsInvalidUuidLines_logsInfo() throws Exception {
        Files.writeString(tempFile, "not-a-valid-uuid:PlayerName\n");
        assertDoesNotThrow(() -> new TextPlayerJoinTracker(logger, tempFile));
        Mockito.verify(logger).info(Mockito.contains("Skipping"));
    }

    @Test
    void init_duplicateUuidInFile_onlyFirstEntryIsKept() throws Exception {
        UUID uuid = UUID.randomUUID();
        Files.writeString(tempFile, uuid + ":FirstName\n" + uuid + ":SecondName\n");

        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
        // putIfAbsent semantics: first entry wins
        assertEquals("FirstName", tracker.exportAll().get(uuid));
    }

    // -----------------------------------------------------------------------
    // exportAll
    // -----------------------------------------------------------------------

    @Test
    void exportAll_emptyTracker_returnsEmptyMap() throws Exception {
        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
        assertTrue(tracker.exportAll().isEmpty());
    }

    @Test
    void exportAll_returnsAllRegisteredPlayers() throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
        tracker.markAsJoined(uuid1, "Alpha");
        tracker.markAsJoined(uuid2, "Beta");

        Map<UUID, String> exported = tracker.exportAll();
        assertEquals(2, exported.size());
        assertEquals("Alpha", exported.get(uuid1));
        assertEquals("Beta", exported.get(uuid2));
    }

    @Test
    void exportAll_returnsUnmodifiableView() throws Exception {
        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
        tracker.markAsJoined(UUID.randomUUID(), "Player");

        Map<UUID, String> exported = tracker.exportAll();
        assertThrows(UnsupportedOperationException.class,
            () -> exported.put(UUID.randomUUID(), "Injected"),
            "exportAll should return an unmodifiable view");
    }

    // -----------------------------------------------------------------------
    // Thread safety -- concurrent access
    // -----------------------------------------------------------------------

    @Test
    void markAsJoined_concurrentCalls_noDataCorruption() throws Exception {
        TextPlayerJoinTracker tracker = new TextPlayerJoinTracker(logger, tempFile);
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
}
