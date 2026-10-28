# InventoryShare Architecture

<p align="center"><b>ENGLISH</b> | <a href="ARCHITECTURE.md">한국어</a></p>

This document explains how InventoryShare works and how its code is organized.

- How to build, test, and contribute: [CONTRIBUTING.en.md](../CONTRIBUTING.en.md)
- Server-internal classes (NMS) and the differences between Spigot and Paper: [NMS_GUIDE.en.md](NMS_GUIDE.en.md)

> [!important]
> The documentation is being reorganized. These documents were edited with the help of AI and may be hard to read. Thank you for your understanding.

---

## 1. Overview

InventoryShare is a Bukkit plugin that makes all players on a server (or all players on the same team) **share a single inventory**.

### How it works

Inside a Minecraft server, a player's inventory is an object holding "slot lists". InventoryShare **swaps the slot lists inside each player's inventory object for one and the same list object.**
Then the moment one player puts an item in or takes one out, it shows up in every other player's inventory as is. Items are not copied to keep them in sync, so no separate synchronization is needed.

The Bukkit API only offers a way to put items in and take them out by copying, so this "reference sharing" requires manipulating server-internal classes (NMS) directly through reflection. NMS class names differ by server type (Spigot / Paper) and by version. For details, see [NMS_GUIDE.en.md](NMS_GUIDE.en.md).

### Supported range

- Minecraft 1.12.2 – 26.x, Spigot and Paper. **A single jar** supports every version.
- The inventory structure changed in 1.21.5, so there are two implementations.

| Structure  | Versions      | What is shared                                                                                  |
|------------|---------------|-------------------------------------------------------------------------------------------------|
| **Legacy** | 1.12 – 1.21.4 | The inventory object's main-inventory/hotbar list, armor list, and offhand list                 |
| **Modern** | 1.21.5 and up | The inventory object's main-inventory/hotbar list, and the equipment map of the equipment object (`EntityEquipment`) |

---

## 2. Modules and Dependency Direction

```
InventoryShare/
├─ InventoryShare-bukkit/      The released plugin (core). Compiled only against spigot-api, Java 8 target
├─ InventoryShare-nms/         NMS access layer: InventoryAccess, ItemCodec, server detection, reflection implementation. Java 8 target
├─ InventoryShare-nms-test/    Tests that verify NMS names against real Spigot and Paper jars
├─ InventoryShare-dev/         Development sandboxes (<ver>/) and local Paper test server tasks. Included only with -Pdev, not released
└─ docs/
```

- `InventoryShare-sponge/`: A folder started for Sponge servers and then discontinued. It is not part of the build.

```
InventoryShare-bukkit ──▶ InventoryShare-nms ◀── InventoryShare-nms-test
        ▲
InventoryShare-dev/<ver>  (the core is unchanged; only an InventoryAccess implementation is injected)
```

Design principles:
- **The core never touches NMS directly.** Swapping inventories goes only through `InventoryAccess` (§4.4), and item storage conversion only through `ItemCodec` (§4.5).
- **The released jar uses only reflection.** That is why the default build does not need BuildTools (Spigot server jars). Spigot server jars are used only in tests (`InventoryShare-nms-test`) and the development sandboxes (`InventoryShare-dev`).
- The `InventoryShare-nms` jar is bundled inside the `InventoryShare-bukkit` jar.
- `InventoryShare-nms` is compiled against spigot-api and DataFixerUpper (for `ItemCodec`; provided by the server).

---

## 3. Core Behavior (`InventoryShare-bukkit`)

### 3.1 Packages and classes

