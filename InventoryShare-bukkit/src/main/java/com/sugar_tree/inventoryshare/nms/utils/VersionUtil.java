/*
 * This file is part of InventoryShare, licensed under the GPL-3.0 License.
 *
 * InventoryShare the minecraft plugin that enables sharing inventory with ohter players
 * Copyright (c) 2023 the-sugar-tree
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

package com.sugar_tree.inventoryshare.nms.utils;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class VersionUtil {

    private VersionUtil() {}

    private static SupportedVersions version;
    private static boolean versionResolved;
    private static String parsedServerVersion;

    /**
     * Returns a Spigot-style Bukkit API version string used to resolve {@link SupportedVersions}.
     *
     * <p>On Spigot (and Paper builds without {@code getMinecraftVersion}), this returns
     * {@link Bukkit#getBukkitVersion()} unchanged, e.g. {@code "1.21.5-R0.1-SNAPSHOT"}.
     *
     * <p>On Paper 1.15.2+, {@link Server#getMinecraftVersion()} is called via reflection and
     * suffixed with {@code "-R0.1-SNAPSHOT"} so the result matches the strings stored in
     * {@link SupportedVersions}. That avoids Paper's alternate {@link Bukkit#getBukkitVersion()}
     * format on Minecraft 26.x (e.g. {@code "26.1.2.build.63-stable"}).
     *
     * <p>{@code getMinecraftVersion()} is a Paper extension, not Spigot API; reflection keeps this
     * module compilable against {@code spigot-api} only.
     *
     * @return a version string for {@link SupportedVersions} lookup, e.g.
     *         {@code "1.21.5-R0.1-SNAPSHOT"} or {@code "26.1.2-R0.1-SNAPSHOT"}
     * @throws RuntimeException if the reflected {@code getMinecraftVersion()} fails unexpectedly
     * @throws Error if rethrown from the reflected method
     * @see <a href="https://github.com/PaperMC/Paper/issues/13885">PaperMC/Paper#13885</a>
     */
    @SuppressWarnings("JavaReflectionMemberAccess")
    public static String parseServerVersion() {
        if (parsedServerVersion != null) return parsedServerVersion;
        String mc;
        try {
            Method m = Server.class.getMethod("getMinecraftVersion");
            mc = (String) m.invoke(Bukkit.getServer());
            mc += "-R0.1-SNAPSHOT";
        } catch (NoSuchMethodException | IllegalAccessException e) {
            mc = Bukkit.getBukkitVersion();
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new RuntimeException(cause);
        }
        parsedServerVersion = mc;
        return mc;
    }

    /**
     * Returns the NMS mapping set detected at plugin load from the running server.
     *
     * @return the matching {@link SupportedVersions}, or {@code null} if
     *         {@link #parseServerVersion()} does not match any supported release
     */
    public static SupportedVersions getVersion() {
        if (!versionResolved) {
            version = SupportedVersions.getFromBukkitVersion(parseServerVersion());
            versionResolved = true;
        }
        return version;
    }

    /**
     * NMS member names for the items/armor/extraSlots inventory model (~1.12–1.21.4).
     */
    private static final class NmsLayout {
        private final boolean flattened;
        private final String inventory;
        private final String items;
        private final String armor;
        private final String extraSlots;
        private final String contents;
        private final String emptyItem;

        private NmsLayout(boolean flattened, String inventory, String items, String armor,
                          String extraSlots, String contents, String emptyItem) {
            this.flattened = flattened;
            this.inventory = inventory;
            this.items = items;
            this.armor = armor;
            this.extraSlots = extraSlots;
            this.contents = contents;
            this.emptyItem = emptyItem;
        }
    }

    private static ImmutableSet<String> bukkit(String... versions) {
        return ImmutableSet.copyOf(versions);
    }

    /** Pre-1.17: {@code net.minecraft.server.vX_Y_RZ} packages, inventory is a field. */
    private static NmsLayout legacy(String inventory, String items, String armor,
                                    String extraSlots, String contents, String emptyItem) {
        return new NmsLayout(false, inventory, items, armor, extraSlots, contents, emptyItem);
    }

    /** 1.17+: flattened NMS packages, inventory is a method. */
    private static NmsLayout flattened(String inventory, String items, String armor,
                                       String extraSlots, String contents, String emptyItem) {
        return new NmsLayout(true, inventory, items, armor, extraSlots, contents, emptyItem);
    }

    @Getter
    public enum SupportedVersions {
        v1_12_R1(bukkit("1.12.2-R0.1-SNAPSHOT", "1.12.1-R0.1-SNAPSHOT", "1.12-R0.1-SNAPSHOT"),
                legacy("inventory", "items", "armor", "extraSlots", "f", "a")),
        v1_13_R1(bukkit("1.13-R0.1-SNAPSHOT"),
                legacy("inventory", "items", "armor", "extraSlots", "f", "a")),
        v1_13_R2(bukkit("1.13.2-R0.1-SNAPSHOT", "1.13.1-R0.1-SNAPSHOT"),
                legacy("inventory", "items", "armor", "extraSlots", "f", "a")),
        v1_14_R1(bukkit("1.14.4-R0.1-SNAPSHOT", "1.14.3-R0.1-SNAPSHOT", "1.14.2-R0.1-SNAPSHOT", "1.14.1-R0.1-SNAPSHOT", "1.14-R0.1-SNAPSHOT"),
                legacy("inventory", "items", "armor", "extraSlots", "f", "a")),
        v1_15_R1(bukkit("1.15.2-R0.1-SNAPSHOT", "1.15.1-R0.1-SNAPSHOT", "1.15-R0.1-SNAPSHOT"),
                legacy("inventory", "items", "armor", "extraSlots", "f", "a")),
        v1_16_R1(bukkit("1.16.1-R0.1-SNAPSHOT"),
                legacy("inventory", "items", "armor", "extraSlots", "f", "b")),
        v1_16_R2(bukkit("1.16.3-R0.1-SNAPSHOT", "1.16.2-R0.1-SNAPSHOT"),
                legacy("inventory", "items", "armor", "extraSlots", "f", "b")),
        v1_16_R3(bukkit("1.16.5-R0.1-SNAPSHOT", "1.16.4-R0.1-SNAPSHOT"),
                legacy("inventory", "items", "armor", "extraSlots", "f", "b")),
        v1_17_R1(bukkit("1.17-R0.1-SNAPSHOT", "1.17.1-R0.1-SNAPSHOT"),
                flattened("getInventory", "h", "i", "j", "n", "b")),
        v1_18_R1(bukkit("1.18-R0.1-SNAPSHOT"),
                flattened("fq", "h", "i", "j", "n", "b")),
        v1_18_R2(bukkit("1.18.2-R0.1-SNAPSHOT", "1.18.1-R0.1-SNAPSHOT"),
                flattened("fr", "h", "i", "j", "n", "b")),
        v1_19_R1(bukkit("1.19-R0.1-SNAPSHOT"),
                flattened("fB", "h", "i", "j", "n", "b")),
        v1_19_1_R1(bukkit("1.19.2-R0.1-SNAPSHOT", "1.19.1-R0.1-SNAPSHOT"),
                flattened("fA", "h", "i", "j", "n", "b")),
        v1_19_R2(bukkit("1.19.3-R0.1-SNAPSHOT"),
                flattened("fE", "h", "i", "j", "n", "b")),
        v1_19_R3(bukkit("1.19.4-R0.1-SNAPSHOT"),
                flattened("fJ", "i", "j", "k", "o", "b")),
        v1_20_R1(bukkit("1.20-R0.1-SNAPSHOT", "1.20.1-R0.1-SNAPSHOT"),
                flattened("fN", "i", "j", "k", "o", "b")),
        v1_20_R2(bukkit("1.20.2-R0.1-SNAPSHOT"),
                flattened("fR", "i", "j", "k", "o", "b")),
        v1_20_R3(bukkit("1.20.4-R0.1-SNAPSHOT", "1.20.3-R0.1-SNAPSHOT"),
                flattened("fS", "i", "j", "k", "o", "f")),
        v1_20_R4(bukkit("1.20.6-R0.1-SNAPSHOT", "1.20.5-R0.1-SNAPSHOT"),
                flattened("gc", "h", "i", "j", "n", "l")),
        v1_21_R1(bukkit("1.21.1-R0.1-SNAPSHOT", "1.21-R0.1-SNAPSHOT"),
                flattened("fY", "h", "i", "j", "n", "l")),
        v1_21_R2(bukkit("1.21.3-R0.1-SNAPSHOT", "1.21.2-R0.1-SNAPSHOT"),
                flattened("gi", "g", "h", "i", "l", "j")),
        v1_21_R3(bukkit("1.21.4-R0.1-SNAPSHOT"),
                flattened("gi", "g", "h", "i", "l", "j")),
        /**
         * 1.21.5 lookup only. The live inventory model uses {@code EntityEquipment},
         * not items/armor/extraSlots. Do not extend this table for 1.21.5+/26.2.
         */
        v1_21_R4(bukkit("1.21.5-R0.1-SNAPSHOT"),
                flattened("gj", "g", "h", "i", "l", "l")),
        ;

        private final ImmutableSet<String> versions;
        private final String PATH_CLASS_PlayerInventory;
        private final String PATH_CLASS_ItemStack;
        private final String PATH_CLASS_NonNullList;
        private final String PATH_METHOD_createItemlist;
        private final String PATH_FIELD_emptyItem;
        private final String PATH_METHOD_getNameSpacedKey;
        private final String PATH_CLASS_EntityPlayer;
        private final String PATH_CLASS_CraftPlayer;
        private final String PATH_CLASS_CraftItemStack;
        private final boolean INVENTORY_USE_FIELD;
        private final String PATH_EntityPlayer_Inventory;
        private final String PATH_PlayerInventory_items;
        private final String PATH_PlayerInventory_armor;
        private final String PATH_PlayerInventory_extraSlots;
        private final String PATH_PlayerInventory_contents;
        private final String PATH_CLASS_EntityHuman;

        SupportedVersions(ImmutableSet<String> versions, NmsLayout nms) {
            this.versions = versions;
            String craftbukkitPackage = name();
            if ("v1_19_1_R1".equals(craftbukkitPackage)) {
                craftbukkitPackage = "v1_19_R1";
            }
            PATH_EntityPlayer_Inventory = nms.inventory;
            PATH_PlayerInventory_items = nms.items;
            PATH_PlayerInventory_armor = nms.armor;
            PATH_PlayerInventory_extraSlots = nms.extraSlots;
            PATH_PlayerInventory_contents = nms.contents;
            PATH_FIELD_emptyItem = nms.emptyItem;
            PATH_METHOD_createItemlist = "a";
            INVENTORY_USE_FIELD = !nms.flattened;
            PATH_CLASS_CraftPlayer = "org.bukkit.craftbukkit." + craftbukkitPackage + ".entity.CraftPlayer";
            PATH_CLASS_CraftItemStack = "org.bukkit.craftbukkit." + craftbukkitPackage + ".inventory.CraftItemStack";
            if (nms.flattened) {
                PATH_CLASS_PlayerInventory = "net.minecraft.world.entity.player.PlayerInventory";
                PATH_CLASS_ItemStack = "net.minecraft.world.item.ItemStack";
                PATH_CLASS_NonNullList = "net.minecraft.core.NonNullList";
                PATH_METHOD_getNameSpacedKey = "fromString";
                PATH_CLASS_EntityPlayer = "net.minecraft.server.level.EntityPlayer";
                PATH_CLASS_EntityHuman = "net.minecraft.world.entity.player.EntityHuman";
            } else {
                PATH_CLASS_PlayerInventory = "net.minecraft.server." + craftbukkitPackage + ".PlayerInventory";
                PATH_CLASS_ItemStack = "net.minecraft.server." + craftbukkitPackage + ".ItemStack";
                PATH_CLASS_NonNullList = "net.minecraft.server." + craftbukkitPackage + ".NonNullList";
                PATH_METHOD_getNameSpacedKey = "minecraft";
                PATH_CLASS_EntityPlayer = "org.bukkit.craftbukkit." + craftbukkitPackage + ".entity.CraftPlayer";
                PATH_CLASS_EntityHuman = "net.minecraft.server." + craftbukkitPackage + ".EntityHuman";
            }
        }

        /**
         * @param bukkitVersion normalized API version from {@link #parseServerVersion()},
         *                      e.g. {@code "1.21.5-R0.1-SNAPSHOT"}
         * @return matching enum constant, or {@code null} if unsupported
         */
        private static @Nullable VersionUtil.SupportedVersions getFromBukkitVersion(String bukkitVersion) {
            for (SupportedVersions value : SupportedVersions.values()) {
                if (value.getVersions().contains(bukkitVersion)) {
                    return value;
                }
            }
            return null;
        }
    }
}
