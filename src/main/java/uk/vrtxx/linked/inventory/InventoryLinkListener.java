package uk.vrtxx.linked.inventory;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;
import uk.vrtxx.linked.config.LinkSettings;

/** Synchronizes all inventory-changing actions to the shared inventory. */
public final class InventoryLinkListener implements Listener {

  private final JavaPlugin plugin;
  private final SharedInventory sharedInventory;
  private final LinkSettings linkSettings;

  private final Set<UUID> pendingPlayers = new LinkedHashSet<>();
  private boolean syncScheduled;

  public InventoryLinkListener(
      JavaPlugin plugin, SharedInventory sharedInventory, LinkSettings linkSettings) {
    this.plugin = plugin;
    this.sharedInventory = sharedInventory;
    this.linkSettings = linkSettings;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              if (!linkSettings.isInventoryLinkEnabled()) {
                return;
              }

              if (!sharedInventory.isInitialized()) {
                sharedInventory.capture(event.getPlayer());
              }

              sharedInventory.syncAllPlayers();
            });
  }

  @EventHandler
  public void onRespawn(PlayerRespawnEvent event) {
    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              if (linkSettings.isInventoryLinkEnabled()) {
                sharedInventory.syncAllPlayers();
              }
            });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    if (event.getWhoClicked() instanceof Player player) {
      handleMutation(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInventoryDrag(InventoryDragEvent event) {
    if (event.getWhoClicked() instanceof Player player) {
      handleMutation(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPickup(EntityPickupItemEvent event) {
    if (event.getEntity() instanceof Player player) {
      handleMutation(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDrop(PlayerDropItemEvent event) {
    handleMutation(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onConsume(PlayerItemConsumeEvent event) {
    handleMutation(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemDamage(PlayerItemDamageEvent event) {
    handleMutation(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onArmorChange(PlayerArmorChangeEvent event) {
    handleMutation(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onSwap(PlayerSwapHandItemsEvent event) {
    handleMutation(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    handleMutation(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBucketEmpty(PlayerBucketEmptyEvent event) {
    handleMutation(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBucketFill(PlayerBucketFillEvent event) {
    handleMutation(event.getPlayer());
  }

  /** Called when a crossbow has finished loading its projectiles. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onCrossbowLoad(EntityLoadCrossbowEvent event) {
    Entity entity = event.getEntity();

    if (entity instanceof Player player) {
      handleMutation(player);
    }
  }

  /** Called when a bow or crossbow shoots a projectile. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBowShoot(EntityShootBowEvent event) {
    Entity shooter = event.getEntity();

    if (shooter instanceof Player player) {
      handleMutation(player);
    }
  }

  /**
   * Queues inventory changes and captures them on the next tick. Events are never cancelled by this
   * listener.
   */
  private void handleMutation(Player player) {
    if (!linkSettings.isInventoryLinkEnabled()) {
      return;
    }

    pendingPlayers.add(player.getUniqueId());

    if (syncScheduled) {
      return;
    }

    syncScheduled = true;

    Bukkit.getScheduler().runTask(plugin, this::processPendingMutations);
  }

  private void processPendingMutations() {
    try {
      if (!linkSettings.isInventoryLinkEnabled()) {
        pendingPlayers.clear();
        return;
      }

      // Capture every player before applying the shared inventory.
      // This prevents one player's synchronization from overwriting
      // another player's pending inventory changes.
      for (UUID uuid : pendingPlayers) {
        Player player = Bukkit.getPlayer(uuid);

        if (player != null && player.isOnline()) {
          sharedInventory.capture(player);
        }
      }

      pendingPlayers.clear();
      sharedInventory.syncAllPlayers();
    } finally {
      syncScheduled = false;

      // Schedule another pass if more changes arrived.
      if (!pendingPlayers.isEmpty()) {
        Player player = Bukkit.getPlayer(pendingPlayers.iterator().next());

        if (player != null) {
          handleMutation(player);
        }
      }
    }
  }
}
