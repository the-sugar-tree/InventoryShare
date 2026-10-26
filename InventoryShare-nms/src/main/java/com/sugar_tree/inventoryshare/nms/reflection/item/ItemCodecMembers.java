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

package com.sugar_tree.inventoryshare.nms.reflection.item;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static com.sugar_tree.inventoryshare.nms.reflection.Reflections.load;

/**
 * Reflective members for {@link ItemCodecNames}. Resolving them loads no class of DataFixerUpper into this plugin's
 * code, so it also runs in tests against an isolated server jar.
 */
@Getter
public final class ItemCodecMembers {

    /** DataFixerUpper's {@code DynamicOps}, the parameter of {@code createSerializationContext} */
    static final String DYNAMIC_OPS_CLASS = "com.mojang.serialization.DynamicOps";

    private final Class<?> itemStack;
    private final Field codec;
    private final Field nbtOps;
    private final Method getMinecraftRegistry;
    private final Method createSerializationContext;
    private final Method parseTag;
    private final Method getDataFixer;
    private final Method asNMSCopy;
    private final Method asCraftMirror;

    private ItemCodecMembers(ItemCodecNames names, ClassLoader loader) throws ReflectiveOperationException {
        itemStack = load(names.getItemStackClass(), loader);
        codec = itemStack.getField(names.getCodec());
        nbtOps = load(names.getNbtOpsClass(), loader).getField(names.getNbtOps());
        getMinecraftRegistry = load(names.getCraftRegistryClass(), loader).getMethod("getMinecraftRegistry");
        createSerializationContext = load(names.getRegistryProviderClass(), loader)
                .getMethod(names.getCreateSerializationContext(), load(DYNAMIC_OPS_CLASS, loader));
        parseTag = load(names.getTagParserClass(), loader).getMethod(names.getParseTag(), String.class);
        getDataFixer = load(names.getDataFixersClass(), loader).getMethod(names.getGetDataFixer());

        Class<?> craftItemStack = load(names.getCraftItemStackClass(), loader);
        asNMSCopy = craftItemStack.getMethod("asNMSCopy", load(names.getBukkitItemStackClass(), loader));
        asCraftMirror = craftItemStack.getMethod("asCraftMirror", itemStack);
    }

    public static @NotNull ItemCodecMembers resolve(@NotNull ItemCodecNames names, @NotNull ClassLoader loader)
            throws ReflectiveOperationException {
        return new ItemCodecMembers(names, loader);
    }
}
