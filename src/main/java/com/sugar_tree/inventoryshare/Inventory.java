/*
 * Copyright (c) 2021 the-sugar-tree
 *
 *  Licensed under the General Public License, Version 3.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/gpl-3.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sugar_tree.inventoryshare;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.sugar_tree.inventoryshare.InventoryShare.*;

public class Inventory {
    final static Plugin plugin = getPlugin(InventoryShare.class);

    private static final NonNullList<ItemStack> items = NonNullList.a(36, ItemStack.b);
    private static final NonNullList<ItemStack> armor = NonNullList.a(4, ItemStack.b);
    private static final NonNullList<ItemStack> extraSlots = NonNullList.a(1, ItemStack.b);

    private static final List<NonNullList<ItemStack>> contents = ImmutableList.of(items, armor, extraSlots);

    public static void invPatch(Player p) {
        if (inventory) {
            EntityPlayer entityPlayer = ((CraftPlayer) p).getHandle();
            PlayerInventory playerInventory = entityPlayer.getInventory();
            try {
                setField(playerInventory, "h", items);
                setField(playerInventory, "i", armor);
                setField(playerInventory, "j", extraSlots);
                setField(playerInventory, "n", contents);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private static void setField(Object obj, String name, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(obj, value);
    }

    public static void save() {
        List<Map<?, ?>> itemslist = new ArrayList<>();
        for (ItemStack itemStack1 : items) {
            itemslist.add(CraftItemStack.asCraftMirror(itemStack1).serialize());
        }
        invconfig.set("items", itemslist);

        List<Map<?, ?>> armorlist = new ArrayList<>();
        for (ItemStack itemStack2 : armor) {
            armorlist.add(CraftItemStack.asCraftMirror(itemStack2).serialize());
        }
        invconfig.set("armor", armorlist);

        List<Map<?, ?>> extraSlotsList = new ArrayList<>();
        for (ItemStack itemStack3 : extraSlots) {
            extraSlotsList.add(CraftItemStack.asCraftMirror(itemStack3).serialize());
        }
        invconfig.set("extraSlots", extraSlotsList);

        List<String> alist = new ArrayList<>();
        for (NamespacedKey namespacedKey : advlist) {
            alist.add(namespacedKey.getKey());
        }
        advconfig.set("advancement", alist);

//        plugin.getConfig().set("inventory", inventory);
        plugin.getConfig().set("advancement", advancement);
        plugin.getConfig().set("AnnounceDeath", AnnounceDeath);
        saveConfigs();
    }

    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored", "ConstantConditions"})
    public static void load() {
        var itemslist = invconfig.getMapList("items");
        for (int i = 0; i <= itemslist.size(); i++) {
            try { itemslist.get(i); } catch (IndexOutOfBoundsException e) { break; }
            if (itemslist.get(i).isEmpty()) {
                continue;
            }
            items.set(i, CraftItemStack.asNMSCopy(CraftItemStack.deserialize((Map<String, Object>) itemslist.get(i))));
        }

        var armorlist = invconfig.getMapList("armor");
        for (int i = 0; i <= armorlist.size(); i++) {
            try { armorlist.get(i); } catch (IndexOutOfBoundsException e) { break; }
            if (armorlist.get(i).isEmpty()) {
                continue;
            }
            armor.set(i, CraftItemStack.asNMSCopy(CraftItemStack.deserialize((Map<String, Object>) armorlist.get(i))));
        }

        var extraSlotslist = invconfig.getMapList("extraSlots");
        for (int i = 0; i <= extraSlotslist.size(); i++) {
            try { extraSlotslist.get(i); } catch (IndexOutOfBoundsException e) { break; }
            if (extraSlotslist.get(i).isEmpty()) {
                continue;
            }
            extraSlots.set(i, CraftItemStack.asNMSCopy(CraftItemStack.deserialize((Map<String, Object>) extraSlotslist.get(i))));
        }

        var alist = advconfig.getStringList("advancement");
        for (int i = 0; i <= alist.size(); i++) {
            try { alist.get(i); } catch (IndexOutOfBoundsException e) { break; }
            advlist.add(plugin.getServer().getAdvancement(NamespacedKey.fromString(alist.get(i))).getKey());
        }

//        if (plugin.getConfig().contains("inventory")) {
//            inventory = plugin.getConfig().getBoolean("inventory");
//        }
        inventory = true;
        if (plugin.getConfig().contains("advancement")) {
            advancement = plugin.getConfig().getBoolean("advancement");
        }
        if (plugin.getConfig().contains("AnnounceDeath")) {
            AnnounceDeath = plugin.getConfig().getBoolean("AnnounceDeath");
        }
    }
}
