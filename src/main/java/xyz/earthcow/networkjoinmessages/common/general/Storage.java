package xyz.earthcow.networkjoinmessages.common.general;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.util.MessageHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Storage {

    private static Storage instance;

    HashMap<UUID, String> previousServer = new HashMap<>();
    HashMap<UUID, Boolean> messageState = new HashMap<>();
    List<UUID> onlinePlayers = new ArrayList<>();
    List<UUID> noJoinMessage = new ArrayList<>();
    List<UUID> noLeaveMessage = new ArrayList<>();
    List<UUID> noSwitchMessage = new ArrayList<>();

    boolean SwapServerMessageEnabled = true;
    boolean FirstJoinNetworkMessageEnabled = true;
    boolean JoinNetworkMessageEnabled = true;
    boolean LeaveNetworkMessageEnabled = true;
    boolean NotifyAdminsOnSilentMove = true;

    boolean SwapViewableByJoined = true;
    boolean SwapViewableByLeft = true;
    boolean SwapViewableByOther = true;

    boolean FirstJoinViewableByJoined = true;
    boolean FirstJoinViewableByOther = true;

    boolean JoinViewableByJoined = true;
    boolean JoinViewableByOther = true;

    boolean LeftViewableByLeft = true;
    boolean LeftViewableByOther = true;

    List<String> ServerFirstJoinMessageDisabled = new ArrayList<>();
    List<String> ServerJoinMessageDisabled = new ArrayList<>();
    List<String> ServerLeaveMessageDisabled = new ArrayList<>();

    //BlackList settings
    List<String> BlacklistedServers = new ArrayList<>();
    boolean useBlacklistAsWhitelist;
    String SwapServerMessageRequires = "ANY";

    // Other plugins
    boolean shouldSuppressLimboSwap = true;
    boolean shouldSuppressLimboJoin = false;
    boolean shouldSuppressLimboLeave = false;

    /**
     * Get current instance. Make new if there is none.
     * @return instance of the storage.
     */
    public static Storage getInstance() {
        if (instance == null) {
            instance = new Storage();
        }

        return instance;
    }

    /**
     * Grab values from config and save them here.
     */
    public void setUpDefaultValuesFromConfig() {
        this.SwapServerMessageEnabled = ConfigManager
                .getPluginConfig()
                .getBoolean("Settings.SwapServerMessageEnabled");
        this.FirstJoinNetworkMessageEnabled = ConfigManager
            .getPluginConfig()
            .getBoolean("Settings.FirstJoinNetworkMessageEnabled");
        this.JoinNetworkMessageEnabled = ConfigManager
                .getPluginConfig()
                .getBoolean("Settings.JoinNetworkMessageEnabled");
        this.LeaveNetworkMessageEnabled = ConfigManager
                .getPluginConfig()
                .getBoolean("Settings.LeaveNetworkMessageEnabled");
        this.NotifyAdminsOnSilentMove = ConfigManager
                .getPluginConfig()
                .getBoolean("Settings.NotifyAdminsOnSilentMove");

        this.SwapViewableByJoined = ConfigManager
                .getPluginConfig()
                .getBoolean("Settings.SwapServerMessageViewableBy.ServerJoined");
        this.SwapViewableByLeft = ConfigManager
                .getPluginConfig()
                .getBoolean("Settings.SwapServerMessageViewableBy.ServerLeft");
        this.SwapViewableByOther = ConfigManager
                .getPluginConfig()
                .getBoolean("Settings.SwapServerMessageViewableBy.OtherServer");

        this.FirstJoinViewableByJoined = ConfigManager
            .getPluginConfig()
            .getBoolean("Settings.FirstJoinNetworkMessageViewableBy.ServerJoined");
        this.FirstJoinViewableByOther = ConfigManager
            .getPluginConfig()
            .getBoolean("Settings.FirstJoinNetworkMessageViewableBy.OtherServer");

        this.JoinViewableByJoined = ConfigManager
                .getPluginConfig()
                .getBoolean("Settings.JoinNetworkMessageViewableBy.ServerJoined");
        this.JoinViewableByOther = ConfigManager
                .getPluginConfig()
                .getBoolean("Settings.JoinNetworkMessageViewableBy.OtherServer");

        this.LeftViewableByLeft = ConfigManager
                .getPluginConfig()
                .getBoolean("Settings.LeaveNetworkMessageViewableBy.ServerLeft");
        this.LeftViewableByOther = ConfigManager
                .getPluginConfig()
                .getBoolean("Settings.LeaveNetworkMessageViewableBy.OtherServer");

        //Blacklist
        this.BlacklistedServers = ConfigManager
                .getPluginConfig()
                .getStringList("Settings.ServerBlacklist");
        this.useBlacklistAsWhitelist = ConfigManager
                .getPluginConfig()
                .getBoolean("Settings.UseBlacklistAsWhitelist");
        this.SwapServerMessageRequires = ConfigManager
                .getPluginConfig()
                .getString("Settings.SwapServerMessageRequires")
                .toUpperCase();

        this.ServerFirstJoinMessageDisabled = ConfigManager
            .getPluginConfig()
            .getStringList("Settings.IgnoreFirstJoinMessagesList");
        this.ServerJoinMessageDisabled = ConfigManager
                .getPluginConfig()
                .getStringList("Settings.IgnoreJoinMessagesList");
        this.ServerLeaveMessageDisabled = ConfigManager
                .getPluginConfig()
                .getStringList("Settings.IgnoreLeaveMessagesList");

        this.shouldSuppressLimboSwap = ConfigManager
            .getPluginConfig()
            .getBoolean("Settings.OtherPlugins.LimboReconnect.SuppressSwapMessages");
        this.shouldSuppressLimboJoin = ConfigManager
            .getPluginConfig()
            .getBoolean("Settings.OtherPlugins.LimboReconnect.SuppressJoinMessages");
        this.shouldSuppressLimboLeave = ConfigManager
            .getPluginConfig()
            .getBoolean("Settings.OtherPlugins.LimboReconnect.SuppressLeaveMessages");

        //Verify Swap Server Message
        switch (SwapServerMessageRequires) {
            case "JOINED":
            case "LEFT":
            case "BOTH":
            case "ANY":
                break;
            default:
                MessageHandler.getInstance()
                        .log(
                                "Setting error: Settings.SwapServerMessageRequires " +
                                        "only allows JOINED LEFT BOTH or ANY. Got " +
                                        SwapServerMessageRequires +
                                        "Defaulting to ANY."
                        );
                this.SwapServerMessageRequires = "ANY";
        }
    }

    public boolean isSwapServerMessageEnabled() {
        return SwapServerMessageEnabled;
    }

    public boolean isFirstJoinNetworkMessageEnabled() {
        return FirstJoinNetworkMessageEnabled;
    }

    public boolean isJoinNetworkMessageEnabled() {
        return JoinNetworkMessageEnabled;
    }

    public boolean isLeaveNetworkMessageEnabled() {
        return LeaveNetworkMessageEnabled;
    }

    public boolean notifyAdminsOnSilentMove() {
        return NotifyAdminsOnSilentMove;
    }

    public boolean getAdminMessageState(CorePlayer p) {
        if (p.hasPermission("networkjoinmessages.silent")) {
            if (messageState.containsKey(p.getUniqueId())) {
                return messageState.get(p.getUniqueId());
            } else {
                boolean state = ConfigManager.getPluginConfig()
                    .getBoolean("Settings.SilentJoinDefaultState");
                messageState.put(p.getUniqueId(), state);
                return state;
            }
        } else {
            return false; //Is not silent by default as they don't have silent perm..
        }
    }

    public void setAdminMessageState(CorePlayer player, Boolean state) {
        messageState.put(player.getUniqueId(), state);
    }

    public Boolean isConnected(CorePlayer p) {
        return onlinePlayers.contains(p.getUniqueId());
    }

    public void setConnected(CorePlayer p, Boolean state) {
        if (state) {
            if (!isConnected(p)) {
                onlinePlayers.add(p.getUniqueId());
            }
        } else {
            if (isConnected(p)) {
                onlinePlayers.remove(p.getUniqueId());
            }
        }
    }

    public String getFrom(CorePlayer p) {
        if (previousServer.containsKey(p.getUniqueId())) {
            return previousServer.get(p.getUniqueId());
        } else {
            return p.getCurrentServer() == null ? "???" : p.getCurrentServer().getName();
        }
    }

    public void setFrom(CorePlayer p, String name) {
        previousServer.put(p.getUniqueId(), name);
    }

    public boolean isElsewhere(CorePlayer player) {
        return previousServer.containsKey(player.getUniqueId());
    }

    public void clearPlayer(CorePlayer player) {
        previousServer.remove(player.getUniqueId());
    }

    private void setJoinState(UUID id, boolean state) {
        if (state) {
            noJoinMessage.remove(id);
        } else {
            if (!noJoinMessage.contains(id)) { //Prevent duplicates.
                noJoinMessage.add(id);
            }
        }
    }

    private void setLeaveState(UUID id, boolean state) {
        if (state) {
            noLeaveMessage.remove(id);
        } else {
            if (!noLeaveMessage.contains(id)) { //Prevent duplicates.
                noLeaveMessage.add(id);
            }
        }
    }

    private void setSwitchState(UUID id, boolean state) {
        if (state) {
            noSwitchMessage.remove(id);
        } else {
            if (!noSwitchMessage.contains(id)) { //Prevent duplicates.
                noSwitchMessage.add(id);
            }
        }
    }

    public void setSendMessageState(String list, UUID id, boolean state) {
        switch (list) {
            case "all":
                setSwitchState(id, state);
                setJoinState(id, state);
                setLeaveState(id, state);
                return;
            case "join":
                setJoinState(id, state);
                return;
            case "leave":
            case "quit":
                setLeaveState(id, state);
                return;
            case "switch":
                setSwitchState(id, state);
                return;
            default:
                return;
        }
    }

    public List<UUID> getIgnorePlayers(String type) {
        switch (type) {
            case "join":
                return noJoinMessage;
            case "leave":
                return noLeaveMessage;
            case "switch":
                return noSwitchMessage;
            default:
                return new ArrayList<UUID>();
        }
    }

    public List<CorePlayer> getSwitchMessageReceivers(String to, String from) {
        List<CorePlayer> receivers = new ArrayList<>();
        //If all are true, add all players:
        if (SwapViewableByJoined && SwapViewableByLeft && SwapViewableByOther) {
            receivers.addAll(
                NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers()
            );
            return receivers;
        }
        //Other server is true, but atleast one of the to or from are set to false:
        else if (SwapViewableByOther) {
            receivers.addAll(
                NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers()
            );
            //Players on the connected server is not allowed to see. Remove them all.
            if (!SwapViewableByJoined) {
                receivers.removeAll(getServerPlayers(to));
            }

            if (!SwapViewableByLeft) {
                receivers.removeAll(getServerPlayers(from));
            }
            return receivers;
        }
        //OtherServer is false.
        else {
            if (SwapViewableByJoined) {
                receivers.addAll(getServerPlayers(to));
            }

            if (SwapViewableByLeft) {
                receivers.addAll(getServerPlayers(from));
            }
            return receivers;
        }
    }

    public List<CorePlayer> getFirstJoinMessageReceivers(String server) {
        List<CorePlayer> receivers = new ArrayList<>();
        //If all are true, add all players:
        if (FirstJoinViewableByJoined && FirstJoinViewableByOther) {
            receivers.addAll(
                NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers()
            );
            return receivers;
        }
        //Other server is true, but atleast one of the to or from are set to false:
        else if (FirstJoinViewableByOther) {
            receivers.addAll(
                NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers()
            );
            receivers.removeAll(getServerPlayers(server));
            return receivers;
        } else {
            if (FirstJoinViewableByJoined) {
                receivers.addAll(getServerPlayers(server));
            }
            return receivers;
        }
    }

    public List<CorePlayer> getJoinMessageReceivers(String server) {
        List<CorePlayer> receivers = new ArrayList<>();
        //If all are true, add all players:
        if (JoinViewableByJoined && JoinViewableByOther) {
            receivers.addAll(
                NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers()
            );
            return receivers;
        }
        //Other server is true, but atleast one of the to or from are set to false:
        else if (JoinViewableByOther) {
            receivers.addAll(
                NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers()
            );
            receivers.removeAll(getServerPlayers(server));
            return receivers;
        } else {
            if (JoinViewableByJoined) {
                receivers.addAll(getServerPlayers(server));
            }
            return receivers;
        }
    }

    public List<CorePlayer> getLeaveMessageReceivers(String server) {
        List<CorePlayer> receivers = new ArrayList<>();
        //If all are true, add all players:
        if (LeftViewableByLeft && LeftViewableByOther) {
            receivers.addAll(
                NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers()
            );
            return receivers;
        }
        //Other server is true, but atleast one of the to or from are set to false:
        else if (LeftViewableByOther) {
            receivers.addAll(
                NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers()
            );
            receivers.removeAll(getServerPlayers(server));
            return receivers;
        } else {
            if (LeftViewableByLeft) {
                receivers.addAll(getServerPlayers(server));
            }
            return receivers;
        }
    }

    public List<CorePlayer> getServerPlayers(String serverName) {
        CoreBackendServer backendServer = NetworkJoinMessagesCore.getInstance()
            .getPlugin()
            .getServer(serverName);
        if (backendServer == null) {
            return new ArrayList<>();
        }
        return backendServer.getPlayersConnected();
    }

    public boolean blacklistCheck(CorePlayer player) {
        if (player.getCurrentServer() == null) {
            MessageHandler.getInstance()
                .log(
                    "Warning: Server of " +
                    player.getName() +
                    " came back as Null. Blackisted Server check failed. #01"
                );
            return false;
        }
        String server = player
            .getCurrentServer()
            .getName();
        //Null check because of Geyser issues.
        if (server == null) {
            MessageHandler.getInstance()
                .log(
                    "Warning: Server of " +
                    player.getName() +
                    " came back as Null. Blackisted Server check failed. #02"
                );
            return false;
        }

        if (BlacklistedServers == null) {
            MessageHandler.getInstance()
                .log(
                    "Warning: Blacklisted servers returned null instead of an empty list..."
                );
            return false;
        }
        boolean listed = BlacklistedServers.contains(server);
        if (useBlacklistAsWhitelist) {
            //WHITELIST
            return !listed;
            //Returns TRUE if it's NOT in the list (Deny MessagE)
            //FALSE if it is in the list. (Allow Message)

        } else {
            //BLACKLIST
            return listed;
            //Returns TRUE if it is listed. (Allow Message)
            //FALSE if it's not listed. (Deny Message)

        }
        //Returning FALSE allows the message to go further, TRUE will stop it.
    }

    public boolean blacklistCheck(String from, String to) {
        boolean fromListed = false;
        if (from != null) {
            fromListed = BlacklistedServers.contains(from);
        }

        boolean toListed = false;
        if (to != null) {
            toListed = BlacklistedServers.contains(to);
        }

        boolean result = true;
        switch (SwapServerMessageRequires.toUpperCase()) {
            case "JOINED":
                result = toListed;
                //MessageHandler.getInstance().log("Joined - toListed = " + toListed);
                break;
            case "LEFT":
                result = fromListed;
                //MessageHandler.getInstance().log("Left - fromListed = " + fromListed);
                break;
            case "ANY":
                result = fromListed || toListed;
                //MessageHandler.getInstance().log("ANY - fromListed = " + fromListed + "toListed = " + toListed + " result = " + result);
                break;
            case "BOTH":
                result = fromListed && toListed;
                //MessageHandler.getInstance().log("BOTH - fromListed = " + fromListed + "toListed = " + toListed + " result = " + result);
                break;
            default:
                //MessageHandler.getInstance().log("Warning: Default action triggered under blacklist check. Is it correctly configured?");
                break;
        }
        //Returning FALSE allows the message to go further, TRUE will stop it.
        if (useBlacklistAsWhitelist) {
            //WHITELIST
            return !result;
        } else {
            //BLACKLIST
            return result;
        }
    }

    public List<UUID> getIgnoredServerPlayers(String type) {
        List<UUID> ignored = new ArrayList<UUID>();
        if (type.equalsIgnoreCase("first-join")) {
            for (String s : ServerFirstJoinMessageDisabled) {
                CoreBackendServer backendServer =
                    NetworkJoinMessagesCore.getInstance().getPlugin().getServer(s);
                if (backendServer != null) {
                    for (CorePlayer p : backendServer.getPlayersConnected()) {
                        ignored.add(p.getUniqueId());
                    }
                }
            }
        } else if (type.equalsIgnoreCase("join")) {
            for (String s : ServerJoinMessageDisabled) {
                CoreBackendServer backendServer =
                    NetworkJoinMessagesCore.getInstance().getPlugin().getServer(s);
                if (backendServer != null) {
                    for (CorePlayer p : backendServer.getPlayersConnected()) {
                        ignored.add(p.getUniqueId());
                    }
                }
            }
        } else if (type.equalsIgnoreCase("leave")) {
            for (String s : ServerLeaveMessageDisabled) {
                CoreBackendServer backendServer =
                        NetworkJoinMessagesCore.getInstance().getPlugin().getServer(s);
                if (backendServer != null) {
                    for (CorePlayer p : backendServer.getPlayersConnected()) {
                        ignored.add(p.getUniqueId());
                    }
                }
            }
        }
        return ignored;
    }

    public boolean shouldSuppressLimboSwap() {
        return shouldSuppressLimboSwap;
    }

    public boolean shouldSuppressLimboJoin() {
        return shouldSuppressLimboJoin;
    }

    public boolean shouldSuppressLimboLeave() {
        return shouldSuppressLimboLeave;
    }
}
