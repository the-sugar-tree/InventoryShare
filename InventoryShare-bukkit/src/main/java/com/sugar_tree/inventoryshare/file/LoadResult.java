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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * What could not be loaded from one inventory file.
 */
final class LoadResult {

    private final int formatVersion;
    private final int serverDataVersion;
    /** In ascending slot order */
    private final List<SkippedItem> skipped;

    LoadResult(int formatVersion, int serverDataVersion, List<SkippedItem> skipped) {
        this.formatVersion = formatVersion;
        this.serverDataVersion = serverDataVersion;
        this.skipped = Collections.unmodifiableList(new ArrayList<>(skipped));
    }

    boolean isComplete() {
        return formatVersion <= Codec.FORMAT_VERSION && skipped.isEmpty();
    }

    List<SkippedItem> getSkipped() {
        return skipped;
    }

    /**
     * @return the highest data version among items saved by a newer Minecraft version, or {@code -1}
     */
    int newestDataVersion() {
        int newest = -1;
        for (SkippedItem item : skipped) {
            if (item.reason == SkippedItem.Reason.NEWER_VERSION) newest = Math.max(newest, item.dataVersion);
        }
        return newest;
    }

    /**
     * @return one line per problem kind, e.g.
     *         {@code 38 item(s) saved by a newer Minecraft version (data version up to 4189 > 3955): slots 0-35, 38, 40}
     */
    List<String> describe() {
        List<String> lines = new ArrayList<>();
        if (formatVersion > Codec.FORMAT_VERSION) {
            lines.add("format-version " + formatVersion + " is newer than this plugin supports ("
                    + Codec.FORMAT_VERSION + ")");
        }
        Map<SkippedItem.Reason, List<SkippedItem>> byReason = new EnumMap<>(SkippedItem.Reason.class);
        for (SkippedItem item : skipped) {
            byReason.computeIfAbsent(item.reason, reason -> new ArrayList<>()).add(item);
        }
        for (Map.Entry<SkippedItem.Reason, List<SkippedItem>> entry : byReason.entrySet()) {
            List<SkippedItem> items = entry.getValue();
            switch (entry.getKey()) {
                case NEWER_VERSION:
                    lines.add(items.size() + " item(s) saved by a newer Minecraft version (data version up to "
                            + newestDataVersion() + " > " + serverDataVersion + "): " + formatSlots(items));
                    break;
                case INVALID_VERSION:
                    lines.add(items.size() + " item(s) with an invalid data version: " + formatDetails(items));
                    break;
                case DESERIALIZE_FAILED:
                    lines.add(items.size() + " item(s) failed to deserialize: " + formatDetails(items));
                    break;
            }
        }
        return lines;
    }

    /**
     * @param items in ascending slot order
     * @return e.g. {@code slots 0-35, 38, 40}
     */
    static String formatSlots(List<SkippedItem> items) {
        List<String> ranges = new ArrayList<>();
        int i = 0;
        while (i < items.size()) {
            int start = items.get(i).slot;
            int end = start;
            while (i + 1 < items.size() && items.get(i + 1).slot == end + 1) {
                end = items.get(++i).slot;
            }
            ranges.add(start == end ? String.valueOf(start) : start + "-" + end);
            i++;
        }
        return (items.size() == 1 ? "slot " : "slots ") + String.join(", ", ranges);
    }

    /**
     * @return e.g. {@code slot 3 ("abc"), slot 12 (IllegalArgumentException: ...)}
     */
    static String formatDetails(List<SkippedItem> items) {
        List<String> details = new ArrayList<>();
        for (SkippedItem item : items) {
            details.add("slot " + item.slot + " (" + item.detail + ")");
        }
        return String.join(", ", details);
    }

    /** One item left out while loading; its slot stays empty. */
    static final class SkippedItem {
        enum Reason { NEWER_VERSION, INVALID_VERSION, DESERIALIZE_FAILED }

        final int slot;
        final Reason reason;
        /** Data version of the item, for {@link Reason#NEWER_VERSION}; otherwise -1 */
        final int dataVersion;
        /** The invalid version value, or the deserialization error; otherwise {@code null} */
        final String detail;

        private SkippedItem(int slot, Reason reason, int dataVersion, String detail) {
            this.slot = slot;
            this.reason = reason;
            this.dataVersion = dataVersion;
            this.detail = detail;
        }

        static SkippedItem newerVersion(int slot, int dataVersion) {
            return new SkippedItem(slot, Reason.NEWER_VERSION, dataVersion, null);
        }

        static SkippedItem invalidVersion(int slot, Object version) {
            return new SkippedItem(slot, Reason.INVALID_VERSION, -1, "\"" + version + "\"");
        }

        static SkippedItem deserializeFailed(int slot, Exception e) {
            return new SkippedItem(slot, Reason.DESERIALIZE_FAILED, -1, e.toString());
        }
    }
}
