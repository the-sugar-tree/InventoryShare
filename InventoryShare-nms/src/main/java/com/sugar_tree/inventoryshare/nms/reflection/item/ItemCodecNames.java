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

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Every class and member name the item codec (1.20.5+) needs on one server. Class names are fully qualified.
 *
 * @see ItemNameFactory
 */
@Getter
@Builder
public final class ItemCodecNames {

    // CraftBukkit and Bukkit
    private final @NotNull String craftItemStackClass;
    private final @NotNull String bukkitItemStackClass;
    /** {@code CraftRegistry}, whose static {@code getMinecraftRegistry()} returns the server's registries */
    private final @NotNull String craftRegistryClass;

    // ItemStack
    private final @NotNull String itemStackClass;
    /** static {@code Codec<ItemStack>} of a non-empty item */
    private final @NotNull String codec;

    // NBT
    private final @NotNull String nbtOpsClass;
    /** static {@code NbtOps} instance */
    private final @NotNull String nbtOps;
    /** {@code HolderLookup.Provider}, the type of the server's registries */
    private final @NotNull String registryProviderClass;
    /** {@code RegistryOps createSerializationContext(DynamicOps)}: ops that can look up registry entries */
    private final @NotNull String createSerializationContext;
    private final @NotNull String tagParserClass;
    /** static {@code CompoundTag parse(String)}: parses SNBT */
    private final @NotNull String parseTag;

    // Data fixer
    private final @NotNull String dataFixersClass;
    /** static {@code DataFixer getDataFixer()} */
    private final @NotNull String getDataFixer;
}
