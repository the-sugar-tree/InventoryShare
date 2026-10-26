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

package com.sugar_tree.inventoryshare.nms.reflection.legacy;

import com.google.common.collect.ImmutableList;
import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.InventoryStorage;
import com.sugar_tree.inventoryshare.nms.reflection.Reflections.FieldWrite;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractList;

import static com.sugar_tree.inventoryshare.nms.reflection.Reflections.get;
import static com.sugar_tree.inventoryshare.nms.reflection.Reflections.invoke;
import static com.sugar_tree.inventoryshare.nms.reflection.Reflections.newInstance;
import static com.sugar_tree.inventoryshare.nms.reflection.Reflections.setAll;

/**
 * {@link InventoryAccess} for 1.12 - 1.21.4. Swaps the {@code items}, {@code armor}, {@code extraSlots}
 * lists of the player's NMS inventory, and rebuilds {@code contents} from them.
 */
@SuppressWarnings("unchecked")
public final class LegacyInventoryAccess implements InventoryAccess {

    private final LegacyMembers members;
    private final Object emptyItem;

    public LegacyInventoryAccess(@NotNull LegacyMembers members) {
        this.members = members;
        this.emptyItem = get(members.getEmptyItem(), null);
        verify();
    }

    @Override
    public @NotNull InventoryStorage createEmpty() {
        return new LegacyStorage(createItemList(STORAGE_SIZE), createItemList(ARMOR_SIZE), createItemList(1));
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
     * loaded or a player joins: converts an item both ways in each list of an empty inventory, then applies that
     * inventory to a new player inventory of no player and captures it back. Applying writes the same fields as
     * {@link #apply}, so a server or JVM that refuses the writes fails here too.
     */
    private void verify() {
        InventoryStorage empty = createEmpty();
        for (int slot : new int[]{STORAGE_START, ARMOR_START, OFFHAND_SLOT}) {
            setItem(empty, slot, getItem(empty, slot));
        }
        Object detached = newInstance(members.getNewPlayerInventory(), (Object) null);
        applyTo(detached, empty);
        captureFrom(detached);
    }

    private InventoryStorage captureFrom(Object inventory) {
        return new LegacyStorage(
                (AbstractList<Object>) get(members.getItems(), inventory),
                (AbstractList<Object>) get(members.getArmor(), inventory),
                (AbstractList<Object>) get(members.getExtraSlots(), inventory));
    }

    private void applyTo(Object target, InventoryStorage inventory) {
        LegacyStorage lists = (LegacyStorage) inventory;
        setAll(FieldWrite.of(members.getItems(), target, lists.items),
                FieldWrite.of(members.getArmor(), target, lists.armor),
                FieldWrite.of(members.getExtraSlots(), target, lists.extraSlots),
                FieldWrite.of(members.getContents(), target, ImmutableList.of(lists.items, lists.armor, lists.extraSlots)));
    }

    @Override
    public @NotNull ItemStack getItem(@NotNull InventoryStorage inventory, int slot) {
        Object item = list((LegacyStorage) inventory, slot).get(index(slot));
        return (ItemStack) invoke(members.getAsCraftCopy(), null, invoke(members.getAsCraftMirror(), null, item));
    }

    @Override
    public void setItem(@NotNull InventoryStorage inventory, int slot, @NotNull ItemStack item) {
        list((LegacyStorage) inventory, slot).set(index(slot), invoke(members.getAsNMSCopy(), null, item));
    }

    private static AbstractList<Object> list(LegacyStorage inventory, int slot) {
        if (slot < 0 || slot >= SIZE) throw new IndexOutOfBoundsException("slot " + slot);
        if (slot < ARMOR_START) return inventory.items;
        if (slot < OFFHAND_SLOT) return inventory.armor;
        return inventory.extraSlots;
    }

    private static int index(int slot) {
        if (slot < ARMOR_START) return slot - STORAGE_START;
        if (slot < OFFHAND_SLOT) return slot - ARMOR_START;
        return slot - OFFHAND_SLOT;
    }

    private Object playerInventory(Player player) {
        Object handle = invoke(members.getGetHandle(), members.getCraftPlayer().cast(player));
        if (members.getInventory() instanceof Field) {
            return get((Field) members.getInventory(), handle);
        }
        return invoke((Method) members.getInventory(), handle);
    }

    private AbstractList<Object> createItemList(int size) {
        return (AbstractList<Object>) invoke(members.getCreateItemList(), null, size, emptyItem);
    }

    private static final class LegacyStorage implements InventoryStorage {
        private final AbstractList<Object> items;
        private final AbstractList<Object> armor;
        private final AbstractList<Object> extraSlots;

        private LegacyStorage(AbstractList<Object> items, AbstractList<Object> armor, AbstractList<Object> extraSlots) {
            this.items = items;
            this.armor = armor;
            this.extraSlots = extraSlots;
        }
    }
}
