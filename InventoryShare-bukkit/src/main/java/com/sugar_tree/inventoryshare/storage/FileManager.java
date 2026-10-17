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
import com.sugar_tree.inventoryshare.inventory.InventoryRegistry;
import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.NmsInventory;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.sugar_tree.inventoryshare.SharedConstants.*;

public final class FileManager implements IFileManager {

    private final InventoryAccess access;
    private final InventoryRegistry registry;
    private final InventoryCodec codec;

    public FileManager(InventoryAccess access, InventoryRegistry registry) {
        this.access = access;
        this.registry = registry;
        this.codec = new InventoryCodec(access, WORLD_VERSION);
    }

    @Override
    public void save() {
        writeInventory(invconfig, registry.getSharedInventory());

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
            NmsInventory teamInventory = registry.getTeamInventory(team.getName());
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
        readInventory(invconfig, invfile, "inventory", registry.getSharedInventory(), "ALL");

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
            registry.putTeamInventory(team.getName(), teamInventory);
            sb.append("[").append(team.getName()).append("] ");
            temp = true;
        }
        if (temp) logger.info(sb.toString());
    }

    private void writeInventory(FileConfiguration config, NmsInventory inventory) {
        codec.write(config, inventory);
    }

    /**
     * Loads every item that can be loaded. If anything is skipped, the source file is backed up first,
     * because the next {@link #save()} overwrites it with what is in memory.
     *
     * @param backupName prefix of the backup file name, e.g. {@code inventory} or {@code team-red}
     */
    private void readInventory(FileConfiguration config, File source, String backupName,
                               NmsInventory inventory, String category) {
        LoadResult result = codec.read(config, inventory);
        if (result.isComplete()) {
            return;
        }
        logger.severe("[" + category + "] Some data in " + source.getName() + " could not be loaded:");
        for (String line : result.describe()) {
            logger.severe("  - " + line);
        }
        File backupDirectory = new File(plugin.getDataFolder(), "backups");
        try {
            File backup = InventoryBackup.create(backupDirectory, source, backupName, result.newestDataVersion());
            logger.severe("The original file was backed up to " + backup.getPath()
                    + ". " + source.getName() + " will be overwritten on the next save. To recover the skipped items, "
                    + "restore the backup on a server that can read them (e.g. the newer version it came from).");
        } catch (IOException e) {
            logger.severe("Failed to back up " + source.getPath() + " into " + backupDirectory.getPath()
                    + ". Copy it manually before the next save!");
            e.printStackTrace();
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
