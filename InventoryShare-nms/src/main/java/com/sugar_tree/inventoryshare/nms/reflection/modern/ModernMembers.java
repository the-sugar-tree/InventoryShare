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

package com.sugar_tree.inventoryshare.nms.reflection.modern;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static com.sugar_tree.inventoryshare.nms.reflection.Reflections.accessible;
import static com.sugar_tree.inventoryshare.nms.reflection.Reflections.load;

/**
 * Reflective members for {@link ModernNames}.
 */
@Getter
public final class ModernMembers {

    private final Class<?> craftPlayer;
    private final Method getHandle;
    private final Method getInventory;
    private final Class<?> inventory;
    /** {@code Inventory(Player, EntityEquipment)}, to verify the field writes on an inventory of no player */
    private final Constructor<?> newInventory;
    private final Field items;
    private final Field equipment;
    private final Class<?> entityEquipment;
    /** {@code EntityEquipment()}, for the inventory above */
    private final Constructor<?> newEntityEquipment;
    private final Field equipmentItems;
    private final Class<?> equipmentSlot;
    private final Field slotMapping;
    private final Class<?> itemStack;
    private final Field emptyItem;
    private final Class<?> nonNullList;
    private final Method createItemList;
    /** {@code asCraftMirror(ItemStack)}: a Bukkit item backed by the NMS item, without going through ItemMeta */
    private final Method asCraftMirror;
    /** {@code asCraftCopy(org.bukkit.inventory.ItemStack)}: copies the NMS item of a CraftItemStack */
    private final Method asCraftCopy;
    private final Method asNMSCopy;

    private ModernMembers(ModernNames names, ClassLoader loader) throws ReflectiveOperationException {
        craftPlayer = load(names.getCraftPlayerClass(), loader);
        getHandle = craftPlayer.getMethod("getHandle");
        Class<?> player = load(names.getPlayerClass(), loader);
        getInventory = player.getMethod(names.getGetInventory());

        inventory = load(names.getInventoryClass(), loader);
        entityEquipment = load(names.getEntityEquipmentClass(), loader);
        newInventory = inventory.getConstructor(player, entityEquipment);
        items = accessible(inventory.getDeclaredField(names.getItems()));
        equipment = accessible(inventory.getDeclaredField(names.getEquipment()));
        slotMapping = accessible(inventory.getDeclaredField(names.getSlotMapping()));

        newEntityEquipment = entityEquipment.getConstructor();
        equipmentItems = accessible(entityEquipment.getDeclaredField(names.getEquipmentItems()));
        equipmentSlot = load(names.getEquipmentSlotClass(), loader);

        itemStack = load(names.getItemStackClass(), loader);
        emptyItem = itemStack.getField(names.getEmptyItem());
        nonNullList = load(names.getNonNullListClass(), loader);
        createItemList = nonNullList.getDeclaredMethod(names.getCreateItemList(), int.class, Object.class);

        Class<?> craftItemStack = load(names.getCraftItemStackClass(), loader);
        Class<?> bukkitItemStack = load(names.getBukkitItemStackClass(), loader);
        asCraftMirror = craftItemStack.getMethod("asCraftMirror", itemStack);
        asCraftCopy = craftItemStack.getMethod("asCraftCopy", bukkitItemStack);
        asNMSCopy = craftItemStack.getMethod("asNMSCopy", bukkitItemStack);
    }

    public static @NotNull ModernMembers resolve(@NotNull ModernNames names, @NotNull ClassLoader loader)
            throws ReflectiveOperationException {
        return new ModernMembers(names, loader);
    }
}
