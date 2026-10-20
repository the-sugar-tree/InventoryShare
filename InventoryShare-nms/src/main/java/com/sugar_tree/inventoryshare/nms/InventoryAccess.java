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

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Version-independent access to a player's NMS inventory storage.
 *
 * <p>Slots follow the order of Bukkit's {@code PlayerInventory#getContents()}:
 * {@code 0-35} storage and hotbar, {@code 36-39} feet, legs, chest, head, {@code 40} offhand.
 */
public interface InventoryAccess {

    int STORAGE_START = 0;
    int STORAGE_SIZE = 36;
    int ARMOR_START = 36;
    int ARMOR_SIZE = 4;
    int OFFHAND_SLOT = 40;
    int SIZE = 41;

    /**
     * @return a new inventory with every slot empty, used for the shared and team inventories
     */
    @NotNull InventoryStorage createEmpty();

    /**
     * @return the storage the player is currently using (references, not copies)
     */
    @NotNull InventoryStorage capture(@NotNull Player player);

    /**
     * Makes the player use the given storage. Players applied with the same {@link InventoryStorage} share it.
     */
    void apply(@NotNull Player player, @NotNull InventoryStorage inventory);

    /**
     * @return a Bukkit copy of the item in the slot
     */
    @NotNull ItemStack getItem(@NotNull InventoryStorage inventory, int slot);

    /**
     * Stores an NMS copy of the item in the slot; later changes to {@code item} do not affect the inventory.
     */
    void setItem(@NotNull InventoryStorage inventory, int slot, @NotNull ItemStack item);
}
