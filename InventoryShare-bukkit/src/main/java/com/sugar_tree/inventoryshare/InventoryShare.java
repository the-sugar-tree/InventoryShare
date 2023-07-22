/*
 * Copyright (c) 2021 the-sugar-tree
 *
 *  Licensed under the General Public License, Version 3.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/lgpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sugar_tree.inventoryshare;

import com.sugar_tree.inventoryshare.metrics.Metrics;
import com.sugar_tree.inventoryshare.nms.NMSLoader;
import com.sugar_tree.inventoryshare.nms.utils.VersionUtil;
import com.sugar_tree.inventoryshare.utils.AdvancementUtil;
import com.sugar_tree.inventoryshare.utils.I18NUtil;
import com.sugar_tree.inventoryshare.utils.ProtocolLibUtil;
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

    public final static boolean isProtocolLib = checkProtocolLib();

    private Listeners listener;

    @Override
    public void onEnable() {
        // Assign shared variable
        plugin = this;
        logger = getLogger();

        // Load language model(s)
        I18NUtil.I18NFileManager.saveDefaultLanguageFiles();
        I18NUtil.init();
        // Store an inaccessible language variable in a shared variable in advance.
        I18N_TEAM_SAVED = I18NUtil.get("team_saved");
        I18N_TEAM_LOADED = I18NUtil.get("team_loaded");

        // Metrics https://bstats.org/plugin/bukkit/InventoryShare/18372
        Metrics metrics = new Metrics(this, 18372);
        metrics.addCustomChart(new Metrics.SimplePie("protocollib", () -> {if (isProtocolLib) return "Using"; else return "Not Using";}));

        // Check Update
        UpdateUtil.checkUpdate();

        // Check version
        if (!VersionUtil.isSupported()) {
            logger.severe(I18NUtil.get("not_supported_version", VersionUtil.getVersion().name()));
            this.setEnabled(false);
            return;
        }

        // Check the server is using ProtocolLib plugin
        if (isProtocolLib) {
            ProtocolLibUtil.ProtocolLib();
            logger.info(I18NUtil.get("protocolLib_found"));
        } else {
            logger.info(I18NUtil.get("protocolLib_need1"));
            logger.info(I18NUtil.get("protocolLib_need2"));
            logger.info("https://www.spigotmc.org/resources/protocollib.1997");
        }

        // Load NMS contents
        // FileManager, InventoryManager are loaded here
        if (!NMSLoader.init()) {
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
            if (inventory) InventoryManager.applyInventory(player);
            getServer().getScheduler().runTaskLater(this, () -> AdvancementUtil.AdvancementPatch(player), 1);
        }

        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.YELLOW + "\"" + I18NUtil.get("plugin_name") + "\" by. " + ChatColor.GREEN + "sugar_tree");
    }

    @Override
    public void onDisable() {
        if (InventoryManager == null || FileManager == null) {
            return;
        }
        for (UUID puuid : InventoryManager.getRegisteredPlayers()) {
            if (getServer().getOfflinePlayer(puuid).isOnline()) {
                Player p = (Player) getServer().getOfflinePlayer(puuid);
                InventoryManager.disApplyInventory(p);
            }
        }
        if (listener != null) Bukkit.getScheduler().cancelTask(listener.getTaskId());
        FileManager.save();
    }

    private static boolean checkProtocolLib() {
        return Bukkit.getServer().getPluginManager().getPlugin("ProtocolLib") != null;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void saveDefaultConfigs() {
        saveDefaultConfig();
        if (!(invfile.exists())) {
            saveResource("inventory.yml", false);
        }
        if (!(advfile.exists())) {
            saveResource("advancements.yml", false);
        }
        if (!(new File(getDataFolder(), "\\teams")).exists()) {
            (new File(getDataFolder(), "\\teams")).mkdir();
        }
    }
}