| Package / class                                | Role                                                                                                                |
|------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| `InventoryShare`                               | Plugin entry point. Startup and shutdown order (§3.4)                                                               |
| `Commands`                                     | The `/inventoryshare` command. Toggling settings, `reload`, `check`                                                 |
| `Listeners`                                    | Join, quit, world save, advancement, and death events; team change detection (`onTick`)                             |
| `PluginSettings`                               | Reading, saving, and `reload` of the `config.yml` settings (`inventory`, `teaminventory`, etc.)                     |
| `SharedConstants`                              | Global values that never change: plugin instance, logger, prefix, data version, etc.                                |
| `inventory/InventoryRegistry`                  | Inventory store: one global shared inventory, one per team, and each player's personal inventory stashed while sharing |
| `inventory/InventoryManager`                   | Switches a player between personal / global shared / team inventory (§3.2)                                          |
| `file/FileManager`, `Codec`, `LoadResult`      | Saving and loading the shared/team inventories and advancements (§5)                                                |
| `utils/I18NUtil`                               | Saving and updating language files (`languages/lang_<locale>.yml`), and messages                                    |
| `utils/AdvancementUtil`, `AdvancementRegistry` | Advancement sharing and the list of shared advancements (§3.5)                                                      |
| `utils/UpdateUtil`                             | Checking for new releases                                                                                           |
| `protocollib/ProtocolLibManager`               | If ProtocolLib is present, compensates for mining being interrupted by item updates while breaking a block (§3.5)   |

### 3.2 Inventory states

`InventoryManager` records which inventory each player is currently using.

| State      | Meaning                                                                                                      |
|------------|--------------------------------------------------------------------------------------------------------------|
| `PERSONAL` | Using their own inventory (initial state)                                                                    |
| `SAVED`    | Their own inventory has been stashed in `InventoryRegistry`, and a shared inventory is about to be applied   |
| `ALL`      | Using the global shared inventory                                                                            |
| `TEAM`     | Using a team inventory. Which team is recorded as well (`appliedTeams`)                                      |

```
                  Before applying sharing: stash their own inventory (capture)
PERSONAL ───────────────────────────────▶ SAVED ──apply──▶ ALL / TEAM
    ▲                                                          │
    └─────── Restore the stashed personal inventory (apply) ───┘
```

Which inventory to apply is decided by `updateInventory(player)` according to the settings.

```
inventory = false                         → personal inventory
inventory = true, teaminventory = false   → global shared inventory
inventory = true, teaminventory = true    → the player's team inventory if they have a team, otherwise the global shared inventory
```

A team inventory is created empty the first time the team is used. Moving to another team switches to that team's inventory.

### 3.3 Handling by event

| When                                         | Handling                                                                                                                                                           |
|----------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Player joins                                 | Apply the inventory that matches the settings, and sync advancements                                                                                               |
| Player quits                                 | Restore the personal inventory → delete that player's state record → save                                                                                          |
| World save                                   | Save shared/team inventories. While sharing, briefly restore each player's personal inventory so the server can save player data, then apply the shared one again |
| Every tick (`onTick`)                        | If team inventory is on, check whether a player's team changed, and re-apply the inventory if it did                                                               |
| `/inventoryshare inventory`, `teaminventory` | Change the setting and re-apply to every online player                                                                                                             |
| `/inventoryshare reload`                     | Re-read `config.yml` and the language files, and re-apply to online players                                                                                        |
| Advancement earned                           | Add it to the advancement list, and if the setting is on, grant it to everyone                                                                                     |
| Death                                        | If `announcedeath` is on, announce the death location. The game itself handles dropping items, and since the lists are shared, it takes effect for everyone       |

**The server's player data always holds the personal inventory.** This is because the personal inventory is restored before saving on quit, world save, and shutdown. The shared inventory is saved only in the plugin's own files (§5).

If applying an inventory (`InventoryAccess#apply`) fails, an error is logged and nothing is changed. The player keeps using the inventory they had, and the state record stays as is. If restoring the personal inventory fails, the personal inventory remains stashed and is applied again at the next attempt (next world save, setting change, etc.). However, if the server saves player data in between, the shared inventory's contents get saved. If it fails on quit, there is no next attempt, and the stashed personal inventory is lost on rejoin.

