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

package com.sugar_tree.inventoryshare.inventory;

import com.sugar_tree.inventoryshare.file.FileManager;
import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.InventoryStorage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Holds every inventory the plugin keeps in memory:
 * <ul>
 *     <li>the inventory shared by all players</li>
 *     <li>one inventory per team</li>
 *     <li>the personal inventories set aside while a shared one is applied</li>
 * </ul>
 * {@link InventoryManager} uses it to switch players between inventories, and
 * {@link FileManager} uses the same instance to save and load them.
 */
public final class InventoryRegistry {

    /** The inventory shared by every player when team inventories are off or the player has no team. */
    private final InventoryStorage sharedInventory;

    /** Team name - the inventory shared by the members of that team */
    private final Map<String, InventoryStorage> teamInventoryMap = new HashMap<>();

    /**
     * UUID - Player's UUID <br>
     * InventoryStorage - the player's own inventory, saved while a shared one is applied
     */
    private final Map<UUID, InventoryStorage> originalPlayerInventoryMap = new HashMap<>();

    /**
     * @param access used to create the empty shared inventory
     */
    public InventoryRegistry(@NotNull InventoryAccess access) {
        this.sharedInventory = access.createEmpty();
    }

    /**
     * @return the inventory shared by every player. It is created once and never replaced,
     *         so loading fills this instance instead of swapping it.
     */
    public @NotNull InventoryStorage getSharedInventory() {
        return sharedInventory;
    }

    /**
     * @return the team's inventory, or {@code null} if the team has none yet
     */
    public @Nullable InventoryStorage getTeamInventory(@NotNull String teamName) {
        return teamInventoryMap.get(teamName);
    }

    /**
     * Sets the team's inventory, replacing any inventory the team already had.
     */
    public void putTeamInventory(@NotNull String teamName, @NotNull InventoryStorage inventory) {
        teamInventoryMap.put(teamName, inventory);
    }

    /**
     * @see #getPlayerInventory(UUID)
     */
    public @Nullable InventoryStorage getPlayerInventory(@NotNull Player p) {
        return getPlayerInventory(p.getUniqueId());
    }

    /**
     * @return the player's saved personal inventory, or {@code null} if none is saved,
     *         i.e. the player is currently using their own inventory
     */
    public @Nullable InventoryStorage getPlayerInventory(@NotNull UUID uuid) {
        return originalPlayerInventoryMap.get(uuid);
    }

    /**
     * @see #putPlayerInventory(UUID, InventoryStorage)
     */
    public void putPlayerInventory(@NotNull Player p, @NotNull InventoryStorage inventory) {
        putPlayerInventory(p.getUniqueId(), inventory);
    }

    /**
     * Saves the player's personal inventory so it can be restored later.
     */
    public void putPlayerInventory(@NotNull UUID uuid, @NotNull InventoryStorage inventory) {
        originalPlayerInventoryMap.put(uuid, inventory);
    }

    /**
     * @see #removePlayerInventory(UUID)
     */
    public void removePlayerInventory(@NotNull Player p) {
        removePlayerInventory(p.getUniqueId());
    }

    /**
     * Discards the player's saved personal inventory. Call this after it has been restored.
     */
    public void removePlayerInventory(@NotNull UUID uuid) {
        originalPlayerInventoryMap.remove(uuid);
    }

    /**
     * @return a copy of the UUIDs of players whose personal inventory is saved.
     */
    public @NotNull Set<UUID> getSavedPlayers() {
        return new HashSet<>(originalPlayerInventoryMap.keySet());
    }

}
