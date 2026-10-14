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

package com.sugar_tree.inventoryshare.nms.modern;

import lombok.Getter;

/**
 * NMS class names of the modern inventory model, which differ between Spigot's mappings (1.21.5 - 1.21.11)
 * and Mojang's unobfuscated names (26.1+).
 */
@Getter
public enum Namespace {
    SPIGOT("net.minecraft.world.entity.player.PlayerInventory",
            "net.minecraft.world.entity.player.EntityHuman",
            "net.minecraft.world.entity.EnumItemSlot"),
    MOJANG("net.minecraft.world.entity.player.Inventory",
            "net.minecraft.world.entity.player.Player",
            "net.minecraft.world.entity.EquipmentSlot"),
    ;

    /** Same in both namespaces */
    public static final String ENTITY_EQUIPMENT = "net.minecraft.world.entity.EntityEquipment";
    public static final String ITEM_STACK = "net.minecraft.world.item.ItemStack";
    public static final String NON_NULL_LIST = "net.minecraft.core.NonNullList";

    private final String inventoryClass;
    /** Class declaring the {@code getInventory} method */
    private final String playerClass;
    private final String equipmentSlotClass;

    Namespace(String inventoryClass, String playerClass, String equipmentSlotClass) {
        this.inventoryClass = inventoryClass;
        this.playerClass = playerClass;
        this.equipmentSlotClass = equipmentSlotClass;
    }
}