### 3.4 Startup and shutdown

Startup (`onEnable`):

```
language files → bStats → update check
→ MinecraftVersion.current()
→ loadNMS(): ServerRuntime.current() → InventoryAccessFactory.create() → InventoryRegistry / InventoryManager / FileManager
→ ProtocolLib integration
→ create config.yml → FileManager.load() (create and load inventory/advancement files) → register commands and listeners → apply sharing to online players (on /reload)
```

If any of the version detection, `loadNMS()`, or `FileManager.load()` fails, the plugin is disabled.

| Failure                                    | Cause                                                              | Log                                                                    |
|--------------------------------------------|--------------------------------------------------------------------|------------------------------------------------------------------------|
| `UnsupportedServerException`               | A server not based on CraftBukkit, or an unreadable version string | "This plugin does not support this server" (no need to report)         |
| `UnsupportedVersionException`              | An unsupported Minecraft version                                   | "This plugin does not support this version" (no need to report)        |
| `IncompatibleServerException`              | A supported version, but server-internal names could not be found (§4.2) | "Could not hook into Minecraft ... This is a bug" + reporting guide |
| `RuntimeException` from `FileManager.load()` | An error while reading the shared/team inventories               | "Could not load the saved inventories ..." + reporting guide           |

Shutdown (`onDisable`): restores each player's personal inventory, stops the team detection task, then saves. If the plugin is disabled with `setEnabled(false)` during startup, `onDisable()` runs immediately, and if startup had not finished, it does not save the files. This is to avoid overwriting files with a partially loaded state.

### 3.5 Extra features

- **Advancement sharing:** Advancements earned by anyone are collected in a list (`advancements.yml`), and if the setting (`advancement`) is on, the remaining criteria are completed for every online player.
- **ProtocolLib compensation (1.13 and above):** When inventories are shared, while one player is mining a block, another player using the same tool can change its durability. The item update packet the server sends then restarts the mining from the beginning. So while a block is being mined, the update packets for the item in hand are blocked, and the inventory is refreshed once when mining ends. The plugin works without ProtocolLib.

---

## 4. NMS Access Layer (`InventoryShare-nms`)

### 4.1 Role and flow

It is divided into three stages — name (string) → reflection object → actual manipulation — and each stage receives only the result of the previous one.

```
MinecraftVersion ─┐
                  ├─▶ *NameFactory ─▶ *Names ─▶ *Members.resolve(names, loader) ─▶ *InventoryAccess, VanillaItemCodec
ServerRuntime ────┘   (assemble strings) (strings)  (Class / Field / Method)        (perform reflection)
```

| Stage       | Classes                                                    | What they do                                                                           |
|-------------|------------------------------------------------------------|----------------------------------------------------------------------------------------|
| Server info | `MinecraftVersion`                                         | Parses and compares Minecraft versions                                                 |
|             | `ServerRuntime`                                            | Detects the CraftBukkit package, naming scheme (`MappingType`), and class loader from the server |
| Name assembly | `InventoryNameFactory`, `ItemNameFactory`                | Build **complete class and member names** from the version and server info             |
|             | `NmsNames`                                                 | Naming rules shared by both Factories                                                  |
|             | `LegacyNameTable`, `ModernNameTable`, `ItemCodecNameTable` | Per-version Spigot member name tables (data only)                                      |
|             | `InventoryModel`                                           | Version → `LEGACY` / `MODERN`                                                          |
| Data        | `LegacyNames`, `ModernNames`, `ItemCodecNames`             | A bundle of names to use on one server. No logic                                       |
| Conversion  | `LegacyMembers`, `ModernMembers`, `ItemCodecMembers`       | Names + class loader → `Class`/`Field`/`Method`                                        |
| Execution   | `LegacyInventoryAccess`, `ModernInventoryAccess`           | Per-structure capture / apply / slot read and write                                    |
|             | `VanillaItemCodec`                                         | Item ↔ stored data (§4.5)                                                              |
|             | `Reflections`                                              | Utility for reflective calls and exception translation                                 |
| Assembly    | `ReflectionAccessProvider`                                 | `InventoryModel` → `InventoryNameFactory` → `Members.resolve` → Access                 |
|             | `ItemCodecFactory`                                         | `ItemNameFactory` → `ItemCodecMembers.resolve` → `VanillaItemCodec`                    |
| Selection   | `InventoryAccessFactory`                                   | Picks the Provider that will handle this version (§4.2)                                |

