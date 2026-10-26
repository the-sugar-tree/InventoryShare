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

package com.sugar_tree.inventoryshare.nms.reflection.inventory.legacy;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Every class and member name the legacy inventory model ({@code items / armor / extraSlots / contents},
 * 1.12 - 1.21.4) needs on one server. Class names are fully qualified.
 *
 * @see com.sugar_tree.inventoryshare.nms.reflection.inventory.InventoryNameFactory
 */
@Getter
@Builder
public final class LegacyNames {

    // CraftBukkit and Bukkit
    private final @NotNull String craftPlayerClass;
    private final @NotNull String craftItemStackClass;
    private final @NotNull String bukkitItemStackClass;

    // Player -> player inventory
    /** Class declaring the {@link #inventory} member; {@code CraftPlayer#getHandle()} returns a subtype of it */
    private final @NotNull String inventoryOwnerClass;
    private final @NotNull String inventory;
    /** {@code true} if {@link #inventory} is a field (pre-1.17), {@code false} if it is a method */
    private final boolean inventoryField;

    // Player inventory
    private final @NotNull String playerInventoryClass;
    /** Parameter of the player inventory's constructor ({@code EntityHuman} / {@code Player}) */
    private final @NotNull String playerClass;
    private final @NotNull String items;
    private final @NotNull String armor;
    private final @NotNull String extraSlots;
    /** List of the three lists above */
    private final @NotNull String contents;

    // Items
    private final @NotNull String itemStackClass;
    /** static empty item field of ItemStack */
    private final @NotNull String emptyItem;
    private final @NotNull String nonNullListClass;
    /** static {@code NonNullList(int size, Object fill)} factory */
    private final @NotNull String createItemList;
}
