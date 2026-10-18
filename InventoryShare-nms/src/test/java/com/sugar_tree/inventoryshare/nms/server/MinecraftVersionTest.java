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

package com.sugar_tree.inventoryshare.nms.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftVersionTest {

    @ParameterizedTest
    @CsvSource({
            "1.21.11, 1.21.11",
            "1.21.11-R0.2-SNAPSHOT, 1.21.11",
            "1.12.2-R0.1-SNAPSHOT, 1.12.2",
            "26.1.2.build.63-stable, 26.1.2",
            "26.2, 26.2",
    })
    void parsesNumericPrefix(String input, String expected) {
        assertEquals(expected, MinecraftVersion.parse(input).toString());
    }

    @Test
    void rejectsNonVersion() {
        assertThrows(IllegalArgumentException.class, () -> MinecraftVersion.parse("git-Paper-1"));
    }

    @Test
    void missingComponentsAreZero() {
        assertEquals(MinecraftVersion.parse("1.21"), MinecraftVersion.parse("1.21.0"));
        assertEquals(MinecraftVersion.parse("1.21").hashCode(), MinecraftVersion.parse("1.21.0").hashCode());
        assertNotEquals(MinecraftVersion.parse("1.21"), MinecraftVersion.parse("1.21.1"));
    }

    @Test
    void comparesNumerically() {
        assertTrue(MinecraftVersion.parse("1.21.10").compareTo(MinecraftVersion.parse("1.21.9")) > 0);
        assertTrue(MinecraftVersion.parse("26.1").compareTo(MinecraftVersion.parse("1.21.11")) > 0);
        assertTrue(MinecraftVersion.parse("1.12.2").isOlderThan("1.13"));
        assertFalse(MinecraftVersion.parse("1.13").isOlderThan("1.13"));
        assertTrue(MinecraftVersion.parse("26.2").isAtLeast("26.1"));
    }
}
