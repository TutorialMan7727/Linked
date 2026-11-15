package uk.vrtxx.linked.death;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import uk.vrtxx.linked.config.LinkSettings;
import uk.vrtxx.linked.inventory.SharedInventory;

/** Makes a player's death fatal for every other alive online player. */
public final class DeathLinkListener implements Listener {

  private final JavaPlugin plugin;
  private final SharedInventory sharedInventory;
  private final LinkSettings linkSettings;
  private final Set<UUID> propagatedDeaths = new HashSet<>();
  private boolean propagatingDeath;

  public DeathLinkListener(
      JavaPlugin plugin, SharedInventory sharedInventory, LinkSettings linkSettings) {
    this.plugin = plugin;
    this.sharedInventory = sharedInventory;
    this.linkSettings = linkSettings;
  }

  /**
   * A successful totem resurrection is observed only after other listeners have had their say. It
   * therefore never enters the death cascade and its benefit is applied to the entire group.
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onResurrect(EntityResurrectEvent event) {
    if (!linkSettings.isDeathLinkEnabled() || !(event.getEntity() instanceof Player player)) {
      return;
    }

    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              if (!player.isOnline() || player.isDead()) {
                return;
              }
              // The totem has now been consumed. Capture once before sharing the result.
              sharedInventory.capture(player);
              sharedInventory.syncAllPlayers();
              applyTotemRescue(player);
            });
  }

  @EventHandler(priority = EventPriority.MONITOR)
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
        propagatedDeaths.add(player.getUniqueId());
        player.setHealth(0.0D);
      }
    }

    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              propagatingDeath = false;
              propagatedDeaths.clear();
            });
  }

  /** Keeps stat-backed death scoreboards from counting each linked victim as a separate mistake. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onStatisticIncrement(PlayerStatisticIncrementEvent event) {
    if (event.getStatistic() == Statistic.DEATHS
        && propagatingDeath
        && propagatedDeaths.contains(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  private static void applyTotemRescue(Player source) {
    double sharedHealth = Math.max(1.0D, source.getHealth());
    PotionEffect regeneration = new PotionEffect(PotionEffectType.REGENERATION, 900, 1);
    PotionEffect absorption = new PotionEffect(PotionEffectType.ABSORPTION, 100, 1);
    PotionEffect fireResistance = new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0);
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!player.isDead()) {
        player.setHealth(Math.min(sharedHealth, maxHealth(player)));
        player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), 8.0D));
        player.addPotionEffect(regeneration, true);
        player.addPotionEffect(absorption, true);
        player.addPotionEffect(fireResistance, true);
      }
    }
  }

  private static double maxHealth(Player player) {
    AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
    return attribute == null ? 20.0D : attribute.getValue();
  }
}
