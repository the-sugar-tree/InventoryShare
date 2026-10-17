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

package com.sugar_tree.inventoryshare.file;

import com.sugar_tree.inventoryshare.file.LoadResult.SkippedItem;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InventoryCodecTest {

    private static final int SERVER = 3955;

    @Test
    void itemWithoutDataVersionIsLoaded() {
        assertNull(InventoryCodec.checkVersion(item(null), 0, SERVER));
    }

    @Test
    void itemFromSameOrOlderVersionIsLoaded() {
        assertNull(InventoryCodec.checkVersion(item(SERVER), 0, SERVER));
        assertNull(InventoryCodec.checkVersion(item(1343), 0, SERVER));
        assertNull(InventoryCodec.checkVersion(item("3955"), 0, SERVER));
    }

    @Test
    void itemFromNewerVersionIsSkipped() {
        SkippedItem skipped = InventoryCodec.checkVersion(item(4189), 38, SERVER);
        assertEquals(SkippedItem.Reason.NEWER_VERSION, skipped.reason);
        assertEquals(38, skipped.slot);
        assertEquals(4189, skipped.dataVersion);
    }

    @Test
    void itemWithInvalidVersionIsSkipped() {
        SkippedItem skipped = InventoryCodec.checkVersion(item("abc"), 3, SERVER);
        assertEquals(SkippedItem.Reason.INVALID_VERSION, skipped.reason);
        assertEquals("\"abc\"", skipped.detail);
    }

    @Test
    void unknownServerVersionSkipsVersionedItems() {
        // WORLD_VERSION is -1 when the server cannot report its data version
        assertEquals(SkippedItem.Reason.NEWER_VERSION, InventoryCodec.checkVersion(item(1343), 0, -1).reason);
    }

    private static Map<?, ?> item(Object version) {
        if (version == null) return Collections.singletonMap("type", "STONE");
        Map<String, Object> item = new HashMap<>();
        item.put("type", "STONE");
        item.put("v", version);
        return item;
    }
}
