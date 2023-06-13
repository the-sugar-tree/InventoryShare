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
package com.sugar_tree.inventoryshare.nms;

import com.google.common.collect.ImmutableList;
import com.sugar_tree.inventoryshare.api.IFileManager;
import com.sugar_tree.inventoryshare.api.IInventoryManager;
import com.sugar_tree.inventoryshare.nms.util.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;

import static com.sugar_tree.inventoryshare.api.SharedConstants.*;
import static com.sugar_tree.inventoryshare.nms.NMSLoader.FileManager.*;

public class NMSLoader {

    protected static final String PATH_CLASS_PlayerInventory;
    protected static final String PATH_CLASS_ItemStack;
    protected static final String PATH_CLASS_NonNullList;
    protected static final String PATH_METHOD_createItemlist;
    protected static final String PATH_FIELD_emptyItem;
    protected static final String version;
    protected static final String PATH_METHOD_getNameSpacedKey;
    //*****************************************************************************************************************//
    protected static final String PATH_CLASS_EntityPlayer;
    protected static final String PATH_CLASS_CraftPlayer;
    protected static final boolean DOES_INVENTORY_USE_FIELD;
    protected static final String PATH_EntityPlayer_Inventory;
    protected static final String PATH_PlayerInventory_items;
    protected static final String PATH_PlayerInventory_armor;
    protected static final String PATH_PlayerInventory_extraSlots;
    protected static final String PATH_PlayerInventory_contents;
    protected static final String PATH_CLASS_EntityHuman;

    static {
        PATH_CLASS_PlayerInventory = VersionUtil.getVersion().getPATH_CLASS_PlayerInventory();
        PATH_CLASS_ItemStack = VersionUtil.getVersion().getPATH_CLASS_ItemStack();
        PATH_CLASS_NonNullList = VersionUtil.getVersion().getPATH_CLASS_NonNullList();
        PATH_METHOD_createItemlist = VersionUtil.getVersion().getPATH_METHOD_createItemlist();
        PATH_FIELD_emptyItem = VersionUtil.getVersion().getPATH_FIELD_emptyItem();
        version = VersionUtil.getVersion().name();
        PATH_METHOD_getNameSpacedKey = VersionUtil.getVersion().getPATH_METHOD_getNameSpacedKey();
        //*****************************************************************************************************************//
        PATH_CLASS_EntityPlayer = VersionUtil.getVersion().getPATH_CLASS_EntityPlayer();
        PATH_CLASS_CraftPlayer = VersionUtil.getVersion().getPATH_CLASS_CraftPlayer();
        DOES_INVENTORY_USE_FIELD = VersionUtil.getVersion().getDOES_INVENTORY_USE_FIELD();
        PATH_EntityPlayer_Inventory = VersionUtil.getVersion().getPATH_EntityPlayer_Inventory();
        PATH_PlayerInventory_items = VersionUtil.getVersion().getPATH_PlayerInventory_items();
        PATH_PlayerInventory_armor = VersionUtil.getVersion().getPATH_PlayerInventory_armor();
        PATH_PlayerInventory_extraSlots = VersionUtil.getVersion().getPATH_PlayerInventory_extraSlots();
        PATH_PlayerInventory_contents = VersionUtil.getVersion().getPATH_PlayerInventory_contents();
        PATH_CLASS_EntityHuman = VersionUtil.getVersion().getPATH_CLASS_EntityHuman();
    }

    private NMSLoader() {}

    public static boolean init() {
        logger.info("Loading Classes...");
        try {
            FileManager = new FileManager();
            InventoryManager = new InventoryManager();
        } catch (ExceptionInInitializerError e) {
            logger.severe("Error while loading Classes");
            e.printStackTrace();
            return false;
        }
        logger.info("Done!");
        return true;
    }

    @SuppressWarnings({"unchecked", "JavaReflectionInvocation"})
    static class FileManager implements IFileManager {

        private static final Class<?> ItemStack;
        private static final Class<?> NonNullList;
        protected static final Method createItemList;
        protected static final Object nullItem;
        private static final Class<?> CraftItemStack;
        private static final Method asCraftMirror;
        private static final Method asNMSCopy;
        private static final Method deserialize;
        private static final Method getAdvName;

        protected static Map<UUID, Object> invList = new HashMap<>();

