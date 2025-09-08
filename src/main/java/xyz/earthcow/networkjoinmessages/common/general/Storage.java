package xyz.earthcow.networkjoinmessages.common.general;

import dev.dejvokep.boostedyaml.YamlDocument;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.util.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.util.MessageType;

import java.util.*;

/**
 * Singleton class for holding config values and user data that should persist after the user leaves the proxy
 */
public final class Storage {

    // Singleton class
    private static Storage instance;

    // User data that should persist after they leave
    // User data that shouldn't persist after they leave can be stored in CorePlayer
    private final HashMap<UUID, String> previousServer = new HashMap<>();
    private final HashMap<UUID, Boolean> messageState = new HashMap<>();
    private final List<UUID> onlinePlayers = new ArrayList<>();
    private final List<UUID> noJoinMessage = new ArrayList<>();
    private final List<UUID> noLeaveMessage = new ArrayList<>();
    private final List<UUID> noSwitchMessage = new ArrayList<>();

    //region Configuration fields

    /**
     * Map from valid server name -> display server name
     */
    private final HashMap<String, String> serverDisplayNames = new HashMap<>();

    // Definite messages
    private String swapServerMessage;
    private String firstJoinNetworkMessage;
    private String joinNetworkMessage;
    private String leaveNetworkMessage;

    // Randomized messages - enabled if the definite message is empty
    private List<String> swapMessages;
    private List<String> firstJoinMessages;
    private List<String> joinMessages;
    private List<String> leaveMessages;

    /**
     * The default silent state of a player joining with the networkjoinmessages.silent permission
     * Default: true - Someone joining with the permission will be silent (not send a join message)
     */
    private boolean silentJoinDefaultState;

    // Whether specific message types are enabled
    private boolean swapServerMessageEnabled;
    private boolean firstJoinNetworkMessageEnabled;
    private boolean joinNetworkMessageEnabled;
    private boolean leaveNetworkMessageEnabled;

    private boolean notifyAdminsOnSilentMove;

    private boolean swapViewableByJoined;
    private boolean swapViewableByLeft;
    private boolean swapViewableByOther;

    private boolean firstJoinViewableByJoined;
    private boolean firstJoinViewableByOther;

    private boolean joinViewableByJoined;
    private boolean joinViewableByOther;

    private boolean leftViewableByLeft;
    private boolean leftViewableByOther;

    private List<String> serverFirstJoinMessageDisabled = new ArrayList<>();
    private List<String> serverJoinMessageDisabled = new ArrayList<>();
    private List<String> serverLeaveMessageDisabled = new ArrayList<>();

    // BlackList settings
    private List<String> blacklistedServers = new ArrayList<>();
    private boolean useBlacklistAsWhitelist;
    private String swapServerMessageRequires = "ANY";

    /// Other plugins
    // PremiumVanish
    private boolean treatVanishedPlayersAsSilent;
    private boolean removeVanishedPlayersFromPlayerCount;
    // LimboAPI
    private boolean shouldSuppressLimboSwap;
    private boolean shouldSuppressLimboJoin;
    private boolean shouldSuppressLimboLeave;
    //endregion

    // Prevent external instantiation
    private Storage() {}

    /**
     * Get the instance. Make new if there is none.
     * @return Storage instance
     */
    public static Storage getInstance() {
        if (instance == null) {
            instance = new Storage();
        }

        return instance;
    }

