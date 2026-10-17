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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerRuntimeTest {

    /** The test classpath has no NMS classes, so the namespace is always detected as SPIGOT here. */
    private static final ClassLoader LOADER = ServerRuntimeTest.class.getClassLoader();

    @Test
    void relocatedCraftBukkit() {
        ServerRuntime runtime = ServerRuntime.detect("org.bukkit.craftbukkit.v1_21_R3.CraftServer", LOADER);
        assertEquals("v1_21_R3", runtime.getCraftbukkitPackage());
        assertEquals(Namespace.SPIGOT, runtime.getNamespace());
        assertEquals("org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer", runtime.craftbukkitClass("entity.CraftPlayer"));
    }

    @Test
    void unrelocatedCraftBukkit() {
        ServerRuntime runtime = ServerRuntime.detect("org.bukkit.craftbukkit.CraftServer", LOADER);
        assertNull(runtime.getCraftbukkitPackage());
        assertEquals("org.bukkit.craftbukkit.entity.CraftPlayer", runtime.craftbukkitClass("entity.CraftPlayer"));
    }

    @Test
    void rejectsNonCraftBukkitServer() {
        assertThrows(IllegalArgumentException.class, () -> ServerRuntime.detect("com.example.Server", LOADER));
        assertThrows(IllegalArgumentException.class,
                () -> ServerRuntime.detect("org.bukkit.craftbukkit.v1_21_R3.inner.CraftServer", LOADER));
    }
}
