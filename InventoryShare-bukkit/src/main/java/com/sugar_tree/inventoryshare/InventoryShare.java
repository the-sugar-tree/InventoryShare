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

package com.sugar_tree.inventoryshare;

import com.sugar_tree.inventoryshare.metrics.Metrics;
import com.sugar_tree.inventoryshare.nms.FileManager;
import com.sugar_tree.inventoryshare.nms.InventoryManager;
import com.sugar_tree.inventoryshare.nms.utils.VersionUtil;
import com.sugar_tree.inventoryshare.protocollib.ProtocolLibManager;
import com.sugar_tree.inventoryshare.protocollib.ProtocolLibStatus;
import com.sugar_tree.inventoryshare.utils.AdvancementUtil;
import com.sugar_tree.inventoryshare.utils.I18NUtil;
import com.sugar_tree.inventoryshare.utils.UpdateUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

import static com.sugar_tree.inventoryshare.SharedConstants.*;

public final class InventoryShare extends JavaPlugin {

    private ProtocolLibStatus protocolLibStatus;

    private Listeners listener;

    @Override
    public void onLoad() {
        plugin = this;
        logger = getLogger();
    }

    @Override
    public void onEnable() {
        // Assign shared variable
        protocolLibStatus = checkProtocolLib();

        // Load language model(s)
        I18NUtil.I18NFileManager.saveDefaultLanguageFiles();
        I18NUtil.init();
        // Store an inaccessible language variable in a shared variable in advance.
        I18N_TEAM_SAVED = I18NUtil.get(false, false, "team_saved");
        I18N_TEAM_LOADED = I18NUtil.get(false, false, "team_loaded");

        // Metrics https://bstats.org/plugin/bukkit/InventoryShare/18372
        Metrics metrics = new Metrics(this, 18372);
        metrics.addCustomChart(new Metrics.SimplePie("protocollib", () -> protocolLibStatus == ProtocolLibStatus.ENABLED ? "Using" : "Not Using"));

        // Check Update
        UpdateUtil.showUpdateInformation();

        // Check version (If null, it means not supported)
        if (VersionUtil.getVersion() == null) {
            logger.severe(I18NUtil.get(false, false, "not_supported_version", Bukkit.getBukkitVersion()));
            this.setEnabled(false);
            return;
        }

        // Check the server is using ProtocolLib plugin
        switch (protocolLibStatus) {
            case ENABLED:
                if (VersionUtil.getVersion().name().equals("v1_12_R1")) break;
                new ProtocolLibManager(this).enable();
                logger.info(I18NUtil.get(false, false, "protocolLib_found"));
                break;
            case NEED:
                logger.warning(I18NUtil.get(false, false, "protocolLib_need1"));
                logger.warning(I18NUtil.get(false, false, "protocolLib_need2"));
                logger.warning("https://www.spigotmc.org/resources/protocollib.1997");
                break;
            case DISABLED:
                logger.warning(I18NUtil.get(false, false, "protocolLib_need_update"));
                logger.warning("https://www.spigotmc.org/resources/protocollib.1997");
                break;
        }

        // Load NMS contents
        // FileManager, InventoryManager are loaded here
        if (!loadNMS()) {
            // if NOT supported version or unexpected exception occurs
            this.setEnabled(false);
            return;
        }

        // Load files
        invfile = new File(getDataFolder(), "inventory.yml");
        advfile = new File(getDataFolder(), "advancements.yml");
        invconfig = YamlConfiguration.loadConfiguration(invfile);
        advconfig = YamlConfiguration.loadConfiguration(advfile);
        saveDefaultConfigs();
        FileManager.load();

        // Load commands and Listeners
        TabExecutor commands = new Commands();
        getCommand("inventoryshare").setExecutor(commands);
        getCommand("inventoryshare").setTabCompleter(commands);
        listener = new Listeners();
        Bukkit.getPluginManager().registerEvents(listener, this);

        // if the server is reloaded
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryManager.updateInventroy(player);
            getServer().getScheduler().runTaskLater(this, () -> AdvancementUtil.AdvancementPatch(player), 1);
        }

        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.YELLOW + "\"" + I18NUtil.get(false, false, "plugin_name") + "\" by. " + ChatColor.GREEN + "sugar_tree");
    }

    @Override
    public void onDisable() {
        if (InventoryManager == null || FileManager == null) {
            // if NOT supported version or unexpected exception occurs {InventoryShare.java-87:9}
            return;
        }
        for (UUID puuid : InventoryManager.getRegisteredPlayers()) {
            if (getServer().getOfflinePlayer(puuid).isOnline()) {
                Player p = (Player) getServer().getOfflinePlayer(puuid);
                InventoryManager.applyPersonalInventory(p);
            }
        }
        if (listener != null) Bukkit.getScheduler().cancelTask(listener.getTaskId());
        FileManager.save();
    }

    private boolean loadNMS() {
        logger.info("Loading Classes...");
        try {
            FileManager = new FileManager();
            InventoryManager = new InventoryManager();
        } catch (ExceptionInInitializerError e) {
            logger.severe("An error occurred while loading the classes!");
            logger.severe("This is NOT EXPECTED ERROR! Report this issue!");
            e.printStackTrace();
            return false;
        }
        logger.info("Done!");
        return true;
    }

    private ProtocolLibStatus checkProtocolLib() {
        if (Bukkit.getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            return ProtocolLibStatus.NEED;
        }

        if (!Bukkit.getServer().getPluginManager().getPlugin("ProtocolLib").isEnabled()) {
            return ProtocolLibStatus.DISABLED;
        } else {
            return ProtocolLibStatus.ENABLED;
        }
    }

    private void saveDefaultConfigs() {
        saveDefaultConfig();
        if (!(invfile.exists())) {
            saveResource("inventory.yml", false);
        }
        if (!(advfile.exists())) {
            saveResource("advancements.yml", false);
        }
        File teamDir = new File(getDataFolder(), "\\teams");
        if (!teamDir.exists() && !teamDir.mkdir()) logger.severe("Failed to creat folder: " + teamDir.getAbsolutePath());
    }
}
