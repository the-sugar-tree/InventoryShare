/*
 * This file is part of InventoryShare, licensed under the GPL-3.0 License.
 *
 * InventoryShare the minecraft plugin that enables sharing inventory with other players
 * Copyright (c) 2023 the-sugar-tree and contributors
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

package com.sugar_tree.inventoryshare.nms.reflection.inventory;

import com.sugar_tree.inventoryshare.nms.reflection.inventory.legacy.LegacyNameTable;
import com.sugar_tree.inventoryshare.nms.reflection.inventory.legacy.LegacyNames;
import com.sugar_tree.inventoryshare.nms.reflection.inventory.modern.ModernNameTable;
import com.sugar_tree.inventoryshare.nms.reflection.inventory.modern.ModernNames;
import com.sugar_tree.inventoryshare.nms.server.IncompatibleServerException;
import com.sugar_tree.inventoryshare.nms.server.MappingType;
import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import org.jetbrains.annotations.NotNull;

import static com.sugar_tree.inventoryshare.nms.reflection.NmsNames.*;

/**
 * Builds the names of the inventory models' classes and members for one server. Only strings are built here;
 * nothing is loaded. Spigot's member names come from {@link LegacyNameTable} and {@link ModernNameTable}; Mojang's did
 * not change within either model.
 *
 * @see com.sugar_tree.inventoryshare.nms.reflection.NmsNames
 */
public final class InventoryNameFactory {

    private static final String NON_NULL_LIST = "net.minecraft.core.NonNullList";
    private static final String ENTITY_EQUIPMENT = "net.minecraft.world.entity.EntityEquipment";

    private InventoryNameFactory() {}

    /**
     * @throws IllegalArgumentException    if the version does not use the legacy model
     * @throws IncompatibleServerException if the server's names are unknown for it (before 1.17: Mojang's names, or
     *                                     a CraftBukkit package without a version)
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
                    .playerClass(versionedNms(pkg, "EntityHuman"))
                    .itemStackClass(versionedNms(pkg, "ItemStack"))
                    .nonNullListClass(versionedNms(pkg, "NonNullList"));
        } else {
            names.inventoryOwnerClass(byMappingType(runtime,
                            "net.minecraft.server.level.EntityPlayer", "net.minecraft.server.level.ServerPlayer"))
                    .playerInventoryClass(byMappingType(runtime,
                            "net.minecraft.world.entity.player.PlayerInventory", "net.minecraft.world.entity.player.Inventory"))
                    .playerClass(byMappingType(runtime,
                            "net.minecraft.world.entity.player.EntityHuman", "net.minecraft.world.entity.player.Player"))
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

    /** Pre-1.17 NMS classes share the versioned package of CraftBukkit */
    private static String versionedNms(String pkg, String simpleName) {
        return "net.minecraft.server." + pkg + "." + simpleName;
    }
}
