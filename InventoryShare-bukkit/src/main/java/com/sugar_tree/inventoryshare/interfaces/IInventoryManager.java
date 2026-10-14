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

package com.sugar_tree.inventoryshare.interfaces;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public interface IInventoryManager {

    /**
     * Apply all inventory to specific player
     * @param p Player to apply
     */
    void applyAllInventory(@NotNull Player p);

    /**
     * Apply team inventory to specific player
     * @param p Player to apply
     * @param team Team name to apply
     */
    void applyTeamInventory(@NotNull Player p, @NotNull String team);

    /**
     * Apply personal(normal) inventory to specific player
     * @param p Player to apply to
     */
    void applyPersonalInventory(@NotNull Player p);

    /**
     * Apply inventory to specific player automatically
     * @param p Player to apply
     */
    void updateInventroy(@NotNull Player p);

    /**
     * Save player's inventory to list
     * @param p Player to save
     */
    void savePlayerInventory(@NotNull Player p);

    /**
     * used for plugin disenable logic
     * @return saved player's uuid list
     */
    Set<UUID> getRegisteredPlayers();

}
