package uk.vrtxx.linked.rest;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import uk.vrtxx.linked.Linked;
import uk.vrtxx.linked.command.LinkedCommand.LinkFeature;

/** Shares TIME_SINCE_REST so sleeping and phantom pressure apply to the whole group. */
public final class InsomniaLinkListener implements Listener {

  private final Linked plugin;
  private boolean syncing;
  private boolean scheduled;
  private Player pendingSource;

  public InsomniaLinkListener(Linked plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onStatisticIncrement(PlayerStatisticIncrementEvent event) {
    if (event.getStatistic() == Statistic.TIME_SINCE_REST) {
      scheduleSync(event.getPlayer());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBedEnter(PlayerBedEnterEvent event) {
    if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
      scheduleSync(event.getPlayer());
    }
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Bukkit.getScheduler().runTask(plugin, () -> syncToJoiner(event.getPlayer()));
  }

  public void syncAll() {
    if (!enabled()) {
      return;
    }
    for (Player player : Bukkit.getOnlinePlayers()) {
      syncFrom(player);
      return;
    }
  }

  private void scheduleSync(Player source) {
    if (!enabled() || syncing) {
      return;
    }
    pendingSource = source;
    if (scheduled) {
      return;
    }
    scheduled = true;
    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              scheduled = false;
              Player sourceToSync = pendingSource;
              pendingSource = null;
              if (sourceToSync != null) {
                syncFrom(sourceToSync);
              }
            });
  }

  private void syncToJoiner(Player joiner) {
    if (!enabled() || !joiner.isOnline()) {
      return;
    }
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player != joiner) {
        setRestTime(joiner, player.getStatistic(Statistic.TIME_SINCE_REST));
        return;
      }
    }
  }

  private void syncFrom(Player source) {
    if (!enabled() || syncing || !source.isOnline()) {
      return;
    }
    syncing = true;
    try {
      int restTime = source.getStatistic(Statistic.TIME_SINCE_REST);
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player != source) {
          setRestTime(player, restTime);
        }
      }
    } finally {
      syncing = false;
    }
  }

  private static void setRestTime(Player player, int restTime) {
    player.setStatistic(Statistic.TIME_SINCE_REST, restTime);
  }

  private boolean enabled() {
    return plugin.isFeatureEnabled(LinkFeature.INSOMNIA);
  }
}
