package uk.vrtxx.linked.command;

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import uk.vrtxx.linked.Linked;

/** Handles the runtime controls for the independently enabled link features. */
public final class LinkedCommand implements CommandExecutor, TabCompleter {

  private static final List<String> ACTIONS = List.of("enable", "disable", "status");

  private final Linked plugin;

  public LinkedCommand(Linked plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(
      CommandSender sender, Command command, String label, String[] arguments) {
    if (arguments.length == 1 && arguments[0].equalsIgnoreCase("reload")) {
      plugin.reloadLinkSettings();
      sender.sendMessage(Component.text("Linked configuration reloaded.", NamedTextColor.GREEN));
      return true;
    }

    if (arguments.length != 2) {
      sendUsage(sender);
      return true;
    }

    LinkFeature feature = LinkFeature.from(arguments[0]);
    LinkAction action = LinkAction.from(arguments[1]);
    if (feature == null || action == null) {
      sendUsage(sender);
      return true;
    }

    if (action == LinkAction.STATUS) {
      sendStatus(sender, feature);
      return true;
    }

    boolean enabled = action == LinkAction.ENABLE;
    plugin.setFeatureEnabled(feature, enabled);
    sender.sendMessage(
        Component.text(
            feature.displayName + " link " + (enabled ? "enabled" : "disabled") + ".",
            enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] arguments) {
    if (arguments.length == 1) {
      return partialMatches(
          arguments[0],
          List.of(
              "inventory",
              "health",
              "effect",
              "death",
              "progress",
              "respawn",
              "insomnia",
              "reload"));
    }
    if (arguments.length == 2 && LinkFeature.from(arguments[0]) != null) {
      return partialMatches(arguments[1], ACTIONS);
    }
    return List.of();
  }

  private void sendStatus(CommandSender sender, LinkFeature feature) {
    boolean enabled = plugin.isFeatureEnabled(feature);
    sender.sendMessage(
        Component.text(
            feature.displayName + " link is " + (enabled ? "enabled" : "disabled") + ".",
            enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
  }

  private void sendUsage(CommandSender sender) {
    sender.sendMessage(
        Component.text(
            "Usage: /linked <inventory|health|effect|death|progress|respawn|insomnia> <enable|disable|status>",
            NamedTextColor.YELLOW));
    sender.sendMessage(Component.text("Reload config: /linked reload", NamedTextColor.YELLOW));
  }

  private List<String> partialMatches(String input, List<String> options) {
    String normalizedInput = input.toLowerCase(Locale.ROOT);
    return options.stream().filter(option -> option.startsWith(normalizedInput)).toList();
  }

  public enum LinkFeature {
    INVENTORY("inventory"),
    HEALTH("health"),
    EFFECT("effect"),
    DEATH("death"),
    PROGRESS("progress"),
    RESPAWN("respawn"),
    INSOMNIA("insomnia");

    private final String displayName;

    LinkFeature(String displayName) {
      this.displayName = displayName;
    }

    private static LinkFeature from(String value) {
      return switch (value.toLowerCase(Locale.ROOT)) {
        case "inventory", "inv", "inventory-link" -> INVENTORY;
        case "death", "death-link" -> DEATH;
        case "health", "health-link" -> HEALTH;
        case "effect", "effects", "effect-link" -> EFFECT;
        case "progress", "progress-link" -> PROGRESS;
        case "respawn", "spawn", "respawn-link" -> RESPAWN;
        case "insomnia", "rest", "insomnia-link" -> INSOMNIA;
        default -> null;
      };
    }
  }

  private enum LinkAction {
    ENABLE,
    DISABLE,
    STATUS;

    private static LinkAction from(String value) {
      return switch (value.toLowerCase(Locale.ROOT)) {
        case "enable", "enabled", "on" -> ENABLE;
        case "disable", "disabled", "off" -> DISABLE;
        case "status" -> STATUS;
        default -> null;
      };
    }
  }
}
