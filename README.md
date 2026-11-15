# Linked

Linked is a Paper plugin that lets a group share one inventory, health state, potion effects, progression, respawn point, rest timer, and optionally one fate: if one player dies, everyone dies.

## Features

- **Inventory link** — every online player uses the same hotbar, storage, armour, off-hand items, and Ender Chest. Cursor and crafting-grid items are returned to the shared inventory when a view closes.
- **Health link** — damage, healing, hunger, saturation, exhaustion, and experience changes are synchronized across online players.
- **Effect link** — potion effects, including their removal, are synchronized across online players.
- **Death link** — when one online player dies, every other alive online player dies too. A successful Totem of Undying rescue is shared instead, and linked victims do not inflate vanilla death-statistic scoreboards.
- **Progress link** — advancements and recipe discoveries earned by one player are granted to all online players and remembered for future joiners.
- **Respawn link** — a successfully used bed or a newly charged Respawn Anchor becomes the shared respawn point.
- **Insomnia link** — `TIME_SINCE_REST` is shared, so sleep resets and phantom pressure apply equally.
- **Persistent settings** — each link can be turned on or off independently and remains configured after a restart.

## Installation

1. Download `linked-<version>.jar` from the server's GitHub Release.
2. Place the JAR in your Paper server's `plugins` folder.
3. Start or restart the server.

The plugin creates `plugins/Linked/config.yml` automatically.

## Commands

These commands require operator permission by default (`linked.admin`).

| Command | What it does |
| --- | --- |
| `/linked inventory enable` | Turn shared inventory on. |
| `/linked inventory disable` | Turn shared inventory off. |
| `/linked inventory status` | Show whether shared inventory is on. |
| `/linked health enable` | Turn health link on. |
| `/linked health disable` | Turn health link off. |
| `/linked health status` | Show whether health link is on. |
| `/linked effect enable` | Turn effect link on. |
| `/linked effect disable` | Turn effect link off. |
| `/linked effect status` | Show whether effect link is on. |
| `/linked death enable` | Turn death link on. |
| `/linked death disable` | Turn death link off. |
| `/linked death status` | Show whether death link is on. |
| `/linked progress <enable\|disable\|status>` | Manage advancement and recipe sharing. |
| `/linked respawn <enable\|disable\|status>` | Manage shared bed/Respawn Anchor locations. |
| `/linked insomnia <enable\|disable\|status>` | Manage the shared rest timer. |
| `/linked reload` | Reload the configuration file. |

`/link` can be used instead of `/linked`.

## Configuration

`plugins/Linked/config.yml`:

```yml
inventory-link:
  enabled: true

health-link:
  enabled: true

effect-link:
  enabled: true

death-link:
  enabled: true

progress-link:
  enabled: true

respawn-link:
  enabled: true

insomnia-link:
  enabled: true
```

You can edit these values while the server is stopped, then use `/linked reload` after starting it. Commands update the same file automatically.

## How deaths work

When death link is enabled, the first player's death clears the shared inventory and kills the remaining alive online players. The original death drops one set of the shared items; linked deaths do not create duplicate drops. Everyone respawns with an empty inventory.

## Existing installations

Linked now saves inventory using Paper's safer item format in `plugins/Linked/shared-inventory.nbt`. If you used an older release, its `shared-inventory.dat` file is kept as a backup. On the first join after updating, the shared inventory is rebuilt from that player's inventory.
