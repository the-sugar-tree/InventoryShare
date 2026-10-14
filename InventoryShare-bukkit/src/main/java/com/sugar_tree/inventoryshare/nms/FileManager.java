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

package com.sugar_tree.inventoryshare.nms;

import com.sugar_tree.inventoryshare.interfaces.IFileManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
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
            NmsInventory teamInventory = inventories.getTeamInventoryMap().get(team.getName());
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
        readInventory(invconfig, inventories.getSharedInventory(), "ALL");

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
                readInventory(YamlConfiguration.loadConfiguration(file), teamInventory, "Team:" + team.getName());
            }
            inventories.getTeamInventoryMap().put(team.getName(), teamInventory);
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

    private void readInventory(FileConfiguration config, NmsInventory inventory, String category) {
        deserializeItems(config.getMapList("items"), inventory, STORAGE_START, STORAGE_SIZE, category + ":items");
        deserializeItems(config.getMapList("armor"), inventory, ARMOR_START, ARMOR_SIZE, category + ":armor");
        deserializeItems(config.getMapList("extraSlots"), inventory, OFFHAND_SLOT, 1, category + ":extraSlots");
    }

    private List<Map<?, ?>> serializeItems(NmsInventory inventory, int firstSlot, int count) {
        List<Map<?, ?>> serialized = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            serialized.add(access.getItem(inventory, firstSlot + i).serialize());
        }
        return serialized;
    }

    @SuppressWarnings("unchecked")
    private void deserializeItems(List<Map<?, ?>> from, NmsInventory to, int firstSlot, int count, String itemCategory) {
        for (int i = 0; i < Math.min(from.size(), count); i++) {
            Map<?, ?> items = from.get(i);
            if (items.isEmpty()) {
                continue;
            }
            try {
                if (items.containsKey("v") && Integer.parseInt(items.get("v").toString()) > WORLD_VERSION) {
                    logger.severe("Newer version! Server downgrades are not supported!");
                    return;
                }
            } catch (NumberFormatException e) {
                logger.warning("An error occurred while loading " + itemCategory + ": Invalid version format at index " + i);
                continue;
            }
            try {
                access.setItem(to, firstSlot + i, ItemStack.deserialize((Map<String, Object>) items));
            } catch (Exception e) {
                logger.warning("An error occurred while loading " + itemCategory + ": Failed to deserialize item at index " + i);
                e.printStackTrace();
            }
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
