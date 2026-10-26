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

import com.sugar_tree.inventoryshare.interfaces.IFileManager;
import com.sugar_tree.inventoryshare.inventory.InventoryRegistry;
import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.InventoryStorage;
import com.sugar_tree.inventoryshare.nms.ItemCodec;
import com.sugar_tree.inventoryshare.utils.AdvancementRegistry;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import static com.sugar_tree.inventoryshare.SharedConstants.*;

public final class FileManager implements IFileManager {

    /** Team inventories are saved as {@code teams/<team name>.yml} */
    static final String TEAM_FILE_EXTENSION = ".yml";

    private final InventoryAccess access;
    private final InventoryRegistry registry;
    private final AdvancementRegistry advancements;
    private final Codec codec;

    private final File invfile = new File(plugin.getDataFolder(), "inventory.yml");
    private final File advfile = new File(plugin.getDataFolder(), "advancements.yml");
    /** Read by {@link #load()} */
    private FileConfiguration invconfig;
    private FileConfiguration advconfig;

    /**
     * @param itemCodec writes and reads the items from 1.20.5, {@code null} before
     */
    public FileManager(InventoryAccess access, InventoryRegistry registry, AdvancementRegistry advancements,
                       @Nullable ItemCodec itemCodec) {
        this.access = access;
        this.registry = registry;
        this.advancements = advancements;
        this.codec = new Codec(access, WORLD_VERSION, itemCodec);
    }

    @Override
    public void save() {
        writeInventory(invconfig, registry.getSharedInventory());

        List<String> serializedAdvancementList = new ArrayList<>();
        for (NamespacedKey namespacedKey : advancements.getAdvancements()) {
            serializedAdvancementList.add(namespacedKey.toString());
        }
        advconfig.set("advancement", serializedAdvancementList);

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
            saveFile(fileConfiguration, file);
            sb.append("[").append(team.getName()).append("] ");
            savedLeastOneTeam = true;
        }
        if (savedLeastOneTeam) logger.info(sb.toString());

        deleteWasteFiles();
        saveFile(invconfig, invfile);
        saveFile(advconfig, advfile);
    }

    private static void saveFile(FileConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not save " + file.getPath(), e);
        }
    }

    @Override
    public boolean load() {
        saveDefaultFiles();
        invconfig = YamlConfiguration.loadConfiguration(invfile);
        advconfig = YamlConfiguration.loadConfiguration(advfile);
        try {
            readInventory(invconfig, invfile, "inventory", registry.getSharedInventory(), "ALL");

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
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Could not load the saved inventories. InventoryShare will be disabled.", e);
            logger.severe("This is a bug, not a configuration problem. Please report it with the error above:");
            logger.severe("https://github.com/the-sugar-tree/InventoryShare/issues");
            return false;
        }

        // Advancements the server no longer has (removed data pack, another version) are dropped
        Map<String, NamespacedKey> serverAdvancements = new HashMap<>();
        Iterator<Advancement> iterator = Bukkit.getServer().advancementIterator();
        while (iterator.hasNext()) {
            NamespacedKey key = iterator.next().getKey();
            serverAdvancements.put(key.toString(), key);
        }
        for (String advancement : advconfig.getStringList("advancement")) {
            NamespacedKey key = serverAdvancements.get(fullAdvancementKey(advancement));
            if (key != null) advancements.add(key);
        }
        return true;
    }

    /**
     * @param saved an advancement key from {@code advancements.yml}: {@code minecraft:story/root}, or
     *              {@code story/root} as saved before 4.0, which only kept the key without the namespace
     * @return the key with its namespace, e.g. {@code minecraft:story/root}
     */
    static String fullAdvancementKey(String saved) {
        return saved.contains(":") ? saved : "minecraft:" + saved;
    }

    /** Creates {@code inventory.yml}, {@code advancements.yml} and the {@code teams} folder if they do not exist */
    private void saveDefaultFiles() {
        if (!invfile.exists()) plugin.saveResource("inventory.yml", false);
        if (!advfile.exists()) plugin.saveResource("advancements.yml", false);
        File teamDir = teamDirectory();
        if (!teamDir.exists() && !teamDir.mkdir()) logger.severe("Failed to create folder: " + teamDir.getAbsolutePath());
    }

    private void writeInventory(FileConfiguration config, InventoryStorage inventory) {
        codec.write(config, inventory);
    }

    /**
     * Loads every item that can be loaded. If anything is skipped, or if items are migrated from Bukkit's format,
     * the source file is backed up first, because the next {@link #save()} overwrites it with what is in memory.
     *
     * @param backupName prefix of the backup file name, e.g. {@code inventory} or {@code team-red}
     */
    private void readInventory(FileConfiguration config, File source, String backupName,
                               InventoryStorage inventory, String category) {
        LoadResult result = codec.read(config, inventory);
        if (result.isComplete() && result.getMigrated() == 0) {
            return;
        }
        if (!result.isComplete()) {
            logger.severe("[" + category + "] Some data in " + source.getName() + " could not be loaded:");
            for (String line : result.describe()) {
                logger.severe(line);
            }
        }
        if (result.getMigrated() > 0) {
            logger.info("[" + category + "] " + result.getMigrated() + " item(s) in " + source.getName()
                    + " are in Bukkit's item format (InventoryShare 3.x, or saved before Minecraft 1.20.5)."
                    + " They will be saved in Minecraft's own item format from now on.");
            if (result.isMigratedFromOlderVersion()) {
                logger.warning("[" + category + "] Some of them were saved by an older Minecraft version (data version "
                        + result.oldestMigratedDataVersion() + " < " + WORLD_VERSION + "). Reading them on a newer version"
                        + " may lose part of their data, such as the contents of shulker boxes. To migrate without loss,"
                        + " restore the backup below and run this version of InventoryShare once on the Minecraft version"
                        + " they were saved with before upgrading the server.");
            }
        }
        File backupDirectory = new File(plugin.getDataFolder(), "backups");
        File backup;
        try {
            backup = backUp(backupDirectory, source, backupName, result.newestDataVersion());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to back up " + source.getPath() + " into " + backupDirectory.getPath()
                    + ". Copy it manually before the next save!", e);
            return;
        }
        if (!result.isComplete()) {
            logger.severe("The original file was backed up to " + backup.getPath()
                    + ". " + source.getName() + " will be overwritten on the next save. To recover the skipped items, "
                    + "restore the backup on a server that can read them (e.g. the newer version it came from).");
        } else {
            logger.info("The original file was backed up to " + backup.getPath() + ".");
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
                logger.log(Level.WARNING, "Could not delete " + file.getPath() + ", the file of a removed team", e);
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
