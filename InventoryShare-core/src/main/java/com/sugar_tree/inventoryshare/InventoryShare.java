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

import com.sugar_tree.inventoryshare.nms.NMSLoader;
import com.sugar_tree.inventoryshare.nms.util.VersionUtil;
import com.sugar_tree.inventoryshare.util.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

import static com.sugar_tree.inventoryshare.api.SharedConstants.*;

public final class InventoryShare extends JavaPlugin {

    public final static boolean isProtocolLib = checkProtocolLib();

    private Listeners listener;

    @Override
    public void onEnable() {
        plugin = this;
        logger = getLogger();

        saveDefaultLanguageFiles();

        I18N_TEAM_SAVED = I18NUtil.get("team_saved");
        I18N_TEAM_LOADED = I18NUtil.get("team_loaded");
        Metrics metrics = new Metrics(this, 18372);
        metrics.addCustomChart(new Metrics.SimplePie("protocollib", () -> {if (isProtocolLib) return "Using"; else return "Not Using";}));
        UpdateUtil.checkUpdate();
        if (!VersionUtil.isSupported()) {
            logger.severe(I18NUtil.get("not_supported_version", VersionUtil.getVersion().name()));
            this.setEnabled(false);
            return;
        }
        if (isProtocolLib) {
            ProtocolLibUtil.ProtocolLib();
            logger.info(I18NUtil.get("protocolLib_found"));
        } else {
            logger.info(I18NUtil.get("protocolLib_need1"));
            logger.info(I18NUtil.get("protocolLib_need2"));
            logger.info("https://www.spigotmc.org/resources/protocollib.1997");
        }

        if (!NMSLoader.init()) {
            this.setEnabled(false);
            return;
        }

        invfile = new File(getDataFolder(), "inventory.yml");
        advfile = new File(getDataFolder(), "advancements.yml");
        invconfig = YamlConfiguration.loadConfiguration(invfile);
        advconfig = YamlConfiguration.loadConfiguration(advfile);
        saveDefaultConfigs();
        getCommand("inventoryshare").setExecutor(new Commands());
        getCommand("inventoryshare").setTabCompleter(new Commands());
        listener = new Listeners();
        Bukkit.getPluginManager().registerEvents(listener, this);
        FileManager.load();
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

    private void saveDefaultLanguageFiles() {
        if (!new File(getDataFolder(), "\\languages\\lang_ko_kr.yml").exists())
            saveResource("languages/lang_ko_kr.yml", false);
        if (!new File(getDataFolder(), "\\languages\\lang_en_us.yml").exists())
            saveResource("languages/lang_en_us.yml", false);
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
