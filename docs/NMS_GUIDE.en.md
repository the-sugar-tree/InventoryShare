<p align="center"><b>ENGLISH</b> | <a href="NMS_GUIDE.md">한국어</a></p>

# Introduction to Server-Internal Classes (NMS): Spigot and Paper

This document explains what the server-internal classes that InventoryShare works with are, and how their names differ by server type and version.
For the plugin's structure, see [ARCHITECTURE.en.md](ARCHITECTURE.en.md); for how to build and contribute, see [CONTRIBUTING.en.md](../CONTRIBUTING.en.md).

> [!important]
> The documentation is being reorganized. These documents were edited with the help of AI and may be hard to read. Thank you for your understanding.

---

## 1. Why does the plugin use server-internal classes directly?

Plugins normally use only the Bukkit API (`org.bukkit.*`). But InventoryShare must make **multiple players share a single inventory object**, and the Bukkit API cannot do that.
That is because the Bukkit API only offers a way to put items in and take them out "by copying". So we find the player inventory object inside the server directly and swap the slot lists (lists) inside it for the **same object** shared with other players.

Two kinds of server-internal code are involved.

| Name                           | What it is                                                                              | Package                     |
|--------------------------------|-----------------------------------------------------------------------------------------|-----------------------------|
| **CraftBukkit**                | The code that actually implements the Bukkit API. E.g., `CraftPlayer`, which implements Bukkit's `Player` | `org.bukkit.craftbukkit...` |
| **NMS** (net.minecraft.server) | The Minecraft server's own code. E.g., the real player object and inventory object      | `net.minecraft...`          |

Calling `CraftPlayer.getHandle()` gives you the NMS player object behind Bukkit's `Player`, and from there you move on to the NMS inventory object.

These classes are not a public API, so **their names differ by server type and version.** That is why InventoryShare fixes the names as strings and looks them up at runtime with reflection (`Class.forName("...")`, `getDeclaredField("...")`, etc.). If a name it has fixed does not exist on the server, the plugin does not enable.

---

## 2. Two reasons names change

### 2.1 The version number in the CraftBukkit package

Spigot puts CraftBukkit classes in a package specific to each Minecraft version. This is called package <strong>relocation</strong>.

```
org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer      ← Spigot 1.21.4 (has a version number)
org.bukkit.craftbukkit.entity.CraftPlayer               ← Paper 1.20.5 and above, and every server on 26.1 and above (no version number)
```

A part like `v1_21_R3` does not map 1:1 to the Minecraft version. For example, 1.19, 1.19.1, and 1.19.2 are all `v1_19_R1`. So this value is not computed from the version; it must be read directly from the running server.

Before 1.17, NMS classes are in the same version-numbered package too (`net.minecraft.server.v1_16_R3.EntityHuman`, etc.). From 1.17, NMS was split into per-feature packages like `net.minecraft.world.entity.player...` (this document calls that **flattening**).

### 2.2 The NMS naming scheme (mappings)

The Minecraft server itself is distributed with class and field names scrambled so they are hard to recognize (**obfuscation**). Each server software has a different way (**mappings**) of turning these names back into human-readable ones.

| Naming scheme     | Example (player inventory)                                                                        | Servers that use it                                                                  |
|-------------------|---------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| **Spigot names**  | Class `net.minecraft.world.entity.player.PlayerInventory`, fields `g`, `h`, `i`                   | Spigot up to 1.21.11, Paper up to 1.20.4 (regular builds)                            |
| **Mojang names**  | Class `net.minecraft.world.entity.player.Inventory`, fields `items`, `armor`, `offhand`           | Paper 1.20.5 and above, Paper 1.20.4 Mojang build, every server on 26.1 and above    |

- With Spigot names, class names are mostly readable, but field and method names are short, like `g` and `gK()`, and **change with every Minecraft version.**
- Mojang names are the original names Mojang published. **The Mojang names InventoryShare uses did not change within the 1.17 – 1.21.4 range or within the 1.21.5 – 26.x range.**
- From 26.1, Mojang no longer obfuscates, so both Spigot and Paper use Mojang names.

### 2.3 The two change independently

| Server                              | Version number in CraftBukkit package             | NMS naming scheme |
|-------------------------------------|---------------------------------------------------|-------------------|
| Spigot 1.12.2 – 1.21.11             | Yes (`org.bukkit.craftbukkit.vX_Y_RZ`)            | Spigot names      |
| Paper 1.12.2 – 1.20.4 (regular build) | Yes (`org.bukkit.craftbukkit.vX_Y_RZ`)          | Spigot names      |
| **Paper 1.20.4 Mojang build**       | **Yes** (`org.bukkit.craftbukkit.v1_20_R3`)       | **Mojang names**  |
| **Paper 1.20.5 – 1.21.11**          | **No** (`org.bukkit.craftbukkit`)                 | **Mojang names**  |
| Spigot / Paper 26.1 and above       | No (`org.bukkit.craftbukkit`)                     | Mojang names      |

