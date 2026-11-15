package uk.vrtxx.linked.effect;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import uk.vrtxx.linked.Linked;
import uk.vrtxx.linked.command.LinkedCommand.LinkFeature;

/** Keeps active potion effects synchronized for every online player. */
public final class EffectLinkListener implements Listener {

  private final Linked plugin;
  private boolean syncing;
  private boolean syncScheduled;
  private Player pendingSource;

  public EffectLinkListener(Linked plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPotionEffectChange(EntityPotionEffectEvent event) {
    if (event.getEntity() instanceof Player player) {
      scheduleSync(player);
    }
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    scheduleSyncFromAnotherPlayer(event.getPlayer());
  }

  @EventHandler
  public void onRespawn(PlayerRespawnEvent event) {
    scheduleSyncFromAnotherPlayer(event.getPlayer());
  }

  /** Copies the first online, alive player's effects to every other alive player. */
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

  private boolean enabled() {
    return plugin.isFeatureEnabled(LinkFeature.EFFECT);
  }

  private void scheduleSyncFromAnotherPlayer(Player target) {
    if (!enabled()) {
      return;
    }

    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              if (!enabled() || !isAlive(target)) {
                return;
              }

              Player source = findSource(target);
              if (source != null) {
                apply(source, target);
              }
            });
  }

  private void scheduleSync(Player source) {
    if (!enabled() || syncing) {
      return;
    }

    // Potion effects are applied after the event has fired. Use the final state next tick and
    // coalesce same-tick changes so an outdated source cannot overwrite a newer group state.
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
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player != source && isAlive(player)) {
          apply(source, player);
        }
      }
    } finally {
      syncing = false;
    }
  }

  private void apply(Player source, Player target) {
    Map<PotionEffectType, PotionEffect> sourceEffects = effectsByType(source);

    for (PotionEffect targetEffect : target.getActivePotionEffects()) {
      PotionEffect sourceEffect = sourceEffects.get(targetEffect.getType());
      if (!targetEffect.equals(sourceEffect)) {
        target.removePotionEffect(targetEffect.getType());
      }
    }

    for (PotionEffect sourceEffect : sourceEffects.values()) {
      PotionEffect targetEffect = target.getPotionEffect(sourceEffect.getType());
      if (!sourceEffect.equals(targetEffect)) {
        target.addPotionEffect(sourceEffect);
      }
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

  private static Map<PotionEffectType, PotionEffect> effectsByType(Player player) {
    Map<PotionEffectType, PotionEffect> effects = new HashMap<>();
    for (PotionEffect effect : player.getActivePotionEffects()) {
      effects.put(effect.getType(), effect);
    }
    return effects;
  }

  private static boolean isAlive(Player player) {
    return player.isOnline() && !player.isDead() && player.getHealth() > 0.0D;
  }
}
