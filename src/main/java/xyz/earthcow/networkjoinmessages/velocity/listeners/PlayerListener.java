package xyz.earthcow.networkjoinmessages.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import xyz.earthcow.networkjoinmessages.velocity.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.velocity.events.NetworkQuitEvent;
import xyz.earthcow.networkjoinmessages.velocity.events.SwapServerEvent;
import xyz.earthcow.networkjoinmessages.velocity.general.Storage;
import xyz.earthcow.networkjoinmessages.velocity.general.VelocityMain;
import xyz.earthcow.networkjoinmessages.velocity.util.HexChat;
import xyz.earthcow.networkjoinmessages.velocity.util.MessageHandler;

import java.util.concurrent.TimeUnit;

public class PlayerListener {

	private final String silent = VelocityMain.getInstance().getRootNode().node("Messages", "Misc", "SilentPrefix").getString("&7[Silent] ");

	@Subscribe
	public void onPreConnect(ServerPreConnectEvent event) {
		Player player = event.getPlayer();
		if (player == null) {
			return;
		}

		ServerInfo serverInfo = event.getOriginalServer().getServerInfo();
		String serverName = serverInfo.getName();
		if (serverName != null) {
			Storage.getInstance().setFrom(player, serverName);
		}
	}

	@Subscribe
	public void onServerConnected(ServerConnectedEvent event) {
		Player player = event.getPlayer();
		RegisteredServer server = event.getServer();

		if (!Storage.getInstance().isConnected(player)) {
			return;
		}

		String to = server.getServerInfo().getName();
		String from = "???";
		if (Storage.getInstance().isElsewhere(player)) {
			from = Storage.getInstance().getFrom(player);
		} else {
			return;
		}

		if (Storage.getInstance().isSwapServerMessageEnabled()) {

			if (Storage.getInstance().blacklistCheck(from, to)) {
				return;
			}

			String message = MessageHandler.getInstance().formatSwitchMessage(player, from, to);

			// Silent
			if (Storage.getInstance().getAdminMessageState(player)) {
				VelocityMain.getInstance().SilentEvent("MOVE", player.getUsername(), from, to);
				if (Storage.getInstance().notifyAdminsOnSilentMove()) {
					Component silentMessage = Component.text(HexChat.translateHexCodes(silent + message));
					for (Player p : VelocityMain.getInstance().getProxy().getAllPlayers()) {
						if (p.hasPermission("networkjoinmessages.silent")) {
							p.sendMessage(silentMessage);
						}
					}
				}
			} else {
				MessageHandler.getInstance().broadcastMessage(HexChat.translateHexCodes(message), "switch", from, to);
			}

			// Call the custom ServerSwapEvent
			SwapServerEvent swapServerEvent = new SwapServerEvent(player, MessageHandler.getInstance().getServerName(from), MessageHandler.getInstance().getServerName(to), Storage.getInstance().getAdminMessageState(player), message);
			VelocityMain.getInstance().getProxy().getEventManager().fireAndForget(swapServerEvent);
		}
	}

	@Subscribe
	public void onLogin(LoginEvent event) {
		Player player = event.getPlayer();
		if (player == null) {
			return;
		}

		ScheduledTask task = VelocityMain.getInstance().getProxy().getScheduler().buildTask(VelocityMain.getInstance(), () -> {
			if (player.isActive()) {
				while (!player.getCurrentServer().isPresent()) {
					try {
						VelocityMain.getInstance().getLogger().warn(player.getUsername() + "'s SERVER IS NULL WAITING A SECOND!!");
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}

				Storage.getInstance().setConnected(player, true);
				if (!Storage.getInstance().isJoinNetworkMessageEnabled()) {
					return;
				}
				String message = MessageHandler.getInstance().formatJoinMessage(player);

				// VanishAPI support
				if (VelocityMain.getInstance().VanishAPI) {
					if (VelocityMain.getInstance().getRootNode().node("OtherPlugins", "PremiumVanish", "ToggleFakemessageWhenVanishing").getBoolean(false)) {
						//Storage.getInstance().setAdminMessageState(player, VelocityAPI.isInvisible(player));
					}
				}

				// Blacklist Check
				if (Storage.getInstance().blacklistCheck(player)) {
					return;
				}

				// Silent
				if (Storage.getInstance().getAdminMessageState(player)) {
					if (player.hasPermission("networkjoinmessages.fakemessage")) {
						String toggleNotif = VelocityMain.getInstance().getRootNode().node("Messages", "Commands", "Fakemessage", "JoinNotification").getString("&7[BungeeJoin] You joined the server while silenced.\n" + "&7To have messages automatically enabled for you until\n" + "&7next reboot, use the command &f/fm toggle&7.");
						player.sendMessage(Component.text(HexChat.translateHexCodes(toggleNotif)));
					}

					// Send to console
					VelocityMain.getInstance().SilentEvent("JOIN", player.getUsername());
					// Send to admin players
					if (Storage.getInstance().notifyAdminsOnSilentMove()) {
						Component silentMessage = Component.text(HexChat.translateHexCodes(silent + message));
						for (Player p : VelocityMain.getInstance().getProxy().getAllPlayers()) {
							if (p.hasPermission("networkjoinmessages.silent")) {
								p.sendMessage(silentMessage);
							}
						}
					}
				} else {
					MessageHandler.getInstance().broadcastMessage(HexChat.translateHexCodes(message), "join", player);
				}

				// All checks have passed to reach this point
				// Call the custom NetworkJoinEvent
				NetworkJoinEvent networkJoinEvent = new NetworkJoinEvent(player, MessageHandler.getInstance().getServerName(player.getCurrentServer().get().getServerInfo().getName()), Storage.getInstance().getAdminMessageState(player), message);
				VelocityMain.getInstance().getProxy().getEventManager().fireAndForget(networkJoinEvent);
			}
		}).delay(VelocityMain.getInstance().getRootNode().node("Messages", "Misc", "JoinMessageDelaySeconds").getInt(3), TimeUnit.SECONDS).schedule();
	}

	@Subscribe
	public void onDisconnect(DisconnectEvent event) {
		Player player = event.getPlayer();
		if (player == null) {
			return;
		}

		if (!Storage.getInstance().isConnected(player)) {
			return;
		}

		if (!Storage.getInstance().isJoinNetworkMessageEnabled()) {
			Storage.getInstance().setConnected(player, false);
			return;
		}

		if (Storage.getInstance().blacklistCheck(player)) {
			return;
		}

		String message = MessageHandler.getInstance().formatQuitMessage(player);

		// Silent
		if (Storage.getInstance().getAdminMessageState(player)) {
			VelocityMain.getInstance().SilentEvent("QUIT", player.getUsername());
			if (Storage.getInstance().notifyAdminsOnSilentMove()) {
				Component silentMessage = Component.text(HexChat.translateHexCodes(silent + message));
				for (Player p : VelocityMain.getInstance().getProxy().getAllPlayers()) {
					if (p.hasPermission("networkjoinmessages.silent")) {
						p.sendMessage(silentMessage);
					}
				}
			}
		} else {
			MessageHandler.getInstance().broadcastMessage(HexChat.translateHexCodes(message), "leave", player);
		}

		Storage.getInstance().setConnected(player, false);

		// Call the custom NetworkQuitEvent
		NetworkQuitEvent networkQuitEvent = new NetworkQuitEvent(player, MessageHandler.getInstance().getServerName(player.getCurrentServer().get().getServerInfo().getName()), Storage.getInstance().getAdminMessageState(player), message);
		VelocityMain.getInstance().getProxy().getEventManager().fireAndForget(networkQuitEvent);
	}
}
