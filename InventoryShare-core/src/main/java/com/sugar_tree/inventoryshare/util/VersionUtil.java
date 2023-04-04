package com.sugar_tree.inventoryshare.util;

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
        v1_19_R3("1.19.4-R0.1-SNAPSHOT"),
        v1_19_R2("1.19.3-R0.1-SNAPSHOT"),
        /**
         * v1_19_1_R1 -> 1.19.2, 1.19.1<br>
         * v1_19_R1 -> 1.19
         */
        v1_19_R1("1.19.2-R0.1-SNAPSHOT", "1.19.1-R0.1-SNAPSHOT", "1.19-R0.1-SNAPSHOT"),
        v1_18_R2("1.18.2-R0.1-SNAPSHOT", "1.18.1-R0.1-SNAPSHOT"),
        v1_18_R1("1.18-R0.1-SNAPSHOT"),
        v1_17_R1("1.17.1-R0.1-SNAPSHOT", "1.17.1-R0.1-SNAPSHOT"),
        v1_16_R3("1.16.5-R0.1-SNAPSHOT", "1.16.4-R0.1-SNAPSHOT"),
        v1_16_R2("1.16.3-R0.1-SNAPSHOT", "1.16.2-R0.1-SNAPSHOT"),
        v1_16_R1("1.16.1-R0.1-SNAPSHOT"),
        v1_15_R1("1.15.2-R0.1-SNAPSHOT", "1.15.1-R0.1-SNAPSHOT", "1.15-R0.1-SNAPSHOT"),
        v1_14_R1("1.14.4-R0.1-SNAPSHOT", "1.14.3-R0.1-SNAPSHOT", "1.14.2-R0.1-SNAPSHOT", "1.14.1-R0.1-SNAPSHOT", "1.14-R0.1-SNAPSHOT"),
        v1_13_R2("1.13.2-R0.1-SNAPSHOT"),
        v1_13_R1("1.13.1-R0.1-SNAPSHOT", "1.13-R0.1-SNAPSHOT"),
        v1_12_R1("1.12.2-R0.1-SNAPSHOT", "1.12.1-R0.1-SNAPSHOT", "1.12-R0.1-SNAPSHOT");

        private final ImmutableSet<String> versions;
        public ImmutableSet<String> getVersions() {
            return versions;
        }

        SupportedVersions(String... versions) {
            this.versions = ImmutableSet.copyOf(versions);
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
    }
}
