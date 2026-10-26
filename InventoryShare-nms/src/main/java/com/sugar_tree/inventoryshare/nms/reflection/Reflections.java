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

package com.sugar_tree.inventoryshare.nms.reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Reflection helpers shared by the inventory models.
 *
 * <p>Members are resolved and made accessible up front, so an access failure afterward is a bug and is thrown as
 * {@link IllegalStateException}. An unchecked exception thrown by the invoked method or constructor itself is rethrown
 * as is; a checked one is wrapped in {@link IllegalStateException}.
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

    /** The public method of {@code type} with one {@code parameter}, under the first of {@code names} it has */
    public static @NotNull Method anyMethod(@NotNull Class<?> type, @NotNull Class<?> parameter,
                                            @NotNull String... names) throws NoSuchMethodException {
        for (String name : names) {
            try {
                return type.getMethod(name, parameter);
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException(
                type.getName() + "." + String.join(" / ", names) + "(" + parameter.getName() + ")");
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

    public static Object newInstance(@NotNull Constructor<?> constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException e) {
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

    /**
     * Writes the fields in order. If one write fails, the fields already written get their previous values back
     * before the failure is rethrown, so the targets are either fully changed or left as they were. A failure while
     * restoring is added to the rethrown one as suppressed.
     */
    public static void setAll(@NotNull FieldWrite... writes) {
        Object[] previous = new Object[writes.length];
        for (int i = 0; i < writes.length; i++) {
            previous[i] = get(writes[i].field, writes[i].target);
        }
        int written = 0;
        try {
            for (; written < writes.length; written++) {
                set(writes[written].field, writes[written].target, writes[written].value);
            }
        } catch (RuntimeException | Error e) {
            for (int i = written - 1; i >= 0; i--) {
                try {
                    set(writes[i].field, writes[i].target, previous[i]);
                } catch (RuntimeException | Error restoreFailure) {
                    e.addSuppressed(restoreFailure);
                }
            }
            throw e;
        }
    }

    /** One field write for {@link #setAll(FieldWrite...)} */
    public static final class FieldWrite {
        private final Field field;
        private final Object target;
        private final Object value;

        private FieldWrite(Field field, Object target, Object value) {
            this.field = field;
            this.target = target;
            this.value = value;
        }

        public static @NotNull FieldWrite of(@NotNull Field field, @Nullable Object target, Object value) {
            return new FieldWrite(field, target, value);
        }
    }
}
