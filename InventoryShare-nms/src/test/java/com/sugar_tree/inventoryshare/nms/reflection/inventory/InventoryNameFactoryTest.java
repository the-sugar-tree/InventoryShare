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

import com.sugar_tree.inventoryshare.nms.reflection.inventory.legacy.LegacyNameTable;
import com.sugar_tree.inventoryshare.nms.reflection.inventory.legacy.LegacyNames;
import com.sugar_tree.inventoryshare.nms.reflection.inventory.modern.ModernNameTable;
import com.sugar_tree.inventoryshare.nms.reflection.inventory.modern.ModernNames;
import com.sugar_tree.inventoryshare.nms.server.IncompatibleServerException;
import com.sugar_tree.inventoryshare.nms.server.MappingType;
import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryNameFactoryTest {

    /** Only names are built here; nothing is loaded */
    private static final ClassLoader LOADER = InventoryNameFactoryTest.class.getClassLoader();

    @ParameterizedTest
    @CsvSource({
            "1.12.2, LEGACY, v1_12_R1",
            "1.13, LEGACY, v1_13_R1",
            "1.13.1, LEGACY, v1_13_R2",
            "1.19.2, LEGACY, v1_19_1_R1",
            "1.21, LEGACY, v1_21_R1",
            "1.21.0, LEGACY, v1_21_R1",
            "1.21.4-R0.1-SNAPSHOT, LEGACY, v1_21_R3",
            "1.21.5, MODERN, v1_21_R4",
            "1.21.7, MODERN, v1_21_R5",
            "1.21.9, MODERN, v1_21_R6",
            "1.21.11-R0.2-SNAPSHOT, MODERN, v1_21_R7",
            "26.1, MODERN, v26",
            "26.1.2.build.63-stable, MODERN, v26",
            "27.0, MODERN, v26",
    })
    void findsModelAndRow(String text, InventoryModel model, String row) {
        MinecraftVersion version = MinecraftVersion.parse(text);
        assertEquals(model, InventoryModel.of(version));
        Enum<?> found = model == InventoryModel.LEGACY ? LegacyNameTable.find(version) : ModernNameTable.find(version);
        assertEquals(row, found == null ? null : found.name());
    }

    @ParameterizedTest
    @CsvSource({"1.11.2", "1.16", "1.21.12", "25.9"})
    void unsupportedVersionHasNoModel(String version) {
        assertNull(InventoryModel.of(MinecraftVersion.parse(version)));
    }

    @Test
    void spigotLegacyBefore117() throws IncompatibleServerException {
        LegacyNames names = InventoryNameFactory.legacy(v("1.16.5"), runtime("v1_16_R3", MappingType.SPIGOT));

        assertEquals("org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer", names.getCraftPlayerClass());
        assertEquals("org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack", names.getCraftItemStackClass());
        assertEquals("net.minecraft.server.v1_16_R3.EntityHuman", names.getInventoryOwnerClass());
        assertEquals("net.minecraft.server.v1_16_R3.PlayerInventory", names.getPlayerInventoryClass());
        assertEquals("net.minecraft.server.v1_16_R3.EntityHuman", names.getPlayerClass());
        assertEquals("net.minecraft.server.v1_16_R3.ItemStack", names.getItemStackClass());
        assertEquals("net.minecraft.server.v1_16_R3.NonNullList", names.getNonNullListClass());
        assertTrue(names.isInventoryField());
        assertEquals("inventory", names.getInventory());
        assertEquals("extraSlots", names.getExtraSlots());
        assertEquals("b", names.getEmptyItem());
        assertEquals("a", names.getCreateItemList());
    }

    @Test
    void spigotLegacyFrom117() throws IncompatibleServerException {
        LegacyNames names = InventoryNameFactory.legacy(v("1.21.4"), runtime("v1_21_R3", MappingType.SPIGOT));

        assertEquals("org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer", names.getCraftPlayerClass());
        assertEquals("net.minecraft.server.level.EntityPlayer", names.getInventoryOwnerClass());
        assertEquals("net.minecraft.world.entity.player.PlayerInventory", names.getPlayerInventoryClass());
        assertEquals("net.minecraft.world.entity.player.EntityHuman", names.getPlayerClass());
        assertEquals("net.minecraft.world.item.ItemStack", names.getItemStackClass());
        assertEquals("net.minecraft.core.NonNullList", names.getNonNullListClass());
        assertFalse(names.isInventoryField());
        assertEquals("gi", names.getInventory());
        assertEquals("g", names.getItems());
        assertEquals("l", names.getContents());
    }

    @Test
    void mojangLegacy() throws IncompatibleServerException {
        // Paper 1.20.5 - 1.21.4
        LegacyNames names = InventoryNameFactory.legacy(v("1.21.4"), runtime(null, MappingType.MOJANG));

        assertEquals("org.bukkit.craftbukkit.entity.CraftPlayer", names.getCraftPlayerClass());
        assertEquals("net.minecraft.server.level.ServerPlayer", names.getInventoryOwnerClass());
        assertEquals("net.minecraft.world.entity.player.Inventory", names.getPlayerInventoryClass());
        assertEquals("net.minecraft.world.entity.player.Player", names.getPlayerClass());
        assertFalse(names.isInventoryField());
        assertEquals("getInventory", names.getInventory());
        assertEquals("items", names.getItems());
        assertEquals("armor", names.getArmor());
        assertEquals("offhand", names.getExtraSlots());
        assertEquals("compartments", names.getContents());
        assertEquals("EMPTY", names.getEmptyItem());
        assertEquals("withSize", names.getCreateItemList());
    }

    @Test
    void mojangLegacyKeepsRelocatedPackage() throws IncompatibleServerException {
        // Paper's Mojang-mapped 1.20.4 build
        LegacyNames names = InventoryNameFactory.legacy(v("1.20.4"), runtime("v1_20_R3", MappingType.MOJANG));

        assertEquals("org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer", names.getCraftPlayerClass());
        assertEquals("net.minecraft.server.level.ServerPlayer", names.getInventoryOwnerClass());
    }

    @Test
    void spigotModern() throws IncompatibleServerException {
        ModernNames names = InventoryNameFactory.modern(v("1.21.5"), runtime("v1_21_R4", MappingType.SPIGOT));

        assertEquals("org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer", names.getCraftPlayerClass());
        assertEquals("net.minecraft.world.entity.player.EntityHuman", names.getPlayerClass());
        assertEquals("net.minecraft.world.entity.player.PlayerInventory", names.getInventoryClass());
        assertEquals("net.minecraft.world.entity.EnumItemSlot", names.getEquipmentSlotClass());
        assertEquals("net.minecraft.world.entity.EntityEquipment", names.getEntityEquipmentClass());
        assertEquals("gj", names.getGetInventory());
        assertEquals("g", names.getSlotMapping());
    }

    @Test
    void mojangModern() throws IncompatibleServerException {
        // Paper 1.21.5+, every 26.1+ server
        ModernNames names = InventoryNameFactory.modern(v("26.1"), runtime(null, MappingType.MOJANG));

        assertEquals("org.bukkit.craftbukkit.entity.CraftPlayer", names.getCraftPlayerClass());
        assertEquals("net.minecraft.world.entity.player.Player", names.getPlayerClass());
        assertEquals("net.minecraft.world.entity.player.Inventory", names.getInventoryClass());
        assertEquals("net.minecraft.world.entity.EquipmentSlot", names.getEquipmentSlotClass());
        assertEquals("getInventory", names.getGetInventory());
        assertEquals("EQUIPMENT_SLOT_MAPPING", names.getSlotMapping());
        assertEquals("EMPTY", names.getEmptyItem());
    }

    @Test
    void noMojangNamesBefore117() {
        assertThrows(IncompatibleServerException.class,
                () -> InventoryNameFactory.legacy(v("1.16.5"), runtime("v1_16_R3", MappingType.MOJANG)));
    }

    @Test
    void noVersionlessPackageBefore117() {
        assertThrows(IncompatibleServerException.class,
                () -> InventoryNameFactory.legacy(v("1.16.5"), runtime(null, MappingType.SPIGOT)));
    }

    @Test
    void noSpigotNamesFrom26() {
        assertThrows(IncompatibleServerException.class,
                () -> InventoryNameFactory.modern(v("26.1"), runtime(null, MappingType.SPIGOT)));
    }

    @Test
    void versionOfTheOtherModel() {
        assertThrows(IllegalArgumentException.class,
                () -> InventoryNameFactory.legacy(v("1.21.5"), runtime(null, MappingType.MOJANG)));
        assertThrows(IllegalArgumentException.class,
                () -> InventoryNameFactory.modern(v("1.21.4"), runtime(null, MappingType.MOJANG)));
    }

    private static MinecraftVersion v(String version) {
        return MinecraftVersion.parse(version);
    }

    private static ServerRuntime runtime(String craftbukkitPackage, MappingType mappingType) {
        return new ServerRuntime(craftbukkitPackage, mappingType, LOADER);
    }
}
