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

import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import org.jetbrains.annotations.NotNull;

/**
 * Creates {@link InventoryAccess} for the Minecraft versions it supports.
 *
 * <p>{@link InventoryAccessFactory} asks the providers in order and uses the first one that supports the version:
 * those registered through {@link java.util.ServiceLoader}
 * ({@code META-INF/services/com.sugar_tree.inventoryshare.nms.InventoryAccessProvider}) first, then the built-in
 * reflection provider ({@link com.sugar_tree.inventoryshare.nms.reflection.ReflectionAccessProvider}).
 * The {@code InventoryShare-dev} sandboxes register one to run the plugin with direct NMS code while a new version
 * is being developed. The released plugin registers none.
 *
 * <p>A provider knows nothing about the other providers or their order.
 */
public interface InventoryAccessProvider {

    /**
     * Whether this provider handles the version. Decided from the version alone: no NMS class is loaded, there are
     * no side effects, and the answer is always the same for the same version.
     */
    boolean supports(@NotNull MinecraftVersion version);

    /**
     * Called only for a version this provider {@link #supports(MinecraftVersion) supports}.
     *
     * @return a ready access, never {@code null}
     * @throws IncompatibleServerException if the access cannot be created on this server
     */
    @NotNull InventoryAccess create(@NotNull MinecraftVersion version, @NotNull ServerRuntime runtime)
            throws IncompatibleServerException;
}
