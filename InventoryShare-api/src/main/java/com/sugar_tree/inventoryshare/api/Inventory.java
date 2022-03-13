/*
 * Copyright (c) 2021 the-sugar-tree
 *
 *  Licensed under the General Public License, Version 3.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/lgpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sugar_tree.inventoryshare.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public interface Inventory {

    void invApplyAll(@NotNull Player p);
    void invDisApply(@NotNull Player p);
    void invApply(@NotNull Player p);
    void savePlayerInventory(@NotNull Player p);

    /**
     * @param obj Object which you want to change field
     * @param name Field name
     * @param value Value to change field
     * @throws NoSuchFieldException if a field with the specified name is not found.
     * @throws IllegalAccessException if this Field object is enforcing Java language access control and the underlying field is inaccessible or final; or if this Field object has no write access.
     */
    static void setField(Object obj, String name, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(obj, value);
    }

}
