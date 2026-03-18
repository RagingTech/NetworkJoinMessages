package xyz.earthcow.networkjoinmessages.common.storage;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Tracks which players have ever joined the network using a plain text file.
 *
 * <p>The file format is intentionally minimal: UUID:player_name\n. The player
 * name can be omitted by omitting the colon, otherwise it is invalid. It
 * remains human-readable and can be edited directly if needed:
 *
 * <pre>
 * 069a79f4-44e9-4726-a5be-fca90e38aaf5:Notch
 * 853c80ef-3c37-49fd-aa49-938b674adae6:jeb_
 * 540a2d51-210e-48a3-abd9-22c21cbaecd4
 * </pre>
 *
 * All mutations are flushed to disk immediately so no data is lost on crash.
 * All public methods are {@code synchronized} for thread safety.
 */
public class TextPlayerJoinTracker implements PlayerJoinTracker {

    private final CoreLogger logger;
    private final Path filePath;

    /**
     * In-memory map of every UUID that has ever joined, keyed by UUID and
     * valued by the player name last seen on that line (or {@code ""}
     */
    private final Map<UUID, String> joinedPlayers = new LinkedHashMap<>();

    public TextPlayerJoinTracker(CoreLogger logger, Path filePath) throws IOException {
        this.logger = logger;
        this.filePath = filePath;
        load();
    }

    @Override
    public synchronized boolean hasJoined(UUID playerUuid) {
        return joinedPlayers.containsKey(playerUuid);
    }

    @Override
    public synchronized void markAsJoined(UUID playerUuid, String playerName) {
        if (joinedPlayers.putIfAbsent(playerUuid, playerName) == null) {
            appendLine(playerUuid, playerName);
        }
    }

    // --- Internal helpers ---

    /**
     * Loads existing UUIDs from the text file into memory.
     * If the file does not exist it is created.
     */
    private void load() throws IOException {
        if (!Files.exists(filePath)) {
            Files.createDirectories(filePath.getParent());
            Files.writeString(
                filePath,
                """
                    # Format: 853c80ef-3c37-49fd-aa49-938b674adae6:jeb_ or 540a2d51-210e-48a3-abd9-22c21cbaecd4 for no player name
                    # Player names can change; UUID required. One entry per line. No inline comments.
                    # This file must always end with an empty new line.
                    """
            );
            return;
        }

        for (String rawLine : Files.readAllLines(filePath)) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String uuidPart;
            String namePart;

            if (line.contains(":")) {
                // Splits on first colon
                int colon = line.indexOf(':');
                uuidPart = line.substring(0, colon).trim();
                namePart = line.substring(colon + 1).trim();
            } else {
                uuidPart = line;
                namePart = "";
            }

            try {
                UUID uuid = UUID.fromString(uuidPart);
                // putIfAbsent preserves the first occurrence when the same UUID
                // appears more than once in the file (shouldn't happen, but safe)
                joinedPlayers.putIfAbsent(uuid, namePart);
            } catch (IllegalArgumentException ignored) {
                logger.info("[TextPlayerJoinTracker] Skipping unrecognised line in "
                    + filePath.getFileName() + ": " + rawLine.trim());
            }
        }

        logger.debug("[TextPlayerJoinTracker] Loaded " + joinedPlayers.size() + " joined-player UUIDs from " + filePath.getFileName());
    }

    /**
     * Appends a single entry to the text file without rewriting the whole file.
     */
    private void appendLine(UUID uuid, String playerName) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                filePath, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
            writer.write(uuid + ":" + playerName);
            writer.newLine();
        } catch (IOException e) {
            logger.severe("[TextPlayerJoinTracker] Failed to persist UUID " + uuid + " (" + playerName + "): " + e.getMessage());
        }
    }

    @Override
    public synchronized Map<UUID, String> exportAll() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(joinedPlayers));
    }
}
