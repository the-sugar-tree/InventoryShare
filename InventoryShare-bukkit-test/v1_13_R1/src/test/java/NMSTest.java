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

import com.sugar_tree.inventoryshare.nms.utils.VersionUtil;
import com.sugar_tree.inventoryshare.v1_13_R1.NmsScratch;
import net.minecraft.server.v1_13_R1.EntityHuman;
import net.minecraft.server.v1_13_R1.EntityPlayer;
import net.minecraft.server.v1_13_R1.NonNullList;
import net.minecraft.server.v1_13_R1.ItemStack;
import net.minecraft.server.v1_13_R1.PlayerInventory;
import org.bukkit.craftbukkit.v1_13_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R1.inventory.CraftItemStack;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.AbstractList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("VersionUtil v1_13_R1 mapping vs Minecraft 1.13 NMS")
public class NMSTest {

    private static final VersionUtil.SupportedVersions VERSION = VersionUtil.SupportedVersions.v1_13_R1;

    @Test
    @DisplayName("obfuscated member names match NmsScratch")
    void memberNamesMatchScratch() {
        assertEquals(NmsScratch.INVENTORY, VERSION.getPATH_EntityPlayer_Inventory());
        assertEquals(NmsScratch.ITEMS, VERSION.getPATH_PlayerInventory_items());
        assertEquals(NmsScratch.ARMOR, VERSION.getPATH_PlayerInventory_armor());
        assertEquals(NmsScratch.EXTRA_SLOTS, VERSION.getPATH_PlayerInventory_extraSlots());
        assertEquals(NmsScratch.CONTENTS, VERSION.getPATH_PlayerInventory_contents());
        assertEquals(NmsScratch.EMPTY_ITEM, VERSION.getPATH_FIELD_emptyItem());
    }

    @Test
    @DisplayName("class paths match this Spigot's NMS types")
    void classPathsMatchLoadedNms() {
        assertEquals(CraftPlayer.class.getName(), VERSION.getPATH_CLASS_CraftPlayer());
        assertEquals(CraftPlayer.class.getName(), VERSION.getPATH_CLASS_EntityPlayer());
        assertEquals(EntityHuman.class.getName(), VERSION.getPATH_CLASS_EntityHuman());
        assertEquals(PlayerInventory.class.getName(), VERSION.getPATH_CLASS_PlayerInventory());
        assertEquals(ItemStack.class.getName(), VERSION.getPATH_CLASS_ItemStack());
        assertEquals(NonNullList.class.getName(), VERSION.getPATH_CLASS_NonNullList());
        assertEquals(CraftItemStack.class.getName(), VERSION.getPATH_CLASS_CraftItemStack());
    }

    @Test
    @DisplayName("bukkit version aliases and layout flags")
    void bukkitVersionAliases() {
        assertTrue(VERSION.getVersions().contains("1.13-R0.1-SNAPSHOT"));
        assertTrue(VERSION.isINVENTORY_USE_FIELD());
        assertEquals("minecraft", VERSION.getPATH_METHOD_getNameSpacedKey());
        assertEquals("a", VERSION.getPATH_METHOD_createItemlist());
    }

    @Test
    @DisplayName("CraftPlayer.getHandle returns EntityPlayer")
    void craftPlayerHandle() throws NoSuchMethodException {
        Method getHandle = CraftPlayer.class.getMethod("getHandle");
        assertEquals(EntityPlayer.class, getHandle.getReturnType());
    }

    @Test
    @DisplayName("inventory accessor is a field on EntityHuman")
    void inventoryAccessor() throws NoSuchFieldException {
        Field inventory = EntityHuman.class.getField(VERSION.getPATH_EntityPlayer_Inventory());
        assertEquals(PlayerInventory.class, inventory.getType());
    }

    @Test
    @DisplayName("items/armor/extraSlots are public NonNullList<ItemStack>")
    void inventoryListFields() throws NoSuchFieldException {
        assertItemListField(PlayerInventory.class.getField(VERSION.getPATH_PlayerInventory_items()));
        assertItemListField(PlayerInventory.class.getField(VERSION.getPATH_PlayerInventory_armor()));
        assertItemListField(PlayerInventory.class.getField(VERSION.getPATH_PlayerInventory_extraSlots()));
    }

    @Test
    @DisplayName("contents is List of item lists")
    void inventoryContentsField() throws NoSuchFieldException {
        Field contents = PlayerInventory.class.getDeclaredField(VERSION.getPATH_PlayerInventory_contents());
        ParameterizedType listType = (ParameterizedType) contents.getGenericType();
        ParameterizedType innerList = (ParameterizedType) listType.getActualTypeArguments()[0];
        assertEquals(ItemStack.class, innerList.getActualTypeArguments()[0]);
    }

    @Test
    @DisplayName("empty ItemStack field is public static")
    void emptyItemField() throws NoSuchFieldException {
        Field empty = ItemStack.class.getField(VERSION.getPATH_FIELD_emptyItem());
        assertTrue(Modifier.isStatic(empty.getModifiers()));
        assertTrue(Modifier.isPublic(empty.getModifiers()));
        assertEquals(ItemStack.class, empty.getType());
    }

    @Test
    @DisplayName("NonNullList factory matches VersionUtil (int, Object)")
    void createItemListFactory() throws NoSuchMethodException {
        Method factory = NonNullList.class.getDeclaredMethod(VERSION.getPATH_METHOD_createItemlist(), int.class, Object.class);
        assertTrue(Modifier.isStatic(factory.getModifiers()));
        assertTrue(AbstractList.class.isAssignableFrom(factory.getReturnType()));
    }

    @Test
    @DisplayName("CraftItemStack conversion methods exist")
    void itemConversionMethods() {
        assertDoesNotThrow(() -> CraftItemStack.class.getMethod("asBukkitCopy", ItemStack.class));
        assertDoesNotThrow(() -> CraftItemStack.class.getMethod("asNMSCopy", org.bukkit.inventory.ItemStack.class));
    }

    @Test
    @DisplayName("NamespacedKey factory exists")
    void namespacedKeyFactory() {
        assertDoesNotThrow(() -> NamespacedKey.class.getDeclaredMethod(VERSION.getPATH_METHOD_getNameSpacedKey(), String.class));
    }

    private static void assertItemListField(Field field) {
        assertTrue(Modifier.isPublic(field.getModifiers()), field.getName() + " should be public (NMSLoader uses getField)");
        ParameterizedType type = (ParameterizedType) field.getGenericType();
        assertEquals(ItemStack.class, type.getActualTypeArguments()[0]);
    }
}
