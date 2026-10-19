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

package com.sugar_tree.inventoryshare.nms.reflection;

import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.sugar_tree.inventoryshare.nms.reflection.VersionRange.atLeast;
import static com.sugar_tree.inventoryshare.nms.reflection.VersionRange.of;

/**
 * Spigot's member names of the modern inventory model ({@code items + EntityEquipment}, 1.21.5+),
 * one row per set of versions with the same names. Rows are named after the CraftBukkit package of the release.
 */
enum ModernNameTable {
    // (versions, getInventory, items, equipment, EntityEquipment.items, slotMapping, EMPTY, createItemList)
    v1_21_R4(of("1.21.5"), "gj", "i", "k", "b", "g", "l", "a"),
    v1_21_R5(of("1.21.6", "1.21.7", "1.21.8"), "gs", "k", "m", "b", "i", "l", "a"),
    v1_21_R6(of("1.21.9", "1.21.10"), "gB", "l", "n", "b", "i", "l", "a"),
    v1_21_R7(of("1.21.11"), "gK", "l", "n", "b", "i", "l", "a"),
    /**
     * 26.1 and every later version. Minecraft is no longer obfuscated, so every server uses Mojang's names and this
     * row has no Spigot names. Future releases are tried with the same names; if a member was renamed, resolution
     * fails and the plugin disables itself.
     */
    v26(atLeast("26.1")),
    ;

    final VersionRange versions;
    /** Spigot's names, all {@code null} if the versions have none */
    final @Nullable String getInventory;
    final @Nullable String items;
    final @Nullable String equipment;
    final @Nullable String equipmentItems;
    final @Nullable String slotMapping;
    final @Nullable String emptyItem;
    final @Nullable String createItemList;

    ModernNameTable(VersionRange versions) {
        this(versions, null, null, null, null, null, null, null);
    }

    ModernNameTable(VersionRange versions, String getInventory, String items, String equipment, String equipmentItems,
                    String slotMapping, String emptyItem, String createItemList) {
        this.versions = versions;
        this.getInventory = getInventory;
        this.items = items;
        this.equipment = equipment;
        this.equipmentItems = equipmentItems;
        this.slotMapping = slotMapping;
        this.emptyItem = emptyItem;
        this.createItemList = createItemList;
    }

    boolean hasSpigotNames() {
        return getInventory != null;
    }

    static @Nullable ModernNameTable find(@NotNull MinecraftVersion version) {
        for (ModernNameTable row : values()) {
            if (row.versions.contains(version)) {
                return row;
            }
        }
        return null;
    }
}
