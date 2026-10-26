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

package com.sugar_tree.inventoryshare.nms;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Converts items to and from Minecraft's own item data (1.20.5+), as {@code ItemStack.CODEC} writes it: the item id,
 * the count and the data components. Unlike Bukkit's {@code ItemStack#serialize()}, nothing goes through
 * {@code ItemMeta}, so nothing is dropped, and Spigot and Paper read each other's data. Data saved by an older
 * Minecraft version is upgraded with the server's data fixer, as the server upgrades its worlds.
 *
 * <p>The map has the layout of Paper's {@code ItemStack#serialize()} from 1.21.5: {@value #DATA_VERSION},
 * {@code id}, {@code count}, {@code components} (each component's value as SNBT) and {@code schema_version}.
 * An empty item has only {@value #DATA_VERSION}, {@code id: minecraft:air} and {@code schema_version}.
 */
public interface ItemCodec {

    /** Key of the data version the item was saved with */
    String DATA_VERSION = "DataVersion";

    /**
     * @param item an item of this server, e.g. from {@link InventoryAccess#getItem}
     */
    @NotNull Map<String, Object> encode(@NotNull ItemStack item);

    /**
     * @param data a map written by {@link #encode}, on this server or another one, of this Minecraft version or an
     *             older one; Paper's {@code ItemStack#serialize()} output from 1.21.5 is read too
     * @return the item, which shares nothing with {@code data}
     * @throws IllegalArgumentException if the data cannot be read: no or a newer {@value #DATA_VERSION}, invalid
     *                                  SNBT, or an item or component this server does not know
     */
    @NotNull ItemStack decode(@NotNull Map<?, ?> data);
}
