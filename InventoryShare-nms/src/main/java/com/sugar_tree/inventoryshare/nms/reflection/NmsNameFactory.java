/*
 * This file is part of InventoryShare, licensed under the GPL-3.0 License.
 *
 * InventoryShare the minecraft plugin that enables sharing inventory with ohter players
 * Copyright (c) 2023 the-sugar-tree
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sugar_tree.inventoryshare.nms.reflection;

import com.sugar_tree.inventoryshare.nms.IncompatibleServerException;
import com.sugar_tree.inventoryshare.nms.reflection.legacy.LegacyNames;
import com.sugar_tree.inventoryshare.nms.reflection.modern.ModernNames;
import com.sugar_tree.inventoryshare.nms.server.MappingType;
import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the names of NMS, CraftBukkit and Bukkit classes and members for one server. Only strings are built here;
 * nothing is loaded.
 *
 * <p>Three things decide the strings, independently of the {@link InventoryModel}:
 * <ul>
 *   <li>the mappings the server uses ({@link MappingType}): Spigot's names come from {@link LegacyNameTable} and
 *       {@link ModernNameTable}, Mojang's did not change within either model</li>
 *   <li>the NMS package layout: {@code net.minecraft.server.vX_Y_RZ} before 1.17, flattened from 1.17</li>
 *   <li>the CraftBukkit package: {@code org.bukkit.craftbukkit.vX_Y_RZ}, or not relocated</li>
 * </ul>
 */
public final class NmsNameFactory {

    private static final String CRAFTBUKKIT = "org.bukkit.craftbukkit";
    private static final String BUKKIT_ITEM_STACK = "org.bukkit.inventory.ItemStack";

    private static final String ITEM_STACK = "net.minecraft.world.item.ItemStack";
    private static final String NON_NULL_LIST = "net.minecraft.core.NonNullList";
    private static final String ENTITY_EQUIPMENT = "net.minecraft.world.entity.EntityEquipment";

    private NmsNameFactory() {}

    /**
     * @throws IllegalArgumentException    if the version does not use the legacy model
     * @throws IncompatibleServerException if the server's names are unknown for it (Mojang's names before 1.17)
     */
    public static @NotNull LegacyNames legacy(@NotNull MinecraftVersion version, @NotNull ServerRuntime runtime)
            throws IncompatibleServerException {
        LegacyNameTable row = LegacyNameTable.find(version);
        if (row == null) {
            throw new IllegalArgumentException("Minecraft " + version + " does not use the legacy inventory model");
        }
        String pkg = runtime.getCraftbukkitPackage();
        LegacyNames.LegacyNamesBuilder names = LegacyNames.builder()
                .craftPlayerClass(craftbukkit(pkg, "entity.CraftPlayer"))
                .craftItemStackClass(craftbukkit(pkg, "inventory.CraftItemStack"))
                .bukkitItemStackClass(BUKKIT_ITEM_STACK)
                .inventoryField(!row.isFlattened());

        if (!row.isFlattened()) {
            if (runtime.getMappingType() == MappingType.MOJANG) {
                throw new IncompatibleServerException("No Mojang names for Minecraft " + version + " (before 1.17)");
            }
            if (pkg == null) {
                throw new IncompatibleServerException("Minecraft " + version + " needs a versioned CraftBukkit package");
            }
            names.inventoryOwnerClass(versionedNms(pkg, "EntityHuman"))
                    .playerInventoryClass(versionedNms(pkg, "PlayerInventory"))
                    .itemStackClass(versionedNms(pkg, "ItemStack"))
                    .nonNullListClass(versionedNms(pkg, "NonNullList"));
        } else {
            names.inventoryOwnerClass(byMappingType(runtime,
                            "net.minecraft.server.level.EntityPlayer", "net.minecraft.server.level.ServerPlayer"))
                    .playerInventoryClass(byMappingType(runtime,
                            "net.minecraft.world.entity.player.PlayerInventory", "net.minecraft.world.entity.player.Inventory"))
                    .itemStackClass(ITEM_STACK)
                    .nonNullListClass(NON_NULL_LIST);
        }

        if (runtime.getMappingType() == MappingType.MOJANG) {
            names.inventory("getInventory")
                    .items("items")
                    .armor("armor")
                    .extraSlots("offhand")
                    .contents("compartments")
                    .emptyItem("EMPTY")
                    .createItemList("withSize");
        } else {
            names.inventory(row.inventory)
                    .items(row.items)
                    .armor(row.armor)
                    .extraSlots(row.extraSlots)
                    .contents(row.contents)
                    .emptyItem(row.emptyItem)
                    .createItemList(LegacyNameTable.CREATE_ITEM_LIST);
        }
        return names.build();
    }

    /**
     * @throws IllegalArgumentException    if the version does not use the modern model
     * @throws IncompatibleServerException if the server's names are unknown for it (Spigot's names from 26.1)
     */
    public static @NotNull ModernNames modern(@NotNull MinecraftVersion version, @NotNull ServerRuntime runtime)
            throws IncompatibleServerException {
        ModernNameTable row = ModernNameTable.find(version);
        if (row == null) {
            throw new IllegalArgumentException("Minecraft " + version + " does not use the modern inventory model");
        }
        String pkg = runtime.getCraftbukkitPackage();
        ModernNames.ModernNamesBuilder names = ModernNames.builder()
                .craftPlayerClass(craftbukkit(pkg, "entity.CraftPlayer"))
                .craftItemStackClass(craftbukkit(pkg, "inventory.CraftItemStack"))
                .bukkitItemStackClass(BUKKIT_ITEM_STACK)
                .playerClass(byMappingType(runtime,
                        "net.minecraft.world.entity.player.EntityHuman", "net.minecraft.world.entity.player.Player"))
                .inventoryClass(byMappingType(runtime,
                        "net.minecraft.world.entity.player.PlayerInventory", "net.minecraft.world.entity.player.Inventory"))
                .entityEquipmentClass(ENTITY_EQUIPMENT)
                .equipmentSlotClass(byMappingType(runtime,
                        "net.minecraft.world.entity.EnumItemSlot", "net.minecraft.world.entity.EquipmentSlot"))
                .itemStackClass(ITEM_STACK)
                .nonNullListClass(NON_NULL_LIST);

        if (runtime.getMappingType() == MappingType.MOJANG) {
            names.getInventory("getInventory")
                    .items("items")
                    .equipment("equipment")
                    .slotMapping("EQUIPMENT_SLOT_MAPPING")
                    .equipmentItems("items")
                    .emptyItem("EMPTY")
                    .createItemList("withSize");
        } else {
            if (!row.hasSpigotNames()) {
                throw new IncompatibleServerException("No Spigot names for Minecraft " + version);
            }
            names.getInventory(row.getInventory)
                    .items(row.items)
                    .equipment(row.equipment)
                    .slotMapping(row.slotMapping)
                    .equipmentItems(row.equipmentItems)
                    .emptyItem(row.emptyItem)
                    .createItemList(row.createItemList);
        }
        return names.build();
    }

    private static String craftbukkit(@Nullable String pkg, String name) {
        return pkg == null ? CRAFTBUKKIT + "." + name : CRAFTBUKKIT + "." + pkg + "." + name;
    }

    /** Pre-1.17 NMS classes share the versioned package of CraftBukkit */
    private static String versionedNms(String pkg, String simpleName) {
        return "net.minecraft.server." + pkg + "." + simpleName;
    }

    private static String byMappingType(ServerRuntime runtime, String spigot, String mojang) {
        return runtime.getMappingType() == MappingType.MOJANG ? mojang : spigot;
    }
}
