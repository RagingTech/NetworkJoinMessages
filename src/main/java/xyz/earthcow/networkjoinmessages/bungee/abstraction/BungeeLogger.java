package xyz.earthcow.networkjoinmessages.bungee.abstraction;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;

import java.util.logging.Logger;

public class BungeeLogger implements CoreLogger {

    private final Logger bungeeLogger;
    private boolean debug = false;

    public BungeeLogger(Logger bungeeLogger) {
        this.bungeeLogger = bungeeLogger;
    }

    @Override
    public void debug(String message) {
        if (debug) {
            bungeeLogger.info("[DEBUG] " + message);
        }
    }

    @Override
    public void info(String message) {
        bungeeLogger.info(message);
    }

    @Override
    public void warn(String message) {
        bungeeLogger.warning(message);
    }

    @Override
    public void severe(String message) {
        bungeeLogger.severe(message);
    }

    @Override
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
