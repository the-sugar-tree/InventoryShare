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

package com.sugar_tree.inventoryshare.nms.reflection.item;

import com.sugar_tree.inventoryshare.nms.reflection.inventory.InventoryModel;
import com.sugar_tree.inventoryshare.nms.reflection.inventory.legacy.LegacyNameTable;
import com.sugar_tree.inventoryshare.nms.reflection.inventory.modern.ModernNameTable;
import com.sugar_tree.inventoryshare.nms.server.IncompatibleServerException;
import com.sugar_tree.inventoryshare.nms.server.MappingType;
import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemNameFactoryTest {

    /** Only names are built here; nothing is loaded */
    private static final ClassLoader LOADER = ItemNameFactoryTest.class.getClassLoader();

    @ParameterizedTest
    @CsvSource({
            "1.20.5, v1_20_R4, b",
            "1.21, v1_21_R1, b",
            "1.21.3, v1_21_R2, a",
            "1.21.4, v1_21_R3, a",
            "1.21.5, v1_21_R4, b",
            "1.21.11-R0.2-SNAPSHOT, v1_21_R7, b",
            "26.1, v26, ",
            "27.0, v26, ",
    })
    void findsItemCodecRow(String text, String row, String codec) {
        ItemCodecNameTable found = ItemCodecNameTable.find(v(text));
        assertEquals(row, found == null ? null : found.name());
        assertEquals(codec, found == null ? null : found.codec);
    }

    @ParameterizedTest
    @CsvSource({"1.12.2", "1.20.4", "1.21.12"})
    void noItemCodecRow(String version) {
        assertNull(ItemCodecNameTable.find(v(version)));
    }

    @Test
    void eachVersionBelongsToOneItemCodecRow() {
        Set<MinecraftVersion> seen = new HashSet<>();
        for (ItemCodecNameTable row : ItemCodecNameTable.values()) {
            for (MinecraftVersion version : row.versions.getVersions()) {
                assertTrue(seen.add(version), version + " is listed twice");
            }
        }
    }

    @Test
    void itemCodecRowsMatchInventoryRows() {
        // The rows follow the CraftBukkit packages, as the inventory model tables do
        for (ItemCodecNameTable row : ItemCodecNameTable.values()) {
            for (MinecraftVersion version : row.versions.getVersions()) {
                Enum<?> inventoryRow = InventoryModel.of(version) == InventoryModel.LEGACY
                        ? LegacyNameTable.find(version) : ModernNameTable.find(version);
                assertEquals(row.name(), inventoryRow == null ? null : inventoryRow.name(), version.toString());
            }
        }
    }

    @Test
    void spigotItemCodec() throws IncompatibleServerException {
        ItemCodecNames names = ItemNameFactory.itemCodec(v("1.21.4"), runtime("v1_21_R3", MappingType.SPIGOT));

        assertEquals("org.bukkit.craftbukkit.v1_21_R3.inventory.CraftItemStack", names.getCraftItemStackClass());
        assertEquals("org.bukkit.craftbukkit.v1_21_R3.CraftRegistry", names.getCraftRegistryClass());
        assertEquals("net.minecraft.world.item.ItemStack", names.getItemStackClass());
        assertEquals("net.minecraft.nbt.DynamicOpsNBT", names.getNbtOpsClass());
        assertEquals("net.minecraft.core.HolderLookup$a", names.getRegistryProviderClass());
        assertEquals("net.minecraft.nbt.MojangsonParser", names.getTagParserClass());
        assertEquals("net.minecraft.util.datafix.DataConverterRegistry", names.getDataFixersClass());
        assertEquals("a", names.getCodec());
        assertEquals("a", names.getParseTag());
    }

    @ParameterizedTest
    @CsvSource({"1.20.5, parseTag", "1.21.4, parseTag", "1.21.5, parseCompoundFully", "26.2, parseCompoundFully"})
    void mojangItemCodec(String version, String parseTag) throws IncompatibleServerException {
        // Paper 1.20.5+, every 26.1+ server
        ItemCodecNames names = ItemNameFactory.itemCodec(v(version), runtime(null, MappingType.MOJANG));

        assertEquals("org.bukkit.craftbukkit.CraftRegistry", names.getCraftRegistryClass());
        assertEquals("net.minecraft.nbt.NbtOps", names.getNbtOpsClass());
        assertEquals("net.minecraft.core.HolderLookup$Provider", names.getRegistryProviderClass());
        assertEquals("net.minecraft.nbt.TagParser", names.getTagParserClass());
        assertEquals("net.minecraft.util.datafix.DataFixers", names.getDataFixersClass());
        assertEquals("CODEC", names.getCodec());
        assertEquals("INSTANCE", names.getNbtOps());
        assertEquals("createSerializationContext", names.getCreateSerializationContext());
        assertEquals(parseTag, names.getParseTag());
        assertEquals("getDataFixer", names.getGetDataFixer());
    }

    @Test
    void noItemCodecBefore1205() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemNameFactory.itemCodec(v("1.20.4"), runtime(null, MappingType.MOJANG)));
    }

    @Test
    void noSpigotItemCodecNamesFrom26() {
        assertThrows(IncompatibleServerException.class,
                () -> ItemNameFactory.itemCodec(v("26.1"), runtime(null, MappingType.SPIGOT)));
    }

    private static MinecraftVersion v(String version) {
        return MinecraftVersion.parse(version);
    }

    private static ServerRuntime runtime(String craftbukkitPackage, MappingType mappingType) {
        return new ServerRuntime(craftbukkitPackage, mappingType, LOADER);
    }
}
