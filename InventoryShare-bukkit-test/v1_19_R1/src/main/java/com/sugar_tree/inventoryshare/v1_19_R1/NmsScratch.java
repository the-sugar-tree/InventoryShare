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

package com.sugar_tree.inventoryshare.v1_19_R1;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;

/**
 * Compile-time NMS name check for Minecraft 1.19 ({@code v1_19_R1}).
 * If this class fails to compile, update
 * {@code VersionUtil.SupportedVersions.v1_19_R1} with the new member names.
 */
@SuppressWarnings("unused")
public final class NmsScratch {

    public static final String INVENTORY = "fB";
    public static final String ITEMS = "h";
    public static final String ARMOR = "i";
    public static final String EXTRA_SLOTS = "j";
    public static final String CONTENTS = "n";
    public static final String EMPTY_ITEM = "b";

    private NmsScratch() {}

    public static PlayerInventory getInventory(Player player) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        return entityPlayer.fB();
    }

    public static NonNullList<ItemStack> items(PlayerInventory inventory) {
        return inventory.h;
    }

    public static NonNullList<ItemStack> armor(PlayerInventory inventory) {
        return inventory.i;
    }

    public static NonNullList<ItemStack> extraSlots(PlayerInventory inventory) {
        return inventory.j;
    }

    public static ItemStack emptyItem() {
        return ItemStack.b;
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
