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

package com.sugar_tree.inventoryshare.nms;

import org.jetbrains.annotations.NotNull;

/**
 * NMS member names of one {@link NmsMapping} row, for one inventory model.
 */
public interface NmsLayout {

    /**
     * The names to use on a server: Mojang's names of the same inventory model if the server runs with
     * {@link Namespace#MOJANG} names, and always the server's own CraftBukkit package.
     *
     * @throws IllegalStateException if the model has no names for the runtime (e.g. Mojang names before 1.17)
     */
    @NotNull NmsLayout forRuntime(@NotNull ServerRuntime runtime);

    /**
     * Resolves every member of {@link #forRuntime(ServerRuntime)} up front, with the runtime's class loader,
     * and builds the access.
     */
    @NotNull InventoryAccess createAccess(@NotNull ServerRuntime runtime) throws ReflectiveOperationException;
}