Some servers, like the Paper 1.20.4 Mojang build, **have a version number in the package but use Mojang names for NMS.** So you cannot decide from the Minecraft version alone, as in "Paper 1.20.5 and above means Mojang"; you must **check each of the two on the server separately.**

---

## 3. The inventory structure comes in two kinds, depending on version

Separately from names, the inventory **structure** itself changed in 1.21.5. InventoryShare handles the two as Legacy and Modern.

| Structure                      | What the inventory object holds                                                                                         | Where armor and offhand live                                     |
|--------------------------------|-------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| **Legacy** (1.12 – 1.21.4)     | A 36-slot main inventory/hotbar list, a 4-slot armor list, a 1-slot offhand list, and a list that bundles these three   | Lists inside the inventory object                                |
| **Modern** (1.21.5 and above)  | A 36-slot main inventory/hotbar list, and a reference to the player's **equipment object** (`net.minecraft.world.entity.EntityEquipment`) | The `equipment slot → item` map (`EnumMap`) inside the equipment object |

InventoryShare's slot numbers are the same as Bukkit's `PlayerInventory.getContents()`: 0 – 35 are the main inventory and hotbar, 36 – 39 are boots, leggings, chestplate, and helmet, and 40 is the offhand. In Modern, a "slot number → equipment slot" table in the inventory class is used to convert slots 36 – 40 to equipment slots.

---

## 4. Names Actually Used per Server

### 4.1 Classes

`{v}` is the version number in the CraftBukkit package (e.g., `v1_21_R3`).

| Server                                                          | CraftBukkit player                                   | NMS player (the class that has the inventory)   | NMS inventory                                       | Equipment slot enum (Modern)               |
|-----------------------------------------------------------------|------------------------------------------------------|-------------------------------------------------|-----------------------------------------------------|--------------------------------------------|
| Spigot / Paper 1.12.2 – 1.16.5                                  | `org.bukkit.craftbukkit.{v}.entity.CraftPlayer`      | `net.minecraft.server.{v}.EntityHuman`          | `net.minecraft.server.{v}.PlayerInventory`          | –                                          |
| Spigot 1.17 – 1.21.4,<br>Paper 1.17 – 1.20.4 (regular build)    | `org.bukkit.craftbukkit.{v}.entity.CraftPlayer`      | `net.minecraft.server.level.EntityPlayer`       | `net.minecraft.world.entity.player.PlayerInventory` | –                                          |
| Paper 1.20.4 Mojang build                                       | `org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer` | `net.minecraft.server.level.ServerPlayer`       | `net.minecraft.world.entity.player.Inventory`       | –                                          |
| Paper 1.20.5 – 1.21.4                                           | `org.bukkit.craftbukkit.entity.CraftPlayer`          | `net.minecraft.server.level.ServerPlayer`       | `net.minecraft.world.entity.player.Inventory`       | –                                          |
| Spigot 1.21.5 – 1.21.11                                         | `org.bukkit.craftbukkit.{v}.entity.CraftPlayer`      | `net.minecraft.world.entity.player.EntityHuman` | `net.minecraft.world.entity.player.PlayerInventory` | `net.minecraft.world.entity.EnumItemSlot`  |
| Paper 1.21.5 – 1.21.11,<br>Spigot / Paper 26.1 and above        | `org.bukkit.craftbukkit.entity.CraftPlayer`          | `net.minecraft.world.entity.player.Player`      | `net.minecraft.world.entity.player.Inventory`       | `net.minecraft.world.entity.EquipmentSlot` |

Classes that every server on 1.17 and above names the same:

| Class                                                   | Purpose                              |
|---------------------------------------------------------|--------------------------------------|
| `net.minecraft.world.item.ItemStack`                    | NMS item                             |
| `net.minecraft.core.NonNullList`                        | Inventory slot list                  |
| `net.minecraft.world.entity.EntityEquipment`            | Armor/offhand equipment (1.21.5 and above) |
| `org.bukkit.craftbukkit[.{v}].inventory.CraftItemStack` | Converting Bukkit item ↔ NMS item    |

From `CraftItemStack`, only `asNMSCopy` and `asCraftMirror` (`asBukkitMirror` since Paper 26.3) are used. When reading an item, the mirror is `clone()`d to copy the NMS item (without going through `ItemMeta`).

### 4.2 Fields and methods

**Legacy (1.17 – 1.21.4)** — example: 1.21.4

| What is looked up                                              | Spigot 1.21.4    | Mojang name             |
|----------------------------------------------------------------|------------------|-------------------------|
| Method that gets the inventory from a player                   | `gi()`           | `getInventory()`        |
| 36 main inventory/hotbar slots                                 | `g`              | `items`                 |
| 4 armor slots                                                  | `h`              | `armor`                 |
| 1 offhand slot                                                 | `i`              | `offhand`               |
| The list bundling the three above                              | `l`              | `compartments`          |
| The empty item (a constant of `ItemStack`)                     | `j`              | `EMPTY`                 |
| Creating an empty slot list (a static method of `NonNullList`) | `a(int, Object)` | `withSize(int, Object)` |

