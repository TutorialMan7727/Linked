package uk.vrtxx.linked;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import uk.vrtxx.linked.command.LinkedCommand;
import uk.vrtxx.linked.command.LinkedCommand.LinkFeature;
import uk.vrtxx.linked.config.LinkSettings;
import uk.vrtxx.linked.death.DeathLinkListener;
import uk.vrtxx.linked.effect.EffectLinkListener;
import uk.vrtxx.linked.health.HealthLinkListener;
import uk.vrtxx.linked.inventory.InventoryLinkListener;
import uk.vrtxx.linked.inventory.SharedInventory;

public final class Linked extends JavaPlugin {

  private SharedInventory sharedInventory;
  private LinkSettings linkSettings;
  private EffectLinkListener effectLinkListener;
  private HealthLinkListener healthLinkListener;

  @Override
  public void onEnable() {
    if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
      getLogger().severe("Could not create the plugin data folder.");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    linkSettings = new LinkSettings(this);
    sharedInventory = new SharedInventory(this);
    sharedInventory.load();

    if (!sharedInventory.isInitialized() && !Bukkit.getOnlinePlayers().isEmpty()) {
      Player firstPlayer = Bukkit.getOnlinePlayers().iterator().next();
      sharedInventory.capture(firstPlayer);
    }

    healthLinkListener = new HealthLinkListener(this);
    effectLinkListener = new EffectLinkListener(this);

    getServer()
        .getPluginManager()
        .registerEvents(new InventoryLinkListener(this, sharedInventory, linkSettings), this);
    getServer()
        .getPluginManager()
        .registerEvents(new DeathLinkListener(this, sharedInventory, linkSettings), this);
    getServer().getPluginManager().registerEvents(healthLinkListener, this);
    getServer().getPluginManager().registerEvents(effectLinkListener, this);
    LinkedCommand linkedCommand = new LinkedCommand(this);
    Objects.requireNonNull(getCommand("linked"), "Missing linked command definition")
        .setExecutor(linkedCommand);
    Objects.requireNonNull(getCommand("linked"), "Missing linked command definition")
        .setTabCompleter(linkedCommand);

    if (linkSettings.isInventoryLinkEnabled()) {
      sharedInventory.syncAllPlayers();
    }
    if (linkSettings.isHealthLinkEnabled()) {
      healthLinkListener.syncAll();
    }
    if (linkSettings.isEffectLinkEnabled()) {
      effectLinkListener.syncAll();
    }
    Bukkit.getScheduler().runTaskTimer(this, sharedInventory::save, 6000L, 6000L);

    getLogger()
        .info("Linked enabled: shared inventory, health, effect, and death links are active.");
  }

  @Override
  public void onDisable() {
    if (sharedInventory != null) {
      sharedInventory.save();
    }

    getLogger().info("Linked disabled.");
  }

  public boolean isFeatureEnabled(LinkFeature feature) {
    return switch (feature) {
      case INVENTORY -> linkSettings.isInventoryLinkEnabled();
      case HEALTH -> linkSettings.isHealthLinkEnabled();
      case EFFECT -> linkSettings.isEffectLinkEnabled();
      case DEATH -> linkSettings.isDeathLinkEnabled();
    };
  }

  public void setFeatureEnabled(LinkFeature feature, boolean enabled) {
    switch (feature) {
      case INVENTORY -> {
        linkSettings.setInventoryLinkEnabled(enabled);
        if (enabled) {
          sharedInventory.syncAllPlayers();
        }
      }
      case HEALTH -> {
        linkSettings.setHealthLinkEnabled(enabled);
        if (enabled) {
          healthLinkListener.syncAll();
        }
      }
      case EFFECT -> {
        linkSettings.setEffectLinkEnabled(enabled);
        if (enabled) {
          effectLinkListener.syncAll();
        }
      }
      case DEATH -> linkSettings.setDeathLinkEnabled(enabled);
    }
  }

  public void reloadLinkSettings() {
    linkSettings.reload();
    if (linkSettings.isInventoryLinkEnabled()) {
      sharedInventory.syncAllPlayers();
    }
    if (linkSettings.isHealthLinkEnabled()) {
      healthLinkListener.syncAll();
    }
    if (linkSettings.isEffectLinkEnabled()) {
      effectLinkListener.syncAll();
    }
  }
}
