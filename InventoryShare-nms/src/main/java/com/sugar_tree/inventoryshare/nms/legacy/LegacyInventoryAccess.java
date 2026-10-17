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

package com.sugar_tree.inventoryshare.nms.legacy;

import com.google.common.collect.ImmutableList;
import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.NmsInventory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractList;

import static com.sugar_tree.inventoryshare.nms.Reflections.get;
import static com.sugar_tree.inventoryshare.nms.Reflections.invoke;
import static com.sugar_tree.inventoryshare.nms.Reflections.set;

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
    }

    @Override
    public @NotNull NmsInventory createEmpty() {
        return new LegacyInventory(createItemList(STORAGE_SIZE), createItemList(ARMOR_SIZE), createItemList(1));
    }

    @Override
    public @NotNull NmsInventory capture(@NotNull Player player) {
        Object inventory = playerInventory(player);
        return new LegacyInventory(
                (AbstractList<Object>) get(members.getItems(), inventory),
                (AbstractList<Object>) get(members.getArmor(), inventory),
                (AbstractList<Object>) get(members.getExtraSlots(), inventory));
    }

    @Override
    public void apply(@NotNull Player player, @NotNull NmsInventory inventory) {
        LegacyInventory lists = (LegacyInventory) inventory;
        Object target = playerInventory(player);
        set(members.getItems(), target, lists.items);
        set(members.getArmor(), target, lists.armor);
        set(members.getExtraSlots(), target, lists.extraSlots);
        set(members.getContents(), target, ImmutableList.of(lists.items, lists.armor, lists.extraSlots));
    }

    @Override
    public @NotNull ItemStack getItem(@NotNull NmsInventory inventory, int slot) {
        Object item = list((LegacyInventory) inventory, slot).get(index(slot));
        return (ItemStack) invoke(members.getAsBukkitCopy(), null, item);
    }

    @Override
    public void setItem(@NotNull NmsInventory inventory, int slot, @NotNull ItemStack item) {
        list((LegacyInventory) inventory, slot).set(index(slot), invoke(members.getAsNMSCopy(), null, item));
    }

    private static AbstractList<Object> list(LegacyInventory inventory, int slot) {
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

    private static final class LegacyInventory implements NmsInventory {
        private final AbstractList<Object> items;
        private final AbstractList<Object> armor;
        private final AbstractList<Object> extraSlots;

        private LegacyInventory(AbstractList<Object> items, AbstractList<Object> armor, AbstractList<Object> extraSlots) {
            this.items = items;
            this.armor = armor;
            this.extraSlots = extraSlots;
        }
    }
}
