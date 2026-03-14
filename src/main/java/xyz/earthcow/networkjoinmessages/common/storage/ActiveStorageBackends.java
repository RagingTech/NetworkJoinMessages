package xyz.earthcow.networkjoinmessages.common.storage;

/**
 * Holds the live {@link PlayerJoinTracker} and {@link PlayerDataStore} instances
 * that the plugin should use after a successful call to {@link StorageInitializer#initialize}.
 *
 * <p>Both fields are guaranteed non-null when returned by the initializer. The plugin
 * is responsible for calling {@link #close()} during shutdown so that any underlying
 * connections or file handles are released cleanly.
 */
public record ActiveStorageBackends(
    PlayerJoinTracker joinTracker,
    PlayerDataStore playerDataStore
) implements AutoCloseable {

    /**
     * Closes both backends. Exceptions from each are caught independently so that
     * a failure in one does not prevent the other from being closed.
     */
    @Override
    public void close() {
        try {
            joinTracker.close();
        } catch (Exception e) {
            // Logged by the caller; swallowed here so playerDataStore still closes.
        }
        try {
            playerDataStore.close();
        } catch (Exception e) {
            // Logged by the caller.
        }
    }
}