Names vary for two reasons, and each is handled in a different place.

> [!tip]
> For more details, see [NMS_GUIDE.en.md](NMS_GUIDE.en.md).

| Axis                   | What varies                                                                      | How it is determined      | Where it is handled                      |
|------------------------|----------------------------------------------------------------------------------|---------------------------|------------------------------------------|
| Structure (Legacy / Modern) | Code structure (Names, Members, Access)                                     | Minecraft version         | `reflection.inventory.legacy`, `.modern` |
| Names                  | Naming scheme (Spigot/Mojang), NMS package shape (before/after 1.17), CraftBukkit package | Server detection + version | `*NameFactory`, `NmsNames`          |

- The naming scheme cannot be known from the version alone. Even for the same 1.20.4, Spigot uses Spigot names while Paper's Mojang build uses Mojang names. That is why there are no classes like `LegacySpigot*` or `LegacyMojang*`; Members and Access work with the same code regardless of the naming scheme.
- Legacy and Modern **do not share domain code.** Similar code (player → inventory, item conversion) exists on both sides, but the intent is to keep Legacy, which no longer changes, unaffected by changes to Modern, which keeps changing. Only the technical utility (`Reflections`) and name assembly (`InventoryNameFactory`) are shared.

### 4.2 Providers and exceptions

An implementation that creates an `InventoryAccess` is called a Provider.

```
Provider list = [Providers found via ServiceLoader (dev sandbox)..., ReflectionAccessProvider (always last)]

InventoryAccessFactory.create(version, runtime) = create() of the first Provider whose supports(version) is true
                                                  if no Provider handles it, UnsupportedVersionException
```

| Class                                       | Responsibility                                                                               |
|---------------------------------------------|----------------------------------------------------------------------------------------------|
| `InventoryAccessProvider` (interface)       | `supports(version)`: decides whether it handles this version. Looks only at the version, no side effects |
|                                             | `create(version, runtime)`: returns a complete Access or throws `IncompatibleServerException` |
|                                             |     Before returning the Access from `create()`, tests that the access works                 |
| `ReflectionAccessProvider`                  | Every version in the name tables. The default implementation used by the released jar        |
| `DevInventoryAccessProvider` (dev sandbox)  | Handles exactly one target version and returns the `DirectInventoryAccess` written directly against NMS |
| `InventoryAccessFactory`                    | The Provider list and its order. **The only place that decides whether something is supported** |

- `ReflectionAccessProvider` is not found through ServiceLoader; the Factory guarantees it comes last.
- That is, since the dev sandbox's Provider comes first, a sandbox can run even a new version that is not yet in the name tables.
- Why `supports` does not take server info: whether something is supported is a policy — "have we decided to support this version?" Failures caused by the server environment are `IncompatibleServerException`.

| Exception                     | Meaning                                                                                        | Thrown from                                                                |
|-------------------------------|------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| `UnsupportedServerException`  | A server not based on CraftBukkit. **No need to report**, regardless of version                | `ServerRuntime.current` (when the server class is not in CraftBukkit)      |
| `UnsupportedVersionException` | A version this plugin does not support. **No need to report**                                  | `InventoryAccessFactory.create` (when no Provider handles it)              |
| `IncompatibleServerException` | A supported version, but the Access or codec could not be created on this server. **A bug; please report** | A Provider's `create`, `ItemCodecFactory.create` (names not found, etc.) |

