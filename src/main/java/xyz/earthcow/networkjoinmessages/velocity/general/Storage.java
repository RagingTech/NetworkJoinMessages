package xyz.earthcow.networkjoinmessages.velocity.general;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.spongepowered.configurate.serialize.SerializationException;
import xyz.earthcow.networkjoinmessages.velocity.util.MessageHandler;

import java.util.*;

public class Storage {

	private static Storage instance;
	
	HashMap<Player,String> previousServer;
	HashMap<UUID,Boolean> messageState;
	List<UUID> onlinePlayers;
	List<UUID> noJoinMessage;
	List<UUID> noLeaveMessage;
	List<UUID> noSwitchMessage;
	
	boolean SwapServerMessageEnabled = true;
	boolean JoinNetworkMessageEnabled = true;
	boolean LeaveNetworkMessageEnabled = true;
	boolean NotifyAdminsOnSilentMove = true;
	
	boolean SwapViewableByJoined = true;
	boolean SwapViewableByLeft = true;
	boolean SwapViewableByOther = true;
	
	boolean JoinViewableByJoined = true;
	boolean JoinViewableByOther = true;
	
	boolean LeftViewableByLeft = true;
	boolean LeftViewableByOther = true;
	
	List<String> ServerJoinMessageDisabled;
	List<String> ServerLeaveMessageDisabled;
	
	//BlackList settings
	List<String> BlacklistedServers;
	boolean useBlacklistAsWhitelist;
	String SwapServerMessageRequires = "ANY";
	
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
	
	public Storage() {
		this.previousServer = new HashMap<Player,String>();
		this.messageState = new HashMap<UUID,Boolean>();
		this.onlinePlayers = new ArrayList<UUID>();
		this.noJoinMessage = new ArrayList<UUID>();
		this.noLeaveMessage = new ArrayList<UUID>();
		this.noSwitchMessage = new ArrayList<UUID>();
	}
	
	/**
	 * Grab values from config and save them here.
	 */
	public void setUpDefaultValuesFromConfig() {
		try {
			SwapServerMessageEnabled = VelocityMain.getInstance().getRootNode().node("Settings", "SwapServerMessageEnabled").getBoolean(true);
			JoinNetworkMessageEnabled = VelocityMain.getInstance().getRootNode().node("Settings", "JoinNetworkMessageEnabled").getBoolean(true);
			LeaveNetworkMessageEnabled = VelocityMain.getInstance().getRootNode().node("Settings", "LeaveNetworkMessageEnabled").getBoolean(true);
			NotifyAdminsOnSilentMove = VelocityMain.getInstance().getRootNode().node("Settings", "NotifyAdminsOnSilentMove").getBoolean(true);

			SwapViewableByJoined = VelocityMain.getInstance().getRootNode().node("Settings", "SwapServerMessageViewableBy", "ServerJoined").getBoolean(true);
			SwapViewableByLeft = VelocityMain.getInstance().getRootNode().node("Settings", "SwapServerMessageViewableBy", "ServerLeft").getBoolean(true);
			SwapViewableByOther = VelocityMain.getInstance().getRootNode().node("Settings", "SwapServerMessageViewableBy", "OtherServer").getBoolean(true);

			JoinViewableByJoined = VelocityMain.getInstance().getRootNode().node("Settings", "JoinNetworkMessageViewableBy", "ServerJoined").getBoolean(true);
			JoinViewableByOther = VelocityMain.getInstance().getRootNode().node("Settings", "JoinNetworkMessageViewableBy", "OtherServer").getBoolean(true);

			LeftViewableByLeft = VelocityMain.getInstance().getRootNode().node("Settings", "LeaveNetworkMessageViewableBy", "ServerLeft").getBoolean(true);
			LeftViewableByOther = VelocityMain.getInstance().getRootNode().node("Settings", "LeaveNetworkMessageViewableBy", "OtherServer").getBoolean(true);

			BlacklistedServers = VelocityMain.getInstance().getRootNode().node("Settings", "ServerBlacklist").getList(String.class, new ArrayList<>());
			useBlacklistAsWhitelist = VelocityMain.getInstance().getRootNode().node("Settings", "UseBlacklistAsWhitelist").getBoolean(false);
			SwapServerMessageRequires = VelocityMain.getInstance().getRootNode().node("Settings", "SwapServerMessageRequires").getString("ANY").toUpperCase();

			ServerJoinMessageDisabled = VelocityMain.getInstance().getRootNode().node("Settings", "IgnoreJoinMessagesList").getList(String.class, new ArrayList<>());
			ServerLeaveMessageDisabled = VelocityMain.getInstance().getRootNode().node("Settings", "IgnoreLeaveMessagesList").getList(String.class, new ArrayList<>());
		} catch (SerializationException serializationException) {
			serializationException.printStackTrace();
			return;
		}
		
		//Verify Swap Server Message
		switch(SwapServerMessageRequires) {
		case "JOINED":
		case "LEFT":
		case "BOTH":
		case "ANY":
			break;
		default:
			MessageHandler.getInstance().log("Setting error: Settings.SwapServerMessageRequires "
					+ "only allows JOINED LEFT BOTH or ANY. Got " + SwapServerMessageRequires +
					"Defaulting to ANY.");
			this.SwapServerMessageRequires = "ANY";
		}
		
		MessageHandler.getInstance().log("Config has been loaded.");
		
	}
	
