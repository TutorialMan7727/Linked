package uk.vrtxx.linked.respawn;

import java.io.File;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import uk.vrtxx.linked.Linked;
import uk.vrtxx.linked.command.LinkedCommand.LinkFeature;

/** Keeps the group's bed or respawn-anchor point as one durable respawn location. */
public final class SharedRespawnListener implements Listener {

  private final Linked plugin;
  private final File file;
  private Location sharedLocation;

  public SharedRespawnListener(Linked plugin) {
    this.plugin = plugin;
    file = new File(plugin.getDataFolder(), "shared-respawn.yml");
    load();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBedEnter(PlayerBedEnterEvent event) {
    if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
      setSharedLocation(event.getBed().getLocation());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onRespawnAnchor(PlayerInteractEvent event) {
    Block block = event.getClickedBlock();
    if (event.getAction() == Action.RIGHT_CLICK_BLOCK
        && block != null
        && block.getType() == Material.RESPAWN_ANCHOR
        && event.getItem() != null
        && event.getItem().getType() == Material.GLOWSTONE) {
      setSharedLocation(block.getLocation());
    }
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Bukkit.getScheduler().runTask(plugin, () -> apply(event.getPlayer()));
  }

  /** Applies the shared point to online players after enabling or reloading the link. */
  public void syncAll() {
    if (!enabled() || sharedLocation == null) {
      return;
    }
    for (Player player : Bukkit.getOnlinePlayers()) {
      apply(player);
    }
  }

  private void setSharedLocation(Location location) {
    if (!enabled() || location.getWorld() == null) {
      return;
    }
    sharedLocation = location.clone();
    save();
    syncAll();
  }

  private void apply(Player player) {
    if (enabled() && sharedLocation != null && player.isOnline()) {
      player.setRespawnLocation(sharedLocation.clone(), true);
    }
  }

  private void load() {
    if (file.exists()) {
      sharedLocation = YamlConfiguration.loadConfiguration(file).getLocation("location");
    }
  }

  private void save() {
    YamlConfiguration configuration = new YamlConfiguration();
    configuration.set("location", sharedLocation);
    try {
      configuration.save(file);
    } catch (IOException exception) {
      plugin.getLogger().severe("Could not save shared respawn point: " + exception.getMessage());
    }
  }

  private boolean enabled() {
    return plugin.isFeatureEnabled(LinkFeature.RESPAWN);
  }
}
