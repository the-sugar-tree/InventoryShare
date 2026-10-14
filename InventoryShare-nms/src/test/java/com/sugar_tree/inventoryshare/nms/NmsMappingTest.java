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

import com.sugar_tree.inventoryshare.nms.legacy.LegacyNames;
import com.sugar_tree.inventoryshare.nms.modern.ModernNames;
import com.sugar_tree.inventoryshare.nms.modern.Namespace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NmsMappingTest {

    @ParameterizedTest
    @CsvSource({
            "1.12.2, v1_12_R1",
            "1.13, v1_13_R1",
            "1.13.1, v1_13_R2",
            "1.19.2, v1_19_1_R1",
            "1.21, v1_21_R1",
            "1.21.0, v1_21_R1",
            "1.21.4-R0.1-SNAPSHOT, v1_21_R3",
            "1.21.5, v1_21_R4",
            "1.21.7, v1_21_R5",
            "1.21.9, v1_21_R6",
            "1.21.11-R0.2-SNAPSHOT, v1_21_R7",
            "26.1, v26",
            "26.1.2.build.63-stable, v26",
            "27.0, v26",
    })
    void findsRow(String version, NmsMapping expected) {
        assertEquals(expected, NmsMapping.find(ServerVersion.parse(version)));
    }

    @ParameterizedTest
    @CsvSource({"1.11.2", "1.16", "1.21.12", "25.9"})
    void unsupportedVersionHasNoRow(String version) {
        assertNull(NmsMapping.find(ServerVersion.parse(version)));
    }

    @Test
    void eachVersionBelongsToOneRow() {
        Set<ServerVersion> seen = new HashSet<>();
        for (NmsMapping mapping : NmsMapping.values()) {
            for (ServerVersion version : mapping.getVersions()) {
                assertTrue(seen.add(version), version + " is listed twice");
            }
        }
    }

    @ParameterizedTest
    @EnumSource(NmsMapping.class)
    void craftBukkitPackageMatchesRowName(NmsMapping mapping) {
        if (mapping.getLayout() instanceof LegacyNames) {
            LegacyNames names = (LegacyNames) mapping.getLayout();
            String expected = mapping == NmsMapping.v1_19_1_R1 ? "v1_19_R1" : mapping.name();
            assertEquals(expected, names.getCraftbukkitPackage());
            assertEquals(mapping.compareTo(NmsMapping.v1_17_R1) >= 0, names.isFlattened());
        } else {
            ModernNames names = (ModernNames) mapping.getLayout();
            if (names.getNamespace() == Namespace.SPIGOT) {
                assertEquals(mapping.name(), names.getCraftbukkitPackage());
            } else {
                assertNull(names.getCraftbukkitPackage());
            }
        }
    }
}
