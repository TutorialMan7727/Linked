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
import uk.vrtxx.linked.progress.ProgressLinkListener;
import uk.vrtxx.linked.progress.SharedProgress;
import uk.vrtxx.linked.respawn.SharedRespawnListener;
import uk.vrtxx.linked.rest.InsomniaLinkListener;

public final class Linked extends JavaPlugin {

  private SharedInventory sharedInventory;
  private LinkSettings linkSettings;
  private EffectLinkListener effectLinkListener;
  private HealthLinkListener healthLinkListener;
  private ProgressLinkListener progressLinkListener;
  private SharedProgress sharedProgress;
  private SharedRespawnListener sharedRespawnListener;
  private InsomniaLinkListener insomniaLinkListener;

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
    sharedProgress = new SharedProgress(this);
    sharedProgress.load();

    if (!sharedInventory.isInitialized() && !Bukkit.getOnlinePlayers().isEmpty()) {
      Player firstPlayer = Bukkit.getOnlinePlayers().iterator().next();
      sharedInventory.capture(firstPlayer);
    }

    healthLinkListener = new HealthLinkListener(this);
    effectLinkListener = new EffectLinkListener(this);
    progressLinkListener = new ProgressLinkListener(this, sharedProgress);
    sharedRespawnListener = new SharedRespawnListener(this);
    insomniaLinkListener = new InsomniaLinkListener(this);

    getServer()
        .getPluginManager()
        .registerEvents(new InventoryLinkListener(this, sharedInventory, linkSettings), this);
    getServer()
        .getPluginManager()
        .registerEvents(new DeathLinkListener(this, sharedInventory, linkSettings), this);
    getServer().getPluginManager().registerEvents(healthLinkListener, this);
    getServer().getPluginManager().registerEvents(effectLinkListener, this);
    getServer().getPluginManager().registerEvents(progressLinkListener, this);
    getServer().getPluginManager().registerEvents(sharedRespawnListener, this);
    getServer().getPluginManager().registerEvents(insomniaLinkListener, this);
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
    if (linkSettings.isProgressLinkEnabled()) {
      progressLinkListener.syncAll();
    }
    if (linkSettings.isRespawnLinkEnabled()) {
      sharedRespawnListener.syncAll();
    }
    if (linkSettings.isInsomniaLinkEnabled()) {
      insomniaLinkListener.syncAll();
    }
    Bukkit.getScheduler().runTaskTimer(this, sharedInventory::save, 6000L, 6000L);

    getLogger()
        .info(
            "Linked enabled: inventory, health, effects, death, progress, respawn, and insomnia links are active.");
  }

  @Override
  public void onDisable() {
    if (sharedInventory != null) {
      sharedInventory.save();
    }
    if (sharedProgress != null) {
      sharedProgress.save();
    }

    getLogger().info("Linked disabled.");
  }

  public boolean isFeatureEnabled(LinkFeature feature) {
    return switch (feature) {
      case INVENTORY -> linkSettings.isInventoryLinkEnabled();
      case HEALTH -> linkSettings.isHealthLinkEnabled();
      case EFFECT -> linkSettings.isEffectLinkEnabled();
      case DEATH -> linkSettings.isDeathLinkEnabled();
      case PROGRESS -> linkSettings.isProgressLinkEnabled();
      case RESPAWN -> linkSettings.isRespawnLinkEnabled();
      case INSOMNIA -> linkSettings.isInsomniaLinkEnabled();
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
      case PROGRESS -> {
        linkSettings.setProgressLinkEnabled(enabled);
        if (enabled) {
          progressLinkListener.syncAll();
        }
      }
      case RESPAWN -> {
        linkSettings.setRespawnLinkEnabled(enabled);
        if (enabled) {
          sharedRespawnListener.syncAll();
        }
      }
      case INSOMNIA -> {
        linkSettings.setInsomniaLinkEnabled(enabled);
        if (enabled) {
          insomniaLinkListener.syncAll();
        }
      }
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
    if (linkSettings.isProgressLinkEnabled()) {
      progressLinkListener.syncAll();
    }
    if (linkSettings.isRespawnLinkEnabled()) {
      sharedRespawnListener.syncAll();
    }
    if (linkSettings.isInsomniaLinkEnabled()) {
      insomniaLinkListener.syncAll();
    }
  }
}
