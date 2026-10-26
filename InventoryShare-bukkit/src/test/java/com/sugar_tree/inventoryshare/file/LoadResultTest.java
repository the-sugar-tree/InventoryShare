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
        LoadResult result = new LoadResult(Codec.FORMAT_VERSION, SERVER, Collections.emptyList(), Collections.emptyList());
        assertTrue(result.isComplete());
        assertEquals(Collections.emptyList(), result.describe());
        assertEquals(-1, result.newestDataVersion());
    }

    @Test
    void newerFormatVersionIsIncomplete() {
        LoadResult result = new LoadResult(Codec.FORMAT_VERSION + 1, SERVER, Collections.emptyList(), Collections.emptyList());
        assertFalse(result.isComplete());
        assertEquals(1, result.describe().size());
    }

    @Test
    void newerItemsAreGroupedIntoOneLine() {
        List<SkippedItem> skipped = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) skipped.add(SkippedItem.newerVersion(slot, 4100));
        skipped.add(SkippedItem.newerVersion(38, 4189));
        skipped.add(SkippedItem.newerVersion(40, 4100));
        LoadResult result = new LoadResult(Codec.FORMAT_VERSION, SERVER, skipped, Collections.emptyList());

        assertFalse(result.isComplete());
        assertEquals(4189, result.newestDataVersion());
        assertEquals(Collections.singletonList(
                "  - 38 item(s) saved by a newer Minecraft version (data version up to 4189 > 3955): slots 0-35, 38, 40"),
                result.describe());
    }

    @Test
    void eachReasonGetsItsOwnLines() {
        LoadResult result = new LoadResult(Codec.FORMAT_VERSION, SERVER, Arrays.asList(
                SkippedItem.invalidVersion(3, "abc"),
                SkippedItem.newerVersion(5, 4189),
                SkippedItem.deserializeFailed(12, new IllegalArgumentException("Unknown material"))), Collections.emptyList());

        assertEquals(Arrays.asList(
                "  - 1 item(s) saved by a newer Minecraft version (data version up to 4189 > 3955): slot 5",
                "  - 1 item(s) with an invalid data version:",
                "      \"abc\": slot 3",
                "  - 1 item(s) failed to deserialize:",
                "      java.lang.IllegalArgumentException: Unknown material: slot 12"),
                result.describe());
    }

    @Test
    void sameErrorIsGroupedIntoOneLine() {
        List<SkippedItem> skipped = new ArrayList<>();
        for (int slot = 0; slot <= 40; slot++) {
            skipped.add(SkippedItem.deserializeFailed(slot, new IllegalArgumentException("Material cannot be null")));
        }
        LoadResult result = new LoadResult(Codec.FORMAT_VERSION, SERVER, skipped, Collections.emptyList());

        assertEquals(Arrays.asList(
                "  - 41 item(s) failed to deserialize:",
                "      java.lang.IllegalArgumentException: Material cannot be null: slots 0-40"),
                result.describe());
    }

    @Test
    void differentErrorsGetOneLineEachInOrderOfFirstAppearance() {
        IllegalArgumentException noMaterial = new IllegalArgumentException("Material cannot be null");
        LoadResult result = new LoadResult(Codec.FORMAT_VERSION, SERVER, Arrays.asList(
                SkippedItem.deserializeFailed(0, noMaterial),
                SkippedItem.deserializeFailed(1, noMaterial),
                SkippedItem.deserializeFailed(12, new IllegalStateException("Broken meta")),
                SkippedItem.deserializeFailed(36, noMaterial)), Collections.emptyList());

        assertEquals(Arrays.asList(
                "  - 4 item(s) failed to deserialize:",
                "      java.lang.IllegalArgumentException: Material cannot be null: slots 0-1, 36",
                "      java.lang.IllegalStateException: Broken meta: slot 12"),
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

    @Test
    void migrationFromTheSameVersionIsNotFromAnOlderOne() {
        LoadResult result = new LoadResult(Codec.FORMAT_VERSION, SERVER, Collections.emptyList(), Arrays.asList(SERVER, SERVER));
        assertEquals(2, result.getMigrated());
        assertEquals(SERVER, result.oldestMigratedDataVersion());
        assertFalse(result.isMigratedFromOlderVersion());
    }

    @Test
    void migrationFromAnOlderVersion() {
        // 0: an item saved before 1.13, without a data version
        LoadResult result = new LoadResult(Codec.FORMAT_VERSION, SERVER, Collections.emptyList(), Arrays.asList(SERVER, 0));
        assertEquals(0, result.oldestMigratedDataVersion());
        assertTrue(result.isMigratedFromOlderVersion());
    }

    @Test
    void noMigration() {
        LoadResult result = new LoadResult(Codec.FORMAT_VERSION, SERVER, Collections.emptyList(), Collections.emptyList());
        assertEquals(0, result.getMigrated());
        assertEquals(-1, result.oldestMigratedDataVersion());
        assertFalse(result.isMigratedFromOlderVersion());
    }
}
