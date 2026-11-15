package uk.vrtxx.linked.death;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import uk.vrtxx.linked.config.LinkSettings;
import uk.vrtxx.linked.inventory.SharedInventory;

/** Makes a player's death fatal for every other alive online player. */
public final class DeathLinkListener implements Listener {

  private final JavaPlugin plugin;
  private final SharedInventory sharedInventory;
  private final LinkSettings linkSettings;
  private boolean propagatingDeath;

  public DeathLinkListener(
      JavaPlugin plugin, SharedInventory sharedInventory, LinkSettings linkSettings) {
    this.plugin = plugin;
    this.sharedInventory = sharedInventory;
    this.linkSettings = linkSettings;
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    if (!linkSettings.isDeathLinkEnabled() || propagatingDeath) {
      return;
    }

    propagatingDeath = true;
    // Leave one set of items at the original death location and make every
    // later linked death empty. Respawns will receive the now-empty state.
    sharedInventory.clearAndSyncAllPlayers();
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!player.equals(event.getPlayer()) && !player.isDead()) {
        player.setHealth(0.0D);
      }
    }

    Bukkit.getScheduler().runTask(plugin, () -> propagatingDeath = false);
  }
}
