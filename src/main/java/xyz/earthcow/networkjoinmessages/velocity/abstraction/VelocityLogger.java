package xyz.earthcow.networkjoinmessages.velocity.abstraction;

import org.slf4j.Logger;
import xyz.earthcow.networkjoinmessages.common.Storage;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;

public class VelocityLogger implements CoreLogger {

    private final Logger velocityLogger;
    private final Storage storage;

    public VelocityLogger(Logger velocityLogger, Storage storage) {
        this.velocityLogger = velocityLogger;
        this.storage = storage;
    }

    @Override
    public void debug(String message) {
        if (storage.getDebug()) {
            velocityLogger.info("[DEBUG] {}", message);
        }
    }

    @Override
    public void info(String message) {
        velocityLogger.info(message);
    }

    @Override
    public void warn(String message) {
        velocityLogger.warn(message);
    }

    @Override
    public void severe(String message) {
        velocityLogger.error(message);
    }
}
