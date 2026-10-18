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

import com.sugar_tree.inventoryshare.interfaces.IFileManager;
import com.sugar_tree.inventoryshare.inventory.InventoryRegistry;
import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.InventoryStorage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.sugar_tree.inventoryshare.SharedConstants.*;

public final class FileManager implements IFileManager {

    /** Team inventories are saved as {@code teams/<team name>.yml} */
    static final String TEAM_FILE_EXTENSION = ".yml";

    private final InventoryAccess access;
    private final InventoryRegistry registry;
    private final Codec codec;

    public FileManager(InventoryAccess access, InventoryRegistry registry) {
        this.access = access;
        this.registry = registry;
        this.codec = new Codec(access, WORLD_VERSION);
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
            File file = teamFile(team.getName());
            FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
            InventoryStorage teamInventory = registry.getTeamInventory(team.getName());
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
            InventoryStorage teamInventory = access.createEmpty();
            File file = teamFile(team.getName());
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

    private void writeInventory(FileConfiguration config, InventoryStorage inventory) {
        codec.write(config, inventory);
    }

    /**
     * Loads every item that can be loaded. If anything is skipped, the source file is backed up first,
     * because the next {@link #save()} overwrites it with what is in memory.
     *
     * @param backupName prefix of the backup file name, e.g. {@code inventory} or {@code team-red}
     */
    private void readInventory(FileConfiguration config, File source, String backupName,
                               InventoryStorage inventory, String category) {
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
            File backup = backUp(backupDirectory, source, backupName, result.newestDataVersion());
            logger.severe("The original file was backed up to " + backup.getPath()
                    + ". " + source.getName() + " will be overwritten on the next save. To recover the skipped items, "
                    + "restore the backup on a server that can read them (e.g. the newer version it came from).");
        } catch (IOException e) {
            logger.severe("Failed to back up " + source.getPath() + " into " + backupDirectory.getPath()
                    + ". Copy it manually before the next save!");
            e.printStackTrace();
        }
    }

    /**
     * Copies {@code source} to {@code <directory>/<name>-<yyyyMMdd-HHmmss>[-dv<dataVersion>].yml}.
     * Backups are never deleted automatically.
     *
     * @param name        e.g. {@code inventory} or {@code team-red}
     * @param dataVersion data version of the newest skipped item, or {@code -1} to leave it out of the name
     * @return the backup file
     */
    static File backUp(File directory, File source, String name, int dataVersion) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File target = new File(directory, name + "-" + timestamp + (dataVersion >= 0 ? "-dv" + dataVersion : "") + ".yml");
        Files.createDirectories(directory.toPath());
        Files.copy(source.toPath(), target.toPath());
        return target;
    }

    @Override
    public void deleteWasteFiles() {
        File[] files = teamDirectory().listFiles();
        if (files == null) return;
        Set<String> teamNames = new HashSet<>();
        for (Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
            teamNames.add(team.getName());
        }
        for (File file : files) {
            if (!isWasteTeamFile(file.getName(), teamNames)) continue;
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private File teamDirectory() {
        return new File(plugin.getDataFolder(), "teams");
    }

    private File teamFile(String teamName) {
        return new File(teamDirectory(), teamName + TEAM_FILE_EXTENSION);
    }

    /**
     * @param fileName  name of a file in the {@code teams} folder, e.g. {@code red.yml}
     * @param teamNames teams that exist on the main scoreboard
     * @return {@code true} if it is the inventory file of a team that no longer exists.
     *         Files that are not {@code .yml} are never treated as waste.
     */
    static boolean isWasteTeamFile(String fileName, Set<String> teamNames) {
        if (!fileName.endsWith(TEAM_FILE_EXTENSION)) return false;
        String teamName = fileName.substring(0, fileName.length() - TEAM_FILE_EXTENSION.length());
        return !teamNames.contains(teamName);
    }

}
