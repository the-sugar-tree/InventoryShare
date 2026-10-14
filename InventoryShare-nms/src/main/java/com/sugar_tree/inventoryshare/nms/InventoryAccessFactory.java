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

public final class InventoryAccessFactory {

    private InventoryAccessFactory() {}

    /**
     * Picks the {@link NmsMapping} row for the version and resolves all of its members.
     *
     * @param loader class loader that can see the server's NMS and CraftBukkit classes
     * @throws UnsupportedVersionException if no row matches, or a member of the row cannot be resolved
     */
    public static @NotNull InventoryAccess create(@NotNull ServerVersion version, @NotNull ClassLoader loader)
            throws UnsupportedVersionException {
        NmsMapping mapping = NmsMapping.find(version);
        if (mapping == null) {
            throw new UnsupportedVersionException("No NMS mapping for Minecraft " + version);
        }
        try {
            return mapping.getLayout().createAccess(loader);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            throw new UnsupportedVersionException(
                    "Failed to resolve NMS members of " + mapping.name() + " for Minecraft " + version, e);
        }
    }
}