    /**
     * Grab values from config and save them here
     */
    public void setUpDefaultValuesFromConfig() {
        YamlDocument config = ConfigManager.getPluginConfig();

        // Load server display names using their real names as defaults
        for (String serverKey : config.getSection("Servers").getRoutesAsStrings(false)) {
            serverDisplayNames.put(
                serverKey.toLowerCase(),
                config.getString("Servers." + serverKey, serverKey)
            );
        }

        /// Messages

        // Set definite messages
        swapServerMessage = config.getString("Messages.SwapServerMessage", "");
        firstJoinNetworkMessage = config.getString("Messages.FirstJoinNetworkMessage", "");
        joinNetworkMessage = config.getString("Messages.JoinNetworkMessage", "");
        leaveNetworkMessage = config.getString("Messages.LeaveNetworkMessage", "");

        // Set randomized messages
        swapMessages = config.getStringList("Messages.SwapServerMessages");
        firstJoinMessages = config.getStringList("Messages.FirstJoinNetworkMessages");
        joinMessages = config.getStringList("Messages.JoinNetworkMessages");
        leaveMessages = config.getStringList("Messages.LeaveNetworkMessages");

        /// Settings

        this.silentJoinDefaultState = config.getBoolean("Settings.SilentJoinDefaultState");

        this.swapServerMessageEnabled = config.getBoolean("Settings.SwapServerMessageEnabled");
        this.firstJoinNetworkMessageEnabled = config.getBoolean("Settings.FirstJoinNetworkMessageEnabled");
        this.joinNetworkMessageEnabled = config.getBoolean("Settings.JoinNetworkMessageEnabled");
        this.leaveNetworkMessageEnabled = config.getBoolean("Settings.LeaveNetworkMessageEnabled");

        this.notifyAdminsOnSilentMove = config.getBoolean("Settings.NotifyAdminsOnSilentMove");

        this.swapViewableByJoined = config.getBoolean("Settings.SwapServerMessageViewableBy.ServerJoined");
        this.swapViewableByLeft = config.getBoolean("Settings.SwapServerMessageViewableBy.ServerLeft");
        this.swapViewableByOther = config.getBoolean("Settings.SwapServerMessageViewableBy.OtherServer");

        this.firstJoinViewableByJoined = config.getBoolean("Settings.FirstJoinNetworkMessageViewableBy.ServerJoined");
        this.firstJoinViewableByOther = config.getBoolean("Settings.FirstJoinNetworkMessageViewableBy.OtherServer");

        this.joinViewableByJoined = config.getBoolean("Settings.JoinNetworkMessageViewableBy.ServerJoined");
        this.joinViewableByOther = config.getBoolean("Settings.JoinNetworkMessageViewableBy.OtherServer");

        this.leftViewableByLeft = config.getBoolean("Settings.LeaveNetworkMessageViewableBy.ServerLeft");
        this.leftViewableByOther = config.getBoolean("Settings.LeaveNetworkMessageViewableBy.OtherServer");

        // Blacklist
        this.blacklistedServers = config.getStringList("Settings.ServerBlacklist");
        this.useBlacklistAsWhitelist = config.getBoolean("Settings.UseBlacklistAsWhitelist");
        this.swapServerMessageRequires = config.getString("Settings.SwapServerMessageRequires").toUpperCase();

        this.serverFirstJoinMessageDisabled = config.getStringList("Settings.IgnoreFirstJoinMessagesList");
        this.serverJoinMessageDisabled = config.getStringList("Settings.IgnoreJoinMessagesList");
        this.serverLeaveMessageDisabled = config.getStringList("Settings.IgnoreLeaveMessagesList");

        this.treatVanishedPlayersAsSilent = config.getBoolean("OtherPlugins.PremiumVanish.TreatVanishedPlayersAsSilent");
        this.removeVanishedPlayersFromPlayerCount = config.getBoolean("OtherPlugins.PremiumVanish.RemoveVanishedPlayersFromPlayerCount");

        this.shouldSuppressLimboSwap = config.getBoolean("OtherPlugins.LimboAPI.SuppressSwapMessages");
        this.shouldSuppressLimboJoin = config.getBoolean("OtherPlugins.LimboAPI.SuppressJoinMessages");
        this.shouldSuppressLimboLeave = config.getBoolean("OtherPlugins.LimboAPI.SuppressLeaveMessages");

        // Verify Swap Server Message
        switch (swapServerMessageRequires) {
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
                            swapServerMessageRequires +
                            "Defaulting to ANY."
                    );
                this.swapServerMessageRequires = "ANY";
        }
    }

    public boolean getSilentMessageState(CorePlayer player) {
        if (!player.hasPermission("networkjoinmessages.silent")) {
            return false;
        }

        if (messageState.containsKey(player.getUniqueId())) {
            return messageState.get(player.getUniqueId());
        } else {
            messageState.put(player.getUniqueId(), silentJoinDefaultState);
            return silentJoinDefaultState;
        }
    }
    public void setSilentMessageState(CorePlayer player, boolean state) {
        messageState.put(player.getUniqueId(), state);
    }

    public boolean isConnected(CorePlayer player) {
        return onlinePlayers.contains(player.getUniqueId());
    }
    public void setConnected(CorePlayer player, boolean state) {
        if (state == isConnected(player)) {
            return;
        }

        if (state) {
            onlinePlayers.add(player.getUniqueId());
        } else {
            onlinePlayers.remove(player.getUniqueId());
        }
    }

    public String getFrom(CorePlayer player) {
        return previousServer.getOrDefault(player.getUniqueId(), player.getCurrentServer().getName());
    }
    public void setFrom(CorePlayer player, String name) {
        previousServer.put(player.getUniqueId(), name);
    }

    private void setJoinState(UUID id, boolean state) {
        if (state) {
            noJoinMessage.remove(id);
        } else {
            if (!noJoinMessage.contains(id)) {
                noJoinMessage.add(id);
            }
        }
    }
    private void setLeaveState(UUID id, boolean state) {
        if (state) {
            noLeaveMessage.remove(id);
        } else {
            if (!noLeaveMessage.contains(id)) {
                noLeaveMessage.add(id);
            }
        }
    }
    private void setSwitchState(UUID id, boolean state) {
        if (state) {
            noSwitchMessage.remove(id);
        } else {
            if (!noSwitchMessage.contains(id)) {
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
        }
    }

    public List<UUID> getIgnorePlayers(MessageType type) {
        return switch (type) {
            case JOIN, FIRST_JOIN -> noJoinMessage;
            case SWAP -> noSwitchMessage;
            case LEAVE -> noLeaveMessage;
        };
    }

    public List<CorePlayer> getSwitchMessageReceivers(String to, String from) {
        List<CorePlayer> receivers = new ArrayList<>();
        //If all are true, add all players:
        if (swapViewableByJoined && swapViewableByLeft && swapViewableByOther) {
            receivers.addAll(
                Core.getInstance().getPlugin().getAllPlayers()
            );
            return receivers;
        }
        //Other server is true, but atleast one of the to or from are set to false:
        else if (swapViewableByOther) {
            receivers.addAll(
                Core.getInstance().getPlugin().getAllPlayers()
            );
            //Players on the connected server is not allowed to see. Remove them all.
            if (!swapViewableByJoined) {
                receivers.removeAll(getServerPlayers(to));
            }

            if (!swapViewableByLeft) {
                receivers.removeAll(getServerPlayers(from));
            }
            return receivers;
        }
        //OtherServer is false.
        else {
            if (swapViewableByJoined) {
                receivers.addAll(getServerPlayers(to));
            }

            if (swapViewableByLeft) {
                receivers.addAll(getServerPlayers(from));
            }
            return receivers;
        }
    }

    public List<CorePlayer> getFirstJoinMessageReceivers(String server) {
        List<CorePlayer> receivers = new ArrayList<>();
        //If all are true, add all players:
        if (firstJoinViewableByJoined && firstJoinViewableByOther) {
            receivers.addAll(
                Core.getInstance().getPlugin().getAllPlayers()
            );
            return receivers;
        }
        //Other server is true, but atleast one of the to or from are set to false:
        else if (firstJoinViewableByOther) {
            receivers.addAll(
                Core.getInstance().getPlugin().getAllPlayers()
            );
            receivers.removeAll(getServerPlayers(server));
            return receivers;
        } else {
            if (firstJoinViewableByJoined) {
                receivers.addAll(getServerPlayers(server));
            }
            return receivers;
        }
    }

    public List<CorePlayer> getJoinMessageReceivers(String server) {
        List<CorePlayer> receivers = new ArrayList<>();
        //If all are true, add all players:
        if (joinViewableByJoined && joinViewableByOther) {
            receivers.addAll(
                Core.getInstance().getPlugin().getAllPlayers()
            );
            return receivers;
        }
        //Other server is true, but atleast one of the to or from are set to false:
        else if (joinViewableByOther) {
            receivers.addAll(
                Core.getInstance().getPlugin().getAllPlayers()
            );
            receivers.removeAll(getServerPlayers(server));
            return receivers;
        } else {
            if (joinViewableByJoined) {
                receivers.addAll(getServerPlayers(server));
            }
            return receivers;
        }
    }

    public List<CorePlayer> getLeaveMessageReceivers(String server) {
        List<CorePlayer> receivers = new ArrayList<>();
        //If all are true, add all players:
        if (leftViewableByLeft && leftViewableByOther) {
            receivers.addAll(
                Core.getInstance().getPlugin().getAllPlayers()
            );
            return receivers;
        }
        //Other server is true, but atleast one of the to or from are set to false:
        else if (leftViewableByOther) {
            receivers.addAll(
                Core.getInstance().getPlugin().getAllPlayers()
            );
            receivers.removeAll(getServerPlayers(server));
            return receivers;
        } else {
            if (leftViewableByLeft) {
                receivers.addAll(getServerPlayers(server));
            }
            return receivers;
        }
    }

    public List<CorePlayer> getServerPlayers(String serverName) {
        CoreBackendServer backendServer = Core.getInstance()
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

        if (blacklistedServers == null) {
            MessageHandler.getInstance()
                .log(
                    "Warning: Blacklisted servers returned null instead of an empty list..."
                );
            return false;
        }
        boolean listed = blacklistedServers.contains(server);
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
            fromListed = blacklistedServers.contains(from);
        }

        boolean toListed = false;
        if (to != null) {
            toListed = blacklistedServers.contains(to);
        }

        boolean result = true;
        switch (swapServerMessageRequires.toUpperCase()) {
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

    public List<UUID> getIgnoredServerPlayers(MessageType type) {
        List<UUID> ignored = new ArrayList<UUID>();
        if (type.equals(MessageType.FIRST_JOIN)) {
            for (String s : serverFirstJoinMessageDisabled) {
                CoreBackendServer backendServer =
                    Core.getInstance().getPlugin().getServer(s);
                if (backendServer != null) {
                    for (CorePlayer p : backendServer.getPlayersConnected()) {
                        ignored.add(p.getUniqueId());
                    }
                }
            }
        } else if (type.equals(MessageType.JOIN)) {
            for (String s : serverJoinMessageDisabled) {
                CoreBackendServer backendServer =
                    Core.getInstance().getPlugin().getServer(s);
                if (backendServer != null) {
                    for (CorePlayer p : backendServer.getPlayersConnected()) {
                        ignored.add(p.getUniqueId());
                    }
                }
            }
        } else if (type.equals(MessageType.LEAVE)) {
            for (String s : serverLeaveMessageDisabled) {
                CoreBackendServer backendServer =
                    Core.getInstance().getPlugin().getServer(s);
                if (backendServer != null) {
                    for (CorePlayer p : backendServer.getPlayersConnected()) {
                        ignored.add(p.getUniqueId());
                    }
                }
            }
        }
        return ignored;
    }



    private String getRandomMessage(List<String> messageList) {
        if (messageList.isEmpty()) {
            return "";
        } else if (messageList.size() == 1) {
            return messageList.get(0);
        }
        Random random = new Random();
        int randomIndex = random.nextInt(messageList.size());
        return messageList.get(randomIndex);
    }

    private String getMessage(String message, List<String> messageList) {
        if (!message.isEmpty()) {
            return message;
        }
        return getRandomMessage(messageList);
    }

    //region Getters

    public List<String> getServerNames() {
        return List.of(serverDisplayNames.keySet().toArray(new String[0]));
    }

    public String getServerDisplayName(String serverName) {
        return serverDisplayNames.getOrDefault(serverName, serverName);
    }

    public String getSwapServerMessage() {
        return getMessage(swapServerMessage, swapMessages);
    }

    public String getFirstJoinNetworkMessage() {
        return getMessage(firstJoinNetworkMessage, firstJoinMessages);
    }

    public String getJoinNetworkMessage() {
        return getMessage(joinNetworkMessage, joinMessages);
    }

    public String getLeaveNetworkMessage() {
        return getMessage(leaveNetworkMessage, leaveMessages);
    }

    public boolean getNotifyAdminsOnSilentMove() {
        return notifyAdminsOnSilentMove;
    }

    public boolean isSwapServerMessageEnabled() {
        return swapServerMessageEnabled;
    }

    public boolean isFirstJoinNetworkMessageEnabled() {
        return firstJoinNetworkMessageEnabled;
    }

    public boolean isJoinNetworkMessageEnabled() {
        return joinNetworkMessageEnabled;
    }

    public boolean isLeaveNetworkMessageEnabled() {
        return leaveNetworkMessageEnabled;
    }

    public boolean getTreatVanishedPlayersAsSilent() {
        return treatVanishedPlayersAsSilent;
    }

    public boolean getRemoveVanishedPlayersFromPlayerCount() {
        return removeVanishedPlayersFromPlayerCount;
    }

    public boolean getShouldSuppressLimboSwap() {
        return shouldSuppressLimboSwap;
    }

    public boolean getShouldSuppressLimboJoin() {
        return shouldSuppressLimboJoin;
    }

    public boolean getShouldSuppressLimboLeave() {
        return shouldSuppressLimboLeave;
    }

    //endregion
}
