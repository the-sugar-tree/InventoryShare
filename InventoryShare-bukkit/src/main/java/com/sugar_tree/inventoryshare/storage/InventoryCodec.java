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

package com.sugar_tree.inventoryshare.storage;

import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.NmsInventory;
import com.sugar_tree.inventoryshare.storage.LoadResult.SkippedItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.sugar_tree.inventoryshare.nms.InventoryAccess.*;

/**
 * Converts an {@link NmsInventory} to and from the {@code items / armor / extraSlots} lists of a config.
 */
final class InventoryCodec {

    /** Version of the inventory file layout */
    static final int FORMAT_VERSION = 1;

    private final InventoryAccess access;
    private final int serverDataVersion;

    /**
     * @param serverDataVersion data version of the running server; items saved by a higher one are skipped
     */
    InventoryCodec(InventoryAccess access, int serverDataVersion) {
        this.access = access;
        this.serverDataVersion = serverDataVersion;
    }

    void write(ConfigurationSection config, NmsInventory inventory) {
        config.set("format-version", FORMAT_VERSION);
        config.set("items", serialize(inventory, STORAGE_START, STORAGE_SIZE));
        config.set("armor", serialize(inventory, ARMOR_START, ARMOR_SIZE));
        config.set("extraSlots", serialize(inventory, OFFHAND_SLOT, 1));
    }

    /**
     * Loads every item that can be loaded into {@code inventory}; the slots of skipped items stay empty.
     */
    LoadResult read(ConfigurationSection config, NmsInventory inventory) {
        List<SkippedItem> skipped = new ArrayList<>();
        deserialize(config.getMapList("items"), inventory, STORAGE_START, STORAGE_SIZE, skipped);
        deserialize(config.getMapList("armor"), inventory, ARMOR_START, ARMOR_SIZE, skipped);
        deserialize(config.getMapList("extraSlots"), inventory, OFFHAND_SLOT, 1, skipped);
        return new LoadResult(config.getInt("format-version", FORMAT_VERSION), serverDataVersion, skipped);
    }

    private List<Map<?, ?>> serialize(NmsInventory inventory, int firstSlot, int count) {
        List<Map<?, ?>> serialized = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            serialized.add(access.getItem(inventory, firstSlot + i).serialize());
        }
        return serialized;
    }

    @SuppressWarnings("unchecked")
    private void deserialize(List<Map<?, ?>> from, NmsInventory to, int firstSlot, int count, List<SkippedItem> skipped) {
        for (int i = 0; i < Math.min(from.size(), count); i++) {
            Map<?, ?> item = from.get(i);
            if (item.isEmpty()) {
                continue;
            }
            int slot = firstSlot + i;
            SkippedItem rejected = checkVersion(item, slot, serverDataVersion);
            if (rejected != null) {
                skipped.add(rejected);
                continue;
            }
            try {
                access.setItem(to, slot, ItemStack.deserialize((Map<String, Object>) item));
            } catch (Exception e) {
                skipped.add(SkippedItem.deserializeFailed(slot, e));
            }
        }
    }

    /**
     * Items saved by a newer Minecraft version (server downgrade) are rejected rather than deserialized:
     * Bukkit may silently drop data it does not understand, and a damaged item is harder to notice than a missing one.
     *
     * @return why the serialized item must not be loaded, or {@code null} if it may be
     */
    static SkippedItem checkVersion(Map<?, ?> item, int slot, int serverDataVersion) {
        Object version = item.get("v");
        if (version == null) {
            return null;
        }
        int dataVersion;
        try {
            dataVersion = Integer.parseInt(version.toString());
        } catch (NumberFormatException e) {
            return SkippedItem.invalidVersion(slot, version);
        }
        return dataVersion > serverDataVersion ? SkippedItem.newerVersion(slot, dataVersion) : null;
    }
}
