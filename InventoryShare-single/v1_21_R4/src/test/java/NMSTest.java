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
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NMSTest {

    static VersionUtil.SupportedVersions currentVersion;

    @BeforeAll
    public static void registerVersion() {
        currentVersion = VersionUtil.SupportedVersions.v1_21_R3;
    }

    @Test
    public void NMSAllTest() {
        System.out.println(this);
        final Class<?> CraftPlayer = assertDoesNotThrow(() -> Class.forName(currentVersion.getPATH_CLASS_CraftPlayer()));
        assertDoesNotThrow(() -> CraftPlayer.getMethod("getHandle"));

        if (!currentVersion.isINVENTORY_USE_FIELD()) {
            final Class<?> EntityPlayer = assertDoesNotThrow(() -> Class.forName(currentVersion.getPATH_CLASS_EntityPlayer(), false, Thread.currentThread().getContextClassLoader()));
            assertDoesNotThrow(() -> EntityPlayer.getMethod(currentVersion.getPATH_EntityPlayer_Inventory()));
        } else {
            final Class<?> EntityHuman = assertDoesNotThrow(() -> Class.forName(currentVersion.getPATH_CLASS_EntityHuman()));
            assertDoesNotThrow(() -> EntityHuman.getMethod(currentVersion.getPATH_EntityPlayer_Inventory()));
        }

        final Class<?> PlayerInventory = assertDoesNotThrow(() -> Class.forName(currentVersion.getPATH_CLASS_PlayerInventory()));
        final Field items = assertDoesNotThrow(() -> PlayerInventory.getField(currentVersion.getPATH_PlayerInventory_items()));
        final Field armor = assertDoesNotThrow(() -> PlayerInventory.getField(currentVersion.getPATH_PlayerInventory_armor()));
        final Field extraSlots = assertDoesNotThrow(() -> PlayerInventory.getField(currentVersion.getPATH_PlayerInventory_extraSlots()));
        final Field contents = assertDoesNotThrow(() -> PlayerInventory.getDeclaredField(currentVersion.getPATH_PlayerInventory_contents()));
        assertEquals(net.minecraft.world.item.ItemStack.class, ((ParameterizedType) items.getGenericType()).getActualTypeArguments()[0]);
        assertEquals(net.minecraft.world.item.ItemStack.class, ((ParameterizedType) armor.getGenericType()).getActualTypeArguments()[0]);
        assertEquals(net.minecraft.world.item.ItemStack.class, ((ParameterizedType) extraSlots.getGenericType()).getActualTypeArguments()[0]);
        assertEquals(net.minecraft.world.item.ItemStack.class, ((ParameterizedType) ((ParameterizedType) contents.getGenericType()).getActualTypeArguments()[0]).getActualTypeArguments()[0]);

        final Class<?> CraftItemStack = assertDoesNotThrow(() -> Class.forName(currentVersion.getPATH_CLASS_CraftItemStack()));
        final Class<?> NMSItemStack = assertDoesNotThrow(() -> Class.forName(currentVersion.getPATH_CLASS_ItemStack(), false, Thread.currentThread().getContextClassLoader()));
        final Class<?> NonNullList = assertDoesNotThrow(() -> Class.forName(currentVersion.getPATH_CLASS_NonNullList()));
        assertDoesNotThrow(() -> CraftItemStack.getMethod("asBukkitCopy", NMSItemStack));
        assertDoesNotThrow(() -> CraftItemStack.getMethod("asNMSCopy", ItemStack.class));
        assertDoesNotThrow(() -> NonNullList.getDeclaredMethod(currentVersion.getPATH_METHOD_createItemlist(), int.class, Object.class));
        final Field nullItemField = assertDoesNotThrow(() -> NMSItemStack.getField(currentVersion.getPATH_FIELD_emptyItem()));
        assertEquals(net.minecraft.world.item.ItemStack.class, nullItemField.getType());

        assertDoesNotThrow(() -> NamespacedKey.class.getDeclaredMethod(currentVersion.getPATH_METHOD_getNameSpacedKey(), String.class));
    }
}
