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

import com.sugar_tree.inventoryshare.v1_18_R1.Inventory_1_18_R1;
import com.sugar_tree.inventoryshare.v1_18_R1.fileManager_1_18_R1;
import com.sugar_tree.inventoryshare.v1_18_R2.Inventory_1_18_R2;
import com.sugar_tree.inventoryshare.v1_18_R2.fileManager_1_18_R2;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

import static com.sugar_tree.inventoryshare.Advancement.AdvancementPatch;
import static com.sugar_tree.inventoryshare.api.variables.*;

public final class InventoryShare extends JavaPlugin {
    private static String sversion;
    private boolean enable = true;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEnable() {
        enable = setupManager();
        if (!enable) {
            this.getLogger().severe("This plugin doesn't support this version: " + sversion);
            this.setEnabled(false);
            return;
        }
        if (sversion.equals("v1_18_R2")) {
            Inventory = new Inventory_1_18_R2(this);
            fileManager = new fileManager_1_18_R2(this);
        }
        else if (sversion.equals("v1_18_R1")) {
            Inventory = new Inventory_1_18_R1(this);
            fileManager = new fileManager_1_18_R1(this);
        }
        invfile = new File(getDataFolder(), "inventory.yml");
        advfile = new File(getDataFolder(), "advancements.yml");
        invconfig = YamlConfiguration.loadConfiguration(invfile);
        advconfig = YamlConfiguration.loadConfiguration(advfile);
        saveDefaultConfigs();
        getCommand("inventoryshare").setExecutor(new Commands());
        getCommand("inventoryshare").setTabCompleter(new Commands());
        Bukkit.getPluginManager().registerEvents(new Listeners(), this);
        fileManager.load();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (inventory) Inventory.invApply(player);
            getServer().getScheduler().runTaskLater(this, () -> AdvancementPatch(player), 1);
        }
        getServer().getConsoleSender().sendMessage(PREFIX + ChatColor.YELLOW + "\"인벤토리 공유 플러그인\" by. " + ChatColor.GREEN + "sugar_tree");
    }

    @Override
    public void onDisable() {
        if (!enable) return;
        for (UUID puuid : invList.keySet()) {
            if (getServer().getOfflinePlayer(puuid).isOnline()) {
                Player p = (Player) getServer().getOfflinePlayer(puuid);
                Inventory.invDisApply(p);
            }
        }
        fileManager.save();
    }

    private boolean setupManager() {
        sversion = "N/A";
        try {
            sversion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
        return sversion.equals("v1_18_R2") || sversion.equals("v1_18_R1");
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
