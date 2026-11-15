package uk.vrtxx.linked.config;

import org.bukkit.plugin.java.JavaPlugin;

/** Persists the independently configurable link features. */
public final class LinkSettings {

  private static final String INVENTORY_LINK_ENABLED = "inventory-link.enabled";
  private static final String HEALTH_LINK_ENABLED = "health-link.enabled";
  private static final String EFFECT_LINK_ENABLED = "effect-link.enabled";
  private static final String DEATH_LINK_ENABLED = "death-link.enabled";
  private static final String PROGRESS_LINK_ENABLED = "progress-link.enabled";
  private static final String RESPAWN_LINK_ENABLED = "respawn-link.enabled";
  private static final String INSOMNIA_LINK_ENABLED = "insomnia-link.enabled";

  private final JavaPlugin plugin;

  public LinkSettings(JavaPlugin plugin) {
    this.plugin = plugin;
    plugin.saveDefaultConfig();
  }

  public boolean isInventoryLinkEnabled() {
    return plugin.getConfig().getBoolean(INVENTORY_LINK_ENABLED, true);
  }

  public boolean isHealthLinkEnabled() {
    return plugin.getConfig().getBoolean(HEALTH_LINK_ENABLED, true);
  }

  public boolean isEffectLinkEnabled() {
    return plugin.getConfig().getBoolean(EFFECT_LINK_ENABLED, true);
  }

  public boolean isDeathLinkEnabled() {
    return plugin.getConfig().getBoolean(DEATH_LINK_ENABLED, true);
  }

  public boolean isProgressLinkEnabled() {
    return plugin.getConfig().getBoolean(PROGRESS_LINK_ENABLED, true);
  }

  public boolean isRespawnLinkEnabled() {
    return plugin.getConfig().getBoolean(RESPAWN_LINK_ENABLED, true);
  }

  public boolean isInsomniaLinkEnabled() {
    return plugin.getConfig().getBoolean(INSOMNIA_LINK_ENABLED, true);
  }

  public void setInventoryLinkEnabled(boolean enabled) {
    setEnabled(INVENTORY_LINK_ENABLED, enabled);
  }

  public void setHealthLinkEnabled(boolean enabled) {
    setEnabled(HEALTH_LINK_ENABLED, enabled);
  }

  public void setEffectLinkEnabled(boolean enabled) {
    setEnabled(EFFECT_LINK_ENABLED, enabled);
  }

  public void setDeathLinkEnabled(boolean enabled) {
    setEnabled(DEATH_LINK_ENABLED, enabled);
  }

  public void setProgressLinkEnabled(boolean enabled) {
    setEnabled(PROGRESS_LINK_ENABLED, enabled);
  }

  public void setRespawnLinkEnabled(boolean enabled) {
    setEnabled(RESPAWN_LINK_ENABLED, enabled);
  }

  public void setInsomniaLinkEnabled(boolean enabled) {
    setEnabled(INSOMNIA_LINK_ENABLED, enabled);
  }

  public void reload() {
    plugin.reloadConfig();
  }

  private void setEnabled(String path, boolean enabled) {
    plugin.getConfig().set(path, enabled);
    plugin.saveConfig();
  }
}
