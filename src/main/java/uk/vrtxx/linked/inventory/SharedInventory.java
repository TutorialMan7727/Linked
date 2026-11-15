package uk.vrtxx.linked.inventory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

/** Holds, applies, and persists the inventory shared by every online player. */
public final class SharedInventory {

  private static final int STORAGE_SIZE = 36;
  private static final int ARMOR_SIZE = 4;
  private static final int FILE_MAGIC = 0x4C4E4B31; // LNK1
  private static final int MAX_SECTION_BYTES = 16 * 1024 * 1024;

  private final JavaPlugin plugin;
  private final File inventoryFile;
  private final File legacyInventoryFile;

  private ItemStack[] storage = new ItemStack[STORAGE_SIZE];
  private ItemStack[] armor = new ItemStack[ARMOR_SIZE];
  private ItemStack offhand;
  private boolean initialized;

  public SharedInventory(JavaPlugin plugin) {
    this.plugin = plugin;
    inventoryFile = new File(plugin.getDataFolder(), "shared-inventory.nbt");
    legacyInventoryFile = new File(plugin.getDataFolder(), "shared-inventory.dat");
  }

  public boolean isInitialized() {
    return initialized;
  }

  public void capture(Player player) {
    PlayerInventory inventory = player.getInventory();
    storage = cloneItems(inventory.getStorageContents());
    armor = cloneItems(inventory.getArmorContents());
    offhand = cloneItem(inventory.getItemInOffHand());
    initialized = true;
  }

  public void apply(Player player) {
    PlayerInventory inventory = player.getInventory();
    inventory.setStorageContents(cloneItems(storage));
    inventory.setArmorContents(cloneItems(armor));
    inventory.setItemInOffHand(cloneItem(offhand));
  }

  public void syncAllPlayers() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      apply(player);
    }
  }

  /** Removes the shared items and immediately clears them from online players. */
  public void clearAndSyncAllPlayers() {
    storage = new ItemStack[STORAGE_SIZE];
    armor = new ItemStack[ARMOR_SIZE];
    offhand = null;
    initialized = true;
    syncAllPlayers();
  }

  public void save() {
    try (DataOutputStream output =
        new DataOutputStream(new BufferedOutputStream(new FileOutputStream(inventoryFile)))) {
      output.writeInt(FILE_MAGIC);
      writeItems(output, storage);
      writeItems(output, armor);
      writeItems(output, new ItemStack[] {offhand});
    } catch (IOException exception) {
      plugin.getLogger().severe("Could not save shared inventory: " + exception.getMessage());
    }
  }

  public void load() {
    if (!inventoryFile.exists()) {
      if (legacyInventoryFile.exists()) {
        plugin
            .getLogger()
            .warning(
                "Legacy shared-inventory.dat was kept as a backup. "
                    + "The shared inventory will be rebuilt from the first player to join.");
      }
      return;
    }

    try (DataInputStream input =
        new DataInputStream(new BufferedInputStream(new FileInputStream(inventoryFile)))) {
      if (input.readInt() != FILE_MAGIC) {
        throw new IOException("Unrecognized shared inventory data.");
      }

      ItemStack[] loadedStorage = readItems(input);
      ItemStack[] loadedArmor = readItems(input);
      ItemStack[] loadedOffhand = readItems(input);

      if (loadedStorage.length != STORAGE_SIZE
          || loadedArmor.length != ARMOR_SIZE
          || loadedOffhand.length != 1) {
        throw new IOException("Invalid shared inventory data.");
      }

      storage = cloneItems(loadedStorage);
      armor = cloneItems(loadedArmor);
      offhand = cloneItem(loadedOffhand[0]);
      initialized = true;
      plugin.getLogger().info("Shared inventory loaded successfully.");
    } catch (IOException exception) {
      plugin.getLogger().severe("Could not load shared inventory: " + exception.getMessage());
    }
  }

  private void writeItems(DataOutputStream output, ItemStack[] items) throws IOException {
    byte[] serializedItems = ItemStack.serializeItemsAsBytes(items);
    output.writeInt(serializedItems.length);
    output.write(serializedItems);
  }

  private ItemStack[] readItems(DataInputStream input) throws IOException {
    int byteCount = input.readInt();
    if (byteCount < 0 || byteCount > MAX_SECTION_BYTES) {
      throw new IOException("Invalid shared inventory section size.");
    }

    byte[] serializedItems = new byte[byteCount];
    input.readFully(serializedItems);
    return ItemStack.deserializeItemsFromBytes(serializedItems);
  }

  private ItemStack[] cloneItems(ItemStack[] items) {
    ItemStack[] cloned = new ItemStack[items.length];
    for (int index = 0; index < items.length; index++) {
      cloned[index] = cloneItem(items[index]);
    }
    return cloned;
  }

  private ItemStack cloneItem(ItemStack item) {
    return item == null ? null : item.clone();
  }
}
