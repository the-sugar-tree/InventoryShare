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

import com.sugar_tree.inventoryshare.nms.reflection.InventoryModel;
import com.sugar_tree.inventoryshare.nms.reflection.NmsNameFactory;
import com.sugar_tree.inventoryshare.nms.reflection.legacy.LegacyInventoryAccess;
import com.sugar_tree.inventoryshare.nms.reflection.legacy.LegacyMembers;
import com.sugar_tree.inventoryshare.nms.reflection.legacy.LegacyNames;
import com.sugar_tree.inventoryshare.nms.reflection.modern.ModernInventoryAccess;
import com.sugar_tree.inventoryshare.nms.reflection.modern.ModernMembers;
import com.sugar_tree.inventoryshare.nms.reflection.modern.ModernNames;
import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import org.jetbrains.annotations.NotNull;

import java.util.ServiceLoader;

public final class InventoryAccessFactory {

    private InventoryAccessFactory() {}

    /**
     * Uses the first {@link InventoryAccessProvider} that supports the version. Otherwise, builds the names of the
     * version's {@link InventoryModel} for the server, resolves every member up front with the server's class loader,
     * and creates the access of that model.
     *
     * <p>Providers are looked up with the class loader of the jar that contains this class (the plugin jar),
     * since the server's class loader cannot see the plugin's {@code META-INF/services}.
     *
     * @param runtime the server's internals, see {@link ServerRuntime#current()}
     * @throws UnsupportedVersionException if the version is not supported, or a member cannot be resolved
     */
    public static @NotNull InventoryAccess create(@NotNull MinecraftVersion version, @NotNull ServerRuntime runtime)
            throws UnsupportedVersionException {
        ClassLoader pluginLoader = InventoryAccessProvider.class.getClassLoader();
        for (InventoryAccessProvider provider : ServiceLoader.load(InventoryAccessProvider.class, pluginLoader)) {
            InventoryAccess access = provider.create(version);
            if (access != null) {
                return access;
            }
        }

        InventoryModel model = InventoryModel.of(version);
        if (model == null) {
            throw new UnsupportedVersionException("No NMS names for Minecraft " + version);
        }
        ClassLoader loader = runtime.getClassLoader();
        switch (model) {
            case LEGACY: {
                LegacyNames names = NmsNameFactory.legacy(version, runtime);
                try {
                    return new LegacyInventoryAccess(LegacyMembers.resolve(names, loader));
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    throw resolveFailed(model, version, runtime, e);
                }
            }
            case MODERN: {
                ModernNames names = NmsNameFactory.modern(version, runtime);
                try {
                    return new ModernInventoryAccess(ModernMembers.resolve(names, loader));
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    throw resolveFailed(model, version, runtime, e);
                }
            }
            default:
                throw new AssertionError(model);
        }
    }

    /**
     * @return whether there are NMS names for the version; providers are not consulted
     */
    public static boolean supports(@NotNull MinecraftVersion version) {
        return InventoryModel.of(version) != null;
    }

    private static UnsupportedVersionException resolveFailed(InventoryModel model, MinecraftVersion version,
                                                             ServerRuntime runtime, Throwable cause) {
        return new UnsupportedVersionException("Failed to resolve NMS members of the " + model
                + " inventory model for Minecraft " + version + " (" + runtime + ")", cause);
    }
}
