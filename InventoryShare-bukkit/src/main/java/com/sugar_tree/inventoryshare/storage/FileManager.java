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

import com.sugar_tree.inventoryshare.interfaces.IFileManager;
import com.sugar_tree.inventoryshare.inventory.InventoryManager;
import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.NmsInventory;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.sugar_tree.inventoryshare.SharedConstants.*;
import static com.sugar_tree.inventoryshare.nms.InventoryAccess.*;

public final class FileManager implements IFileManager {

    /**
     * Version of the inventory file layout ({@code items} / {@code armor} / {@code extraSlots}).
     */
    private static final int FORMAT_VERSION = 1;

    private final InventoryManager inventories;
    private final InventoryAccess access;

    public FileManager(InventoryManager inventories, InventoryAccess access) {
        this.inventories = inventories;
        this.access = access;
    }

    @Override
    public void save() {
        writeInventory(invconfig, inventories.getSharedInventory());

        List<String> serializedAdvancementList = new ArrayList<>();
        for (NamespacedKey namespacedKey : advlist) {
            serializedAdvancementList.add(namespacedKey.getKey());
        }
        advconfig.set("advancement", serializedAdvancementList);

        plugin.getConfig().set("inventory", inventory);
        plugin.getConfig().set("advancement", advancement);
        plugin.getConfig().set("announcedeath", announcedeath);
        plugin.getConfig().set("teaminventory", teaminventory);
        StringBuilder sb = new StringBuilder();
        boolean savedLeastOneTeam = false;
        sb.append(I18N_TEAM_SAVED);
        for (Team team : Bukkit.getServer().getScoreboardManager().getMainScoreboard().getTeams()) {
            if (team == null) continue;
            File file = new File(new File(plugin.getDataFolder(), "teams"), team.getName() + ".yml");
            FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
            NmsInventory teamInventory = inventories.getTeamInventory(team.getName());
            if (teamInventory == null) continue;
            writeInventory(fileConfiguration, teamInventory);
            teamInvFileList.put(fileConfiguration, file);
            sb.append("[").append(team.getName()).append("] ");
            savedLeastOneTeam = true;
        }
        if (savedLeastOneTeam) logger.info(sb.toString());
        saveConfigs();
    }

    @Override
    public void load() {
        readInventory(invconfig, invfile, "inventory", inventories.getSharedInventory(), "ALL");

        List<String> advancementlist = advconfig.getStringList("advancement");
        for (String advancement : advancementlist) {
            try {
                advlist.add(plugin.getServer().getAdvancement(NamespacedKey.minecraft(advancement)).getKey());
            } catch (NullPointerException ignored) {
            }
        }

        if (plugin.getConfig().contains("inventory")) {
            inventory = plugin.getConfig().getBoolean("inventory");
        }
        if (plugin.getConfig().contains("advancement")) {
            advancement = plugin.getConfig().getBoolean("advancement");
        }
        if (plugin.getConfig().contains("announcedeath")) {
            announcedeath = plugin.getConfig().getBoolean("announcedeath");
        }
        if (plugin.getConfig().contains("teaminventory")) {
            teaminventory = plugin.getConfig().getBoolean("teaminventory");
        }

        StringBuilder sb = new StringBuilder();
        boolean temp = false;
        sb.append(I18N_TEAM_LOADED);
        for (Team team : Bukkit.getServer().getScoreboardManager().getMainScoreboard().getTeams()) {
            NmsInventory teamInventory = access.createEmpty();
            File file = new File(new File(plugin.getDataFolder(), "teams"), team.getName() + ".yml");
            if (file.exists()) {
                readInventory(YamlConfiguration.loadConfiguration(file), file, "team-" + team.getName(),
                        teamInventory, "Team:" + team.getName());
            }
            inventories.putTeamInventory(team.getName(), teamInventory);
            sb.append("[").append(team.getName()).append("] ");
            temp = true;
        }
        if (temp) logger.info(sb.toString());
    }

    private void writeInventory(FileConfiguration config, NmsInventory inventory) {
        config.set("format-version", FORMAT_VERSION);
        config.set("items", serializeItems(inventory, STORAGE_START, STORAGE_SIZE));
        config.set("armor", serializeItems(inventory, ARMOR_START, ARMOR_SIZE));
        config.set("extraSlots", serializeItems(inventory, OFFHAND_SLOT, 1));
    }

    /**
     * Loads every item that can be loaded. If anything is skipped, the source file is backed up first,
     * because the next {@link #save()} overwrites it with what is in memory.
     *
     * @param backupName prefix of the backup file name, e.g. {@code inventory} or {@code team-red}
     */
    private void readInventory(FileConfiguration config, File source, String backupName,
                               NmsInventory inventory, String category) {
        int formatVersion = config.getInt("format-version", FORMAT_VERSION);
        List<SkippedItem> skipped = new ArrayList<>();
        skipped.addAll(deserializeItems(config.getMapList("items"), inventory, STORAGE_START, STORAGE_SIZE));
        skipped.addAll(deserializeItems(config.getMapList("armor"), inventory, ARMOR_START, ARMOR_SIZE));
        skipped.addAll(deserializeItems(config.getMapList("extraSlots"), inventory, OFFHAND_SLOT, 1));
        if (formatVersion > FORMAT_VERSION || !skipped.isEmpty()) {
            reportLoss(source, backupName, category, formatVersion, skipped);
        }
    }

