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

import com.sugar_tree.inventoryshare.util.AdvancementUtil;
import com.sugar_tree.inventoryshare.util.I18NUtil;
import com.sugar_tree.inventoryshare.metrics.Metrics;
import com.sugar_tree.inventoryshare.util.ProtocolLibUtil;
import com.sugar_tree.inventoryshare.v1_19_R3.FileManager_1_19_R3;
import com.sugar_tree.inventoryshare.v1_19_R3.Inventory_1_19_R3;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

import static com.sugar_tree.inventoryshare.api.SharedConstants.*;

public final class InventoryShare extends JavaPlugin {
    @SuppressWarnings("FieldCanBeLocal")
    private boolean isProtocolLib = false;

    private Listeners listener;

    final String supportedVersion = "v1_19_R3";

    @Override
    public void onEnable() {
        Metrics metrics = new Metrics(this, 18372);
        plugin = this;
        logger = getLogger();

        I18NUtil.I18NFileManager.saveDefaultLanguageFiles();
        I18NUtil.init();

        isProtocolLib = checkProtocolLib();
        metrics.addCustomChart(new Metrics.SimplePie("protocollib", () -> {if (isProtocolLib) return "Using"; else return "Not Using";}));
        metrics.addCustomChart(new Metrics.SimplePie("single_version", () -> "v1_19_R3"));
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        if (!isSupported()) {
            logger.severe(I18NUtil.get("not_supported_version", version));
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
        if (isSupported()) {
            InventoryManager = new Inventory_1_19_R3();
            FileManager = new FileManager_1_19_R3();
        } else {
            logger.severe("알 수 없는 오류로 이 버전을 지원하지 않습니다!");
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

    public boolean isSupported() {
        return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].equals(supportedVersion);
    }

    @Override
    public void onDisable() {
        if (InventoryManager == null) {
            return;
        }
        for (UUID puuid : InventoryManager.getRegisteredPlayers()) {
            if (getServer().getOfflinePlayer(puuid).isOnline()) {
                Player p = (Player) getServer().getOfflinePlayer(puuid);
                InventoryManager.disApplyInventory(p);
            }
        }
        Bukkit.getScheduler().cancelTask(listener.getTaskId());
        FileManager.save();
    }

    private boolean checkProtocolLib() {
        return getServer().getPluginManager().getPlugin("ProtocolLib") != null;
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
