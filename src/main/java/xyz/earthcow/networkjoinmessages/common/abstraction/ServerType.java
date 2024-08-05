package xyz.earthcow.networkjoinmessages.common.abstraction;

public enum ServerType {
    BUNGEE ("BungeeCord"),
    VELOCITY("Velocity");

    private final String displayName;

    ServerType() {
        this(null);
    }

    ServerType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return (displayName != null) ? displayName : super.toString();
    }
}