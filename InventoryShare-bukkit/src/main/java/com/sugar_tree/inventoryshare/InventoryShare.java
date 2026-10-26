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

package com.sugar_tree.inventoryshare;

import com.sugar_tree.inventoryshare.file.FileManager;
import com.sugar_tree.inventoryshare.interfaces.IFileManager;
import com.sugar_tree.inventoryshare.interfaces.IInventoryManager;
import com.sugar_tree.inventoryshare.inventory.InventoryManager;
import com.sugar_tree.inventoryshare.inventory.InventoryRegistry;
import com.sugar_tree.inventoryshare.metrics.Metrics;
import com.sugar_tree.inventoryshare.nms.IncompatibleServerException;
import com.sugar_tree.inventoryshare.nms.InventoryAccess;
import com.sugar_tree.inventoryshare.nms.InventoryAccessFactory;
import com.sugar_tree.inventoryshare.nms.ItemCodec;
import com.sugar_tree.inventoryshare.nms.ItemCodecFactory;
import com.sugar_tree.inventoryshare.nms.UnsupportedServerException;
import com.sugar_tree.inventoryshare.nms.UnsupportedVersionException;
import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import com.sugar_tree.inventoryshare.nms.server.ServerRuntime;
import com.sugar_tree.inventoryshare.protocollib.ProtocolLibManager;
import com.sugar_tree.inventoryshare.protocollib.ProtocolLibStatus;
import com.sugar_tree.inventoryshare.utils.AdvancementRegistry;
import com.sugar_tree.inventoryshare.utils.AdvancementUtil;
import com.sugar_tree.inventoryshare.utils.I18NUtil;
import com.sugar_tree.inventoryshare.utils.UpdateUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.logging.Level;

import static com.sugar_tree.inventoryshare.SharedConstants.*;

public final class InventoryShare extends JavaPlugin {

    private ProtocolLibStatus protocolLibStatus; // Status of ProtocolLib plugin

    private Listeners listener; // Listener for events

    private PluginSettings settings;
    private final AdvancementRegistry advancements = new AdvancementRegistry();
    private IInventoryManager inventoryManager;
    private IFileManager fileManager;

    /**
     * Whether {@link #onEnable()} finished. {@code setEnabled(false)} inside {@code onEnable()} calls
     * {@link #onDisable()} right away, which must not save a partly loaded state over the files.
     */
    private boolean started;

    @Override
    public void onLoad() {
        // Assign shared variable for the plugin instance and logger
        plugin = this;
        logger = getLogger();
    }

    @Override
    public void onEnable() {
        settings = new PluginSettings(this);

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

        MinecraftVersion minecraftVersion;
        try {
            minecraftVersion = MinecraftVersion.current();
        } catch (UnsupportedServerException e) {
            // An unreadable version: not a bug, nothing to report
            logger.severe(I18NUtil.get(false, false, "not_supported_server", Bukkit.getName() + " " + Bukkit.getVersion()));
            this.setEnabled(false);
            return;
        }
        // Load NMS (Net Minecraft Server) contents; this also decides whether the version is supported
        // FileManager and InventoryManager are initialized here
        if (!loadNMS(minecraftVersion)) {
            // Disable the plugin if the version is not supported or an error occurs
            this.setEnabled(false);
            return;
        }

        // Check if the ProtocolLib plugin is being used
        switch (protocolLibStatus) {
            case ENABLED:
                // Skip version 1.12 due to differences in packet system
                if (minecraftVersion.isOlderThan("1.13")) break;
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

        saveDefaultConfig(); // Save config.yml if it does not exist
        // Load the saved inventories and advancements
        if (!fileManager.load()) {
            this.setEnabled(false);
            return;
        }
        settings.load();

        // Load commands and event listeners
        TabExecutor commands = new Commands(settings, inventoryManager);
        getCommand("inventoryshare").setExecutor(commands);
        getCommand("inventoryshare").setTabCompleter(commands);
        listener = new Listeners(settings, inventoryManager, fileManager, advancements);
        Bukkit.getPluginManager().registerEvents(listener, this);

        // Update inventories for online players if the server is reloaded
        for (Player player : Bukkit.getOnlinePlayers()) {
            inventoryManager.updateInventory(player);
            getServer().getScheduler().runTaskLater(this, () -> AdvancementUtil.AdvancementPatch(player, settings, advancements), 1);
        }
        started = true;

        // Send a message to the console indicating the plugin is loaded
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.YELLOW + "\"" + I18NUtil.get(false, false, "plugin_name") + "\" by. " + ChatColor.GREEN + "sugar_tree");
    }

    @Override
    public void onDisable() {
        // Check if inventoryManager or fileManager is null
        if (inventoryManager == null || fileManager == null) {
            // If not supported version or unexpected exception occurs
            return;
        }
        // Apply personal inventory for registered players, so the server saves their own inventory.
        for (UUID puuid : inventoryManager.getRegisteredPlayers()) {
            if (getServer().getOfflinePlayer(puuid).isOnline()) {
                Player p = (Player) getServer().getOfflinePlayer(puuid);
                inventoryManager.applyPersonalInventory(p);
            }
        }
        // Cancel any scheduled tasks for the listener
        if (listener != null) Bukkit.getScheduler().cancelTask(listener.getTaskId());
        // After a failed start the loaded state may be incomplete; keep the files as they are
        if (!started) return;
        fileManager.save(); // Save the inventories and advancements
        settings.save();
    }

    private boolean loadNMS(MinecraftVersion minecraftVersion) {
        logger.info("Loading Classes...");
        try {
            ServerRuntime runtime = ServerRuntime.current();
            InventoryAccess access = InventoryAccessFactory.create(minecraftVersion, runtime);

            InventoryRegistry registry = new InventoryRegistry(access);
            logger.info("Minecraft " + minecraftVersion + " (" + runtime + "), using " + access.getClass().getSimpleName());
            // From 1.20.5 items are saved in Minecraft's own format; before, in Bukkit's
            ItemCodec itemCodec = ItemCodecFactory.supports(minecraftVersion)
                    ? ItemCodecFactory.create(minecraftVersion, runtime, WORLD_VERSION) : null;

            inventoryManager = new InventoryManager(access, registry, settings);
            fileManager = new FileManager(access, registry, advancements, itemCodec);
        } catch (UnsupportedServerException e) {
            // Not a CraftBukkit-based server: not a bug, nothing to report
            logger.severe(I18NUtil.get(false, false, "not_supported_server", Bukkit.getName() + " " + Bukkit.getVersion()));
            return false;
        } catch (UnsupportedVersionException e) {
            // No provider supports the version: not a bug, nothing to report
            logger.severe(I18NUtil.get(false, false, "not_supported_version", minecraftVersion.toString()));
            return false;
        } catch (IncompatibleServerException | RuntimeException e) {
            logger.log(Level.SEVERE, "Could not hook into Minecraft " + minecraftVersion
                    + " (" + Bukkit.getVersion() + "). InventoryShare will be disabled; no inventories were changed.", e);
            logger.severe("This is a bug, not a configuration problem. Please report it with the error above:");
            logger.severe("https://github.com/the-sugar-tree/InventoryShare/issues");
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
}