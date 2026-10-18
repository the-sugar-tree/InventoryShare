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

package com.sugar_tree.inventoryshare.nms.server;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Minecraft release version such as {@code 1.21.11} or {@code 26.1.2}.
 *
 * <p>Only the numeric part is kept, so {@code "1.21.11-R0.2-SNAPSHOT"} and
 * {@code "26.1.2.build.63-stable"} parse to {@code 1.21.11} and {@code 26.1.2}.
 * Missing trailing components count as zero ({@code 1.21} equals {@code 1.21.0}).
 */
public final class MinecraftVersion implements Comparable<MinecraftVersion> {

    private static final Pattern NUMERIC_PREFIX = Pattern.compile("^(\\d+(?:\\.\\d+)*)");

    private final int[] parts;
    private final String text;

    private MinecraftVersion(int[] parts, String text) {
        this.parts = parts;
        this.text = text;
    }

    /**
     * @param version e.g. {@code "1.21.11"}, {@code "1.21.11-R0.2-SNAPSHOT"}, {@code "26.1.2.build.63-stable"}
     * @throws IllegalArgumentException if the string does not start with a version number
     */
    public static @NotNull MinecraftVersion parse(@NotNull String version) {
        Matcher matcher = NUMERIC_PREFIX.matcher(version.trim());
        if (!matcher.find()) {
            throw new IllegalArgumentException("Not a Minecraft version: " + version);
        }
        String text = matcher.group(1);
        String[] split = text.split("\\.");
        int[] parts = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            parts[i] = Integer.parseInt(split[i]);
        }
        return new MinecraftVersion(parts, text);
    }

    /**
     * Detects the running server's Minecraft version.
     *
     * <p>Paper exposes {@code Server#getMinecraftVersion()}; it is called by reflection because it is not
     * Spigot API. Spigot falls back to {@link Bukkit#getBukkitVersion()}, whose numeric prefix is the
     * Minecraft version. Paper 26.x changed the Bukkit version format, see
     * <a href="https://github.com/PaperMC/Paper/issues/13885">PaperMC/Paper#13885</a>.
     */
    @SuppressWarnings("JavaReflectionMemberAccess")
    public static @NotNull MinecraftVersion current() {
        String version;
        try {
            Method getMinecraftVersion = Server.class.getMethod("getMinecraftVersion");
            version = (String) getMinecraftVersion.invoke(Bukkit.getServer());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            version = Bukkit.getBukkitVersion();
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new RuntimeException(cause);
        }
        return parse(version);
    }

    public boolean isOlderThan(@NotNull String version) {
        return compareTo(parse(version)) < 0;
    }

    public boolean isAtLeast(@NotNull String version) {
        return compareTo(parse(version)) >= 0;
    }

    @Override
    public int compareTo(@NotNull MinecraftVersion other) {
        int length = Math.max(parts.length, other.parts.length);
        for (int i = 0; i < length; i++) {
            int a = i < parts.length ? parts[i] : 0;
            int b = i < other.parts.length ? other.parts[i] : 0;
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MinecraftVersion && compareTo((MinecraftVersion) o) == 0;
    }

    @Override
    public int hashCode() {
        int length = parts.length;
        while (length > 0 && parts[length - 1] == 0) length--;
        int hash = 1;
        for (int i = 0; i < length; i++) {
            hash = 31 * hash + parts[i];
        }
        return hash;
    }

    @Override
    public String toString() {
        return text;
    }
}
