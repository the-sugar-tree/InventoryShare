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

package com.sugar_tree.inventoryshare.nms.server;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The running server's internals: how it names them, and the class loader to find them with.
 * Detected at runtime rather than derived from the Minecraft version.
 *
 * <p>The CraftBukkit package and the mapping type change independently: Paper's Mojang-mapped 1.20.4 build keeps the
 * {@code v1_20_R3} package but uses Mojang's mappings, while Paper 1.20.5+ drops the package version too.
 */
@Getter
public final class ServerRuntime {

    private static final String CRAFTBUKKIT = "org.bukkit.craftbukkit";
    /** Exists only with Mojang's mappings; Spigot calls the player inventory class {@code PlayerInventory} (1.17 – 1.21.11) */
    static final String MOJANG_PROBE_CLASS = "net.minecraft.world.entity.player.Inventory";

    /** {@code vX_Y_RZ}, or {@code null} if CraftBukkit is not relocated (Paper 1.20.5+, 26.1+) */
    private final @Nullable String craftbukkitPackage;
    private final @NotNull MappingType mappingType;
    /** Loads the server's NMS, CraftBukkit and Bukkit classes */
    private final @NotNull ClassLoader classLoader;

    public ServerRuntime(@Nullable String craftbukkitPackage, @NotNull MappingType mappingType,
                         @NotNull ClassLoader classLoader) {
        this.craftbukkitPackage = craftbukkitPackage;
        this.mappingType = mappingType;
        this.classLoader = classLoader;
    }

    /**
     * Detects the running server, using the class loader of its own {@code CraftServer} class. Unlike a plugin's
     * class loader, it does not search other plugins, so a class bundled in another plugin cannot be mistaken for
     * the server's.
     */
    public static @NotNull ServerRuntime current() {
        Class<?> craftServer = Bukkit.getServer().getClass();
        return detect(craftServer.getName(), craftServer.getClassLoader());
    }

    /**
     * @param craftServerClass name of the server's {@code CraftServer} class,
     *                         e.g. {@code org.bukkit.craftbukkit.v1_21_R3.CraftServer}
     * @param classLoader      class loader that can see the server's NMS, CraftBukkit and Bukkit classes
     * @throws IllegalArgumentException if the class is not CraftBukkit's server
     */
    public static @NotNull ServerRuntime detect(@NotNull String craftServerClass, @NotNull ClassLoader classLoader) {
        int lastDot = craftServerClass.lastIndexOf('.');
        String pkg = lastDot < 0 ? "" : craftServerClass.substring(0, lastDot);
        String craftbukkitPackage;
        if (pkg.equals(CRAFTBUKKIT)) {
            craftbukkitPackage = null;
        } else if (pkg.startsWith(CRAFTBUKKIT + ".") && pkg.indexOf('.', CRAFTBUKKIT.length() + 1) < 0) {
            craftbukkitPackage = pkg.substring(CRAFTBUKKIT.length() + 1);
        } else {
            throw new IllegalArgumentException("Not a CraftBukkit server: " + craftServerClass);
        }
        MappingType mappingType = classExists(MOJANG_PROBE_CLASS, classLoader) ? MappingType.MOJANG : MappingType.SPIGOT;
        return new ServerRuntime(craftbukkitPackage, mappingType, classLoader);
    }

    private static boolean classExists(String name, ClassLoader loader) {
        try {
            Class.forName(name, false, loader);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "CraftBukkit " + (craftbukkitPackage == null ? "(not relocated)" : craftbukkitPackage)
                + ", " + mappingType + " mappings";
    }
}
