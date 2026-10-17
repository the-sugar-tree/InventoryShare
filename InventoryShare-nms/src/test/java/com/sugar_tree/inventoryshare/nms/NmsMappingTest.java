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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NmsMappingTest {

    /** Only the names are checked here; nothing is loaded */
    private static final ClassLoader LOADER = NmsMappingTest.class.getClassLoader();

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

    @ParameterizedTest
    @EnumSource(NmsMapping.class)
    void spigotRuntimeKeepsRowNamesWithServerPackage(NmsMapping mapping) {
        ServerRuntime runtime = new ServerRuntime("v9_99_R9", Namespace.SPIGOT, LOADER);
        NmsLayout names = mapping.getLayout().forRuntime(runtime);
        if (names instanceof LegacyNames) {
            LegacyNames row = (LegacyNames) mapping.getLayout();
            LegacyNames resolved = (LegacyNames) names;
            assertEquals("v9_99_R9", resolved.getCraftbukkitPackage());
            assertEquals(row.getItems(), resolved.getItems());
            assertEquals(row.getInventory(), resolved.getInventory());
        } else {
            ModernNames row = (ModernNames) mapping.getLayout();
            ModernNames resolved = (ModernNames) names;
            assertEquals("v9_99_R9", resolved.getCraftbukkitPackage());
            assertEquals(row.getItems(), resolved.getItems());
            assertEquals(row.getNamespace(), resolved.getNamespace());
        }
    }

    @Test
    void mojangRuntimeUsesMojangLegacyNames() {
        ServerRuntime paper1214 = new ServerRuntime(null, Namespace.MOJANG, LOADER);
        LegacyNames names = (LegacyNames) NmsMapping.v1_21_R3.getLayout().forRuntime(paper1214);

        assertEquals(Namespace.MOJANG, names.getNamespace());
        assertNull(names.getCraftbukkitPackage());
        assertEquals("org.bukkit.craftbukkit.entity.CraftPlayer", names.getCraftPlayerClass());
        assertEquals("net.minecraft.server.level.ServerPlayer", names.getInventoryOwnerClass());
        assertEquals("net.minecraft.world.entity.player.Inventory", names.getPlayerInventoryClass());
        assertEquals("getInventory", names.getInventory());
        assertEquals("items", names.getItems());
        assertEquals("armor", names.getArmor());
        assertEquals("offhand", names.getExtraSlots());
        assertEquals("compartments", names.getContents());
        assertEquals("EMPTY", names.getEmptyItem());
        assertEquals("withSize", names.getCreateItemList());
    }

    @Test
    void mojangRuntimeKeepsRelocatedPackage() {
        // Paper's Mojang-mapped 1.20.4 build
        ServerRuntime paper1204Mojang = new ServerRuntime("v1_20_R3", Namespace.MOJANG, LOADER);
        LegacyNames names = (LegacyNames) NmsMapping.v1_20_R3.getLayout().forRuntime(paper1204Mojang);

        assertEquals("org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer", names.getCraftPlayerClass());
        assertEquals("net.minecraft.server.level.ServerPlayer", names.getInventoryOwnerClass());
    }

    @Test
    void mojangRuntimeUsesMojangModernNames() {
        ServerRuntime paper1215 = new ServerRuntime(null, Namespace.MOJANG, LOADER);
        ModernNames names = (ModernNames) NmsMapping.v1_21_R4.getLayout().forRuntime(paper1215);

        assertEquals(Namespace.MOJANG, names.getNamespace());
        assertEquals("org.bukkit.craftbukkit.entity.CraftPlayer", names.getCraftPlayerClass());
        assertEquals("getInventory", names.getGetInventory());
        assertEquals("EQUIPMENT_SLOT_MAPPING", names.getSlotMapping());
    }

    @Test
    void noMojangNamesBefore117() {
        ServerRuntime mojang = new ServerRuntime("v1_16_R3", Namespace.MOJANG, LOADER);
        assertThrows(IllegalStateException.class, () -> NmsMapping.v1_16_R3.getLayout().forRuntime(mojang));
    }
}