### 4.3 Packages

```
com.sugar_tree.inventoryshare.nms                    API: what the core and the dev sandbox use
├─ InventoryAccess, InventoryStorage                 Swapping inventory slot lists (§4.4)
├─ InventoryAccessFactory, InventoryAccessProvider   Provider selection and its contract (§4.2)
├─ ItemCodec, ItemCodecFactory                       Item ↔ stored data, 1.20.5 and above (§4.5)
│
├─ server/                 MinecraftVersion, ServerRuntime, MappingType, 3 startup-failure exceptions
│
└─ reflection/             Reflections, VersionRange, NmsNames
    ├─ inventory/          ReflectionAccessProvider, InventoryModel, InventoryNameFactory
    │   ├─ legacy/         LegacyNameTable, LegacyNames, LegacyMembers, LegacyInventoryAccess
    │   └─ modern/         ModernNameTable, ModernNames, ModernMembers, ModernInventoryAccess
    └─ item/               ItemCodecNameTable, ItemNameFactory, ItemCodecNames, ItemCodecMembers, VanillaItemCodec
```

### 4.4 `InventoryAccess`

```java
public interface InventoryAccess {
    InventoryStorage createEmpty();                    // an empty inventory for sharing/teams
    InventoryStorage capture(Player player);           // the slot lists (by reference) the player is currently using
    void apply(Player player, InventoryStorage inv);   // swap the player's slot lists for inv (the heart of sharing)
    ItemStack getItem(InventoryStorage inv, int slot); // a copy (CraftItemStack, NMS data as is)
    void setItem(InventoryStorage inv, int slot, ItemStack item);
}
```

Slot numbers follow the same order as Bukkit's `PlayerInventory.getContents()`.

| Slots   | Meaning                  | Legacy                    | Modern                                          |
|---------|--------------------------|---------------------------|-------------------------------------------------|
| 0 – 35  | Main inventory & hotbar  | Main inventory list `[i]` | Main inventory list `[i]`                       |
| 36 – 39 | Boots, leggings, chestplate, helmet | Armor list `[i - 36]` | Equipment map (slot number converted via a slot → equipment slot table) |
| 40      | Offhand                  | Offhand list `[0]`        | Equipment map (OFFHAND)                         |

### 4.5 `ItemCodec` (1.20.5 and above)

- It converts an item to NBT with Minecraft's `ItemStack.CODEC`, and stores each component as an SNBT string. The format is the same as Paper 1.21.5+'s `ItemStack#serialize()` (§5).
- It does not go through `ItemMeta`, so no data is dropped, and Spigot and Paper can read each other's data.
- Items from a lower data version are converted with the server's DataFixer before being read.
- `VanillaItemCodec` uses DataFixerUpper types directly, so it is loaded only on 1.20.5 and above. When created, it saves and reads back one item, and throws `IncompatibleServerException` on failure.

### 4.6 Server detection and name assembly

- **Server detection (`ServerRuntime`)**
  - CraftBukkit package: read from the package of the class of `Bukkit.getServer()` (`CraftServer`).
  - Naming scheme: if the server class loader has `net.minecraft.world.entity.player.Inventory`, it is Mojang names (`MOJANG`); otherwise Spigot names (`SPIGOT`).
  - Class loader: uses `CraftServer`'s class loader. The plugin class loader is not used because it also searches other plugins' jars.
- **Name assembly (`InventoryNameFactory`, `ItemNameFactory`)**
  - CraftBukkit classes: `org.bukkit.craftbukkit[.<package>].entity.CraftPlayer`, etc.
  - NMS classes: before 1.17, `net.minecraft.server.<package>.<name>`; from 1.17 on, Spigot or Mojang class names depending on the naming scheme.
  - Spigot member names: per-version name tables (`LegacyNameTable`, `ModernNameTable`, `ItemCodecNameTable`).
  - Mojang member names: fixed per structure. They did not change within the Legacy (1.17 – 1.21.4) and Modern (1.21.5 – 26.x) ranges.
  - Impossible combinations (before 1.17 + Mojang names, 26.1 and above + Spigot names, etc.) are treated as a server detection mistake and throw `IncompatibleServerException`.

