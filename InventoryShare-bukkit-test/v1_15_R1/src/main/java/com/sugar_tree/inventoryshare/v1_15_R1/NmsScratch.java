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

package com.sugar_tree.inventoryshare.v1_15_R1;

import net.minecraft.server.v1_15_R1.EntityPlayer;
import net.minecraft.server.v1_15_R1.NonNullList;
import net.minecraft.server.v1_15_R1.ItemStack;
import net.minecraft.server.v1_15_R1.PlayerInventory;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;

/**
 * Compile-time NMS name check for Minecraft 1.15–1.15.2 ({@code v1_15_R1}).
 * If this class fails to compile, update
 * {@code VersionUtil.SupportedVersions.v1_15_R1} with the new member names.
 */
@SuppressWarnings("unused")
public final class NmsScratch {

    public static final String INVENTORY = "inventory";
    public static final String ITEMS = "items";
    public static final String ARMOR = "armor";
    public static final String EXTRA_SLOTS = "extraSlots";
    public static final String CONTENTS = "f";
    public static final String EMPTY_ITEM = "a";

    private NmsScratch() {}

    public static PlayerInventory getInventory(Player player) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        return entityPlayer.inventory;
    }

    public static NonNullList<ItemStack> items(PlayerInventory inventory) {
        return inventory.items;
    }

    public static NonNullList<ItemStack> armor(PlayerInventory inventory) {
        return inventory.armor;
    }

    public static NonNullList<ItemStack> extraSlots(PlayerInventory inventory) {
        return inventory.extraSlots;
    }

    public static ItemStack emptyItem() {
        return ItemStack.a;
    }

    public static NonNullList<ItemStack> createItemList(int size) {
        return NonNullList.a(size, emptyItem());
    }

    public static org.bukkit.inventory.ItemStack asBukkitCopy(ItemStack nms) {
        return CraftItemStack.asBukkitCopy(nms);
    }

    public static ItemStack asNMSCopy(org.bukkit.inventory.ItemStack bukkit) {
        return CraftItemStack.asNMSCopy(bukkit);
    }
}
