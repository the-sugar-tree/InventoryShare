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

import com.sugar_tree.inventoryshare.nms.server.MappingType;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Naming rules shared by {@code InventoryNameFactory} and {@code ItemNameFactory}.
 *
 * <p>Three things decide a name, independently of each other:
 * <ul>
 *   <li>the mappings the server uses ({@link MappingType}): Spigot's member names come from the name tables</li>
 *   <li>the NMS package layout: {@code net.minecraft.server.vX_Y_RZ} before 1.17, flattened from 1.17</li>
 *   <li>the CraftBukkit package: {@code org.bukkit.craftbukkit.vX_Y_RZ}, or not relocated</li>
 * </ul>
 */
public final class NmsNames {

    public static final String BUKKIT_ITEM_STACK = "org.bukkit.inventory.ItemStack";
    /** NMS {@code ItemStack} from 1.17, under both mappings */
    public static final String ITEM_STACK = "net.minecraft.world.item.ItemStack";

    private static final String CRAFTBUKKIT = "org.bukkit.craftbukkit";

    private NmsNames() {}

    /**
     * @param pkg  the CraftBukkit package version ({@code ServerRuntime#getCraftbukkitPackage()}), or {@code null}
     * @param name the class name under {@code org.bukkit.craftbukkit}, e.g. {@code inventory.CraftItemStack}
     */
    public static @NotNull String craftbukkit(@Nullable String pkg, @NotNull String name) {
        return pkg == null ? CRAFTBUKKIT + "." + name : CRAFTBUKKIT + "." + pkg + "." + name;
    }

    /** @return {@code spigot} or {@code mojang}, whichever the server's mappings use */
    public static @NotNull String byMappingType(@NotNull ServerRuntime runtime, @NotNull String spigot,
                                                @NotNull String mojang) {
        return runtime.getMappingType() == MappingType.MOJANG ? mojang : spigot;
    }
}
