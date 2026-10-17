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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Reflection helpers shared by the inventory models.
 *
 * <p>Members are resolved and made accessible up front, so an access failure afterwards is a bug and is thrown as
 * {@link IllegalStateException}. An exception thrown by the invoked method itself is rethrown as is.
 */
public final class Reflections {

    private Reflections() {}

    /**
     * Looks up a class without initializing it, so the same lookup runs on a live server and in tests against an
     * isolated server jar.
     */
    public static @NotNull Class<?> load(@NotNull String name, @NotNull ClassLoader loader)
            throws ClassNotFoundException {
        return Class.forName(name, false, loader);
    }

    public static @NotNull Field accessible(@NotNull Field field) {
        field.setAccessible(true);
        return field;
    }

    public static Object invoke(@NotNull Method method, @Nullable Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IllegalStateException(cause);
        }
    }

    public static Object get(@NotNull Field field, @Nullable Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void set(@NotNull Field field, @Nullable Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
