package xyz.earthcow.networkjoinmessages.common.abstraction;

public interface CoreLogger {
    void debug(String message);
    void info(String message);
    void warn(String message);
    void severe(String message);
    void setDebug(boolean debug);
}