        protected static AbstractList<Object> items;
        protected static AbstractList<Object> armor;
        protected static AbstractList<Object> extraSlots;
        protected static List<AbstractList<Object>> contents;

        protected static Map<String, Map<String, AbstractList<Object>>> InventoryList = new HashMap<>();

        static {
            try {
                ItemStack = Class.forName(PATH_CLASS_ItemStack);
                NonNullList = Class.forName(PATH_CLASS_NonNullList);
                createItemList = NonNullList.getDeclaredMethod(PATH_METHOD_createItemlist, int.class, Object.class);
                nullItem = ItemStack.getField(PATH_FIELD_emptyItem).get(null);
                CraftItemStack = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
                asCraftMirror = CraftItemStack.getMethod("asCraftMirror", ItemStack);
                asNMSCopy = CraftItemStack.getMethod("asNMSCopy", org.bukkit.inventory.ItemStack.class);
                deserialize = CraftItemStack.getMethod("deserialize", Map.class);

                items = (AbstractList<Object>) createItemList.invoke(null, 36, nullItem);
                armor = (AbstractList<Object>) createItemList.invoke(null, 4, nullItem);
                extraSlots = (AbstractList<Object>) createItemList.invoke(null, 1, nullItem);
                contents = ImmutableList.of(items, armor, extraSlots);

                getAdvName = NamespacedKey.class.getDeclaredMethod(PATH_METHOD_getNameSpacedKey, String.class);
            } catch (NoSuchFieldException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void save() {
            try {
                List<Map<?, ?>> itemslist = new ArrayList<>();
                for (Object itemStack : items) {
                    itemslist.add(((org.bukkit.inventory.ItemStack) asCraftMirror.invoke(null, itemStack)).serialize());
                }
                invconfig.set("items", itemslist);

                List<Map<?, ?>> armorlist = new ArrayList<>();
                for (Object itemStack : armor) {
                    armorlist.add(((org.bukkit.inventory.ItemStack) asCraftMirror.invoke(null, itemStack)).serialize());
                }
                invconfig.set("armor", armorlist);

                List<Map<?, ?>> extraSlotsList = new ArrayList<>();
                for (Object itemStack : extraSlots) {
                    extraSlotsList.add(((org.bukkit.inventory.ItemStack) asCraftMirror.invoke(null, itemStack)).serialize());
                }
                invconfig.set("extraSlots", extraSlotsList);

                List<String> alist = new ArrayList<>();
                for (NamespacedKey namespacedKey : advlist) {
                    alist.add(namespacedKey.getKey());
                }
                advconfig.set("advancement", alist);

                plugin.getConfig().set("inventory", inventory);
                plugin.getConfig().set("advancement", advancement);
                plugin.getConfig().set("announcedeath", announcedeath);
                plugin.getConfig().set("teaminventory", teaminventory);
                StringBuilder sb = new StringBuilder();
                boolean temp = false;
                sb.append(I18N_TEAM_SAVED);
                for (Team team : Bukkit.getServer().getScoreboardManager().getMainScoreboard().getTeams()) {
                    if (team == null) continue;
                    File file = new File(new File(plugin.getDataFolder(), "\\teams"), team.getName() + ".yml");
                    FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
                    List<Map<?, ?>> itemslistT = new ArrayList<>();
                    Map<String, AbstractList<Object>> invT = InventoryList.get(team.getName());
                    if (invT == null) continue;
                    for (Object itemStack : invT.get("items")) {
                        itemslistT.add(((org.bukkit.inventory.ItemStack) asCraftMirror.invoke(null, itemStack)).serialize());
                    }
                    fileConfiguration.set("items", itemslistT);

                    List<Map<?, ?>> armorlistT = new ArrayList<>();
                    for (Object itemStack : invT.get("armor")) {
                        armorlistT.add(((org.bukkit.inventory.ItemStack) asCraftMirror.invoke(null, itemStack)).serialize());
                    }
                    fileConfiguration.set("armor", armorlistT);

                    List<Map<?, ?>> extraSlotsListT = new ArrayList<>();
                    for (Object itemStack : invT.get("extraSlots")) {
                        extraSlotsListT.add(((org.bukkit.inventory.ItemStack) asCraftMirror.invoke(null, itemStack)).serialize());
                    }
                    fileConfiguration.set("extraSlots", extraSlotsListT);
                    teamInvFileList.put(fileConfiguration, file);
                    sb.append("[").append(team.getName()).append("] ");
                    temp = true;
                }
                if (temp) logger.info(sb.toString());
                saveConfigs();
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void load() {
            try {
                List<Map<?, ?>> itemslist = invconfig.getMapList("items");
                for (int i = 0; i <= itemslist.size(); i++) {
                    try {
                        if (itemslist.get(i).isEmpty()) {
                            continue;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        break;
                    }
                    if (itemslist.get(i).containsKey("v") && Integer.parseInt(itemslist.get(i).get("v").toString()) > WORLD_VERSION) {
                        logger.severe("Newer version! Server downgrades are not supported!");
                        return;
                    }
                    items.set(i, asNMSCopy.invoke(null, deserialize.invoke(null, itemslist.get(i))));
                }

                List<Map<?, ?>> armorlist = invconfig.getMapList("armor");
                for (int i = 0; i <= armorlist.size(); i++) {
                    try {
                        if (armorlist.get(i).isEmpty()) {
                            continue;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        break;
                    }
                    armor.set(i, asNMSCopy.invoke(null, deserialize.invoke(null, armorlist.get(i))));
                }

                List<Map<?, ?>> extraSlotslist = invconfig.getMapList("extraSlots");
                for (int i = 0; i <= extraSlotslist.size(); i++) {
                    try {
                        if (extraSlotslist.get(i).isEmpty()) {
                            continue;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        break;
                    }
                    extraSlots.set(i, asNMSCopy.invoke(null, deserialize.invoke(null, extraSlotslist.get(i))));
                }

                List<String> alist = advconfig.getStringList("advancement");
                for (int i = 0; i <= alist.size(); i++) {
                    try {
                        advlist.add(plugin.getServer().getAdvancement((NamespacedKey) getAdvName.invoke(null, alist.get(i))).getKey());
                    } catch (IndexOutOfBoundsException e) {
                        break;
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
                    AbstractList<Object> items = (AbstractList<Object>) createItemList.invoke(null, 36, nullItem);
                    AbstractList<Object> armor = (AbstractList<Object>) createItemList.invoke(null, 4, nullItem);
                    AbstractList<Object> extraSlots = (AbstractList<Object>) createItemList.invoke(null, 1, nullItem);
                    File file = new File(new File(plugin.getDataFolder(), "\\teams"), team.getName() + ".yml");
                    if (file.exists()) {
                        FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
                        List<Map<?, ?>> itemslistT = fileConfiguration.getMapList("items");
                        for (int i = 0; i <= itemslistT.size(); i++) {
                            try {
                                if (itemslistT.get(i).isEmpty()) {
                                    continue;
                                }
                            } catch (IndexOutOfBoundsException e) {
                                break;
                            }
                            items.set(i, asNMSCopy.invoke(null, deserialize.invoke(null, itemslistT.get(i))));
                        }

                        List<Map<?, ?>> armorlistT = fileConfiguration.getMapList("armor");
                        for (int i = 0; i <= armorlistT.size(); i++) {
                            try {
                                if (armorlistT.get(i).isEmpty()) {
                                    continue;
                                }
                            } catch (IndexOutOfBoundsException e) {
                                break;
                            }
                            armor.set(i, asNMSCopy.invoke(null, deserialize.invoke(null, armorlistT.get(i))));
                        }

                        List<Map<?, ?>> extraSlotslistT = fileConfiguration.getMapList("extraSlots");
                        for (int i = 0; i <= extraSlotslistT.size(); i++) {
                            try {
                                if (extraSlotslistT.get(i).isEmpty()) {
                                    continue;
                                }
                            } catch (IndexOutOfBoundsException e) {
                                break;
                            }
                            extraSlots.set(i, asNMSCopy.invoke(null, deserialize.invoke(null, extraSlotslistT.get(i))));
                        }
                    }
                    Map<String, AbstractList<Object>> m = new HashMap<>();
                    m.put("items", items);
                    m.put("armor", armor);
                    m.put("extraSlots", extraSlots);
                    InventoryList.put(team.getName(), m);
                    sb.append("[").append(team.getName()).append("] ");
                    temp = true;
                }
                if (temp) logger.info(sb.toString());
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void deleteWasteFiles() {
            File[] files = new File(plugin.getDataFolder(), "\\teams").listFiles();
            if (files != null) {
                for (File file : files) {
                    List<String> list = new ArrayList<>();
                    for (Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
                        list.add(team.getName());
                    }
                    if (!list.contains(file.getName())) {
                        try {
                            Files.delete(file.toPath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings({"DataFlowIssue", "unchecked", "SuspiciousMethodCalls"})
    static class InventoryManager implements IInventoryManager {
        private static final Class<?> EntityPlayer;
        private static final Class<?> CraftPlayer;
        private static final Class<?> PlayerInventory;
        private static final Method CraftPlayer_getHandle;
        private static final Method inventory_method;
        private static final Field inventory_field;
        private static final Class<?> EntityHuman;

        private static final Field inv_items;
        private static final Field inv_armor;
        private static final Field inv_extraSlots;

        static {
            try {
                PlayerInventory = Class.forName(PATH_CLASS_PlayerInventory);
                EntityPlayer = Class.forName(PATH_CLASS_EntityPlayer);
                CraftPlayer = Class.forName(PATH_CLASS_CraftPlayer);
                EntityHuman = Class.forName(PATH_CLASS_EntityHuman);
                CraftPlayer_getHandle = CraftPlayer.getMethod("getHandle");
                if (DOES_INVENTORY_USE_FIELD) {
                    inventory_field = EntityHuman.getField(PATH_EntityPlayer_Inventory);
                    inventory_method = null;
                } else {
                    inventory_method = EntityPlayer.getMethod(PATH_EntityPlayer_Inventory);
                    inventory_field = null;
                }
                inv_items = PlayerInventory.getField(PATH_PlayerInventory_items);
                inv_armor = PlayerInventory.getField(PATH_PlayerInventory_armor);
                inv_extraSlots = PlayerInventory.getField(PATH_PlayerInventory_extraSlots);
            } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void applyAllInventory(@NotNull Player p) {
            try {
                Object playerInventory;
                if (DOES_INVENTORY_USE_FIELD) {
                    playerInventory = inventory_field.get(CraftPlayer_getHandle.invoke(CraftPlayer.cast(p)));
                } else {
                    playerInventory = inventory_method.invoke(CraftPlayer_getHandle.invoke(CraftPlayer.cast(p)));
                }
                try {
                    setField(playerInventory, PATH_PlayerInventory_items, items);
                    setField(playerInventory, PATH_PlayerInventory_armor, armor);
                    setField(playerInventory, PATH_PlayerInventory_extraSlots, extraSlots);
                    setField(playerInventory, PATH_PlayerInventory_contents, contents);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void disApplyInventory(@NotNull Player p) {
            try {
                Object entityPlayer = CraftPlayer_getHandle.invoke(CraftPlayer.cast(p));
                Object playerInventory;
                if (DOES_INVENTORY_USE_FIELD) {
                    playerInventory = inventory_field.get(CraftPlayer_getHandle.invoke(CraftPlayer.cast(p)));
                } else {
                    playerInventory = inventory_method.invoke(CraftPlayer_getHandle.invoke(CraftPlayer.cast(p)));
                }
                if (invList.containsKey(p.getUniqueId())) {
                    AbstractList<Object> items1 = (AbstractList<Object>) inv_items.get(invList.get(p.getUniqueId()));
                    AbstractList<Object> armor1 = (AbstractList<Object>) inv_armor.get(invList.get(p.getUniqueId()));
                    AbstractList<Object> extraSlots1 = (AbstractList<Object>) inv_extraSlots.get(invList.get(p.getUniqueId()));
                    List<AbstractList<Object>> contents1 = ImmutableList.of(items1, armor1, extraSlots1);
                    try {
                        setField(playerInventory, PATH_PlayerInventory_items, items1);
                        setField(playerInventory, PATH_PlayerInventory_armor, armor1);
                        setField(playerInventory, PATH_PlayerInventory_extraSlots, extraSlots1);
                        setField(playerInventory, PATH_PlayerInventory_contents, contents1);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    // 사용될 일이 없지만, 혹시 모른 버그 방지
                    AbstractList<Object> items1 = (AbstractList<Object>) createItemList.invoke(null, 36, nullItem);
                    AbstractList<Object> armor1 = (AbstractList<Object>) createItemList.invoke(null, 4, nullItem);
                    AbstractList<Object> extraSlots1 = (AbstractList<Object>) createItemList.invoke(null, 1, nullItem);
                    List<AbstractList<Object>> contents1 = ImmutableList.of(items1, armor1, extraSlots1);
                    try {
                        setField(playerInventory, PATH_PlayerInventory_items, items1);
                        setField(playerInventory, PATH_PlayerInventory_armor, armor1);
                        setField(playerInventory, PATH_PlayerInventory_extraSlots, extraSlots1);
                        setField(playerInventory, PATH_PlayerInventory_contents, contents1);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                invList.remove(entityPlayer);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public void applyInventory(@NotNull Player p) {
            try {
                if (!(teaminventory)) {
                    applyAllInventory(p);
                    return;
                }
                if (plugin.getServer().getScoreboardManager().getMainScoreboard().getPlayerTeam(p) == null) {
                    applyAllInventory(p);
                    return;
                }
                String teamName = plugin.getServer().getScoreboardManager().getMainScoreboard().getPlayerTeam(p).getName();
                AbstractList<Object> itemsT;
                AbstractList<Object> armorT;
                AbstractList<Object> extraSlotsT;
                if (!InventoryList.containsKey(teamName)) {
                    Map<String, AbstractList<Object>> map = new HashMap<>();
                    itemsT = (AbstractList<Object>) createItemList.invoke(null, 36, nullItem);
                    armorT = (AbstractList<Object>) createItemList.invoke(null, 4, nullItem);
                    extraSlotsT = (AbstractList<Object>) createItemList.invoke(null, 1, nullItem);
                    map.put("items", itemsT);
                    map.put("armor", armorT);
                    map.put("extraSlots", extraSlotsT);
                    InventoryList.put(teamName, map);
                } else {
                    Map<String, AbstractList<Object>> map = InventoryList.get(teamName);
                    itemsT = map.get("items");
                    armorT = map.get("armor");
                    extraSlotsT = map.get("extraSlots");
                }
                List<AbstractList<Object>> contentsT = ImmutableList.of(itemsT, armorT, extraSlotsT);
                Object playerInventory;
                if (DOES_INVENTORY_USE_FIELD) {
                    playerInventory = inventory_field.get(CraftPlayer_getHandle.invoke(CraftPlayer.cast(p)));
                } else {
                    playerInventory = inventory_method.invoke(CraftPlayer_getHandle.invoke(CraftPlayer.cast(p)));
                }
                try {
                    setField(playerInventory, PATH_PlayerInventory_items, itemsT);
                    setField(playerInventory, PATH_PlayerInventory_armor, armorT);
                    setField(playerInventory, PATH_PlayerInventory_extraSlots, extraSlotsT);
                    setField(playerInventory, PATH_PlayerInventory_contents, contentsT);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void savePlayerInventory(@NotNull Player p) {
            try {
                Object pinvsecond = PlayerInventory.getConstructor(EntityHuman).newInstance((Object) null);
                Object entityPlayer = CraftPlayer_getHandle.invoke(CraftPlayer.cast(p));
                Object pinvfirst;
                if (DOES_INVENTORY_USE_FIELD) {
                    pinvfirst = inventory_field.get(entityPlayer);
                } else {
                    pinvfirst = inventory_method.invoke(entityPlayer);
                }
                try {
                    setField(pinvsecond, PATH_PlayerInventory_items, inv_items.get(pinvfirst));
                    setField(pinvsecond, PATH_PlayerInventory_armor, inv_armor.get(pinvfirst));
                    setField(pinvsecond, PATH_PlayerInventory_extraSlots, inv_extraSlots.get(pinvfirst));
                    setField(pinvsecond, PATH_PlayerInventory_contents, ImmutableList.of(inv_items.get(pinvfirst), inv_armor.get(pinvfirst), inv_extraSlots.get(pinvfirst)));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                invList.put(p.getUniqueId(), pinvsecond);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException |
                     NoSuchMethodException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }

        public Set<UUID> getRegisteredPlayers() {
            return invList.keySet();
        }
    }
}
