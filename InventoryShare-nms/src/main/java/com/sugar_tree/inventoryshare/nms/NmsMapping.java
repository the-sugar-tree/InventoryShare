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

import com.sugar_tree.inventoryshare.nms.legacy.LegacyNames;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.sugar_tree.inventoryshare.nms.legacy.LegacyNames.flattened;
import static com.sugar_tree.inventoryshare.nms.legacy.LegacyNames.legacy;

/**
 * Supported Minecraft versions and the NMS member names for each. Rows are keyed by Minecraft version,
 * not by the Bukkit version string, whose {@code -R0.x} suffix differs between servers.
 */
@Getter
public enum NmsMapping {
    // ── Legacy: items / armor / extraSlots / contents (frozen) ──
    v1_12_R1(versions("1.12", "1.12.1", "1.12.2"),
            legacy("v1_12_R1", "inventory", "items", "armor", "extraSlots", "f", "a")),
    v1_13_R1(versions("1.13"),
            legacy("v1_13_R1", "inventory", "items", "armor", "extraSlots", "f", "a")),
    v1_13_R2(versions("1.13.1", "1.13.2"),
            legacy("v1_13_R2", "inventory", "items", "armor", "extraSlots", "f", "a")),
    v1_14_R1(versions("1.14", "1.14.1", "1.14.2", "1.14.3", "1.14.4"),
            legacy("v1_14_R1", "inventory", "items", "armor", "extraSlots", "f", "a")),
    v1_15_R1(versions("1.15", "1.15.1", "1.15.2"),
            legacy("v1_15_R1", "inventory", "items", "armor", "extraSlots", "f", "a")),
    v1_16_R1(versions("1.16.1"),
            legacy("v1_16_R1", "inventory", "items", "armor", "extraSlots", "f", "b")),
    v1_16_R2(versions("1.16.2", "1.16.3"),
            legacy("v1_16_R2", "inventory", "items", "armor", "extraSlots", "f", "b")),
    v1_16_R3(versions("1.16.4", "1.16.5"),
            legacy("v1_16_R3", "inventory", "items", "armor", "extraSlots", "f", "b")),
    v1_17_R1(versions("1.17", "1.17.1"),
            flattened("v1_17_R1", "getInventory", "h", "i", "j", "n", "b")),
    v1_18_R1(versions("1.18"),
            flattened("v1_18_R1", "fq", "h", "i", "j", "n", "b")),
    v1_18_R2(versions("1.18.1", "1.18.2"),
            flattened("v1_18_R2", "fr", "h", "i", "j", "n", "b")),
    v1_19_R1(versions("1.19"),
            flattened("v1_19_R1", "fB", "h", "i", "j", "n", "b")),
    /** 1.19.1 and 1.19.2 renamed members but kept the {@code v1_19_R1} CraftBukkit package. */
    v1_19_1_R1(versions("1.19.1", "1.19.2"),
            flattened("v1_19_R1", "fA", "h", "i", "j", "n", "b")),
    v1_19_R2(versions("1.19.3"),
            flattened("v1_19_R2", "fE", "h", "i", "j", "n", "b")),
    v1_19_R3(versions("1.19.4"),
            flattened("v1_19_R3", "fJ", "i", "j", "k", "o", "b")),
    v1_20_R1(versions("1.20", "1.20.1"),
            flattened("v1_20_R1", "fN", "i", "j", "k", "o", "b")),
    v1_20_R2(versions("1.20.2"),
            flattened("v1_20_R2", "fR", "i", "j", "k", "o", "b")),
    v1_20_R3(versions("1.20.3", "1.20.4"),
            flattened("v1_20_R3", "fS", "i", "j", "k", "o", "f")),
    v1_20_R4(versions("1.20.5", "1.20.6"),
            flattened("v1_20_R4", "gc", "h", "i", "j", "n", "l")),
    v1_21_R1(versions("1.21", "1.21.1"),
            flattened("v1_21_R1", "fY", "h", "i", "j", "n", "l")),
    v1_21_R2(versions("1.21.2", "1.21.3"),
            flattened("v1_21_R2", "gi", "g", "h", "i", "l", "j")),
    v1_21_R3(versions("1.21.4"),
            flattened("v1_21_R3", "gi", "g", "h", "i", "l", "j")),
    ;

    private final List<ServerVersion> versions;
    private final NmsLayout layout;

    NmsMapping(List<ServerVersion> versions, NmsLayout layout) {
        this.versions = versions;
        this.layout = layout;
    }

    /**
     * @return the row for the version, or {@code null} if the version is not supported
     */
    public static @Nullable NmsMapping find(@NotNull ServerVersion version) {
        for (NmsMapping mapping : values()) {
            if (mapping.versions.contains(version)) {
                return mapping;
            }
        }
        return null;
    }

    private static List<ServerVersion> versions(String... versions) {
        List<ServerVersion> list = new ArrayList<>(versions.length);
        for (String version : versions) {
            list.add(ServerVersion.parse(version));
        }
        return Collections.unmodifiableList(list);
    }
}
