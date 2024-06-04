package xyz.earthcow.networkjoinmessages.velocity.listeners;

public class VanishListener {

	/*@Subscribe
	public void playerHideEvent(PlayerHideEvent e) {
		Player player = e.getPlayer();
		if (VelocityMain.getInstance().getRootNode().node("OtherPlugins", "PremiumVanish", "ToggleFakemessageWhenVanishing").getBoolean(true)) {
			if (player.hasPermission("networkjoinmessages.silent")) {
				Storage.getInstance().setAdminMessageState(player, true);
				VelocityMain.getInstance().getLogger().info("PremiumVanish has toggled the MessageState of " + player.getUsername() + " to true");
			}
		}
	}

	@Subscribe
	public void playerShowEvent(PlayerShowEvent e) {
		Player player = e.getPlayer();
		VelocityMain.getInstance().getLogger().info("playerShowEvent triggered");
		if (VelocityMain.getInstance().getRootNode().node("OtherPlugins", "PremiumVanish", "ToggleFakemessageWhenVanishing").getBoolean(true)) {
			if (player.hasPermission("networkjoinmessages.silent")) {
				Storage.getInstance().setAdminMessageState(player, false);
				VelocityMain.getInstance().getLogger().info("PremiumVanish has toggled the MessageState of " + player.getUsername() + " to false");
			}
		}
	}*/
}