    /**
     * Logs what could not be loaded, one line per {@link SkippedItem.Reason}, and backs up the source file.
     */
    private void reportLoss(File source, String backupName, String category, int formatVersion,
                            List<SkippedItem> skipped) {
        logger.severe("[" + category + "] Some data in " + source.getName() + " could not be loaded:");
        if (formatVersion > FORMAT_VERSION) {
            logger.severe("  - format-version " + formatVersion + " is newer than this plugin supports (" + FORMAT_VERSION + ")");
        }

        Map<SkippedItem.Reason, List<SkippedItem>> byReason = new EnumMap<>(SkippedItem.Reason.class);
        for (SkippedItem item : skipped) {
            byReason.computeIfAbsent(item.reason, reason -> new ArrayList<>()).add(item);
        }
        int newestDataVersion = -1;
        for (Map.Entry<SkippedItem.Reason, List<SkippedItem>> entry : byReason.entrySet()) {
            List<SkippedItem> items = entry.getValue();
            switch (entry.getKey()) {
                case NEWER_VERSION:
                    for (SkippedItem item : items) newestDataVersion = Math.max(newestDataVersion, item.dataVersion);
                    logger.severe("  - " + items.size() + " item(s) saved by a newer Minecraft version (data version up to "
                            + newestDataVersion + " > " + WORLD_VERSION + "): " + formatSlots(items));
                    break;
                case INVALID_VERSION:
                    logger.severe("  - " + items.size() + " item(s) with an invalid data version: " + formatDetails(items));
                    break;
                case DESERIALIZE_FAILED:
                    logger.severe("  - " + items.size() + " item(s) failed to deserialize: " + formatDetails(items));
                    break;
            }
        }

        File backup = backup(source, backupName, newestDataVersion);
        if (backup != null) {
            logger.severe("The original file was backed up to " + backup.getPath()
                    + ". " + source.getName() + " will be overwritten on the next save. To recover the skipped items, "
                    + "restore the backup on a server that can read them (e.g. the newer version it came from).");
        }
    }

    /**
     * @return the backup file, or {@code null} if there was nothing to back up or copying failed
     */
    private File backup(File source, String backupName, int dataVersion) {
        if (!source.exists()) return null;
        File directory = new File(plugin.getDataFolder(), "backups");
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String name = backupName + "-" + timestamp + (dataVersion >= 0 ? "-dv" + dataVersion : "") + ".yml";
        File target = new File(directory, name);
        try {
            Files.createDirectories(directory.toPath());
            Files.copy(source.toPath(), target.toPath());
            return target;
        } catch (IOException e) {
            logger.severe("Failed to back up " + source.getPath() + " to " + target.getPath()
                    + ". Copy it manually before the next save!");
            e.printStackTrace();
            return null;
        }
    }

    private List<Map<?, ?>> serializeItems(NmsInventory inventory, int firstSlot, int count) {
        List<Map<?, ?>> serialized = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            serialized.add(access.getItem(inventory, firstSlot + i).serialize());
        }
        return serialized;
    }

    /**
     * Items saved by a newer Minecraft version (server downgrade) are skipped rather than deserialized:
     * Bukkit may silently drop data it does not understand, and a damaged item is harder to notice than a missing one.
     *
     * @return the items left out; their slots stay empty
     */
    @SuppressWarnings("unchecked")
    private List<SkippedItem> deserializeItems(List<Map<?, ?>> from, NmsInventory to, int firstSlot, int count) {
        List<SkippedItem> skipped = new ArrayList<>();
        for (int i = 0; i < Math.min(from.size(), count); i++) {
            Map<?, ?> item = from.get(i);
            if (item.isEmpty()) {
                continue;
            }
            int slot = firstSlot + i;
            Object version = item.get("v");
            if (version != null) {
                int dataVersion;
                try {
                    dataVersion = Integer.parseInt(version.toString());
                } catch (NumberFormatException e) {
                    skipped.add(new SkippedItem(slot, SkippedItem.Reason.INVALID_VERSION, -1, "\"" + version + "\""));
                    continue;
                }
                if (dataVersion > WORLD_VERSION) {
                    skipped.add(new SkippedItem(slot, SkippedItem.Reason.NEWER_VERSION, dataVersion, null));
                    continue;
                }
            }
            try {
                access.setItem(to, slot, ItemStack.deserialize((Map<String, Object>) item));
            } catch (Exception e) {
                skipped.add(new SkippedItem(slot, SkippedItem.Reason.DESERIALIZE_FAILED, -1, e.toString()));
            }
        }
        return skipped;
    }

    /**
     * @param items in ascending slot order
     * @return e.g. {@code slots 0-35, 38, 40}
     */
    private static String formatSlots(List<SkippedItem> items) {
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
    private static String formatDetails(List<SkippedItem> items) {
        StringBuilder sb = new StringBuilder();
        for (SkippedItem item : items) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("slot ").append(item.slot).append(" (").append(item.detail).append(")");
        }
        return sb.toString();
    }

    /** One item left out while loading an inventory file. */
    private static final class SkippedItem {
        enum Reason { NEWER_VERSION, INVALID_VERSION, DESERIALIZE_FAILED }

        private final int slot;
        private final Reason reason;
        /** Data version of the item, for {@link Reason#NEWER_VERSION}; otherwise -1 */
        private final int dataVersion;
        /** The invalid version value, or the deserialization error; otherwise {@code null} */
        private final String detail;

        private SkippedItem(int slot, Reason reason, int dataVersion, String detail) {
            this.slot = slot;
            this.reason = reason;
            this.dataVersion = dataVersion;
            this.detail = detail;
        }
    }

    @Override
    public void deleteWasteFiles() {
        File[] files = new File(plugin.getDataFolder(), "teams").listFiles();
        if (files != null) {
            for (File file : files) {
                List<String> list = new ArrayList<>();
                for (Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
                    list.add(team.getName());
                }
                if (!list.contains(file.getName())) {
                    try {
                        Files.delete(file.toPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
