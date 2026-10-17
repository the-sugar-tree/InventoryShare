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

package com.sugar_tree.inventoryshare.nms.legacy;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Every class and member name the legacy inventory model ({@code items / armor / extraSlots / contents},
 * 1.12 - 1.21.4) needs on one server. Class names are fully qualified.
 *
 * @see com.sugar_tree.inventoryshare.nms.NmsNameFactory
 */
@Getter
@Builder
public final class LegacyNames {

    // CraftBukkit and Bukkit
    private final @NonNull String craftPlayerClass;
    private final @NonNull String craftItemStackClass;
    private final @NonNull String bukkitItemStackClass;

    // Player -> player inventory
    /** Class declaring the {@link #inventory} member; {@code CraftPlayer#getHandle()} returns a subtype of it */
    private final @NonNull String inventoryOwnerClass;
    private final @NonNull String inventory;
    /** {@code true} if {@link #inventory} is a field (pre-1.17), {@code false} if it is a method */
    private final boolean inventoryField;

    // Player inventory
    private final @NonNull String playerInventoryClass;
    private final @NonNull String items;
    private final @NonNull String armor;
    private final @NonNull String extraSlots;
    /** List of the three lists above */
    private final @NonNull String contents;

    // Items
    private final @NonNull String itemStackClass;
    /** static empty item field of ItemStack */
    private final @NonNull String emptyItem;
    private final @NonNull String nonNullListClass;
    /** static {@code NonNullList(int size, Object fill)} factory */
    private final @NonNull String createItemList;
}
