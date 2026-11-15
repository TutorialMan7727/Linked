package uk.vrtxx.linked.inventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
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
import org.bukkit.event.player.PlayerInteractEvent;
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
  private boolean mutationPending;

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

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInventoryClick(InventoryClickEvent event) {
    if (event.getWhoClicked() instanceof Player player) {
      handleMutation(player, event);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInventoryDrag(InventoryDragEvent event) {
    if (event.getWhoClicked() instanceof Player player) {
      handleMutation(player, event);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPickup(EntityPickupItemEvent event) {
    if (event.getEntity() instanceof Player player) {
      handleMutation(player, event);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onDrop(PlayerDropItemEvent event) {
    handleMutation(event.getPlayer(), event);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onConsume(PlayerItemConsumeEvent event) {
    handleMutation(event.getPlayer(), event);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onItemDamage(PlayerItemDamageEvent event) {
    handleMutation(event.getPlayer(), event);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onSwap(PlayerSwapHandItemsEvent event) {
    handleMutation(event.getPlayer(), event);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onBlockPlace(BlockPlaceEvent event) {
    handleMutation(event.getPlayer(), event);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onBucketEmpty(PlayerBucketEmptyEvent event) {
    handleMutation(event.getPlayer(), event);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onBucketFill(PlayerBucketFillEvent event) {
    handleMutation(event.getPlayer(), event);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onBowShoot(EntityShootBowEvent event) {
    Entity shooter = event.getEntity();
    if (shooter instanceof Player player) {
      handleMutation(player, event);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onCrossbowInteract(PlayerInteractEvent event) {
    if (!linkSettings.isInventoryLinkEnabled()) {
      return;
    }

    Material type = event.getItem() != null ? event.getItem().getType() : null;

    if (type != Material.CROSSBOW) {
      return;
    }

    handleMutation(event.getPlayer(), event);
  }

  private void handleMutation(Player player, Cancellable event) {
    if (!linkSettings.isInventoryLinkEnabled()) {
      return;
    }

    if (mutationPending) {
      event.setCancelled(true);
      return;
    }

    mutationPending = true;
    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              if (player.isOnline()) {
                sharedInventory.capture(player);
              }
              sharedInventory.syncAllPlayers();
              mutationPending = false;
            });
  }
}
