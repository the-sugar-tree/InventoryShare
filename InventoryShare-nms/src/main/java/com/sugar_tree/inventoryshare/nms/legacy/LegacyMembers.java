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

package com.sugar_tree.inventoryshare.nms.legacy;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * Reflective members for {@link LegacyNames}.
 *
 * <p>Every class is looked up in the given loader without initializing it, so the same resolution runs
 * on a live server and in tests against an isolated Spigot jar.
 */
@Getter
public final class LegacyMembers {

    private final Class<?> craftPlayer;
    private final Method getHandle;
    /** {@link Field} before 1.17, {@link Method} from 1.17 */
    private final Member inventory;
    private final Class<?> playerInventory;
    private final Field items;
    private final Field armor;
    private final Field extraSlots;
    private final Field contents;
    private final Class<?> itemStack;
    private final Field emptyItem;
    private final Method createItemList;
    private final Method asBukkitCopy;
    private final Method asNMSCopy;

    private LegacyMembers(LegacyNames names, ClassLoader loader) throws ReflectiveOperationException {
        craftPlayer = load(names.getCraftPlayerClass(), loader);
        getHandle = craftPlayer.getMethod("getHandle");
        Class<?> inventoryOwner = load(names.getInventoryOwnerClass(), loader);
        inventory = names.isFlattened()
                ? inventoryOwner.getMethod(names.getInventory())
                : inventoryOwner.getField(names.getInventory());

        playerInventory = load(names.getPlayerInventoryClass(), loader);
        items = accessible(playerInventory.getField(names.getItems()));
        armor = accessible(playerInventory.getField(names.getArmor()));
        extraSlots = accessible(playerInventory.getField(names.getExtraSlots()));
        contents = accessible(playerInventory.getDeclaredField(names.getContents()));

        itemStack = load(names.getItemStackClass(), loader);
        emptyItem = itemStack.getField(names.getEmptyItem());
        createItemList = load(names.getNonNullListClass(), loader)
                .getDeclaredMethod(names.getCreateItemList(), int.class, Object.class);

        Class<?> craftItemStack = load(names.getCraftItemStackClass(), loader);
        asBukkitCopy = craftItemStack.getMethod("asBukkitCopy", itemStack);
        asNMSCopy = craftItemStack.getMethod("asNMSCopy", load("org.bukkit.inventory.ItemStack", loader));
    }

    public static @NotNull LegacyMembers resolve(@NotNull LegacyNames names, @NotNull ClassLoader loader)
            throws ReflectiveOperationException {
        return new LegacyMembers(names, loader);
    }

    private static Class<?> load(String name, ClassLoader loader) throws ClassNotFoundException {
        return Class.forName(name, false, loader);
    }

    private static Field accessible(Field field) {
        field.setAccessible(true);
        return field;
    }
}
