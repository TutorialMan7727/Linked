package uk.vrtxx.linked.progress;

import java.util.Collection;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import uk.vrtxx.linked.Linked;
import uk.vrtxx.linked.command.LinkedCommand.LinkFeature;

/** Grants every advancement and recipe earned by one group member to the whole group. */
public final class ProgressLinkListener implements Listener {

  private final Linked plugin;
  private final SharedProgress sharedProgress;
  private boolean granting;

  public ProgressLinkListener(Linked plugin, SharedProgress sharedProgress) {
    this.plugin = plugin;
    this.sharedProgress = sharedProgress;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onAdvancement(PlayerAdvancementDoneEvent event) {
    if (!enabled() || granting) {
      return;
    }
    if (sharedProgress.addAdvancement(event.getAdvancement().getKey().toString())) {
      sharedProgress.save();
    }
    grantAdvancementToOnlinePlayers(event.getAdvancement());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onRecipeDiscover(PlayerRecipeDiscoverEvent event) {
    if (!enabled() || granting) {
      return;
    }
    boolean changed = false;
    NamespacedKey recipe = event.getRecipe();
    changed = sharedProgress.addRecipe(recipe.toString());
    if (changed) {
      sharedProgress.save();
    }
    grantRecipes(java.util.List.of(recipe), Bukkit.getOnlinePlayers());
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Bukkit.getScheduler().runTask(plugin, () -> syncPlayer(event.getPlayer()));
  }

  /** Applies the durable group progress to every online player. */
  public void syncAll() {
    if (!enabled()) {
      return;
    }
    for (Player player : Bukkit.getOnlinePlayers()) {
      syncPlayer(player);
    }
  }

  private void syncPlayer(Player player) {
    if (!enabled() || !player.isOnline()) {
      return;
    }
    granting = true;
    try {
      for (String value : sharedProgress.advancements()) {
        NamespacedKey key = NamespacedKey.fromString(value);
        Advancement advancement = key == null ? null : Bukkit.getAdvancement(key);
        if (advancement != null) {
          grant(player, advancement);
        }
      }
      for (String value : sharedProgress.recipes()) {
        NamespacedKey key = NamespacedKey.fromString(value);
        if (key != null) {
          player.discoverRecipe(key);
        }
      }
    } finally {
      granting = false;
    }
  }

  private void grantAdvancementToOnlinePlayers(Advancement advancement) {
    granting = true;
    try {
      for (Player player : Bukkit.getOnlinePlayers()) {
        grant(player, advancement);
      }
    } finally {
      granting = false;
    }
  }

  private void grantRecipes(
      Collection<NamespacedKey> recipes, Collection<? extends Player> players) {
    granting = true;
    try {
      for (Player player : players) {
        for (NamespacedKey recipe : recipes) {
          player.discoverRecipe(recipe);
        }
      }
    } finally {
      granting = false;
    }
  }

  private static void grant(Player player, Advancement advancement) {
    AdvancementProgress progress = player.getAdvancementProgress(advancement);
    for (String criterion : progress.getRemainingCriteria()) {
      progress.awardCriteria(criterion);
    }
  }

  private boolean enabled() {
    return plugin.isFeatureEnabled(LinkFeature.PROGRESS);
  }
}
