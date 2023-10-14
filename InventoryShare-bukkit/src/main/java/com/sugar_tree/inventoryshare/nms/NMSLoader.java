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

import com.google.common.collect.ImmutableList;
import com.sugar_tree.inventoryshare.nms.utils.VersionUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class NMSLoader {

    private NMSLoader() {}

    //
    static final AbstractList<Object> sharedItems;
    static final AbstractList<Object> sharedArmor;
    static final AbstractList<Object> sharedExtraSlots;
    static final List<AbstractList<Object>> sharedContents;

    static final Map<String, PlayerInventory> TeamInventoryMap;
    //

    private static final Class<?> CraftPlayer;
    private static final Method toEntityPlayer;
    private static final Class<?> EntityHuman;
    private static final Method inventory_method;
    private static final Field inventory_field;
    private static final boolean DOES_INVENTORY_USE_FIELD;
    private static final Class<?> PlayerInventory;
    private static final Field inventory_items;
    private static final Field inventory_armor;
    private static final Field inventory_extraSlots;

    private static final Method asBukkitCopy;
    private static final Method asNMSCopy;
    private static final Method getNamespacedKey;

    private static final Method createItemList;

    static  {
        try {
            final VersionUtil.SupportedVersions VERSION_INFO = VersionUtil.getVersion();
            DOES_INVENTORY_USE_FIELD = VERSION_INFO.isINVENTORY_USE_FIELD();

            // Load Reflections
            CraftPlayer = Class.forName(VERSION_INFO.getPATH_CLASS_CraftPlayer());
            Class<?> EntityPlayer = Class.forName(VERSION_INFO.getPATH_CLASS_EntityPlayer());
            EntityHuman = Class.forName(VERSION_INFO.getPATH_CLASS_EntityHuman());
            PlayerInventory = Class.forName(VERSION_INFO.getPATH_CLASS_PlayerInventory());
            Class<?> CraftItemStack = Class.forName(VERSION_INFO.getPATH_CLASS_CraftItemStack());
            Class<?> NMSItemStack = Class.forName(VERSION_INFO.getPATH_CLASS_ItemStack());
            Class<?> NonNullList = Class.forName(VERSION_INFO.getPATH_CLASS_NonNullList());

            asBukkitCopy = CraftItemStack
                    .getMethod("asBukkitCopy", NMSItemStack);
            asNMSCopy = CraftItemStack
                    .getMethod("asNMSCopy", ItemStack.class);
            getNamespacedKey = NamespacedKey.class
                    .getDeclaredMethod(VERSION_INFO.getPATH_METHOD_getNameSpacedKey(), String.class);
            createItemList = NonNullList
                    .getDeclaredMethod(VERSION_INFO.getPATH_METHOD_createItemlist(), int.class, Object.class);
            nullItem = NMSItemStack
                    .getField(VERSION_INFO.getPATH_FIELD_emptyItem()).get(null);
            toEntityPlayer = CraftPlayer.getMethod("getHandle");
            if (DOES_INVENTORY_USE_FIELD) {
                inventory_field = EntityHuman
                        .getField(VERSION_INFO.getPATH_EntityPlayer_Inventory());
                inventory_method = null;
            } else {
                inventory_method = EntityPlayer
                        .getMethod(VERSION_INFO.getPATH_EntityPlayer_Inventory());
                inventory_field = null;
            }
            inventory_items = PlayerInventory
                    .getField(VERSION_INFO.getPATH_PlayerInventory_items());
            inventory_armor = PlayerInventory
                    .getField(VERSION_INFO.getPATH_PlayerInventory_armor());
            inventory_extraSlots = PlayerInventory
                    .getField(VERSION_INFO.getPATH_PlayerInventory_extraSlots());
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        sharedItems = createEmptyItemList(36);
        sharedArmor = createEmptyItemList(4);
        sharedExtraSlots = createEmptyItemList(1);
        sharedContents = ImmutableList.of(sharedItems, sharedArmor, sharedExtraSlots);
        TeamInventoryMap = new HashMap<>();
    }

    /**
     * Transform NMS's ItemStack into {@link ItemStack}
     * @param nmsItemStack {@link net.minecraft.world.item}.ItemStack or {@link net.minecraft.server}.v1_00.R0.ItemStack
     * @return {@link ItemStack}
     */
    public static ItemStack asBukkitCopy(final Object nmsItemStack) {
        try {
            return (ItemStack) asBukkitCopy.invoke(null, nmsItemStack);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Transform {@link ItemStack} into NMS's ItemStack
     * @param bukkitItemStack {@link ItemStack}
     * @return {@link net.minecraft.world.item}.ItemStack or {@link net.minecraft.server}.v1_00.R0.ItemStack
     */
    public static Object asNMSCopy(final ItemStack bukkitItemStack) {
        try {
            return asNMSCopy.invoke(null, bukkitItemStack);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static NamespacedKey getNamespacedKey(String name) {
        try {
            return (NamespacedKey) getNamespacedKey.invoke(null, name);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Object nullItem;
    public static AbstractList<Object> createEmptyItemList(int count) {
        try {
            return (AbstractList<Object>) createItemList.invoke(null, count, nullItem);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getPlayerInventory(final Player player) {
        try {
            Object entityPlayer = toEntityPlayer.invoke(CraftPlayer.cast(player));
            if (DOES_INVENTORY_USE_FIELD) {
                return inventory_field.get(entityPlayer);
            } else {
                return inventory_method.invoke(entityPlayer);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static AbstractList<Object> getInventoryItems(Object playerInventory) {
        try {
            return ((AbstractList<Object>) inventory_items.get(playerInventory));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static AbstractList<Object> getInventoryArmor(Object playerInventory) {
        try {
            return ((AbstractList<Object>) inventory_armor.get(playerInventory));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static AbstractList<Object> getInventoryExtraSlots(Object playerInventory) {
        try {
            return ((AbstractList<Object>) inventory_extraSlots.get(playerInventory));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
