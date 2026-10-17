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

import com.sugar_tree.inventoryshare.nms.Namespace;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflective members for {@link ModernNames}.
 *
 * <p>Every class is looked up in the given loader without initializing it, so the same resolution runs
 * on a live server and in tests against an isolated Spigot jar.
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
        Namespace namespace = names.getNamespace();

        craftPlayer = load(names.getCraftPlayerClass(), loader);
        getHandle = craftPlayer.getMethod("getHandle");
        getInventory = load(namespace.getPlayerClass(), loader).getMethod(names.getGetInventory());

        inventory = load(namespace.getInventoryClass(), loader);
        items = accessible(inventory.getDeclaredField(names.getItems()));
        equipment = accessible(inventory.getDeclaredField(names.getEquipment()));
        slotMapping = accessible(inventory.getDeclaredField(names.getSlotMapping()));

        entityEquipment = load(Namespace.ENTITY_EQUIPMENT, loader);
        equipmentItems = accessible(entityEquipment.getDeclaredField(names.getEquipmentItems()));
        equipmentSlot = load(namespace.getEquipmentSlotClass(), loader);

        itemStack = load(Namespace.ITEM_STACK, loader);
        emptyItem = itemStack.getField(names.getEmptyItem());
        nonNullList = load(Namespace.NON_NULL_LIST, loader);
        createItemList = nonNullList.getDeclaredMethod(names.getCreateItemList(), int.class, Object.class);

        Class<?> craftItemStack = load(names.getCraftItemStackClass(), loader);
        asBukkitCopy = craftItemStack.getMethod("asBukkitCopy", itemStack);
        asNMSCopy = craftItemStack.getMethod("asNMSCopy", load("org.bukkit.inventory.ItemStack", loader));
    }

    public static @NotNull ModernMembers resolve(@NotNull ModernNames names, @NotNull ClassLoader loader)
            throws ReflectiveOperationException {
        return new ModernMembers(names, loader);
    }

    private static Class<?> load(String name, ClassLoader loader) throws ClassNotFoundException {
        return Class.forName(name, false, loader);
    }

    private static Field accessible(Field field) {
        field.setAccessible(true);
        return field;
    }
}
