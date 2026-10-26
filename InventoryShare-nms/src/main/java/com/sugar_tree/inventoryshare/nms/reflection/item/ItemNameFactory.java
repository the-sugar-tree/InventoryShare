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

import com.sugar_tree.inventoryshare.nms.server.IncompatibleServerException;
import com.sugar_tree.inventoryshare.nms.server.MappingType;
import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import org.jetbrains.annotations.NotNull;

import static com.sugar_tree.inventoryshare.nms.reflection.NmsNames.*;

/**
 * Builds the names of the item codec's classes and members for one server. Only strings are built here; nothing is
 * loaded. Spigot's member names come from {@link ItemCodecNameTable}.
 *
 * @see com.sugar_tree.inventoryshare.nms.reflection.NmsNames
 */
public final class ItemNameFactory {

    /** Mojang renamed {@code TagParser.parseTag} to {@code parseCompoundFully} in this version */
    private static final String PARSE_COMPOUND_FULLY_SINCE = "1.21.5";

    private ItemNameFactory() {}

    /**
     * @throws IllegalArgumentException    if the version has no item codec (before 1.20.5)
     * @throws IncompatibleServerException if the server's names are unknown for it (Spigot's names from 26.1)
     */
    public static @NotNull ItemCodecNames itemCodec(@NotNull MinecraftVersion version, @NotNull ServerRuntime runtime)
            throws IncompatibleServerException {
        ItemCodecNameTable row = ItemCodecNameTable.find(version);
        if (row == null) {
            throw new IllegalArgumentException("Minecraft " + version + " has no item codec");
        }
        String pkg = runtime.getCraftbukkitPackage();
        ItemCodecNames.ItemCodecNamesBuilder names = ItemCodecNames.builder()
                .craftItemStackClass(craftbukkit(pkg, "inventory.CraftItemStack"))
                .bukkitItemStackClass(BUKKIT_ITEM_STACK)
                .craftRegistryClass(craftbukkit(pkg, "CraftRegistry"))
                .itemStackClass(ITEM_STACK)
                .nbtOpsClass(byMappingType(runtime, "net.minecraft.nbt.DynamicOpsNBT", "net.minecraft.nbt.NbtOps"))
                .registryProviderClass(byMappingType(runtime,
                        "net.minecraft.core.HolderLookup$a", "net.minecraft.core.HolderLookup$Provider"))
                .tagParserClass(byMappingType(runtime, "net.minecraft.nbt.MojangsonParser", "net.minecraft.nbt.TagParser"))
                .dataFixersClass(byMappingType(runtime,
                        "net.minecraft.util.datafix.DataConverterRegistry", "net.minecraft.util.datafix.DataFixers"));

        if (runtime.getMappingType() == MappingType.MOJANG) {
            names.codec("CODEC")
                    .nbtOps("INSTANCE")
                    .createSerializationContext("createSerializationContext")
                    .parseTag(version.isOlderThan(PARSE_COMPOUND_FULLY_SINCE) ? "parseTag" : "parseCompoundFully")
                    .getDataFixer("getDataFixer");
        } else {
            if (!row.hasSpigotNames()) {
                throw new IncompatibleServerException("No Spigot names for Minecraft " + version);
            }
            names.codec(row.codec)
                    .nbtOps(ItemCodecNameTable.OTHER_MEMBERS)
                    .createSerializationContext(ItemCodecNameTable.OTHER_MEMBERS)
                    .parseTag(ItemCodecNameTable.OTHER_MEMBERS)
                    .getDataFixer(ItemCodecNameTable.OTHER_MEMBERS);
        }
        return names.build();
    }
}