	public boolean isSwapServerMessageEnabled() {
		return SwapServerMessageEnabled;
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
	
	public boolean getAdminMessageState(Player p) {
		if(p.hasPermission("networkjoinmessages.silent")) {
			if(messageState.containsKey(p.getUniqueId())) {
				return messageState.get(p.getUniqueId());
			} else {
				boolean state = VelocityMain.getInstance().getRootNode().node("Settings", "SilentJoinDefaultState").getBoolean(true);
				messageState.put(p.getUniqueId(), state);
				return state;
			}
		} else {
			return false; //Is not silent by default as they don't have silent perm..
		}
	}
	
	public void setAdminMessageState(Player player, Boolean state) {
		messageState.put(player.getUniqueId(), state);
	}
	
	public Boolean isConnected(Player p) {
		return onlinePlayers.contains(p.getUniqueId());
	}
	
	public void setConnected(Player p, Boolean state) {
		if(state) {
			if(!isConnected(p)) {
				onlinePlayers.add(p.getUniqueId());
			}
		} else {
			if(isConnected(p)) {
				onlinePlayers.remove(p.getUniqueId());
			}
		}
	}

	public String getFrom(Player p) {
		if(previousServer.containsKey(p)) {
			return previousServer.get(p);
		} else {
			return p.getCurrentServer().get().getServerInfo().getName();
		}
	}
	
	public void setFrom(Player p, String name) {
		previousServer.put(p, name);
	}

	
	public boolean isElsewhere(Player player) {
		return previousServer.containsKey(player);
	}

	public void clearPlayer(Player player) {
		previousServer.remove(player);
		
	}
	
	private void setJoinState(UUID id, boolean state) {
		if(state) {
			noJoinMessage.remove(id);
		} else {
			if(!noJoinMessage.contains(id)) { //Prevent duplicates.
				noJoinMessage.add(id);
			}
		}
	}
	private void setLeaveState(UUID id, boolean state) {
		if(state) {
			noLeaveMessage.remove(id);
		} else {
			if(!noLeaveMessage.contains(id)) { //Prevent duplicates.
				noLeaveMessage.add(id);
			}
		}
	}
	private void setSwitchState(UUID id, boolean state) {
		if(state) {
			noSwitchMessage.remove(id);
		} else {
			if(!noSwitchMessage.contains(id)) { //Prevent duplicates.
				noSwitchMessage.add(id);
			}
		}
	}
	
	public void setSendMessageState(String list, UUID id, boolean state) {
		switch(list) {
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
		switch(type) {
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

	public List<Player> getSwitchMessageReceivers(String to, String from) {
		List<Player> receivers = new ArrayList<>();
		//If all are true, add all players:
		if(SwapViewableByJoined && SwapViewableByLeft && SwapViewableByOther) {
			receivers.addAll(VelocityMain.getInstance().getProxy().getAllPlayers());
			return receivers;
		} 

		//Other server is true, but atleast one of the to or from are set to false:
		else if(SwapViewableByOther){
			receivers.addAll(VelocityMain.getInstance().getProxy().getAllPlayers());
			//Players on the connected server is not allowed to see. Remove them all.
			if(!SwapViewableByJoined) {
				receivers.removeAll(getServerPlayers(to));
			}
			
			if(!SwapViewableByLeft) {
				receivers.removeAll(getServerPlayers(from));
			}
			return receivers;
		} 
		
		//OtherServer is false.
		else {
			if(SwapViewableByJoined) {
				receivers.addAll(getServerPlayers(to));
			}
			
			if(SwapViewableByLeft) {
				receivers.addAll(getServerPlayers(from));
			}
			return receivers;
		}
	}
	
	public List<Player> getJoinMessageReceivers(String server) {
		List<Player> receivers = new ArrayList<>();
		//If all are true, add all players:
		if(JoinViewableByJoined && JoinViewableByOther) {
			receivers.addAll(VelocityMain.getInstance().getProxy().getAllPlayers());
			return receivers;
		} 

		//Other server is true, but atleast one of the to or from are set to false:
		else if(JoinViewableByOther){
			receivers.addAll(VelocityMain.getInstance().getProxy().getAllPlayers());
			receivers.removeAll(getServerPlayers(server));
			return receivers;
		} 
		else {
			if(JoinViewableByJoined) {
				receivers.addAll(getServerPlayers(server));
			}
			return receivers;
		}
	}
	
	public List<Player> getLeaveMessageReceivers(String server) {
		List<Player> receivers = new ArrayList<>();
		//If all are true, add all players:
		if(LeftViewableByLeft && LeftViewableByOther) {
			receivers.addAll(VelocityMain.getInstance().getProxy().getAllPlayers());
			return receivers;
		} 

		//Other server is true, but atleast one of the to or from are set to false:
		else if(LeftViewableByOther){
			receivers.addAll(VelocityMain.getInstance().getProxy().getAllPlayers());
			receivers.removeAll(getServerPlayers(server));
			return receivers;
		} 
		else {
			if(LeftViewableByLeft) {
				receivers.addAll(getServerPlayers(server));
			}
			return receivers;
		}
	}
	
	
	public List<Player> getServerPlayers(String serverName){
		Optional<RegisteredServer> registeredServer = VelocityMain.getInstance().getProxy().getServer(serverName);
		return registeredServer.map(server -> new ArrayList<>(server.getPlayersConnected())).orElseGet(ArrayList::new);
	}

	
	public boolean blacklistCheck(Player player) {
		if(!player.getCurrentServer().isPresent()) {
			MessageHandler.getInstance().log("Warning: Server of " + player.getUsername() + " came back as Null. Blackisted Server check failed. #01");
			return false;
		}
		String server = player.getCurrentServer().get().getServerInfo().getName();
		//Null check because of Geyser issues.
		if(server == null) {
			MessageHandler.getInstance().log("Warning: Server of " + player.getUsername() + " came back as Null. Blackisted Server check failed. #02");
			return false;
		}
		
		if(BlacklistedServers == null) {
			MessageHandler.getInstance().log("Warning: Blacklisted servers returned null instead of an empty list...");
			return false;
		}
		boolean listed = BlacklistedServers.contains(server);
		if(useBlacklistAsWhitelist) {
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
			if(from != null) {
				fromListed = BlacklistedServers.contains(from);
			}

		boolean toListed = false;
			if(to != null) {
				toListed = BlacklistedServers.contains(to);
			}

		boolean result = true;
		switch(SwapServerMessageRequires.toUpperCase()) {
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
		if(useBlacklistAsWhitelist) {
			//WHITELIST
				return !result;
			} else {
			//BLACKLIST
				return result;
			}
	}

	
	public List<UUID> getIgnoredServerPlayers(String type) {
		List<UUID> ignored = new ArrayList<UUID>();
		if(type.equalsIgnoreCase("join")) {
			for(String s : ServerJoinMessageDisabled) {
				Optional<RegisteredServer> registeredServer = VelocityMain.getInstance().getProxy().getServer(s);
				if(registeredServer.isPresent()) {
					for(Player p : registeredServer.get().getPlayersConnected()) {
						ignored.add(p.getUniqueId());
					}
				}
			}
		} else if(type.equalsIgnoreCase("leave")) {
			for(String s : ServerLeaveMessageDisabled) {
				Optional<RegisteredServer> registeredServer = VelocityMain.getInstance().getProxy().getServer(s);
				if(registeredServer.isPresent()) {
					for(Player p : registeredServer.get().getPlayersConnected()) {
						ignored.add(p.getUniqueId());
					}
				}
			}
		}
		return ignored;
	}
}
