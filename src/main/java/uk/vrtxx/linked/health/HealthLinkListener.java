package uk.vrtxx.linked.health;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import uk.vrtxx.linked.Linked;
import uk.vrtxx.linked.command.LinkedCommand.LinkFeature;

/** Keeps health, hunger, saturation, and exhaustion synchronized for online players. */
public final class HealthLinkListener implements Listener {

  private final Linked plugin;
  private boolean syncing;
  private boolean syncScheduled;
  private Player pendingSource;

  public HealthLinkListener(Linked plugin) {
    this.plugin = plugin;
  }

  private boolean enabled() {
    return plugin.isFeatureEnabled(LinkFeature.HEALTH);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    if (event.getEntity() instanceof Player player) {
      scheduleSync(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onRegain(EntityRegainHealthEvent event) {
    if (event.getEntity() instanceof Player player) {
      scheduleSync(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onFoodChange(FoodLevelChangeEvent event) {
    if (event.getEntity() instanceof Player player) {
      scheduleSync(player);
    }
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (!enabled()) {
      return;
    }
    Player joiner = event.getPlayer();
    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              if (!enabled() || !isAlive(joiner)) {
                return;
              }
              Player source = findSource(joiner);
              if (source != null) {
                apply(source, joiner);
              }
            });
  }

  @EventHandler
  public void onRespawn(PlayerRespawnEvent event) {
    if (!enabled()) {
      return;
    }
    Player respawned = event.getPlayer();
    // Wait a tick so the player is alive again, then match everyone else who is still alive.
    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              if (!enabled() || !isAlive(respawned)) {
                return;
              }
              Player source = findSource(respawned);
              if (source != null) {
                apply(source, respawned);
              }
            });
  }

  /** Copies the first online, alive player's state to every other alive player. */
  public void syncAll() {
    if (!enabled()) {
      return;
    }
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (isAlive(player)) {
        syncFrom(player);
        return;
      }
    }
  }

  private void scheduleSync(Player source) {
    if (!enabled() || syncing) {
      return;
    }

    // Health and food are applied after their events fire, so read them next tick. Coalescing all
    // events that occur during this tick prevents older scheduled tasks from copying an already
    // overwritten player state back to the group.
    pendingSource = source;
    if (syncScheduled) {
      return;
    }
    syncScheduled = true;
    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              syncScheduled = false;
              Player sourceToSync = pendingSource;
              pendingSource = null;
              if (sourceToSync != null) {
                syncFrom(sourceToSync);
              }
            });
  }

  private void syncFrom(Player source) {
    if (!enabled() || syncing || !isAlive(source)) {
      return;
    }
    syncing = true;
    try {
      for (Player other : Bukkit.getOnlinePlayers()) {
        if (other != source && isAlive(other)) {
          apply(source, other);
        }
      }
    } finally {
      syncing = false;
    }
  }

  private void apply(Player source, Player target) {
    if (!isAlive(source) || !isAlive(target)) {
      return;
    }

    double previous = target.getHealth();
    double maximum = maxHealth(target);
    if (maximum <= 0.0D) {
      return;
    }

    double wanted = Math.min(source.getHealth(), maximum);
    // Never set 0 here: deaths are handled by the death link, not by syncing.
    target.setHealth(Math.max(wanted, Math.min(0.01D, maximum)));
    target.setFoodLevel(source.getFoodLevel());
    target.setSaturation(source.getSaturation());
    target.setExhaustion(source.getExhaustion());
    if (target.getHealth() < previous) {
      target.playHurtAnimation(0f);
    }
  }

  private Player findSource(Player exclude) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player != exclude && isAlive(player)) {
        return player;
      }
    }
    return null;
  }

  private static boolean isAlive(Player player) {
    return player.isOnline() && !player.isDead() && player.getHealth() > 0;
  }

  private static double maxHealth(Player player) {
    AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
    return attribute == null ? 20.0 : attribute.getValue();
  }
}
