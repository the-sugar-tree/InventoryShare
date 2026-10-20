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

package com.sugar_tree.inventoryshare.nms.reflection;

import com.sugar_tree.inventoryshare.nms.IncompatibleServerException;
import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.InventoryAccessProvider;
import com.sugar_tree.inventoryshare.nms.InventoryStorage;
import com.sugar_tree.inventoryshare.nms.reflection.legacy.LegacyInventoryAccess;
import com.sugar_tree.inventoryshare.nms.reflection.legacy.LegacyMembers;
import com.sugar_tree.inventoryshare.nms.reflection.modern.ModernInventoryAccess;
import com.sugar_tree.inventoryshare.nms.reflection.modern.ModernMembers;
import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import org.jetbrains.annotations.NotNull;

/**
 * The built-in provider: every version in the name tables, through reflection. Builds the names of the version's
 * {@link InventoryModel} for the server, resolves every member up front with the server's class loader, creates
 * the access of that model, and {@linkplain #verify(InventoryAccess) uses it once} before returning it.
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
        try {
            InventoryAccess access = access(model, version, runtime);
            verify(access);
            return access;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            throw new IncompatibleServerException("Failed to set up the " + model
                    + " inventory model for Minecraft " + version + " (" + runtime + ")", e);
        }
    }

    private static InventoryAccess access(InventoryModel model, MinecraftVersion version, ServerRuntime runtime)
            throws ReflectiveOperationException, IncompatibleServerException {
        ClassLoader loader = runtime.getClassLoader();
        switch (model) {
            case LEGACY:
                return new LegacyInventoryAccess(LegacyMembers.resolve(NmsNameFactory.legacy(version, runtime), loader));
            case MODERN:
                return new ModernInventoryAccess(ModernMembers.resolve(NmsNameFactory.modern(version, runtime), loader));
            default:
                throw new AssertionError(model);
        }
    }

    /**
     * Uses the access once on an empty inventory: creates the empty lists, and converts an item both ways in each
     * part (storage, armor, offhand). A broken member then fails here, while the plugin is starting, instead of
     * while it loads the saved inventories.
     */
    static void verify(@NotNull InventoryAccess access) {
        InventoryStorage empty = access.createEmpty();
        for (int slot : new int[]{InventoryAccess.STORAGE_START, InventoryAccess.ARMOR_START, InventoryAccess.OFFHAND_SLOT}) {
            access.setItem(empty, slot, access.getItem(empty, slot));
        }
    }
}
