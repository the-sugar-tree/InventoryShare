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

package com.sugar_tree.inventoryshare.nms.reflection;

import com.sugar_tree.inventoryshare.nms.IncompatibleServerException;
import com.sugar_tree.inventoryshare.nms.server.MappingType;
import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReflectionAccessProviderTest {

    private static final ClassLoader LOADER = ReflectionAccessProviderTest.class.getClassLoader();
    private final ReflectionAccessProvider provider = new ReflectionAccessProvider();

    @ParameterizedTest
    @CsvSource({"1.12.2, true", "1.21.4, true", "1.21.5, true", "26.1, true", "27.0, true",
            "1.11.2, false", "1.16, false", "1.21.12, false"})
    void supportsVersionsInTheNameTables(String version, boolean expected) {
        assertEquals(expected, provider.supports(MinecraftVersion.parse(version)));
    }

    @Test
    void failsWithoutServerClasses() {
        // Supported, but this test classpath has no CraftBukkit/NMS classes
        ServerRuntime runtime = new ServerRuntime("v1_21_R3", MappingType.SPIGOT, LOADER);
        assertThrows(IncompatibleServerException.class,
                () -> provider.create(MinecraftVersion.parse("1.21.4"), runtime));
    }

    @Test
    void impossibleServerIsIncompatible() {
        // Spigot names were never published for 26.1+; detecting them means the detection is wrong
        ServerRuntime runtime = new ServerRuntime(null, MappingType.SPIGOT, LOADER);
        assertThrows(IncompatibleServerException.class,
                () -> provider.create(MinecraftVersion.parse("26.1"), runtime));
    }
}
