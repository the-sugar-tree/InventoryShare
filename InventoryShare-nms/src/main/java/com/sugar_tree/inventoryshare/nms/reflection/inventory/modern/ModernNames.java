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

package com.sugar_tree.inventoryshare.nms.reflection.inventory.modern;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Every class and member name the modern inventory model ({@code items + EntityEquipment}, 1.21.5+) needs on one
 * server. Class names are fully qualified.
 *
 * @see com.sugar_tree.inventoryshare.nms.reflection.inventory.InventoryNameFactory
 */
@Getter
@Builder
public final class ModernNames {

    // CraftBukkit and Bukkit
    private final @NotNull String craftPlayerClass;
    private final @NotNull String craftItemStackClass;
    private final @NotNull String bukkitItemStackClass;

    // Player -> inventory
    /**
     * Class declaring {@link #getInventory}; {@code CraftPlayer#getHandle()} returns a subtype of it. Also the first
     * parameter of the inventory's constructor.
     */
    private final @NotNull String playerClass;
    private final @NotNull String getInventory;

    // Inventory
    private final @NotNull String inventoryClass;
    private final @NotNull String items;
    /** EntityEquipment of the inventory's player */
    private final @NotNull String equipment;
    /** static {@code Int2ObjectMap}: inventory slot (36 - 40) to EquipmentSlot */
    private final @NotNull String slotMapping;

    // EntityEquipment
    private final @NotNull String entityEquipmentClass;
    /** {@code EnumMap<EquipmentSlot, ItemStack>} */
    private final @NotNull String equipmentItems;
    private final @NotNull String equipmentSlotClass;

    // Items
    private final @NotNull String itemStackClass;
    /** static empty item field of ItemStack */
    private final @NotNull String emptyItem;
    private final @NotNull String nonNullListClass;
    /** static {@code NonNullList(int size, Object fill)} factory */
    private final @NotNull String createItemList;
}
