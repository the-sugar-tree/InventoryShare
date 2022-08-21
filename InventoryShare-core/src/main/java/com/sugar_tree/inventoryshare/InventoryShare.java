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

import com.sugar_tree.inventoryshare.v1_19_R1.*;
import com.sugar_tree.inventoryshare.v1_19_R1_P0.*;
import com.sugar_tree.inventoryshare.v1_19_R1_P1.*;
import com.sugar_tree.inventoryshare.v1_18_R2.*;
import com.sugar_tree.inventoryshare.v1_18_R1.*;
import com.sugar_tree.inventoryshare.v1_17_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

import static com.sugar_tree.inventoryshare.Advancement.AdvancementPatch;
import static com.sugar_tree.inventoryshare.ProtocolLib.protocolLib;
import static com.sugar_tree.inventoryshare.api.Variables.*;

public final class InventoryShare extends JavaPlugin {
    private static String minorVersion;
    private static String patchVersion;
    private boolean isSupportedVersion = true;
    private boolean isPaper = false;
    @SuppressWarnings("FieldCanBeLocal")
    private boolean isProtocolLib = false;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEnable() {
        logger = getLogger();
        isSupportedVersion = checkVersion();
        isPaper = checkPaper();
        isProtocolLib = checkProtocolLib();
        if (!isSupportedVersion) {
            this.getLogger().severe("이 플러그인은 이 버젼을 지원하지 않습니다: " + minorVersion);
            this.setEnabled(false);
            return;
        }
        if (!isPaper) {
            this.getLogger().severe("이 플러그인은 페이퍼 버킷만 지원합니다.");
            this.setEnabled(false);
            return;
        }
        if (isProtocolLib) {
            protocolLib(this);
        } else {
            getLogger().warning("이 플러그인의 모든 기능을 사용하시려면 ProtocolLib 플러그인이 필요합니다.");
        }
        switch (minorVersion) {
            case "v1_19_R1" -> {
                if (patchVersion.equals("1.19-R0.1-SNAPSHOT")) {
                    InventoryClass = new Inventory_1_19_R1_P0(this);
                    FileManagerClass = new FileManager_1_19_R1_P0(this);
                }
                else if (patchVersion.equals("1.19.1-R0.1-SNAPSHOT")) {
                    InventoryClass = new Inventory_1_19_R1_P1(this);
                    FileManagerClass = new FileManager_1_19_R1_P1(this);
                }
                else {
                    InventoryClass = new Inventory_1_19_R1(this);
                    FileManagerClass = new FileManager_1_19_R1(this);
                }
            }
            case "v1_18_R2" -> {
                InventoryClass = new Inventory_1_18_R2(this);
                FileManagerClass = new FileManager_1_18_R2(this);
            }
            case "v1_18_R1" -> {
                InventoryClass = new Inventory_1_18_R1(this);
                FileManagerClass = new FileManager_1_18_R1(this);
            }
            case "v1_17_R1" -> {
                InventoryClass = new Inventory_1_17_R1(this);
                FileManagerClass = new FileManager_1_17_R1(this);
            }
        }
        invfile = new File(getDataFolder(), "inventory.yml");
        advfile = new File(getDataFolder(), "advancements.yml");
        invconfig = YamlConfiguration.loadConfiguration(invfile);
        advconfig = YamlConfiguration.loadConfiguration(advfile);
        saveDefaultConfigs();
        getCommand("inventoryshare").setExecutor(new Commands());
        getCommand("inventoryshare").setTabCompleter(new Commands());
        Bukkit.getPluginManager().registerEvents(new Listeners(), this);
        FileManagerClass.load();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (inventory) InventoryClass.invApply(player);
            getServer().getScheduler().runTaskLater(this, () -> AdvancementPatch(player), 1);
        }
        getServer().getConsoleSender().sendMessage(PREFIX + ChatColor.YELLOW + "\"인벤토리 공유 플러그인\" by. " + ChatColor.GREEN + "sugar_tree");
    }

    @Override
    public void onDisable() {
        if (!isSupportedVersion) return;
        if (!isPaper) return;
        for (UUID puuid : invList.keySet()) {
            if (getServer().getOfflinePlayer(puuid).isOnline()) {
                Player p = (Player) getServer().getOfflinePlayer(puuid);
                InventoryClass.invDisApply(p);
            }
        }
        FileManagerClass.save();
    }

    private boolean checkVersion() {
        minorVersion = "N/A";
        patchVersion = "N/A";
        try {
            minorVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
        patchVersion = Bukkit.getBukkitVersion();
        return minorVersion.equals("v1_19_R1") || minorVersion.equals("v1_18_R2") || minorVersion.equals("v1_18_R1") || minorVersion.equals("v1_17_R1");
    }
    private boolean checkPaper() {
        return Bukkit.getVersion().contains("Paper");
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
