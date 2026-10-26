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
 * Spigot's member names for the item codec (1.20.5+), one row per CraftBukkit package. Only
 * {@code ItemStack.CODEC} changes between releases; the other members are named {@link #OTHER_MEMBERS} in every
 * release up to 1.21.11.
 */
enum ItemCodecNameTable {
    // (versions, ItemStack.CODEC)
    v1_20_R4(of("1.20.5", "1.20.6"), "b"),
    v1_21_R1(of("1.21", "1.21.1"), "b"),
    v1_21_R2(of("1.21.2", "1.21.3"), "a"),
    v1_21_R3(of("1.21.4"), "a"),
    v1_21_R4(of("1.21.5"), "b"),
    v1_21_R5(of("1.21.6", "1.21.7", "1.21.8"), "b"),
    v1_21_R6(of("1.21.9", "1.21.10"), "b"),
    v1_21_R7(of("1.21.11"), "b"),
    /** 26.1 and every later version: Mojang's names only, as in {@link ModernNameTable#v26} */
    v26(atLeast("26.1"), null),
    ;

    /**
     * {@code NbtOps.INSTANCE}, {@code HolderLookup.Provider#createSerializationContext},
     * {@code TagParser.parseTag} / {@code parseCompoundFully} and {@code DataFixers.getDataFixer}
     */
    static final String OTHER_MEMBERS = "a";

    final VersionRange versions;
    /** Spigot's name, {@code null} if the versions have none */
    final @Nullable String codec;

    ItemCodecNameTable(VersionRange versions, @Nullable String codec) {
        this.versions = versions;
        this.codec = codec;
    }

    boolean hasSpigotNames() {
        return codec != null;
    }

    static @Nullable ItemCodecNameTable find(@NotNull MinecraftVersion version) {
        for (ItemCodecNameTable row : values()) {
            if (row.versions.contains(version)) {
                return row;
            }
        }
        return null;
    }
}
