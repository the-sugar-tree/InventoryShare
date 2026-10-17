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

package com.sugar_tree.inventoryshare.nms.modern;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static com.sugar_tree.inventoryshare.nms.Reflections.accessible;
import static com.sugar_tree.inventoryshare.nms.Reflections.load;

/**
 * Reflective members for {@link ModernNames}.
 */
@Getter
public final class ModernMembers {

    private final Class<?> craftPlayer;
    private final Method getHandle;
    private final Method getInventory;
    private final Class<?> inventory;
    private final Field items;
    private final Field equipment;
    private final Class<?> entityEquipment;
    private final Field equipmentItems;
    private final Class<?> equipmentSlot;
    private final Field slotMapping;
    private final Class<?> itemStack;
    private final Field emptyItem;
    private final Class<?> nonNullList;
    private final Method createItemList;
    private final Method asBukkitCopy;
    private final Method asNMSCopy;

    private ModernMembers(ModernNames names, ClassLoader loader) throws ReflectiveOperationException {
        craftPlayer = load(names.getCraftPlayerClass(), loader);
        getHandle = craftPlayer.getMethod("getHandle");
        getInventory = load(names.getPlayerClass(), loader).getMethod(names.getGetInventory());

        inventory = load(names.getInventoryClass(), loader);
        items = accessible(inventory.getDeclaredField(names.getItems()));
        equipment = accessible(inventory.getDeclaredField(names.getEquipment()));
        slotMapping = accessible(inventory.getDeclaredField(names.getSlotMapping()));

        entityEquipment = load(names.getEntityEquipmentClass(), loader);
        equipmentItems = accessible(entityEquipment.getDeclaredField(names.getEquipmentItems()));
        equipmentSlot = load(names.getEquipmentSlotClass(), loader);

        itemStack = load(names.getItemStackClass(), loader);
        emptyItem = itemStack.getField(names.getEmptyItem());
        nonNullList = load(names.getNonNullListClass(), loader);
        createItemList = nonNullList.getDeclaredMethod(names.getCreateItemList(), int.class, Object.class);

        Class<?> craftItemStack = load(names.getCraftItemStackClass(), loader);
        asBukkitCopy = craftItemStack.getMethod("asBukkitCopy", itemStack);
        asNMSCopy = craftItemStack.getMethod("asNMSCopy", load(names.getBukkitItemStackClass(), loader));
    }

    public static @NotNull ModernMembers resolve(@NotNull ModernNames names, @NotNull ClassLoader loader)
            throws ReflectiveOperationException {
        return new ModernMembers(names, loader);
    }
}
