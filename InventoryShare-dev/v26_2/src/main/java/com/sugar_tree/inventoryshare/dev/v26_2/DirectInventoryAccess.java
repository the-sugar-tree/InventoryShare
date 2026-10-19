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

package com.sugar_tree.inventoryshare.dev.v26_2;

import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.InventoryStorage;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.EnumMap;

/**
 * {@link InventoryAccess} for Minecraft 26.2, written against NMS directly.
 * Only the private final fields are reached by reflection.
 */
@SuppressWarnings("unchecked")
public final class DirectInventoryAccess implements InventoryAccess {

    private static final Field ITEMS = accessible(Inventory.class, "items");
    private static final Field EQUIPMENT = accessible(Inventory.class, "equipment");
    private static final Field EQUIPMENT_ITEMS = accessible(EntityEquipment.class, "items");

    @Override
    public @NotNull InventoryStorage createEmpty() {
        return new Storage(NonNullList.withSize(STORAGE_SIZE, ItemStack.EMPTY), new EnumMap<>(EquipmentSlot.class));
    }

    @Override
    public @NotNull InventoryStorage capture(@NotNull Player player) {
        Inventory inventory = inventory(player);
        try {
            return new Storage((NonNullList<ItemStack>) ITEMS.get(inventory),
                    (EnumMap<EquipmentSlot, ItemStack>) EQUIPMENT_ITEMS.get(EQUIPMENT.get(inventory)));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void apply(@NotNull Player player, @NotNull InventoryStorage nmsInventory) {
        Storage storage = (Storage) nmsInventory;
        Inventory inventory = inventory(player);
        try {
            ITEMS.set(inventory, storage.items);
            EQUIPMENT_ITEMS.set(EQUIPMENT.get(inventory), storage.equipment);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public @NotNull org.bukkit.inventory.ItemStack getItem(@NotNull InventoryStorage nmsInventory, int slot) {
        Storage storage = (Storage) nmsInventory;
        ItemStack item = slot < ARMOR_START
                ? storage.items.get(slot)
                : storage.equipment.getOrDefault(equipmentSlot(slot), ItemStack.EMPTY);
        return CraftItemStack.asBukkitCopy(item);
    }

    @Override
    public void setItem(@NotNull InventoryStorage nmsInventory, int slot, @NotNull org.bukkit.inventory.ItemStack item) {
        Storage storage = (Storage) nmsInventory;
        ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        if (slot < ARMOR_START) {
            storage.items.set(slot, nmsItem);
        } else {
            storage.equipment.put(equipmentSlot(slot), nmsItem);
        }
    }

    private static EquipmentSlot equipmentSlot(int slot) {
        EquipmentSlot equipmentSlot = Inventory.EQUIPMENT_SLOT_MAPPING.get(slot);
        if (equipmentSlot == null) throw new IndexOutOfBoundsException("slot " + slot);
        return equipmentSlot;
    }

    private static Inventory inventory(Player player) {
        return ((CraftPlayer) player).getHandle().getInventory();
    }

    private static Field accessible(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final class Storage implements InventoryStorage {
        private final NonNullList<ItemStack> items;
        private final EnumMap<EquipmentSlot, ItemStack> equipment;

        private Storage(NonNullList<ItemStack> items, EnumMap<EquipmentSlot, ItemStack> equipment) {
            this.items = items;
            this.equipment = equipment;
        }
    }
}
