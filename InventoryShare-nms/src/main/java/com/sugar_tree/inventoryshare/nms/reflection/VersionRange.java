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

import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Minecraft versions of a name table row: an exact list, or a version and every later one.
 */
final class VersionRange {

    private final List<MinecraftVersion> versions;
    private final @Nullable MinecraftVersion since;

    private VersionRange(List<MinecraftVersion> versions, @Nullable MinecraftVersion since) {
        this.versions = versions;
        this.since = since;
    }

    static @NotNull VersionRange of(String... versions) {
        List<MinecraftVersion> list = new ArrayList<>(versions.length);
        for (String version : versions) {
            list.add(MinecraftVersion.parse(version));
        }
        return new VersionRange(Collections.unmodifiableList(list), null);
    }

    static @NotNull VersionRange atLeast(String version) {
        return new VersionRange(Collections.emptyList(), MinecraftVersion.parse(version));
    }

    boolean contains(@NotNull MinecraftVersion version) {
        return versions.contains(version) || since != null && version.compareTo(since) >= 0;
    }

    /** The exact versions; empty for an open range */
    @NotNull List<MinecraftVersion> getVersions() {
        return versions;
    }
}
