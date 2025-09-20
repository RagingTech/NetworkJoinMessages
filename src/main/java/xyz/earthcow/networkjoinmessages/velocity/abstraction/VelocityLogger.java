package xyz.earthcow.networkjoinmessages.velocity.abstraction;

import org.slf4j.Logger;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;

public class VelocityLogger implements CoreLogger {

    private final Logger velocityLogger;
    private boolean debug = false;

    public VelocityLogger(Logger velocityLogger) {
        this.velocityLogger = velocityLogger;
    }

    @Override
    public void debug(String message) {
        if (debug) {
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

    @Override
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
