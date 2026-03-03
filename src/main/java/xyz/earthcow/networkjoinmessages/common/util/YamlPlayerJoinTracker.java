package xyz.earthcow.networkjoinmessages.common.util;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players have ever joined the network using a plain YAML file.
 *
 * <p>The file format is intentionally minimal — one UUID per line — so it
 * remains human-readable and can be edited directly if needed:
 *
 * <pre>
 * joined:
 *   - 069a79f4-44e9-4726-a5be-fca90e38aaf5  # Notch
 *   - 853c80ef-3c37-49fd-aa49-938b674adae6  # jeb_
 * </pre>
 *
 * All mutations are flushed to disk immediately so no data is lost on crash.
 * All public methods are {@code synchronized} for thread safety.
 */
public class YamlPlayerJoinTracker implements PlayerJoinTracker {

    private final CoreLogger logger;
    private final Path filePath;

    /** In-memory set of every UUID that has ever joined. */
    private final Set<UUID> joinedUuids = new LinkedHashSet<>();

    public YamlPlayerJoinTracker(CoreLogger logger, Path filePath) throws IOException {
        this.logger = logger;
        this.filePath = filePath;
        load();
    }

    @Override
    public synchronized boolean hasJoined(UUID playerUuid) {
        return joinedUuids.contains(playerUuid);
    }

    @Override
    public synchronized void markAsJoined(UUID playerUuid, String playerName) {
        if (joinedUuids.add(playerUuid)) {
            appendLine(playerUuid, playerName);
        }
    }

    // --- Internal helpers ---

    /**
     * Loads existing UUIDs from the YAML file into memory.
     * If the file does not exist it is created with an empty {@code joined:} section.
     */
    private void load() throws IOException {
        if (!Files.exists(filePath)) {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, "joined:\n");
            return;
        }

        for (String rawLine : Files.readAllLines(filePath)) {
            String line = rawLine.trim();
            // Skip the header line and blank lines
            if (line.isEmpty() || line.equals("joined:") || line.startsWith("#")) continue;
            // Strip leading "- " list marker and any trailing comment
            if (line.startsWith("- ")) {
                line = line.substring(2).trim();
            }
            // Strip inline comment (e.g. "uuid  # name")
            int commentIdx = line.indexOf('#');
            if (commentIdx >= 0) {
                line = line.substring(0, commentIdx).trim();
            }
            if (line.isEmpty()) continue;
            try {
                joinedUuids.add(UUID.fromString(line));
            } catch (IllegalArgumentException ignored) {
                logger.info("[YamlPlayerJoinTracker] Skipping unrecognised line in joined.yml: " + rawLine.trim());
            }
        }

        logger.info("[YamlPlayerJoinTracker] Loaded " + joinedUuids.size() + " joined-player UUIDs from " + filePath.getFileName());
    }

    /**
     * Appends a single entry to the YAML file without rewriting the whole file.
     * Uses an inline comment to record the player name for human readability.
     */
    private void appendLine(UUID uuid, String playerName) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                filePath, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
            writer.write("  - " + uuid + "  # " + playerName);
            writer.newLine();
        } catch (IOException e) {
            logger.severe("[YamlPlayerJoinTracker] Failed to persist UUID " + uuid + " (" + playerName + "): " + e.getMessage());
        }
    }
}
