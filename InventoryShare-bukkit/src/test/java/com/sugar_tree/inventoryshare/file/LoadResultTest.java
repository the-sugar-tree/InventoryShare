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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadResultTest {

    private static final int SERVER = 3955;

    @Test
    void nothingSkippedIsComplete() {
        LoadResult result = new LoadResult(InventoryCodec.FORMAT_VERSION, SERVER, Collections.emptyList());
        assertTrue(result.isComplete());
        assertEquals(Collections.emptyList(), result.describe());
        assertEquals(-1, result.newestDataVersion());
    }

    @Test
    void newerFormatVersionIsIncomplete() {
        LoadResult result = new LoadResult(InventoryCodec.FORMAT_VERSION + 1, SERVER, Collections.emptyList());
        assertFalse(result.isComplete());
        assertEquals(1, result.describe().size());
    }

    @Test
    void newerItemsAreGroupedIntoOneLine() {
        List<SkippedItem> skipped = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) skipped.add(SkippedItem.newerVersion(slot, 4100));
        skipped.add(SkippedItem.newerVersion(38, 4189));
        skipped.add(SkippedItem.newerVersion(40, 4100));
        LoadResult result = new LoadResult(InventoryCodec.FORMAT_VERSION, SERVER, skipped);

        assertFalse(result.isComplete());
        assertEquals(4189, result.newestDataVersion());
        assertEquals(Collections.singletonList(
                "38 item(s) saved by a newer Minecraft version (data version up to 4189 > 3955): slots 0-35, 38, 40"),
                result.describe());
    }

    @Test
    void eachReasonGetsOneLine() {
        LoadResult result = new LoadResult(InventoryCodec.FORMAT_VERSION, SERVER, Arrays.asList(
                SkippedItem.invalidVersion(3, "abc"),
                SkippedItem.newerVersion(5, 4189),
                SkippedItem.deserializeFailed(12, new IllegalArgumentException("Unknown material"))));

        assertEquals(Arrays.asList(
                "1 item(s) saved by a newer Minecraft version (data version up to 4189 > 3955): slot 5",
                "1 item(s) with an invalid data version: slot 3 (\"abc\")",
                "1 item(s) failed to deserialize: slot 12 (java.lang.IllegalArgumentException: Unknown material)"),
                result.describe());
    }

    @Test
    void formatsSlotRanges() {
        assertEquals("slot 7", LoadResult.formatSlots(slots(7)));
        assertEquals("slots 0-2, 5", LoadResult.formatSlots(slots(0, 1, 2, 5)));
        assertEquals("slots 36-40", LoadResult.formatSlots(slots(36, 37, 38, 39, 40)));
    }

    private static List<SkippedItem> slots(int... slots) {
        List<SkippedItem> items = new ArrayList<>();
        for (int slot : slots) items.add(SkippedItem.newerVersion(slot, 4189));
        return items;
    }
}
