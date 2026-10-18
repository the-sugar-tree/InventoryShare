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

package com.sugar_tree.inventoryshare.nms;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.sugar_tree.inventoryshare.nms.VersionRange.of;

/**
 * Spigot's member names of the legacy inventory model ({@code items / armor / extraSlots / contents}, 1.12 - 1.21.4),
 * one row per set of versions with the same names. Rows are named after the CraftBukkit package of the release.
 */
enum LegacyNameTable {
    // (versions, inventory, items, armor, extraSlots, contents, EMPTY)
    v1_12_R1(of("1.12", "1.12.1", "1.12.2"), "inventory", "items", "armor", "extraSlots", "f", "a"),
    v1_13_R1(of("1.13"), "inventory", "items", "armor", "extraSlots", "f", "a"),
    v1_13_R2(of("1.13.1", "1.13.2"), "inventory", "items", "armor", "extraSlots", "f", "a"),
    v1_14_R1(of("1.14", "1.14.1", "1.14.2", "1.14.3", "1.14.4"), "inventory", "items", "armor", "extraSlots", "f", "a"),
    v1_15_R1(of("1.15", "1.15.1", "1.15.2"), "inventory", "items", "armor", "extraSlots", "f", "a"),
    v1_16_R1(of("1.16.1"), "inventory", "items", "armor", "extraSlots", "f", "b"),
    v1_16_R2(of("1.16.2", "1.16.3"), "inventory", "items", "armor", "extraSlots", "f", "b"),
    v1_16_R3(of("1.16.4", "1.16.5"), "inventory", "items", "armor", "extraSlots", "f", "b"),
    // From 1.17 the inventory is reached through a method
    v1_17_R1(of("1.17", "1.17.1"), "getInventory", "h", "i", "j", "n", "b"),
    v1_18_R1(of("1.18"), "fq", "h", "i", "j", "n", "b"),
    v1_18_R2(of("1.18.1", "1.18.2"), "fr", "h", "i", "j", "n", "b"),
    v1_19_R1(of("1.19"), "fB", "h", "i", "j", "n", "b"),
    /** 1.19.1 and 1.19.2 renamed members but kept the {@code v1_19_R1} CraftBukkit package. */
    v1_19_1_R1(of("1.19.1", "1.19.2"), "fA", "h", "i", "j", "n", "b"),
    v1_19_R2(of("1.19.3"), "fE", "h", "i", "j", "n", "b"),
    v1_19_R3(of("1.19.4"), "fJ", "i", "j", "k", "o", "b"),
    v1_20_R1(of("1.20", "1.20.1"), "fN", "i", "j", "k", "o", "b"),
    v1_20_R2(of("1.20.2"), "fR", "i", "j", "k", "o", "b"),
    v1_20_R3(of("1.20.3", "1.20.4"), "fS", "i", "j", "k", "o", "f"),
    v1_20_R4(of("1.20.5", "1.20.6"), "gc", "h", "i", "j", "n", "l"),
    v1_21_R1(of("1.21", "1.21.1"), "fY", "h", "i", "j", "n", "l"),
    v1_21_R2(of("1.21.2", "1.21.3"), "gi", "g", "h", "i", "l", "j"),
    v1_21_R3(of("1.21.4"), "gi", "g", "h", "i", "l", "j"),
    ;

    /** {@code NonNullList#withSize(int, Object)}, the same in every Spigot release */
    static final String CREATE_ITEM_LIST = "a";

    final VersionRange versions;
    /** inventory field (pre-1.17) or method (1.17+) of the player */
    final String inventory;
    final String items;
    final String armor;
    final String extraSlots;
    final String contents;
    final String emptyItem;

    LegacyNameTable(VersionRange versions, String inventory, String items, String armor, String extraSlots,
                    String contents, String emptyItem) {
        this.versions = versions;
        this.inventory = inventory;
        this.items = items;
        this.armor = armor;
        this.extraSlots = extraSlots;
        this.contents = contents;
        this.emptyItem = emptyItem;
    }

    /**
     * 1.17+: NMS packages are flattened ({@code net.minecraft.world...}) instead of {@code net.minecraft.server.vX_Y_RZ},
     * and the player inventory is reached through a method instead of a field.
     */
    boolean isFlattened() {
        return compareTo(v1_17_R1) >= 0;
    }

    static @Nullable LegacyNameTable find(@NotNull MinecraftVersion version) {
        for (LegacyNameTable row : values()) {
            if (row.versions.contains(version)) {
                return row;
            }
        }
        return null;
    }
}
