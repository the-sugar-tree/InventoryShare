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
package com.sugar_tree.inventoryshare.nms.utils;

import com.google.common.collect.ImmutableSet;
import com.sugar_tree.inventoryshare.SharedConstants;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public class VersionUtil {
    private static final SupportedVersions version;
    private static final boolean supported;
    static {
        version = SupportedVersions.getFromBukkitVersion(Bukkit.getBukkitVersion());
        supported = (version != null);
    }

    public static SupportedVersions getVersion() {
        return version;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isSupported() {
        return supported;
    }

    public enum SupportedVersions {
        v1_20_R1(ImmutableSet.of("1.20-R0.1-SNAPSHOT", "1.20.1-R0.1-SNAPSHOT"),
                "fN", "i", "j", "k", "o"),
        v1_19_R3(ImmutableSet.of("1.19.4-R0.1-SNAPSHOT"),
                "fJ", "i", "j", "k", "o"),
        v1_19_R2(ImmutableSet.of("1.19.3-R0.1-SNAPSHOT"),
                "fE", "h", "i", "j", "n"),
        v1_19_1_R1(ImmutableSet.of("1.19.2-R0.1-SNAPSHOT", "1.19.1-R0.1-SNAPSHOT"),
                "fA", "h", "i", "j", "n"),
        v1_19_R1(ImmutableSet.of("1.19-R0.1-SNAPSHOT"),
                "fB", "h", "i", "j", "n"),
        v1_18_R2(ImmutableSet.of("1.18.2-R0.1-SNAPSHOT", "1.18.1-R0.1-SNAPSHOT"),
                "fr", "h", "i", "j", "n"),
        v1_18_R1(ImmutableSet.of("1.18-R0.1-SNAPSHOT"),
                "fq", "h", "i", "j", "n"),
        v1_17_R1(ImmutableSet.of("1.17-R0.1-SNAPSHOT", "1.17.1-R0.1-SNAPSHOT"),
                "getInventory", "h", "i", "j", "n"),
        v1_16_R3(ImmutableSet.of("1.16.5-R0.1-SNAPSHOT", "1.16.4-R0.1-SNAPSHOT"),
                "inventory", "items", "armor", "extraSlots", "f"),
        v1_16_R2(ImmutableSet.of("1.16.3-R0.1-SNAPSHOT", "1.16.2-R0.1-SNAPSHOT"),
                "inventory", "items", "armor", "extraSlots", "f"),
        v1_16_R1(ImmutableSet.of("1.16.1-R0.1-SNAPSHOT"),
                "inventory", "items", "armor", "extraSlots", "f"),
        v1_15_R1(ImmutableSet.of("1.15.2-R0.1-SNAPSHOT", "1.15.1-R0.1-SNAPSHOT", "1.15-R0.1-SNAPSHOT"),
                "inventory", "items", "armor", "extraSlots", "f"),
        v1_14_R1(ImmutableSet.of("1.14.4-R0.1-SNAPSHOT", "1.14.3-R0.1-SNAPSHOT", "1.14.2-R0.1-SNAPSHOT", "1.14.1-R0.1-SNAPSHOT", "1.14-R0.1-SNAPSHOT"),
                "inventory", "items", "armor", "extraSlots", "f"),
        v1_13_R2(ImmutableSet.of("1.13.2-R0.1-SNAPSHOT"),
                "inventory", "items", "armor", "extraSlots", "f"),
        v1_13_R1(ImmutableSet.of("1.13.1-R0.1-SNAPSHOT", "1.13-R0.1-SNAPSHOT"),
                "inventory", "items", "armor", "extraSlots", "f"),
        v1_12_R1(ImmutableSet.of("1.12.2-R0.1-SNAPSHOT", "1.12.1-R0.1-SNAPSHOT", "1.12-R0.1-SNAPSHOT"),
                "inventory", "items", "armor", "extraSlots", "f");



        private final ImmutableSet<String> versions;
        private final String PATH_CLASS_PlayerInventory;
        private final String PATH_CLASS_ItemStack;
        private final String PATH_CLASS_NonNullList;
        private final String PATH_METHOD_createItemlist;
        private final String PATH_FIELD_emptyItem;
        private final String PATH_METHOD_getNameSpacedKey;
        //*****************************************************************************************************************//
        private final String PATH_CLASS_EntityPlayer;
        private final String PATH_CLASS_CraftPlayer;
        private final boolean DOES_INVENTORY_USE_FIELD;
        private final String PATH_EntityPlayer_Inventory;
        private final String PATH_PlayerInventory_items;
        private final String PATH_PlayerInventory_armor;
        private final String PATH_PlayerInventory_extraSlots;
        private final String PATH_PlayerInventory_contents;
        private final String PATH_CLASS_EntityHuman;

        public ImmutableSet<String> getVersions() {
            return versions;
        }

        SupportedVersions(ImmutableSet<String> versions, String... args) {
            this.versions = versions;
            if (args.length != 5) {
                throw new IllegalArgumentException("The number of arguments is not satisfied");
            }
            //1.17+
            if (SharedConstants.WORLD_VERSION >= 2724) {
                PATH_CLASS_PlayerInventory = "net.minecraft.world.entity.player.PlayerInventory";
                PATH_CLASS_ItemStack = "net.minecraft.world.item.ItemStack";
                PATH_CLASS_NonNullList = "net.minecraft.core.NonNullList";
                PATH_METHOD_getNameSpacedKey = "fromString";
                PATH_CLASS_EntityPlayer = "net.minecraft.server.level.EntityPlayer";
                DOES_INVENTORY_USE_FIELD = false;
                PATH_EntityPlayer_Inventory = args[0];
                PATH_PlayerInventory_items = args[1];
                PATH_PlayerInventory_armor = args[2];
                PATH_PlayerInventory_extraSlots = args[3];
                PATH_PlayerInventory_contents = args[4];
                PATH_CLASS_EntityHuman = "net.minecraft.world.entity.player.EntityHuman";
            } else {
                PATH_CLASS_PlayerInventory = "net.minecraft.server." + name() + ".PlayerInventory";
                PATH_CLASS_ItemStack = "net.minecraft.server."+ name() + ".ItemStack";
                PATH_CLASS_NonNullList = "net.minecraft.server." + name() + ".NonNullList";
                PATH_METHOD_getNameSpacedKey = "minecraft";
                PATH_CLASS_EntityPlayer = "org.bukkit.craftbukkit." + name() + ".entity.CraftPlayer";
                DOES_INVENTORY_USE_FIELD = true;
                PATH_EntityPlayer_Inventory = args[0];
                PATH_PlayerInventory_items = args[1];
                PATH_PlayerInventory_armor = args[2];
                PATH_PlayerInventory_extraSlots = args[3];
                PATH_PlayerInventory_contents = args[4];
                PATH_CLASS_EntityHuman = "net.minecraft.server." + name() + ".EntityHuman";
            }
            //1.16+
            if (SharedConstants.WORLD_VERSION >= 2566) {
                PATH_METHOD_createItemlist = "a";
                PATH_FIELD_emptyItem = "b";
            } else {
                PATH_METHOD_createItemlist = "a";
                PATH_FIELD_emptyItem = "a";
            }
            String name = name();
            if (name.equals("v1_19_1_R1")) {
                name = "v1_19_R1";
            }
            PATH_CLASS_CraftPlayer = "org.bukkit.craftbukkit." + name + ".entity.CraftPlayer";
        }

        /**
         * @param bukkitVersion {@link Bukkit#getBukkitVersion()}
         * @return {@link SupportedVersions} value or null if not supported
         */
        public static @Nullable VersionUtil.SupportedVersions getFromBukkitVersion(String bukkitVersion) {
            for (SupportedVersions value : SupportedVersions.values()) {
                if (value.getVersions().contains(bukkitVersion)) {
                    return value;
                }
            }
            return null;
        }

        public String getPATH_CLASS_PlayerInventory() {
            return PATH_CLASS_PlayerInventory;
        }
        public String getPATH_CLASS_ItemStack() {
            return PATH_CLASS_ItemStack;
        }
        public String getPATH_CLASS_NonNullList() {
            return PATH_CLASS_NonNullList;
        }
        public String getPATH_METHOD_createItemlist() {
            return PATH_METHOD_createItemlist;
        }
        public String getPATH_FIELD_emptyItem() {
            return PATH_FIELD_emptyItem;
        }
        public String getPATH_METHOD_getNameSpacedKey() {
            return PATH_METHOD_getNameSpacedKey;
        }
        public String getPATH_CLASS_EntityPlayer() {
            return PATH_CLASS_EntityPlayer;
        }
        public String getPATH_CLASS_CraftPlayer() {
            return PATH_CLASS_CraftPlayer;
        }
        public boolean getDOES_INVENTORY_USE_FIELD() {
            return DOES_INVENTORY_USE_FIELD;
        }
        public String getPATH_EntityPlayer_Inventory() {
            return PATH_EntityPlayer_Inventory;
        }
        public String getPATH_PlayerInventory_items() {
            return PATH_PlayerInventory_items;
        }
        public String getPATH_PlayerInventory_armor() {
            return PATH_PlayerInventory_armor;
        }
        public String getPATH_PlayerInventory_extraSlots() {
            return PATH_PlayerInventory_extraSlots;
        }
        public String getPATH_PlayerInventory_contents() {
            return PATH_PlayerInventory_contents;
        }
        public String getPATH_CLASS_EntityHuman() {
            return PATH_CLASS_EntityHuman;
        }
    }
}
