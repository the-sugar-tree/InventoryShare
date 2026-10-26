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

import com.sugar_tree.inventoryshare.file.LoadResult.SkippedItem;
import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.InventoryStorage;
import com.sugar_tree.inventoryshare.nms.ItemCodec;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodecTest {

    private static final int SERVER = 3955;

    @Test
    void itemWithoutDataVersionIsLoaded() {
        assertNull(Codec.checkVersion(item(null), 0, SERVER));
    }

    @Test
    void itemFromSameOrOlderVersionIsLoaded() {
        assertNull(Codec.checkVersion(item(SERVER), 0, SERVER));
        assertNull(Codec.checkVersion(item(1343), 0, SERVER));
        assertNull(Codec.checkVersion(item("3955"), 0, SERVER));
    }

    @Test
    void itemFromNewerVersionIsSkipped() {
        SkippedItem skipped = Codec.checkVersion(item(4189), 38, SERVER);
        assertEquals(SkippedItem.Reason.NEWER_VERSION, skipped.reason);
        assertEquals(38, skipped.slot);
        assertEquals(4189, skipped.dataVersion);
    }

    @Test
    void newerItemInMinecraftsFormatIsSkipped() {
        // Saved by Paper 1.21.5+ or by this plugin on 1.20.5+, read after a downgrade
        SkippedItem skipped = Codec.checkVersion(codecItem(4325, "minecraft:stone"), 0, SERVER);
        assertEquals(SkippedItem.Reason.NEWER_VERSION, skipped.reason);
        assertEquals(4325, skipped.dataVersion);
    }

    @Test
    void itemWithInvalidVersionIsSkipped() {
        SkippedItem skipped = Codec.checkVersion(item("abc"), 3, SERVER);
        assertEquals(SkippedItem.Reason.INVALID_VERSION, skipped.reason);
        assertEquals("\"abc\"", skipped.detail);
    }

    @Test
    void unknownServerVersionSkipsVersionedItems() {
        // WORLD_VERSION is -1 when the server cannot report its data version
        assertEquals(SkippedItem.Reason.NEWER_VERSION, Codec.checkVersion(item(1343), 0, -1).reason);
    }

    @Test
    void itemThatCannotBeDeserializedIsSkipped() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("items", Collections.singletonList(typedItem("NOT_A_MATERIAL")));
        LoadResult result = new Codec(new FailingAccess(), SERVER, null).read(config, new FailingAccess().createEmpty());
        assertEquals(1, result.getSkipped().size());
        assertEquals(SkippedItem.Reason.DESERIALIZE_FAILED, result.getSkipped().get(0).reason);
    }

    @Test
    void accessFailureIsThrownInsteadOfSkipped() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("items", Collections.singletonList(typedItem("STONE")));
        FailingAccess access = new FailingAccess();
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> new Codec(access, SERVER, null).read(config, access.createEmpty()));
        assertSame(FailingAccess.FAILURE, thrown);
    }

    @Test
    void fileOfVersion311IsLoaded() throws InvalidConfigurationException {
        // As v3.1.1 saved it on 1.21.1: no format-version, every slot serialized by Bukkit, empty ones as AIR
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("items:\n"
                + "- v: 3955\n  type: STONE\n  amount: 3\n"
                + "- v: 3955\n  type: AIR\n  amount: 0\n"
                + "- v: 3955\n  type: DIAMOND_SWORD\n"
                + "armor:\n- v: 3955\n  type: AIR\n  amount: 0\n"
                + "extraSlots:\n- v: 3955\n  type: SHIELD\n");
        RecordingAccess access = new RecordingAccess();
        InventoryStorage inventory = access.createEmpty();

        LoadResult result = new Codec(access, SERVER, null).read(config, inventory);

        assertTrue(result.isComplete());
        assertEquals(0, result.getMigrated(), "nothing to migrate before 1.20.5");
        assertItem(Material.STONE, 3, access.items(inventory).get(0));
        assertEquals(Material.DIAMOND_SWORD, access.items(inventory).get(2).getType());
        assertEquals(Material.SHIELD, access.items(inventory).get(InventoryAccess.OFFHAND_SLOT).getType());
    }

    @Test
    void bukkitItemsAreMigratedWhenTheItemCodecIsAvailable() throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("items:\n"
                + "- v: 3955\n  type: STONE\n"
                + "- v: 3955\n  type: AIR\n  amount: 0\n"
                + "- DataVersion: 3955\n  id: minecraft:dirt\n  count: 2\n  schema_version: 1\n");
        RecordingAccess access = new RecordingAccess();
        InventoryStorage inventory = access.createEmpty();

        LoadResult result = new Codec(access, SERVER, new FakeItemCodec()).read(config, inventory);

        assertTrue(result.isComplete());
        assertEquals(2, result.getMigrated(), "the stone and the air are Bukkit items");
        assertItem(Material.DIRT, 2, access.items(inventory).get(2));
    }

    @Test
    void itemInMinecraftsFormatNeedsTheItemCodec() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("items", Collections.singletonList(codecItem(SERVER, "minecraft:stone")));
        RecordingAccess access = new RecordingAccess();

        LoadResult result = new Codec(access, SERVER, null).read(config, access.createEmpty());

        assertEquals(SkippedItem.Reason.DESERIALIZE_FAILED, result.getSkipped().get(0).reason);
    }

    @Test
    void itemTheItemCodecRejectsIsSkipped() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("items", Collections.singletonList(codecItem(SERVER, "minecraft:not_an_item")));
        RecordingAccess access = new RecordingAccess();

        LoadResult result = new Codec(access, SERVER, new FakeItemCodec()).read(config, access.createEmpty());

        assertEquals(1, result.getSkipped().size());
        assertEquals(SkippedItem.Reason.DESERIALIZE_FAILED, result.getSkipped().get(0).reason);
    }

    @Test
    void itemCodecWritesFormat2AndEmptySlotsAsEmptyMaps() {
        RecordingAccess access = new RecordingAccess();
        InventoryStorage inventory = access.createEmpty();
        access.setItem(inventory, 0, new ItemStack(Material.STONE, 5));
        MemoryConfiguration config = new MemoryConfiguration();

        new Codec(access, SERVER, new FakeItemCodec()).write(config, inventory);

        assertEquals(Codec.ITEM_CODEC_FORMAT, config.getInt("format-version"));
        List<Map<?, ?>> items = config.getMapList("items");
        assertEquals(InventoryAccess.STORAGE_SIZE, items.size());
        assertEquals("minecraft:stone", items.get(0).get("id"));
        assertEquals(Collections.emptyMap(), items.get(1));
        assertEquals(InventoryAccess.ARMOR_SIZE, config.getMapList("armor").size());
        assertEquals(1, config.getMapList("extraSlots").size());
    }

    @Test
    void writtenItemsAreReadBack() {
        RecordingAccess access = new RecordingAccess();
        InventoryStorage inventory = access.createEmpty();
        access.setItem(inventory, 3, new ItemStack(Material.STONE, 5));
        access.setItem(inventory, InventoryAccess.ARMOR_START, new ItemStack(Material.IRON_BOOTS));
        Codec codec = new Codec(access, SERVER, new FakeItemCodec());
        MemoryConfiguration config = new MemoryConfiguration();
        codec.write(config, inventory);

        InventoryStorage read = access.createEmpty();
        LoadResult result = codec.read(config, read);

        assertTrue(result.isComplete());
        assertEquals(0, result.getMigrated());
        assertItem(Material.STONE, 5, access.items(read).get(3));
        assertItem(Material.IRON_BOOTS, 1, access.items(read).get(InventoryAccess.ARMOR_START));
    }

    /** {@code ItemStack#equals} needs a running server */
    private static void assertItem(Material type, int amount, ItemStack item) {
        assertEquals(type, item.getType());
        assertEquals(amount, item.getAmount());
    }

    /** A serialized item of the current format, with a data version the server can read */
    private static Map<String, Object> typedItem(String type) {
        Map<String, Object> item = new HashMap<>();
        item.put("v", SERVER);
        item.put("type", type);
        return item;
    }

    private static Map<String, Object> codecItem(int dataVersion, String id) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put(ItemCodec.DATA_VERSION, dataVersion);
        item.put("id", id);
        return item;
    }

    private static Map<?, ?> item(Object version) {
        if (version == null) return Collections.singletonMap("type", "STONE");
        Map<String, Object> item = new HashMap<>();
        item.put("type", "STONE");
        item.put("v", version);
        return item;
    }

    /** Writes {@code id} and {@code count} only; ids are {@code minecraft:} + the material name in lower case */
    private static final class FakeItemCodec implements ItemCodec {
        @Override
        public Map<String, Object> encode(ItemStack item) {
            Map<String, Object> data = codecItem(SERVER, "minecraft:" + item.getType().name().toLowerCase());
            data.put("count", item.getAmount());
            return data;
        }

        @Override
        public ItemStack decode(Map<?, ?> data) {
            String id = String.valueOf(data.get("id"));
            Material material = Material.getMaterial(id.substring("minecraft:".length()).toUpperCase());
            if (material == null) throw new IllegalArgumentException("Unknown item " + id);
            Object count = data.get("count");
            return new ItemStack(material, count == null ? 1 : ((Number) count).intValue());
        }
    }

    /** Keeps the items of each storage in a map from slot to item; an unset slot is AIR */
    private static final class RecordingAccess implements InventoryAccess {
        private static final class Storage implements InventoryStorage {
            final Map<Integer, ItemStack> items = new HashMap<>();
        }

        Map<Integer, ItemStack> items(InventoryStorage inventory) {
            return ((Storage) inventory).items;
        }

        @Override
        public InventoryStorage createEmpty() {
            return new Storage();
        }

        @Override
        public InventoryStorage capture(Player player) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void apply(Player player, InventoryStorage inventory) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ItemStack getItem(InventoryStorage inventory, int slot) {
            ItemStack item = items(inventory).get(slot);
            return item == null ? new ItemStack(Material.AIR) : item.clone();
        }

        @Override
        public void setItem(InventoryStorage inventory, int slot, ItemStack item) {
            items(inventory).put(slot, item.clone());
        }
    }

    /** Creates storages, but fails every item write */
    private static final class FailingAccess implements InventoryAccess {
        static final IllegalStateException FAILURE = new IllegalStateException("access failed");

        @Override
        public InventoryStorage createEmpty() {
            return new InventoryStorage() {};
        }

        @Override
        public InventoryStorage capture(Player player) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void apply(Player player, InventoryStorage inventory) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ItemStack getItem(InventoryStorage inventory, int slot) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setItem(InventoryStorage inventory, int slot, ItemStack item) {
            throw FAILURE;
        }
    }
}
