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

package com.sugar_tree.inventoryshare.nms.reflection.inventory.modern;

import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.InventoryStorage;
import com.sugar_tree.inventoryshare.nms.reflection.Reflections.FieldWrite;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractList;
import java.util.EnumMap;
import java.util.Map;

import static com.sugar_tree.inventoryshare.nms.reflection.Reflections.get;
import static com.sugar_tree.inventoryshare.nms.reflection.Reflections.invoke;
import static com.sugar_tree.inventoryshare.nms.reflection.Reflections.newInstance;
import static com.sugar_tree.inventoryshare.nms.reflection.Reflections.setAll;

/**
 * {@link InventoryAccess} for 1.21.5+. Swaps the player's {@code Inventory#items} list and the
 * {@code EntityEquipment#items} map (armor and offhand) of the equipment the inventory belongs to.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class ModernInventoryAccess implements InventoryAccess {

    private final ModernMembers members;
    private final Object emptyItem;
    /** EquipmentSlot for slots 36 - 40, indexed by {@code slot - ARMOR_START} */
    private final Object[] equipmentSlots = new Object[SIZE - ARMOR_START];

    public ModernInventoryAccess(@NotNull ModernMembers members) {
        this.members = members;
        this.emptyItem = get(members.getEmptyItem(), null);
        Map<Integer, ?> slotMapping = (Map<Integer, ?>) get(members.getSlotMapping(), null);
        for (int slot = ARMOR_START; slot < SIZE; slot++) {
            Object equipmentSlot = slotMapping.get(slot);
            if (equipmentSlot == null) {
                throw new IllegalStateException("No equipment slot mapped to inventory slot " + slot);
            }
            equipmentSlots[slot - ARMOR_START] = equipmentSlot;
        }
        verify();
    }

    @Override
    public @NotNull InventoryStorage createEmpty() {
        AbstractList<Object> items = (AbstractList<Object>) invoke(members.getCreateItemList(), null, STORAGE_SIZE, emptyItem);
        Map<Object, Object> equipment = new EnumMap(members.getEquipmentSlot());
        return new ModernStorage(items, equipment);
    }

    @Override
    public @NotNull InventoryStorage capture(@NotNull Player player) {
        return captureFrom(playerInventory(player));
    }

    @Override
    public void apply(@NotNull Player player, @NotNull InventoryStorage inventory) {
        applyTo(playerInventory(player), inventory);
    }

    /**
     * Uses every member once, so a broken one fails while the plugin is starting instead of when an inventory is
     * loaded or a player joins: converts an item both ways in the storage, armor and offhand of an empty inventory,
     * then applies that inventory to a new inventory of no player (with its own new equipment) and captures it back.
     * Applying writes the same fields as {@link #apply}, so a server or JVM that refuses the writes fails here too.
     */
    private void verify() {
        InventoryStorage empty = createEmpty();
        for (int slot : new int[]{STORAGE_START, ARMOR_START, OFFHAND_SLOT}) {
            setItem(empty, slot, getItem(empty, slot));
        }
        Object detached = newInstance(members.getNewInventory(), null, newInstance(members.getNewEntityEquipment()));
        applyTo(detached, empty);
        captureFrom(detached);
    }

    private InventoryStorage captureFrom(Object inventory) {
        Object entityEquipment = get(members.getEquipment(), inventory);
        return new ModernStorage(
                (AbstractList<Object>) get(members.getItems(), inventory),
                (Map<Object, Object>) get(members.getEquipmentItems(), entityEquipment));
    }

    private void applyTo(Object target, InventoryStorage inventory) {
        ModernStorage storage = (ModernStorage) inventory;
        // The inventory's EntityEquipment is the player's own equipment, so this also changes what others see
        Object entityEquipment = get(members.getEquipment(), target);
        setAll(FieldWrite.of(members.getItems(), target, storage.items),
                FieldWrite.of(members.getEquipmentItems(), entityEquipment, storage.equipment));
    }

    @Override
    public @NotNull ItemStack getItem(@NotNull InventoryStorage inventory, int slot) {
        ModernStorage storage = (ModernStorage) inventory;
        Object item;
        if (slot < ARMOR_START) {
            item = storage.items.get(storageIndex(slot));
        } else {
            item = storage.equipment.get(equipmentSlot(slot));
            if (item == null) item = emptyItem;
        }
        // clone() copies the NMS item of a CraftItemStack
        return ((ItemStack) invoke(members.getCraftMirror(), null, item)).clone();
    }

    @Override
    public void setItem(@NotNull InventoryStorage inventory, int slot, @NotNull ItemStack item) {
        ModernStorage storage = (ModernStorage) inventory;
        Object nmsItem = invoke(members.getAsNMSCopy(), null, item);
        if (slot < ARMOR_START) {
            storage.items.set(storageIndex(slot), nmsItem);
        } else {
            storage.equipment.put(equipmentSlot(slot), nmsItem);
        }
    }

    private static int storageIndex(int slot) {
        if (slot < STORAGE_START) throw new IndexOutOfBoundsException("slot " + slot);
        return slot - STORAGE_START;
    }

    private Object equipmentSlot(int slot) {
        if (slot >= SIZE) throw new IndexOutOfBoundsException("slot " + slot);
        return equipmentSlots[slot - ARMOR_START];
    }

    private Object playerInventory(Player player) {
        Object handle = invoke(members.getGetHandle(), members.getCraftPlayer().cast(player));
        return invoke(members.getGetInventory(), handle);
    }

    private static final class ModernStorage implements InventoryStorage {
        private final AbstractList<Object> items;
        private final Map<Object, Object> equipment;

        private ModernStorage(AbstractList<Object> items, Map<Object, Object> equipment) {
            this.items = items;
            this.equipment = equipment;
        }
    }
}
