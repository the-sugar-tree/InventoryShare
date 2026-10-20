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

package com.sugar_tree.inventoryshare.interfaces;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public interface IInventoryManager {

    /**
     * Makes the player use the inventory shared by all players. The player's own inventory is saved first if they
     * are using it. If the inventory cannot be applied, the error is logged and nothing changes.
     * @param p Player to apply
     */
    void applyAllInventory(@NotNull Player p);

    /**
     * Makes the player use the team's inventory, created empty if the team has none yet. The player's own inventory
     * is saved first if they are using it. If the inventory cannot be applied, the error is logged and nothing
     * changes (the team's new empty inventory is kept).
     * @param p Player to apply
     * @param team Team name to apply
     */
    void applyTeamInventory(@NotNull Player p, @NotNull String team);

    /**
     * Gives the player back their own inventory, if it was saved. If it cannot be applied, the error is logged, and the
     * player keeps the inventory they are using while their own stays saved.
     * @param p Player to apply to
     */
    void applyPersonalInventory(@NotNull Player p);

    /**
     * Applies the inventory the current settings call for: the player's own one if inventory sharing is off,
     * otherwise their team's one if team inventories are on and they have a team, otherwise the shared one
     * @param p Player to apply
     */
    void updateInventroy(@NotNull Player p);

    /**
     * Saves the player's own inventory so it can be given back later
     * @param p Player to save
     * @throws IllegalStateException if the player is not using their own inventory
     */
    void savePlayerInventory(@NotNull Player p);

    /**
     * Used when the plugin is disabled, to give these players back their own inventory
     * @return UUIDs of the players whose own inventory is saved
     */
    Set<UUID> getRegisteredPlayers();

    /**
     * Drops what is kept for the player; call after {@link #applyPersonalInventory(Player)} when the player leaves
     * @param p Player who left
     */
    void forgetPlayer(@NotNull Player p);

}
