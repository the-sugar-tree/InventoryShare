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

import com.sugar_tree.inventoryshare.nms.reflection.inventory.ReflectionAccessProvider;
import com.sugar_tree.inventoryshare.nms.server.IncompatibleServerException;
import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import com.sugar_tree.inventoryshare.nms.server.UnsupportedVersionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Picks the {@link InventoryAccessProvider} for a version: the providers registered through
 * {@link ServiceLoader} first, then the built-in {@link ReflectionAccessProvider}.
 */
public final class InventoryAccessFactory {

    private InventoryAccessFactory() {}

    /**
     * Creates the access with the first provider that supports the version.
     *
     * @param runtime the server's internals, see {@link ServerRuntime#current()}
     * @throws UnsupportedVersionException if no provider supports the version
     * @throws IncompatibleServerException if the provider fails to create the access on this server
     */
    public static @NotNull InventoryAccess create(@NotNull MinecraftVersion version, @NotNull ServerRuntime runtime)
            throws UnsupportedVersionException, IncompatibleServerException {
        InventoryAccessProvider provider = find(version);
        if (provider == null) {
            throw new UnsupportedVersionException("Minecraft " + version + " is not supported");
        }
        return provider.create(version, runtime);
    }

    private static @Nullable InventoryAccessProvider find(MinecraftVersion version) {
        for (InventoryAccessProvider provider : providers()) {
            if (provider.supports(version)) {
                return provider;
            }
        }
        return null;
    }

    /**
     * Registered providers are looked up with the class loader of the jar that contains this class (the plugin jar),
     * since the server's class loader cannot see the plugin's {@code META-INF/services}.
     */
    private static List<InventoryAccessProvider> providers() {
        List<InventoryAccessProvider> providers = new ArrayList<>();
        ClassLoader pluginLoader = InventoryAccessProvider.class.getClassLoader();
        for (InventoryAccessProvider provider : ServiceLoader.load(InventoryAccessProvider.class, pluginLoader)) {
            providers.add(provider);
        }
        providers.add(new ReflectionAccessProvider());
        return providers;
    }
}
