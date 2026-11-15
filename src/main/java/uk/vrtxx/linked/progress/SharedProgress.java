package uk.vrtxx.linked.progress;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Persists progress that has been earned by any member of the linked group. */
public final class SharedProgress {

  private final JavaPlugin plugin;
  private final File file;
  private final Set<String> advancements = new LinkedHashSet<>();
  private final Set<String> recipes = new LinkedHashSet<>();

  public SharedProgress(JavaPlugin plugin) {
    this.plugin = plugin;
    file = new File(plugin.getDataFolder(), "shared-progress.yml");
  }

  public void load() {
    if (!file.exists()) {
      return;
    }
    YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
    advancements.addAll(configuration.getStringList("advancements"));
    recipes.addAll(configuration.getStringList("recipes"));
  }

  public void save() {
    YamlConfiguration configuration = new YamlConfiguration();
    configuration.set("advancements", advancements.stream().sorted().toList());
    configuration.set("recipes", recipes.stream().sorted().toList());
    try {
      configuration.save(file);
    } catch (IOException exception) {
      plugin.getLogger().severe("Could not save shared progress: " + exception.getMessage());
    }
  }

  public boolean addAdvancement(String key) {
    return advancements.add(key);
  }

  public boolean addRecipe(String key) {
    return recipes.add(key);
  }

  public Set<String> advancements() {
    return Set.copyOf(advancements);
  }

  public Set<String> recipes() {
    return Set.copyOf(recipes);
  }
}
