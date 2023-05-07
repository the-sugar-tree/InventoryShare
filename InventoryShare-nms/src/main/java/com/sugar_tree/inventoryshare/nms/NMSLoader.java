package com.sugar_tree.inventoryshare.nms;

import com.google.common.collect.ImmutableList;
import com.sugar_tree.inventoryshare.api.FileManager;
import com.sugar_tree.inventoryshare.api.Inventory;
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
import static com.sugar_tree.inventoryshare.nms.NMSLoader.FileManagerLoader.*;

public class NMSLoader {

    // FIXME: This MUST BE fixed as it is hardcoded for version 1.19.4.
    protected static final String PATH_CLASS_PlayerInventory = "net.minecraft.world.entity.player.PlayerInventory";
    protected static final String PATH_CLASS_ItemStack = "net.minecraft.world.item.ItemStack";
    protected static final String PATH_CLASS_NonNullList = "net.minecraft.core.NonNullList";
    protected static final String PATH_METHOD_createItemlist = "a";
    protected static final String PATH_FIELD_emptyItem = "b";
    protected static final String version = VersionUtil.getVersion().name();
    protected static final String PATH_METHOD_getNameSpacedKey = "minecraft";
    /*****************************************************************************************************************/
    protected static final String PATH_CLASS_EntityPlayer = "net.minecraft.server.level.EntityPlayer";
    protected static final String PATH_CLASS_CraftPlayer = "org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer";
    protected static final boolean DOES_INVENTORY_USE_FIELD = false;
    protected static final String PATH_EntityPlayer_Inventory = "fJ";
    protected static final String PATH_PlayerInventory_items = "i";
    protected static final String PATH_PlayerInventory_armor = "j";
    protected static final String PATH_PlayerInventory_extraSlots = "k";
    protected static final String PATH_PlayerInventory_contents = "o";
    protected static final String PATH_CLASS_EntityHuman = "net.minecraft.world.entity.player.EntityHuman";
    // FIXME: --END HERE

    private NMSLoader() {}

    public static void init() {
        FileManagerClass = new FileManagerLoader();
        InventoryClass = new InventoryLoader();
    }

