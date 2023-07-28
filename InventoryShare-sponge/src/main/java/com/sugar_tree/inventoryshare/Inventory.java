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

package com.sugar_tree.inventoryshare;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.entity.living.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractList;

import static com.sugar_tree.inventoryshare.InventoryShare.logger;

public class Inventory {
    private static AbstractList<Object> items;
    private static AbstractList<Object> armor;
    private static AbstractList<Object> offhand;
    static {
        try {
            //noinspection JavaReflectionMemberAccess, unchecked
            items = (AbstractList<Object>) Class.forName("net.minecraft.core.NonNullList")
                    .getDeclaredMethod("withSize", int.class, Object.class)
                    .invoke(null, 36, Class.forName("net.minecraft.world.item.ItemStack").getField("EMPTY").get(null));
            //noinspection JavaReflectionMemberAccess, unchecked
            armor = (AbstractList<Object>) Class.forName("net.minecraft.core.NonNullList")
                    .getDeclaredMethod("withSize", int.class, Object.class)
                    .invoke(null, 4, Class.forName("net.minecraft.world.item.ItemStack").getField("EMPTY").get(null));
            //noinspection JavaReflectionMemberAccess, unchecked
            offhand = (AbstractList<Object>) Class.forName("net.minecraft.core.NonNullList")
                    .getDeclaredMethod("withSize", int.class, Object.class)
                    .invoke(null, 1, Class.forName("net.minecraft.world.item.ItemStack").getField("EMPTY").get(null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void task(Player p) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
//        setField(Class.forName("net.minecraft.world.entity.player.Inventory").cast(p.inventory()), "items", items);
//        setField(Class.forName("net.minecraft.world.entity.player.Inventory").cast(p.inventory()), "armor", armor);
//        setField(Class.forName("net.minecraft.world.entity.player.Inventory").cast(p.inventory()), "offhand", offhand);
        setField(p.inventory(), "items", items);
        setField(p.inventory(), "armor", armor);
        setField(p.inventory(), "offhand", offhand);
        p.inventory().children().forEach(i -> {
            try {
                Class.forName("org.spongepowered.common.entity.player.SpongeUserInventory").getMethod("setChanged")
                        .invoke(Class.forName("org.spongepowered.common.entity.player.SpongeUserInventory").cast((i)));
            } catch (IllegalAccessException | ClassNotFoundException | NoSuchMethodException |
                     InvocationTargetException e) {
                e.printStackTrace();
            }
        });
//        Class.forName("org.spongepowered.common.entity.player.SpongeUserInventory").getMethod("setChanged")
//                .invoke(Class.forName("org.spongepowered.common.entity.player.SpongeUserInventory").cast(((Container) p.inventory())));
    }

    public static void updateInventory(Player p) {
        try {
//            debug(p.inventory().getClass());
//            logger.info("///////////////////////////////////");
//            debug(Class.forName("net.minecraft.server.level.ServerPlayer"));
//            logger.info("///////////////////////////////////");
//            for (Method method : Class.forName("net.minecraft.server.level.ServerPlayer").getMethods()) {
//                logger.info(method.getName());
//            }
            Class.forName("net.minecraft.server.level.ServerPlayer")
                    .cast(Class.forName("net.minecraft.world.entity.player.Inventory").getField("player")
                    .get(Class.forName("net.minecraft.world.entity.player.Inventory").cast(p.inventory())));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void debug(@NotNull Class<?> clazz) {
        Class<?> clazz1 = clazz;
        while (clazz != null) {
            logger.info(clazz.getCanonicalName());
            clazz = clazz.getSuperclass();
        }
        logger.info("///////////////////////////////////");
        for (Class<?> anInterface : clazz1.getInterfaces()) {
            logger.info(anInterface.getCanonicalName());
        }
    }

    /**
     * @param obj Object which you want to change field
     * @param name Field name
     * @param value Value to change field
     * @throws NoSuchFieldException if a field with the specified name is not found.
     * @throws IllegalAccessException if this Field object is enforcing Java language access control and the underlying field is inaccessible or final; or if this Field object has no write access.
     */
    private static void setField(@NotNull Object obj, @NotNull String name, Object value) throws NoSuchFieldException, IllegalAccessException {
//        logger.info("setField()");
//        logger.info("obj: " + obj);
//        logger.info("name: " + name);
//        logger.info("value: " + value);
//        Field[] fields = obj.getClass().getFields();
//        for (int i = 0; i < fields.length; i++) {
//            logger.info("#" + i + " " + fields[i].getName());
//        }
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
