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

package com.sugar_tree.inventoryshare.dev.v1_21_R4;

import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.NmsInventory;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R4.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.EnumMap;

/**
 * {@link InventoryAccess} for Minecraft 1.21.5, written against NMS directly.
 * Only the private final fields are reached by reflection.
 */
@SuppressWarnings("unchecked")
public final class DirectInventoryAccess implements InventoryAccess {

    /** {@code Inventory#items} */
    private static final Field ITEMS = accessible(PlayerInventory.class, "i");
    /** {@code Inventory#equipment} */
    private static final Field EQUIPMENT = accessible(PlayerInventory.class, "k");
    /** {@code EntityEquipment#items} */
    private static final Field EQUIPMENT_ITEMS = accessible(EntityEquipment.class, "b");

    @Override
    public @NotNull NmsInventory createEmpty() {
        return new Storage(NonNullList.a(STORAGE_SIZE, ItemStack.l), new EnumMap<>(EnumItemSlot.class));
    }

    @Override
    public @NotNull NmsInventory capture(@NotNull Player player) {
        PlayerInventory inventory = inventory(player);
        try {
            return new Storage((NonNullList<ItemStack>) ITEMS.get(inventory),
                    (EnumMap<EnumItemSlot, ItemStack>) EQUIPMENT_ITEMS.get(EQUIPMENT.get(inventory)));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void apply(@NotNull Player player, @NotNull NmsInventory nmsInventory) {
        Storage storage = (Storage) nmsInventory;
        PlayerInventory inventory = inventory(player);
        try {
            ITEMS.set(inventory, storage.items);
            EQUIPMENT_ITEMS.set(EQUIPMENT.get(inventory), storage.equipment);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public @NotNull org.bukkit.inventory.ItemStack getItem(@NotNull NmsInventory nmsInventory, int slot) {
        Storage storage = (Storage) nmsInventory;
        ItemStack item = slot < ARMOR_START
                ? storage.items.get(slot)
                : storage.equipment.getOrDefault(equipmentSlot(slot), ItemStack.l);
        return CraftItemStack.asBukkitCopy(item);
    }

    @Override
    public void setItem(@NotNull NmsInventory nmsInventory, int slot, @NotNull org.bukkit.inventory.ItemStack item) {
        Storage storage = (Storage) nmsInventory;
        ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        if (slot < ARMOR_START) {
            storage.items.set(slot, nmsItem);
        } else {
            storage.equipment.put(equipmentSlot(slot), nmsItem);
        }
    }

    private static EnumItemSlot equipmentSlot(int slot) {
        EnumItemSlot equipmentSlot = PlayerInventory.g.get(slot);
        if (equipmentSlot == null) throw new IndexOutOfBoundsException("slot " + slot);
        return equipmentSlot;
    }

    private static PlayerInventory inventory(Player player) {
        return ((CraftPlayer) player).getHandle().gj();
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

    private static final class Storage implements NmsInventory {
        private final NonNullList<ItemStack> items;
        private final EnumMap<EnumItemSlot, ItemStack> equipment;

        private Storage(NonNullList<ItemStack> items, EnumMap<EnumItemSlot, ItemStack> equipment) {
            this.items = items;
            this.equipment = equipment;
        }
    }
}