    @SuppressWarnings({"unchecked", "JavaReflectionInvocation"})
    static class FileManagerLoader implements FileManager {

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
                plugin.getConfig().set("AnnounceDeath", AnnounceDeath);
                plugin.getConfig().set("teaminventory", teaminventory);
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
                }
                saveConfigs();
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public void load() {
            try {
                List<Map<?, ?>> itemslist = invconfig.getMapList("items");
                for (int i = 0; i <= itemslist.size(); i++) {
                    try {
                        itemslist.get(i);
                    } catch (IndexOutOfBoundsException e) {
                        break;
                    }
                    if (itemslist.get(i).isEmpty()) {
                        continue;
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
                        armorlist.get(i);
                    } catch (IndexOutOfBoundsException e) {
                        break;
                    }
                    if (armorlist.get(i).isEmpty()) {
                        continue;
                    }
                    armor.set(i, asNMSCopy.invoke(null, deserialize.invoke(null, armorlist.get(i))));
                }

                List<Map<?, ?>> extraSlotslist = invconfig.getMapList("extraSlots");
                for (int i = 0; i <= extraSlotslist.size(); i++) {
                    try {
                        extraSlotslist.get(i);
                    } catch (IndexOutOfBoundsException e) {
                        break;
                    }
                    if (extraSlotslist.get(i).isEmpty()) {
                        continue;
                    }
                    extraSlots.set(i, asNMSCopy.invoke(null, deserialize.invoke(null, extraSlotslist.get(i))));
                }

                List<String> alist = advconfig.getStringList("advancement");
                for (int i = 0; i <= alist.size(); i++) {
                    try {
                        alist.get(i);
                    } catch (IndexOutOfBoundsException e) {
                        break;
                    }
                    advlist.add(plugin.getServer().getAdvancement((NamespacedKey) getAdvName.invoke(null, alist.get(i))).getKey());
                }

                if (plugin.getConfig().contains("inventory")) {
                    inventory = plugin.getConfig().getBoolean("inventory");
                }
                if (plugin.getConfig().contains("advancement")) {
                    advancement = plugin.getConfig().getBoolean("advancement");
                }
                if (plugin.getConfig().contains("AnnounceDeath")) {
                    AnnounceDeath = plugin.getConfig().getBoolean("AnnounceDeath");
                }
                if (plugin.getConfig().contains("teaminventory")) {
                    teaminventory = plugin.getConfig().getBoolean("teaminventory");
                }

                for (Team team : Bukkit.getServer().getScoreboardManager().getMainScoreboard().getTeams()) {
                    AbstractList<Object> items = (AbstractList<Object>) createItemList.invoke(36, nullItem);
                    AbstractList<Object> armor = (AbstractList<Object>) createItemList.invoke(4, nullItem);
                    AbstractList<Object> extraSlots = (AbstractList<Object>) createItemList.invoke(1, nullItem);
                    File file = new File(new File(plugin.getDataFolder(), "\\teams"), team.getName() + ".yml");
                    if (file.exists()) {
                        FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
                        List<Map<?, ?>> itemslistT = fileConfiguration.getMapList("items");
                        for (int i = 0; i <= itemslistT.size(); i++) {
                            try {
                                itemslistT.get(i);
                            } catch (IndexOutOfBoundsException e) {
                                break;
                            }
                            if (itemslistT.get(i).isEmpty()) {
                                continue;
                            }
                            items.set(i, asNMSCopy.invoke(null, deserialize.invoke(null, itemslistT.get(i))));
                        }

                        List<Map<?, ?>> armorlistT = fileConfiguration.getMapList("armor");
                        for (int i = 0; i <= armorlistT.size(); i++) {
                            try {
                                armorlistT.get(i);
                            } catch (IndexOutOfBoundsException e) {
                                break;
                            }
                            if (armorlistT.get(i).isEmpty()) {
                                continue;
                            }
                            armor.set(i, asNMSCopy.invoke(null, deserialize.invoke(null, armorlistT.get(i))));
                        }

                        List<Map<?, ?>> extraSlotslistT = fileConfiguration.getMapList("extraSlots");
                        for (int i = 0; i <= extraSlotslistT.size(); i++) {
                            try {
                                extraSlotslistT.get(i);
                            } catch (IndexOutOfBoundsException e) {
                                break;
                            }
                            if (extraSlotslistT.get(i).isEmpty()) {
                                continue;
                            }
                            extraSlots.set(i, asNMSCopy.invoke(null, deserialize.invoke(null, extraSlotslistT.get(i))));
                        }
                    }
                    Map<String, AbstractList<Object>> m = new HashMap<>();
                    m.put("items", items);
                    m.put("armor", armor);
                    m.put("extraSlots", extraSlots);
                    InventoryList.put(team.getName(), m);
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

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

    @SuppressWarnings({"JavaReflectionMemberAccess", "DataFlowIssue", "unchecked", "SuspiciousMethodCalls"})
    static class InventoryLoader implements Inventory {
        private static final Class<?> EntityPlayer;
        private static final Class<?> CraftPlayer;
        private static final Class<?> PlayerInventory;
        private static final Method CraftPlayer_getHandle;
        private static final Method inventory_method;
        private static final Field inventory_field;

        private static final Field inv_items;
        private static final Field inv_armor;
        private static final Field inv_extraSlots;

        static {
            try {
                PlayerInventory = Class.forName(PATH_CLASS_PlayerInventory);
                EntityPlayer = Class.forName(PATH_CLASS_EntityPlayer);
                CraftPlayer = Class.forName(PATH_CLASS_CraftPlayer);
                CraftPlayer_getHandle = CraftPlayer.getMethod("getHandle");
                if (DOES_INVENTORY_USE_FIELD) {
                    inventory_field = EntityPlayer.getField(PATH_EntityPlayer_Inventory);
                    inventory_method = null;
                }
                else {
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

        public void invApplyAll(@NotNull Player p) {
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

        public void invDisApply(@NotNull Player p) {
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
        public void invApply(@NotNull Player p) {
            try {
                if (!(teaminventory)) {
                    invApplyAll(p);
                    return;
                }
                if (plugin.getServer().getScoreboardManager().getMainScoreboard().getPlayerTeam(p) == null) {
                    invApplyAll(p);
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

        public void savePlayerInventory(@NotNull Player p) {
            try {
                Object pinvsecond = PlayerInventory.getConstructor(Class.forName(PATH_CLASS_EntityHuman)).newInstance((Object) null);
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
                     ClassNotFoundException | NoSuchMethodException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }

        public Set<UUID> getRegisteredPlayers() {
            return invList.keySet();
        }
    }
}