For 1.12 – 1.16.5, the inventory is obtained through a field `inventory` rather than a method, and the slot list fields use the readable names `items`, `armor`, and `extraSlots`.

**Modern (1.21.5 and above)** — example: 1.21.11

| What is looked up                                           | Spigot 1.21.11   | Mojang name (Paper, 26.1 and above) |
|-------------------------------------------------------------|------------------|-------------------------------------|
| Method that gets the inventory from a player                | `gK()`           | `getInventory()`                    |
| 36 main inventory/hotbar slots                              | `l`              | `items`                             |
| Equipment object                                            | `n`              | `equipment`                         |
| Per-slot items inside the equipment object (field of `EntityEquipment`) | `b`  | `items`                             |
| Table from inventory slot number → equipment slot           | `i`              | `EQUIPMENT_SLOT_MAPPING`            |
| The empty item                                              | `l`              | `EMPTY`                             |
| Creating an empty slot list                                 | `a(int, Object)` | `withSize(int, Object)`             |

Spigot names differ from version to version, so the tables above are examples. The complete per-version names are defined by `LegacyNameTable` and `ModernNameTable` in the code (`nms/reflection/` in `InventoryShare-nms`).

---

## 5. How InventoryShare Matches Them

When the plugin enables, it checks two things on the server **separately** (`com.sugar_tree.inventoryshare.nms.server.ServerRuntime`).

| What is checked         | How                                                                    | Example result                                                                                                                  |
|-------------------------|------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| CraftBukkit package     | Look at the class name of the server object (`Bukkit.getServer()`)     | `org.bukkit.craftbukkit.v1_20_R3.CraftServer` → `v1_20_R3`<br>`org.bukkit.craftbukkit.CraftServer` → no version number         |
| NMS naming scheme       | Check whether the class `net.minecraft.world.entity.player.Inventory` exists | If it does, Mojang names; if not, Spigot names                                                                          |

Then it picks the inventory structure (Legacy / Modern) from the Minecraft version, and assembles class and member names that match the package and naming scheme found above (`com.sugar_tree.inventoryshare.nms.reflection.inventory.InventoryNameFactory`; for item storage, `…reflection.item.ItemNameFactory`).
So even on the same Minecraft 1.21.4, it looks for `g` in `net.minecraft.world.entity.player.PlayerInventory` on Spigot, and `items` in `net.minecraft.world.entity.player.Inventory` on Paper.

- Lookups use the class loader that runs the server (`CraftServer`'s class loader). This avoids mistaking a same-named class in another plugin's jar for the server's.
- All names are looked up at once when the plugin enables. If even one is missing, the plugin is disabled with a "this is a bug, please report it" message, and the inventory is left untouched.
- Paper automatically translates some reflection calls made with Spigot names into Mojang names, but InventoryShare does not rely on this; it uses the names that fit the server directly.

When the plugin enables, the check result is printed to the server log.

```
Minecraft 1.21.4 (CraftBukkit v1_21_R3, SPIGOT mappings), using LegacyInventoryAccess          ← Spigot 1.21.4
Minecraft 1.20.4 (CraftBukkit v1_20_R3, MOJANG mappings), using LegacyInventoryAccess          ← Paper 1.20.4 Mojang build
Minecraft 1.21.4 (CraftBukkit (not relocated), MOJANG mappings), using LegacyInventoryAccess   ← Paper 1.21.4
Minecraft 1.21.11 (CraftBukkit (not relocated), MOJANG mappings), using ModernInventoryAccess  ← Paper 1.21.11
```

---

## 6. How to Check Names Yourself

These are methods you can use when supporting a new version or when you suspect a name is wrong.

| Method                  | Description                                                                                                                                                                                                                                                                                                                   |
|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Automated tests         | `InventoryShare-nms-test` loads the Spigot jars installed via BuildTools, the `remapped-mojang` jars (Mojang names), and the Paper jars fetched by `preparePaperJars`, finds names using the same code as the plugin, and even checks field types. [CONTRIBUTING.en.md §4.2](../CONTRIBUTING.en.md#42-nms-name-verification-tests-buildtools-required) |
| `javap`                 | Open the server jar's classes directly. E.g., `javap -p -cp <server jar> net.minecraft.world.entity.player.Inventory`                                                                                                                                                                                                          |
| Writing directly in NMS | In an `InventoryShare-dev` sandbox, call NMS directly to verify behavior first, then carry the same names over into the name tables. [CONTRIBUTING.en.md §5](../CONTRIBUTING.en.md#5-development-environment-inventoryshare-dev)                                                                                                |

A Paper server jar (`versions/<version>/paper-<version>.jar`) contains the names exactly as they run (Mojang names), so you can check it directly with `javap`.
