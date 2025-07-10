package xyz.earthcow.networkjoinmessages.common.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLitePlayerJoinTrackerTest {

    private File tempDbFile;
    private SQLitePlayerJoinTracker tracker;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = Files.createTempFile("testdb", ".sqlite").toFile();
        tracker = new SQLitePlayerJoinTracker(tempDbFile.getAbsolutePath());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (tempDbFile.exists()) tempDbFile.delete();
    }

    @Test
    void testNewUUIDHasNotJoined() {
        UUID uuid = UUID.randomUUID();
        assertFalse(tracker.hasJoined(uuid), "New UUID should not be marked as joined");
    }

    @Test
    void testMarkAsJoined() {
        UUID uuid = UUID.randomUUID();
        assertFalse(tracker.hasJoined(uuid));
        tracker.markAsJoined(uuid, "Player 1");
        assertTrue(tracker.hasJoined(uuid), "UUID should be marked as joined after insert");
    }

    @Test
    void testDoubleInsert() {
        UUID uuid = UUID.randomUUID();
        tracker.markAsJoined(uuid, "Player 2");
        tracker.markAsJoined(uuid, "Player 2"); // Should not throw any exceptions
        assertTrue(tracker.hasJoined(uuid));
    }

    @Test
    void testMultipleUUIDs() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        tracker.markAsJoined(uuid1, "Player 3");
        assertTrue(tracker.hasJoined(uuid1));
        assertFalse(tracker.hasJoined(uuid2));
    }
}
