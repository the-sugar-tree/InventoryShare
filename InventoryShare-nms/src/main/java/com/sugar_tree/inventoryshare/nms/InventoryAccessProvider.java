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
import org.jetbrains.annotations.Nullable;

/**
 * Overrides the built-in {@link NmsMapping} with another {@link InventoryAccess}, found through
 * {@link java.util.ServiceLoader} ({@code META-INF/services/com.sugar_tree.inventoryshare.nms.InventoryAccessProvider}).
 *
 * <p>Used by the {@code InventoryShare-dev} sandboxes to run the plugin with direct NMS code while a new
 * version is being developed. The released plugin registers no provider.
 */
public interface InventoryAccessProvider {

    /**
     * @return an access for this server, or {@code null} to fall back to the next provider or {@link NmsMapping}
     */
    @Nullable InventoryAccess create(@NotNull MinecraftVersion version);
}
