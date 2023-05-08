package com.sugar_tree.inventoryshare.nms.util;

import com.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public class VersionUtil {
    private static final SupportedVersions version;
    private static final boolean supported;
    private static final byte unusual;
    static {
        version = SupportedVersions.getFromBukkitVersion(Bukkit.getBukkitVersion());
        byte t = 0;
        if (version != null) {
            if (version == SupportedVersions.v1_19_R1) {
                if (Bukkit.getBukkitVersion().equals("1.19-R0.1-SNAPSHOT")) t = 1;
            }
//            switch (version) {
//                case v1_19_R1:
//                    if (Bukkit.getBukkitVersion().equals("1.19-R0.1-SNAPSHOT")) t = 1;
//            }
        }
        unusual = t;
        supported = (version != null);
    }

    public static SupportedVersions getVersion() {
        return version;
    }

    public static int getUnusual() {
        return unusual;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isSupported() {
        return supported;
    }

    public enum SupportedVersions {
        v1_19_R3(ImmutableSet.of("1.19.4-R0.1-SNAPSHOT"), "net.minecraft.world.entity.player.PlayerInventory",
                "net.minecraft.world.item.ItemStack", "net.minecraft.core.NonNullList", "a", "b", "minecraft",
                "net.minecraft.server.level.EntityPlayer", "org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer", "false",
                "fJ", "i", "j", "k", "o", "net.minecraft.world.entity.player.EntityHuman"),
        v1_19_R2(ImmutableSet.of("1.19.3-R0.1-SNAPSHOT")),
        /**
         * v1_19_1_R1 -> 1.19.2, 1.19.1<br>
         * v1_19_R1 -> 1.19
         */
        v1_19_R1(ImmutableSet.of("1.19.2-R0.1-SNAPSHOT", "1.19.1-R0.1-SNAPSHOT", "1.19-R0.1-SNAPSHOT")),
        v1_18_R2(ImmutableSet.of("1.18.2-R0.1-SNAPSHOT", "1.18.1-R0.1-SNAPSHOT")),
        v1_18_R1(ImmutableSet.of("1.18-R0.1-SNAPSHOT")),
        v1_17_R1(ImmutableSet.of("1.17-R0.1-SNAPSHOT", "1.17.1-R0.1-SNAPSHOT")),
        v1_16_R3(ImmutableSet.of("1.16.5-R0.1-SNAPSHOT", "1.16.4-R0.1-SNAPSHOT")),
        v1_16_R2(ImmutableSet.of("1.16.3-R0.1-SNAPSHOT", "1.16.2-R0.1-SNAPSHOT")),
        v1_16_R1(ImmutableSet.of("1.16.1-R0.1-SNAPSHOT")),
        v1_15_R1(ImmutableSet.of("1.15.2-R0.1-SNAPSHOT", "1.15.1-R0.1-SNAPSHOT", "1.15-R0.1-SNAPSHOT")),
        v1_14_R1(ImmutableSet.of("1.14.4-R0.1-SNAPSHOT", "1.14.3-R0.1-SNAPSHOT", "1.14.2-R0.1-SNAPSHOT", "1.14.1-R0.1-SNAPSHOT", "1.14-R0.1-SNAPSHOT")),
        v1_13_R2(ImmutableSet.of("1.13.2-R0.1-SNAPSHOT")),
        v1_13_R1(ImmutableSet.of("1.13.1-R0.1-SNAPSHOT", "1.13-R0.1-SNAPSHOT")),
        v1_12_R1(ImmutableSet.of("1.12.2-R0.1-SNAPSHOT", "1.12.1-R0.1-SNAPSHOT", "1.12-R0.1-SNAPSHOT"));

        private final ImmutableSet<String> versions;
        private final String PATH_CLASS_PlayerInventory;
        private final String PATH_CLASS_ItemStack;
        private final String PATH_CLASS_NonNullList;
        private final String PATH_METHOD_createItemlist;
        private final String PATH_FIELD_emptyItem;
        private final String PATH_METHOD_getNameSpacedKey;
        /*****************************************************************************************************************/
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
            if (args.length != 15) {
                throw new IllegalArgumentException("The number of arguments is not satisfied");
            }
            PATH_CLASS_PlayerInventory = args[0];
            PATH_CLASS_ItemStack = args[1];
            PATH_CLASS_NonNullList = args[2];
            PATH_METHOD_createItemlist = args[3];
            PATH_FIELD_emptyItem = args[4];
            PATH_METHOD_getNameSpacedKey = args[5];
            PATH_CLASS_EntityPlayer = args[6];
            PATH_CLASS_CraftPlayer = args[7];
            DOES_INVENTORY_USE_FIELD = Boolean.parseBoolean(args[8]);
            PATH_EntityPlayer_Inventory = args[9];
            PATH_PlayerInventory_items = args[10];
            PATH_PlayerInventory_armor = args[11];
            PATH_PlayerInventory_extraSlots = args[12];
            PATH_PlayerInventory_contents = args[13];
            PATH_CLASS_EntityHuman = args[14];
        }

        /**
         * @param bukkitVersion {@link Bukkit#getBukkitVersion()}
         * @return {@link SupportedVersions} value or null if not supported
         */
        public static @Nullable VersionUtil.SupportedVersions getFromBukkitVersion(String bukkitVersion) {
            for (SupportedVersions value : SupportedVersions.values()) {
                if (value.getVersions().contains(bukkitVersion)) {
                    if (isCurrent(value)) return value;
                }
            }
            return null;
        }

        private static boolean isCurrent(@Nullable SupportedVersions v) {
            if (v == null) {
                return false;
            }
            return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].equals(v.name());
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
