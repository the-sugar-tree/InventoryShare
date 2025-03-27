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

    private ProtocolLibStatus protocolLibStatus; // Status of ProtocolLib plugin

    private Listeners listener; // Listener for events

    @Override
    public void onLoad() {
        // Assign shared variable for the plugin instance and logger
        plugin = this;
        logger = getLogger();
    }

    @Override
    public void onEnable() {
        // Check the status of ProtocolLib
        protocolLibStatus = checkProtocolLib();

        // Load language files
        I18NUtil.I18NFileManager.saveDefaultLanguageFiles();
        I18NUtil.init();

        // Initialize metrics for tracking usage
        Metrics metrics = new Metrics(this, 18372);
        metrics.addCustomChart(new Metrics.SimplePie("protocollib", () -> protocolLibStatus == ProtocolLibStatus.ENABLED ? "Using" : "Not Using"));

        // Check for updates to the plugin
        UpdateUtil.showUpdateInformation();

        // Check if the server version is supported
        if (VersionUtil.getVersion() == null) {
            logger.severe(I18NUtil.get(false, false, "not_supported_version", Bukkit.getBukkitVersion()));
            this.setEnabled(false); // Disable the plugin if not supported
            return;
        }

        // Check if the ProtocolLib plugin is being used
        switch (protocolLibStatus) {
            case ENABLED:
                // Skip version 1.12 due to differences in packet system
                if (VersionUtil.getVersion().name().equals("v1_12_R1")) break;
                new ProtocolLibManager(this).enable(); // Enable ProtocolLib manager
                logger.info(I18NUtil.get(false, false, "protocolLib_found"));
                break;
            case NEED:
                // Log warnings if ProtocolLib is needed
                logger.warning(I18NUtil.get(false, false, "protocolLib_need1"));
                logger.warning(I18NUtil.get(false, false, "protocolLib_need2"));
                logger.warning("https://www.spigotmc.org/resources/protocollib.1997");
                logger.warning("https://ci.dmulloy2.net/job/ProtocolLib/");
                break;
            case DISABLED:
                // Log warnings if ProtocolLib is disabled
                logger.warning(I18NUtil.get(false, false, "protocolLib_need_update"));
                logger.warning("https://www.spigotmc.org/resources/protocollib.1997");
                logger.warning("https://ci.dmulloy2.net/job/ProtocolLib/");
                break;
        }

        // Load NMS (Net Minecraft Server) contents
        // FileManager and InventoryManager are initialized here
        if (!loadNMS()) {
            // Disable the plugin if the version is not supported or an error occurs
            this.setEnabled(false);
            return;
        }

        // Load configuration files
        invfile = new File(getDataFolder(), "inventory.yml");
        advfile = new File(getDataFolder(), "advancements.yml");
        invconfig = YamlConfiguration.loadConfiguration(invfile);
        advconfig = YamlConfiguration.loadConfiguration(advfile);
        saveDefaultConfigs(); // Save default configurations
        FileManager.load(); // Load file manager

        // Load commands and event listeners
        TabExecutor commands = new Commands();
        getCommand("inventoryshare").setExecutor(commands);
        getCommand("inventoryshare").setTabCompleter(commands);
        listener = new Listeners();
        Bukkit.getPluginManager().registerEvents(listener, this);

        // Update inventories for online players if the server is reloaded
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryManager.updateInventroy(player);
            getServer().getScheduler().runTaskLater(this, () -> AdvancementUtil.AdvancementPatch(player), 1);
        }

        // Send a message to the console indicating the plugin is loaded
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.YELLOW + "\"" + I18NUtil.get(false, false, "plugin_name") + "\" by. " + ChatColor.GREEN + "sugar_tree");
    }

    @Override
    public void onDisable() {
        // Check if InventoryManager or FileManager is null
        if (InventoryManager == null || FileManager == null) {
            // If not supported version or unexpected exception occurs
            return;
        }
        // Apply personal inventory for registered players
        for (UUID puuid : InventoryManager.getRegisteredPlayers()) {
            if (getServer().getOfflinePlayer(puuid).isOnline()) {
                Player p = (Player) getServer().getOfflinePlayer(puuid);
                InventoryManager.applyPersonalInventory(p);
            }
        }
        // Cancel any scheduled tasks for the listener
        if (listener != null) Bukkit.getScheduler().cancelTask(listener.getTaskId());
        FileManager.save(); // Save file manager data
    }

    private boolean loadNMS() {
        logger.info("Loading Classes...");
        try {
            FileManager = new FileManager(); // Initialize FileManager
            InventoryManager = new InventoryManager(); // Initialize InventoryManager
        } catch (ExceptionInInitializerError e) {
            logger.severe("An error occurred while loading the classes!");
            logger.severe("This is NOT EXPECTED ERROR! Report this issue!");
            e.printStackTrace();
            return false; // Return false if an error occurs
        }
        logger.info("Done!"); // Log completion of loading classes
        return true; // Return true if successful
    }

    private ProtocolLibStatus checkProtocolLib() {
        // Check if ProtocolLib is installed
        if (Bukkit.getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            return ProtocolLibStatus.NEED; // Return NEED status if not found
        }

        // Check if ProtocolLib is enabled
        if (!Bukkit.getServer().getPluginManager().getPlugin("ProtocolLib").isEnabled()) {
            return ProtocolLibStatus.DISABLED; // Return DISABLED status if not enabled
        } else {
            return ProtocolLibStatus.ENABLED; // Return ENABLED status if found and enabled
        }
    }

    private void saveDefaultConfigs() {
        saveDefaultConfig(); // Save the default configuration
        // Save inventory.yml if it does not exist
        if (!(invfile.exists())) {
            saveResource("inventory.yml", false);
        }
        // Save advancements.yml if it does not exist
        if (!(advfile.exists())) {
            saveResource("advancements.yml", false);
        }
        // Create a directory for teams if it does not exist
        File teamDir = new File(getDataFolder(), "\teams");
        if (!teamDir.exists() && !teamDir.mkdir()) logger.severe("Failed to create folder: " + teamDir.getAbsolutePath());
    }
}