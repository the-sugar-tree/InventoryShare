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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sugar_tree.inventoryshare.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;

public interface IInventoryManager {

    /**
     * Apply all inventory to specific player
     * @param p Player to apply
     * @deprecated Do not directly refer this
     */
    @Deprecated
    void applyAllInventory(@NotNull Player p);

    /**
     * Disapply inventory to specific player
     * @param p Player to disapply
     */
    void disApplyInventory(@NotNull Player p);

    /**
     * Apply inventory to specific player automatically
     * @param p Player to apply
     */
    void applyInventory(@NotNull Player p);

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

    /**
     * @param obj Object which you want to change field
     * @param name Field name
     * @param value Value to change field
     * @throws NoSuchFieldException if a field with the specified name is not found.
     * @throws IllegalAccessException if this Field object is enforcing Java language access control and the underlying field is inaccessible or final; or if this Field object has no write access.
     */
    default void setField(@NotNull Object obj, @NotNull String name, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(obj, value);
    }

}
