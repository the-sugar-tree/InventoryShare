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

package com.sugar_tree.inventoryshare.nms.reflection.inventory;

import com.sugar_tree.inventoryshare.nms.server.IncompatibleServerException;
import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.InventoryAccessProvider;
import com.sugar_tree.inventoryshare.nms.reflection.inventory.legacy.LegacyInventoryAccess;
import com.sugar_tree.inventoryshare.nms.reflection.inventory.legacy.LegacyMembers;
import com.sugar_tree.inventoryshare.nms.reflection.inventory.modern.ModernInventoryAccess;
import com.sugar_tree.inventoryshare.nms.reflection.inventory.modern.ModernMembers;
import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import org.jetbrains.annotations.NotNull;

/**
 * The built-in provider: every version in the name tables, through reflection. Builds the names of the version's
 * {@link InventoryModel} for the server, resolves every member up front with the server's class loader, and creates
 * the access of that model, which uses every member once while it is constructed.
 *
 * <p>Not registered through {@link java.util.ServiceLoader}; {@link com.sugar_tree.inventoryshare.nms.InventoryAccessFactory}
 * always asks it last.
 */
public final class ReflectionAccessProvider implements InventoryAccessProvider {

    @Override
    public boolean supports(@NotNull MinecraftVersion version) {
        return InventoryModel.of(version) != null;
    }

    @Override
    public @NotNull InventoryAccess create(@NotNull MinecraftVersion version, @NotNull ServerRuntime runtime)
            throws IncompatibleServerException {
        InventoryModel model = InventoryModel.of(version);
        if (model == null) {
            throw new IllegalArgumentException("Minecraft " + version + " is not supported by this provider");
        }
        ClassLoader loader = runtime.getClassLoader();
        try {
            switch (model) {
                case LEGACY:
                    return new LegacyInventoryAccess(LegacyMembers.resolve(InventoryNameFactory.legacy(version, runtime), loader));
                case MODERN:
                    return new ModernInventoryAccess(ModernMembers.resolve(InventoryNameFactory.modern(version, runtime), loader));
                default:
                    throw new AssertionError(model);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            throw new IncompatibleServerException("Failed to set up the " + model
                    + " inventory model for Minecraft " + version + " (" + runtime + ")", e);
        }
    }
}
