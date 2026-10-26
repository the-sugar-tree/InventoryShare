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

package com.sugar_tree.inventoryshare.nms;

import com.sugar_tree.inventoryshare.nms.reflection.NmsNameFactory;
import com.sugar_tree.inventoryshare.nms.reflection.item.ItemCodecMembers;
import com.sugar_tree.inventoryshare.nms.reflection.item.VanillaItemCodec;
import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the {@link ItemCodec} of a server. Before 1.20.5 items have no data components and there is none.
 */
public final class ItemCodecFactory {

    /** The first version with data components and {@code ItemStack.CODEC} */
    public static final String FIRST_VERSION = "1.20.5";

    private ItemCodecFactory() {}

    public static boolean supports(@NotNull MinecraftVersion version) {
        return version.isAtLeast(FIRST_VERSION);
    }

    /**
     * Resolves every member up front and converts an item both ways once, so a broken name fails here instead of
     * when an inventory is saved.
     *
     * @param dataVersion data version of the running server; older data is upgraded to it
     * @throws IllegalArgumentException    if the version is not {@link #supports(MinecraftVersion) supported}
     * @throws IncompatibleServerException if the codec cannot be created on this server
     */
    public static @NotNull ItemCodec create(@NotNull MinecraftVersion version, @NotNull ServerRuntime runtime,
                                            int dataVersion) throws IncompatibleServerException {
        if (!supports(version)) {
            throw new IllegalArgumentException("Minecraft " + version + " has no item codec");
        }
        try {
            return new VanillaItemCodec(ItemCodecMembers.resolve(NmsNameFactory.itemCodec(version, runtime),
                    runtime.getClassLoader()), dataVersion);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            throw new IncompatibleServerException("Failed to set up the item codec for Minecraft " + version
                    + " (" + runtime + ")", e);
        }
    }
}
