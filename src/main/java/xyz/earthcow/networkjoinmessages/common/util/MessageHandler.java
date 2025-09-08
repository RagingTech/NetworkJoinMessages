package xyz.earthcow.networkjoinmessages.common.util;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.william278.papiproxybridge.api.PlaceholderAPI;
import org.jetbrains.annotations.NotNull;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.PremiumVanish;
import xyz.earthcow.networkjoinmessages.common.general.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;
import xyz.earthcow.networkjoinmessages.common.general.Storage;
import xyz.earthcow.networkjoinmessages.common.modules.MiniPlaceholdersHook;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MessageHandler {

    private static MessageHandler instance;
    private static Storage storage;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final Pattern essentialsPattern = Pattern.compile("§x(§[0-9a-fA-F]){6}");

    private LuckPerms luckPerms = null;
    private PlaceholderAPI placeholderAPI = null;
    private static MiniPlaceholdersHook miniPlaceholders = null;

    public static MessageHandler getInstance() {
        if (instance == null) {
            instance = new MessageHandler();
        }
        return instance;
    }

    private static String translateLegacyCodes(String str) {
        str = replaceEssentialsColorCodes(str);
        return str
            .replace('§', '&')
            .replace("&0", convertToTag(NamedTextColor.BLACK.asHexString()))
            .replace("&1", convertToTag(NamedTextColor.DARK_BLUE.asHexString()))
            .replace("&2", convertToTag(NamedTextColor.DARK_GREEN.asHexString()))
            .replace("&3", convertToTag(NamedTextColor.DARK_AQUA.asHexString()))
            .replace("&4", convertToTag(NamedTextColor.DARK_RED.asHexString()))
            .replace("&5", convertToTag(NamedTextColor.DARK_PURPLE.asHexString()))
            .replace("&6", convertToTag(NamedTextColor.GOLD.asHexString()))
            .replace("&7", convertToTag(NamedTextColor.GRAY.asHexString()))
            .replace("&8", convertToTag(NamedTextColor.DARK_GRAY.asHexString()))
            .replace("&9", convertToTag(NamedTextColor.BLUE.asHexString()))
            .replace("&a", convertToTag(NamedTextColor.GREEN.asHexString()))
            .replace("&b", convertToTag(NamedTextColor.AQUA.asHexString()))
            .replace("&c", convertToTag(NamedTextColor.RED.asHexString()))
            .replace("&d", convertToTag(NamedTextColor.LIGHT_PURPLE.asHexString()))
            .replace("&e", convertToTag(NamedTextColor.YELLOW.asHexString()))
            .replace("&f", convertToTag(NamedTextColor.WHITE.asHexString()))
            .replace("&k", convertToTag("obfuscated"))
            .replace("&l", convertToTag("bold"))
            .replace("&m", convertToTag("strikethrough"))
            .replace("&n", convertToTag("underlined"))
            .replace("&o", convertToTag("italic"))
            .replace("&r", convertToTag("reset"))
            .replace("\\n", convertToTag("newline"))

            // "&#FFC0CBHello! -> <#FFC0CB>Hello!
            .replaceAll("&#([A-Fa-f0-9]{6})", "<#$1>");
    }

    private static String replaceEssentialsColorCodes(String str) {
        // "§x§f§b§6§3§f§5Hello!" -> "&#fb63f5Hello!"
        Matcher matcher = essentialsPattern.matcher(str);

        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String hexColor = matcher.group(0)
                .replace("§x", "")
                .replace("§", "");
            matcher.appendReplacement(result, "&#" + hexColor);
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private static String convertToTag(String str) {
        return "<" + str + ">";
    }

    public static Component deserialize(String str) {
        return deserialize(str, null);
    }

    public static Component deserialize(String str, CorePlayer parseTarget) {
        if (miniPlaceholders != null) {
            if (parseTarget == null) {
                return miniMessage.deserialize(translateLegacyCodes(str), miniPlaceholders.getGlobalResolver());
            } else {
                return miniMessage.deserialize(translateLegacyCodes(str), miniPlaceholders.getGlobalResolver(), miniPlaceholders.getAudienceResolver(parseTarget.getAudience()));
            }
        }
        return miniMessage.deserialize(translateLegacyCodes(str));
    }

    public static String serialize(Component component) {
        return miniMessage.serialize(component);
    }

    private MessageHandler() {
        storage = Storage.getInstance();

        try {
            luckPerms = LuckPermsProvider.get();
            log("Successfully hooked into LuckPerms!");
        } catch (IllegalStateException | NoClassDefFoundError e) {
            NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().warn("Could not find LuckPerms. Corresponding placeholders will be unavailable.");
        }

        try {
            placeholderAPI = PlaceholderAPI.createInstance();
            log("Successfully hooked into PAPIProxyBridge!");
        } catch (NoClassDefFoundError e) {
            NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().warn("Could not find PAPIProxyBridge. Corresponding placeholders will be unavailable.");
        }

        if (NetworkJoinMessagesCore.getInstance().getPlugin().isPluginLoaded("MiniPlaceholders")) {
            miniPlaceholders = new MiniPlaceholdersHook();
            log("Successfully hooked into MiniPlaceholders!");
        }
    }

    public static String sanitize(String str) {
        return stripColor(deserialize(str));
    }

    public static String stripColor(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    String SwapServerMessage = "";
    String FirstJoinNetworkMessage = "";
    String JoinNetworkMessage = "";
    String LeaveNetworkMessage = "";

    List<String> swapMessages = new ArrayList<>();
    List<String> firstJoinMessages = new ArrayList<>();
    List<String> joinMessages = new ArrayList<>();
    List<String> leaveMessages = new ArrayList<>();

    HashMap<String, String> serverNames;

    public void setupConfigMessages() {
        YamlDocument config = ConfigManager.getPluginConfig();
        SwapServerMessage = config.getString("Messages.SwapServerMessage", "");
        FirstJoinNetworkMessage = config.getString("Messages.FirstJoinNetworkMessage", "");
        JoinNetworkMessage = config.getString("Messages.JoinNetworkMessage", "");
        LeaveNetworkMessage = config.getString("Messages.LeaveNetworkMessage", "");

        swapMessages = config.getStringList("Messages.SwapServerMessages");
        firstJoinMessages = config.getStringList("Messages.FirstJoinNetworkMessages");
        joinMessages = config.getStringList("Messages.JoinNetworkMessages");
        leaveMessages = config.getStringList("Messages.LeaveNetworkMessages");

        HashMap<String, String> serverNames = new HashMap<String, String>();

        for (String serverKey : config.getSection("Servers").getRoutesAsStrings(false)) {
            serverNames.put(
                    serverKey.toLowerCase(),
                    config.getString("Servers." + serverKey, serverKey)
            );
        }

        this.serverNames = serverNames;
    }

    public void parsePlaceholdersAndThen(@NotNull String message, @NotNull CorePlayer parseTarget, Consumer<String> then) {
        if (miniPlaceholders != null) {
            message = serialize(deserialize(message, parseTarget));
        }
        if (placeholderAPI != null) {
            placeholderAPI.formatPlaceholders(message, parseTarget.getUniqueId()).thenAccept(then);
        } else {
            then.accept(message);
        }
    }

    /**
     * Sends a message to the specified command sender.
     * If the command sender is a CorePlayer object
     * then it will be used to parse PAPI and MiniPlaceholders.
     * Designed for self invoked messages such as command responses.
     * @param sender The CoreCommandSender that will receive the message
     * @param message The message to be sent
     */
    public void sendMessage(CoreCommandSender sender, String message) {
        if (sender instanceof CorePlayer parseTarget) {
            sendMessage(sender, message, parseTarget);
        } else {
            sendMessage(sender, message, null);
        }
    }

    /**
     * Sends a message to the specified command sender.
     * Parses PAPI and MiniPlaceholders against the specified parse target
     * Designed for broadcast messages.
     * @param sender The CoreCommandSender that will receive the message
     * @param message The message to be sent
     * @param parseTarget The CorePlayer that will be the target for PAPI and MiniPlaceholders
     */
    public void sendMessage(CoreCommandSender sender, String message, CorePlayer parseTarget) {
        if (parseTarget != null) {
            parsePlaceholdersAndThen(message, parseTarget, formatted -> {
                sender.sendMessage(deserialize(formatted));
            });
            return;
        }
        sender.sendMessage(deserialize(message));
    }

    /**
     * Send a message globally, based on the players current server.
     * @param text - The text to be displayed
     * @param type - What type of message should be sent (switch/join/leave)
     * @param player - The player to fetch the server from.
     */
    public void broadcastMessage(String text, String type, CorePlayer player) {
        if (player.getCurrentServer() == null) {
            NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().warn(
                "Broadcast message of type: '" + type + "' for player: " + player.getName() + " failed to parse server name placeholders as " + player.getName() + "'s current server returned null."
            );
        }
        broadcastMessage(text, type, player.getCurrentServer() == null ? "???" : player.getCurrentServer().getName(), "???", player);
    }

    public void broadcastMessage(String text, String type, String from, String to, CorePlayer parseTarget) {
        List<CorePlayer> receivers = new ArrayList<>();
        if (type.equalsIgnoreCase("switch")) {
            receivers.addAll(storage.getSwitchMessageReceivers(to, from));
        } else if (type.equalsIgnoreCase("first-join")) {
            receivers.addAll(storage.getFirstJoinMessageReceivers(from));
        } else if (type.equalsIgnoreCase("join")) {
            receivers.addAll(storage.getJoinMessageReceivers(from));
        } else if (type.equalsIgnoreCase("leave")) {
            receivers.addAll(storage.getLeaveMessageReceivers(from));
        } else {
            receivers.addAll(NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers());
        }

        // Send message to console
        sendMessage(NetworkJoinMessagesCore.getInstance().getPlugin().getConsole(), text, parseTarget);

        List<UUID> ignorePlayers = storage.getIgnorePlayers(type.equalsIgnoreCase("first-join") ? "join" : type);
        ignorePlayers.addAll(storage.getIgnoredServerPlayers(type));

        for (CorePlayer player : receivers) {
            if (ignorePlayers.contains(player.getUniqueId())) {
                continue;
            }
            sendMessage(player, text, parseTarget);
        }
    }

    public String getFirstJoinNetworkMessage() {
        if (!FirstJoinNetworkMessage.isEmpty()) {
            return FirstJoinNetworkMessage;
        }
        return getRandomMessage(firstJoinMessages);
    }
    public String getJoinNetworkMessage() {
        if (!JoinNetworkMessage.isEmpty()) {
            return JoinNetworkMessage;
        }
        return getRandomMessage(joinMessages);
    }

    public String getLeaveNetworkMessage() {
        if (!LeaveNetworkMessage.isEmpty()) {
            return LeaveNetworkMessage;
        }
        return getRandomMessage(leaveMessages);
    }

    public String getSwapServerMessage() {
        if (!SwapServerMessage.isEmpty()) {
            return SwapServerMessage;
        }
        return getRandomMessage(swapMessages);
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

    public List<String> getServerNames() {
        if (serverNames != null) {
            return List.of(serverNames.keySet().toArray(new String[0]));
        }
        return null;
    }

    public String getServerPlayerCount(CorePlayer player, boolean leaving) {
        if (player.getCurrentServer() != null) {
            return getServerPlayerCount(
                player.getCurrentServer(),
                leaving,
                player
            );
        }
        return "?";
    }

    public String getServerPlayerCount(
        String serverName,
        boolean leaving,
        CorePlayer player
    ) {
        return getServerPlayerCount(
            NetworkJoinMessagesCore.getInstance().getPlugin().getServer(serverName),
            leaving,
            player
        );
    }

    public String getServerPlayerCount(
        CoreBackendServer backendServer,
        boolean leaving,
        CorePlayer player
    ) {
        String serverPlayerCount = "?";
        if (backendServer != null) {
            List<CorePlayer> players = backendServer.getPlayersConnected();

            PremiumVanish premiumVanish = NetworkJoinMessagesCore.getInstance().getPlugin().getVanishAPI();

            if (premiumVanish != null && ConfigManager.getPluginConfig()
                .getBoolean("OtherPlugins.PremiumVanish.RemoveVanishedPlayersFromPlayerCount")) {
                List<UUID> vanishedPlayers = premiumVanish.getInvisiblePlayers();
                // Filter out vanished players
                players = players.stream().filter(corePlayer -> !vanishedPlayers.contains(corePlayer.getUniqueId())).collect(Collectors.toList());
            }

            int count = players.size();

            if (player != null && leaving) {
                if (players.stream().anyMatch(corePlayer -> corePlayer.getUniqueId().equals(player.getUniqueId()))) {
                    count--;
                }
            }

            serverPlayerCount = count + "";
        }
        return serverPlayerCount;
    }

    public String getNetworkPlayerCount(CorePlayer player, Boolean leaving) {
        Collection<CorePlayer> players = NetworkJoinMessagesCore.getInstance()
            .getPlugin()
            .getAllPlayers();
        int count = players.size();

        PremiumVanish premiumVanish = NetworkJoinMessagesCore.getInstance().getPlugin().getVanishAPI();

        boolean vanished = false;
        if (premiumVanish != null && ConfigManager.getPluginConfig()
            .getBoolean("OtherPlugins.PremiumVanish.RemoveVanishedPlayersFromPlayerCount")) {
            count -= premiumVanish.getInvisiblePlayers().size();
            if (player != null) {
                vanished = premiumVanish.isVanished(player.getUniqueId());
            }
        }

        if (player != null && !vanished && leaving) {
            if (players.stream().map(CorePlayer::getUniqueId).collect(Collectors.toList()).contains(player.getUniqueId())) {
                count--;
            }
        }
        return count + "";
    }

    public String handleLpPlaceholders(String str, CorePlayer player) {
        if (luckPerms == null) return str;
        User lpUser = luckPerms.getUserManager().getUser(player.getUniqueId());
        String prefix = "";
        String suffix = "";
        if (lpUser != null) {
            if (lpUser.getCachedData().getMetaData().getPrefix() != null) {
                prefix = lpUser.getCachedData().getMetaData().getPrefix();
            }
            if (lpUser.getCachedData().getMetaData().getSuffix() != null) {
                suffix = lpUser.getCachedData().getMetaData().getSuffix();
            }
        }
        return str
                .replace("%player_prefix%", prefix)
                .replace("%player_suffix%", suffix);
    }

    public String formatMessage(String msg, CorePlayer player) {
        String serverName = player.getCurrentServer() != null
                ? storage.getServerDisplayName(player.getCurrentServer().getName())
                : "???";
        return handleLpPlaceholders(msg, player)
                .replace("%player%", player.getName())
                .replace("%displayname%", player.getName())
                .replace("%server_name%", serverName)
                .replace("%server_name_clean%", sanitize(serverName));
    }

    public String parseSwitchMessage(CorePlayer player, String fromName, String toName) {
        String from = storage.getServerDisplayName(fromName);
        String to = storage.getServerDisplayName(toName);
        return formatMessage(
                getSwapServerMessage()
                    .replace("%to%", to)
                    .replace("%to_clean%", sanitize(to))
                    .replace("%from%", from)
                    .replace("%from_clean%", sanitize(from))
                    .replace("%playercount_from%", getServerPlayerCount(fromName, true, player))
                    .replace("%playercount_to%", getServerPlayerCount(toName, false, player))
                    .replace("%playercount_network%", getNetworkPlayerCount(player, false))
                , player);
    }

    public String formatFirstJoinMessage(CorePlayer player) {
        return formatMessage(
            getFirstJoinNetworkMessage()
                .replace("%playercount_server%", getServerPlayerCount(player, false))
                .replace("%playercount_network%", getNetworkPlayerCount(player, false))
            , player);
    }

    public String formatJoinMessage(CorePlayer player) {
        return formatMessage(
                getJoinNetworkMessage()
                    .replace("%playercount_server%", getServerPlayerCount(player, false))
                    .replace("%playercount_network%", getNetworkPlayerCount(player, false))
                , player);
    }

    public String formatQuitMessage(CorePlayer player) {
        return formatMessage(
                getLeaveNetworkMessage()
                        .replace("%playercount_server%", getServerPlayerCount(player, true))
                        .replace("%playercount_network%", getNetworkPlayerCount(player, true))
                , player);
    }

    public void log(String string) {
        NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().info(string);
    }
}
