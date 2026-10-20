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
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        LoadResult result = new Codec(new FailingAccess(), SERVER).read(config, new FailingAccess().createEmpty());
        assertEquals(1, result.getSkipped().size());
        assertEquals(SkippedItem.Reason.DESERIALIZE_FAILED, result.getSkipped().get(0).reason);
    }

    @Test
    void accessFailureIsThrownInsteadOfSkipped() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("items", Collections.singletonList(typedItem("STONE")));
        FailingAccess access = new FailingAccess();
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> new Codec(access, SERVER).read(config, access.createEmpty()));
        assertSame(FailingAccess.FAILURE, thrown);
    }

    /** A serialized item of the current format, with a data version the server can read */
    private static Map<String, Object> typedItem(String type) {
        Map<String, Object> item = new HashMap<>();
        item.put("v", SERVER);
        item.put("type", type);
        return item;
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

    private static Map<?, ?> item(Object version) {
        if (version == null) return Collections.singletonMap("type", "STONE");
        Map<String, Object> item = new HashMap<>();
        item.put("type", "STONE");
        item.put("v", version);
        return item;
    }
}
