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

package com.sugar_tree.inventoryshare.file;

import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.InventoryStorage;
import com.sugar_tree.inventoryshare.nms.ItemCodec;
import com.sugar_tree.inventoryshare.file.LoadResult.SkippedItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.sugar_tree.inventoryshare.nms.InventoryAccess.*;

/**
 * Converts an {@link InventoryStorage} to and from the {@code items / armor / extraSlots} lists of a config.
 *
 * <p>Two item formats, told apart per item:
 * <ul>
 *   <li>Bukkit's {@code ItemStack#serialize()} ({@code v}, {@code type}, {@code meta}, ...): written before 1.20.5,
 *       as v3.1.1 wrote it everywhere</li>
 *   <li>Minecraft's own item data through {@link ItemCodec} ({@value ItemCodec#DATA_VERSION}, {@code id},
 *       {@code components}, ...): written from 1.20.5, and what Paper's {@code serialize()} writes from 1.21.5</li>
 * </ul>
 * A Bukkit item read on 1.20.5+ is migrated: it is saved in the new format the next time.
 */
final class Codec {

    /** {@code format-version} of a file whose items are written by Bukkit; a file without the key is this too */
    static final int BUKKIT_FORMAT = 1;
    /** {@code format-version} of a file whose items are written by {@link ItemCodec} */
    static final int ITEM_CODEC_FORMAT = 2;
    /** The newest {@code format-version} this plugin reads */
    static final int FORMAT_VERSION = ITEM_CODEC_FORMAT;

    private final InventoryAccess access;
    private final int serverDataVersion;
    /** {@code null} before 1.20.5 */
    private final @Nullable ItemCodec itemCodec;

    /**
     * @param serverDataVersion data version of the running server; items saved by a higher one are skipped
     * @param itemCodec         writes and reads the items from 1.20.5, {@code null} before
     */
    Codec(InventoryAccess access, int serverDataVersion, @Nullable ItemCodec itemCodec) {
        this.access = access;
        this.serverDataVersion = serverDataVersion;
        this.itemCodec = itemCodec;
    }

    void write(ConfigurationSection config, InventoryStorage inventory) {
        config.set("format-version", itemCodec == null ? BUKKIT_FORMAT : ITEM_CODEC_FORMAT);
        config.set("items", serialize(inventory, STORAGE_START, STORAGE_SIZE));
        config.set("armor", serialize(inventory, ARMOR_START, ARMOR_SIZE));
        config.set("extraSlots", serialize(inventory, OFFHAND_SLOT, 1));
    }

    /**
     * Loads every item that can be loaded into {@code inventory}; the slots of skipped items stay empty.
     *
     * @throws RuntimeException if {@link InventoryAccess#setItem} fails; such an item is not skipped
     */
    LoadResult read(ConfigurationSection config, InventoryStorage inventory) {
        List<SkippedItem> skipped = new ArrayList<>();
        int migrated = deserialize(config.getMapList("items"), inventory, STORAGE_START, STORAGE_SIZE, skipped)
                + deserialize(config.getMapList("armor"), inventory, ARMOR_START, ARMOR_SIZE, skipped)
                + deserialize(config.getMapList("extraSlots"), inventory, OFFHAND_SLOT, 1, skipped);
        return new LoadResult(config.getInt("format-version", BUKKIT_FORMAT), serverDataVersion, skipped, migrated);
    }

    private List<Map<?, ?>> serialize(InventoryStorage inventory, int firstSlot, int count) {
        List<Map<?, ?>> serialized = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack item = access.getItem(inventory, firstSlot + i);
            if (itemCodec == null) {
                serialized.add(item.serialize());
            } else if (item.getType() == Material.AIR) {
                // An empty slot is an empty map, which reading skips
                serialized.add(Collections.emptyMap());
            } else {
                serialized.add(itemCodec.encode(item));
            }
        }
        return serialized;
    }

    /**
     * @return how many Bukkit items were read while the item codec is available, i.e. migrated
     */
    @SuppressWarnings("unchecked")
    private int deserialize(List<Map<?, ?>> from, InventoryStorage to, int firstSlot, int count,
                            List<SkippedItem> skipped) {
        int migrated = 0;
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
            boolean itemCodecFormat = item.containsKey(ItemCodec.DATA_VERSION);
            ItemStack deserialized;
            try {
                if (itemCodecFormat) {
                    if (itemCodec == null) {
                        throw new IllegalArgumentException("Minecraft's item format needs 1.20.5 or later");
                    }
                    deserialized = itemCodec.decode(item);
                } else {
                    deserialized = ItemStack.deserialize((Map<String, Object>) item);
                }
            } catch (Exception e) {
                skipped.add(SkippedItem.deserializeFailed(slot, e));
                continue;
            }
            // Not caught: a failure here is the inventory access failing, not bad item data
            access.setItem(to, slot, deserialized);
            if (!itemCodecFormat && itemCodec != null) {
                migrated++;
            }
        }
        return migrated;
    }

    /**
     * Items saved by a newer Minecraft version (server downgrade) are rejected rather than deserialized:
     * Bukkit may silently drop data it does not understand, and a damaged item is harder to notice than a missing one.
     * The data version is {@code v} in Bukkit's format and {@value ItemCodec#DATA_VERSION} in Minecraft's.
     *
     * @return why the serialized item must not be loaded, or {@code null} if it may be
     */
    static SkippedItem checkVersion(Map<?, ?> item, int slot, int serverDataVersion) {
        Object version = item.containsKey(ItemCodec.DATA_VERSION) ? item.get(ItemCodec.DATA_VERSION) : item.get("v");
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