The actual class and member names per server are collected in [NMS_GUIDE.en.md §4](NMS_GUIDE.en.md#4-names-actually-used-per-server), and the complete per-version names are defined by the name tables in the code.

When the plugin enables, the detection result is logged.
```
Minecraft 1.21.4 (CraftBukkit (not relocated), MOJANG mappings), using LegacyInventoryAccess
```

### 4.7 Member resolution and version detection

- `*Members.resolve(names, loader)` finds every class in the **class loader passed as an argument**. The server passes the server class loader and tests pass a per-version isolated class loader, so **the tests run the same code as the plugin.**
- All names are resolved at once when the plugin enables. If even one is missing, the plugin is disabled with `IncompatibleServerException` and the inventory is left untouched.
- The Access exercises every member once in its constructor. It first creates an empty inventory, then reads items from the main inventory, armor, and offhand slots (0, 36, 40) and writes them back. Next it constructs an inventory without a player (`PlayerInventory(null)`; for Modern, `Inventory(null, new EntityEquipment())`), writes that empty inventory in with the same code as `apply`, and reads it back. A failure here also yields `IncompatibleServerException`. So even an environment where the final fields that `apply` changes cannot be written (e.g., the JVM's restrictions on mutating final fields) shows up at startup rather than on the first join. The constructors only assign values to fields, so a `null` player is safe (verified on Spigot and Paper 1.12.2 – 26.3).
- `apply` changes several fields (4 for Legacy, 2 for Modern) with `Reflections.setAll`. If any one fails midway, the fields already changed are restored to their original values before the exception is thrown, so a player's inventory is never left partially changed.
- The dev sandbox's Provider is listed in the plugin jar's `META-INF/services`, so it is found through the plugin jar's class loader rather than the server class loader.
- Minecraft version: Paper uses `Server#getMinecraftVersion()`, and Spigot uses the leading part of `Bukkit.getBukkitVersion()`. The name tables are looked up by Minecraft version (`1.21.11`), not by the Bukkit version string (`1.21.11-R0.2-SNAPSHOT`).
- 26.1 and above are covered by a single name table row (`ModernNameTable.v26`, "26.1 and above"). Names are stable because there is no obfuscation, so new versions are attempted anyway, and the plugin is disabled if the names are not found.

---

## 5. Storage Format

The shared inventory is saved to `plugins/InventoryShare/inventory.yml`, and team inventories to `plugins/InventoryShare/teams/<team name>.yml`.

```yaml
format-version: 2
items:      [ ...36 entries ]   # slots 0 – 35
armor:      [ ...4 entries ]    # slots 36 – 39 (feet, legs, chest, head)
extraSlots: [ ...1 entry ]      # slot 40 (offhand)
```

| `format-version` | Server that writes it | Item format                          |
|------------------|-----------------------|--------------------------------------|
| 2                | 1.20.5 and above      | `ItemCodec` (§4.5). Empty slots are `{}` |
| 1, or no key     | 1.20.4 and below, 3.x | Bukkit `ItemStack#serialize()`       |

```yaml
- DataVersion: 4903
  id: minecraft:diamond_sword
  count: 1
  components:
    minecraft:damage: '100'
    minecraft:enchantments: '{"minecraft:sharpness":5}'
  schema_version: 1
```

- When reading, the format is determined per item (if there is a `DataVersion` key, it is `ItemCodec`).
- Other files: `advancements.yml` (shared advancements), `config.yml` (settings), `languages/` (language files)

### Migration

- If a Bukkit-format item is read on 1.20.5 and above, it is converted to the `ItemCodec` format at the next save. The original is first copied to `backups/`.
- If that item was saved at a lower data version than the server, a warning is shown. When Bukkit reads old data on a newer version, it may lose some of it (shulker box contents, etc.). Running once first on a server of the version it was saved on avoids the loss ([wiki](https://github.com/the-sugar-tree/InventoryShare/wiki/Updating-to-4.0)).

### Unreadable data

To prepare for reading files saved on a server of a different version, handling is done **per item**.

- The following items are skipped and their slot is left empty. The remaining items are read normally.
  - Items saved at a higher data version (`v`, `DataVersion`) than the server (downgrade)
  - Items whose data version is not a number, or that cannot be turned back into an item
- If even one is skipped, **the original file is copied to `backups/<inventory|team-name>-<date-time>[-dv<data version>].yml`**, and the skipped slots and the backup location are logged. The original file is overwritten at the next save, so to recover, put the backup file back in its original place on a server that can read that data.
- If the file's `format-version` is higher than the value the plugin knows, a warning is shown.
- If the step of putting the restored item into the inventory (`InventoryAccess#setItem`) fails, it is not skipped. That is a problem with the Access, not the data, so `FileManager.load()` fails and the plugin is disabled (§3.4).

---

## 6. Test Structure

| Test                                       | What it verifies                                                                                                                                                                                    | Spigot jar                    |
|--------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------|
| `InventoryShare-nms` unit tests            | Version parsing, CraftBukkit package detection, name assembly (Spigot/Mojang, by package shape), name table rows, Provider selection and exceptions                                                  | Not needed                    |
| `InventoryShare-bukkit` unit tests         | Item format detection, migration, load decisions and log wording, backup files, deciding which team files to delete, language file paths                                                            | Not needed                    |
| `InventoryShare-nms-test` (`*MembersTest`) | On per-version Spigot jars, `remapped-mojang` jars (Mojang names), and Paper jars, detects the server, finds every name with the same code as the plugin, then checks field types and the slot → equipment slot table | Verifies if present, skips if not |

- Tests live in the same package as the class under test, so even package-private name tables can be checked directly.
- It also tests that every row of the name tables is checked by at least one version's jar.
- `ItemStack.CODEC` has several fields of the same type, so it checks that the field the Spigot name points to is at the same position as `CODEC` in the `remapped-mojang` jar.
- For how to run them, see [CONTRIBUTING.en.md §4](../CONTRIBUTING.en.md#4-testing).

---

## 7. Design Decisions and Known Limitations

- **Why reference sharing, and its risk:** Because list objects are shared instead of items being copied to stay in sync, no synchronization is needed and duplication bugs cannot occur. In exchange, server-internal `final` fields must be changed via reflection. Starting with JDK 26 this produces a warning, and it will be blocked later (JEP 500). The interface is split so that, if it gets blocked, it can be replaced behind `InventoryAccess` with a "copy contents to sync" implementation.
- **Sharing the Modern equipment map:** The whole equipment map is shared, so equipment slots the player does not use (BODY and SADDLE, used for horse armor, etc.) are shared too.
- **Moving data between Paper and Spigot:** On 1.20.5 and above it moves over losslessly. With the Bukkit format (1.20.4 and below), Spigot cannot read some items saved by Paper (shulker box contents, etc.) (verified on 1.21.4).
- **Upgrading from 1.20.4 or below to 1.20.5 or above:** Those versions have no `ItemCodec`, so migration cannot be done first on the same version. The conversion happens during the upgrade, so the losses described in §5 can occur.
- **Startup failure messages:** Some startup failures (such as errors in a dev sandbox) end with the server's generic error instead of a per-failure-type message. The plugin is disabled in that case too.

For the procedure to support a new Minecraft version, see [CONTRIBUTING.en.md §6.2](../CONTRIBUTING.en.md#62-supporting-a-new-minecraft-version).
