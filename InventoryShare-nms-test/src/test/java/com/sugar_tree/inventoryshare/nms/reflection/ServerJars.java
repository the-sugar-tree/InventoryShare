/*
 * This file is part of InventoryShare, licensed under the GPL-3.0 License.
 *
 * InventoryShare the minecraft plugin that enables sharing inventory with other players
 * Copyright (c) 2023 the-sugar-tree and contributors
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

package com.sugar_tree.inventoryshare.nms.reflection;

import com.sugar_tree.inventoryshare.nms.server.MappingType;
import com.sugar_tree.inventoryshare.nms.server.MinecraftVersion;
import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Spigot and Paper server classpaths written by {@code InventoryShare-nms-test/build.gradle}.
 */
public final class ServerJars {

    private static final Pattern CRAFT_SERVER = Pattern.compile("^(org/bukkit/craftbukkit/(?:v[0-9_R]+/)?CraftServer)\\.class$");

    private static Properties classpaths;

    private ServerJars() {}

    public enum Kind {
        /** The jar a Spigot server runs, installed into mavenLocal by BuildTools */
        SPIGOT,
        /** The same Spigot version remapped to Mojang's names (1.17+, BuildTools {@code --remapped}) */
        MOJANG_REMAPPED,
        /** A Paper server, prepared by {@code preparePaperJars}. Paper changes CraftBukkit itself */
        PAPER
    }

    /**
     * A server jar to check.
     */
    public static final class Jar {
        public final String mc;
        public final Kind kind;

        private Jar(String mc, Kind kind) {
            this.mc = mc;
            this.kind = kind;
        }

        public String key() {
            switch (kind) {
                case MOJANG_REMAPPED:
                    return mc + "@mojang";
                case PAPER:
                    return mc + "@paper";
                default:
                    return mc;
            }
        }

        public MinecraftVersion version() {
            return MinecraftVersion.parse(mc);
        }

        /** Remapped jars, Paper from 1.20.5, and every 26.1+ jar use Mojang's names */
        public MappingType expectedMappingType() {
            boolean mojang = kind == Kind.MOJANG_REMAPPED
                    || (kind == Kind.PAPER && version().isAtLeast("1.20.5"))
                    || version().isAtLeast("26.1");
            return mojang ? MappingType.MOJANG : MappingType.SPIGOT;
        }

        @Override
        public String toString() {
            switch (kind) {
                case MOJANG_REMAPPED:
                    return mc + " (remapped-mojang)";
                case PAPER:
                    return "Paper " + mc;
                default:
                    return mc;
            }
        }

        private String missing() {
            switch (kind) {
                case MOJANG_REMAPPED:
                    return "Spigot " + this + " is not installed in mavenLocal (BuildTools --rev " + mc + " --remapped)";
                case PAPER:
                    return this + " is not prepared (./gradlew :InventoryShare-nms-test:preparePaperJars)";
                default:
                    return "Spigot " + this + " is not installed in mavenLocal (BuildTools --rev " + mc + ")";
            }
        }
    }

    /**
     * @return every Minecraft version listed for Spigot in the build script, installed or not
     */
    public static Stream<String> versions() {
        return list("spigot.versions").stream();
    }

    /**
     * @return the runtime jar of every listed Spigot version, then the remapped-mojang jars, then the Paper jars
     */
    public static Stream<Jar> jars() {
        Set<String> mojang = new HashSet<>(list("spigot.mojangVersions"));
        return Stream.of(
                versions().map(mc -> new Jar(mc, Kind.SPIGOT)),
                versions().filter(mojang::contains).map(mc -> new Jar(mc, Kind.MOJANG_REMAPPED)),
                list("paper.versions").stream().map(mc -> new Jar(mc, Kind.PAPER))
        ).flatMap(jars -> jars);
    }

    public static Stream<Jar> jarsBelow(String version) {
        return jars().filter(jar -> jar.version().isOlderThan(version));
    }

    public static Stream<Jar> jarsFrom(String version) {
        return jars().filter(jar -> jar.version().isAtLeast(version));
    }

    public static Stream<String> versionsBelow(String version) {
        return versions().filter(mc -> MinecraftVersion.parse(mc).isOlderThan(version));
    }

    public static Stream<String> versionsFrom(String version) {
        return versions().filter(mc -> MinecraftVersion.parse(mc).isAtLeast(version));
    }

    /**
     * Opens the classpath of the jar in a loader isolated from the test classpath,
     * or skips the test if the jar is not installed.
     */
    public static URLClassLoader open(Jar jar) throws IOException {
        String classpath = classpaths().getProperty(jar.key(), "");
        Assumptions.assumeFalse(classpath.isEmpty(), jar.missing());
        String[] paths = classpath.split(File.pathSeparator);
        URL[] urls = new URL[paths.length];
        for (int i = 0; i < paths.length; i++) {
            urls[i] = new File(paths[i]).toURI().toURL();
        }
        return new URLClassLoader(urls, ClassLoader.getPlatformClassLoader());
    }

    /**
     * @return the name of the {@code CraftServer} class in the server jar, as a server would report it
     */
    public static String craftServerClass(Jar jar) throws IOException {
        String server = classpaths().getProperty(jar.key() + ".server", "");
        Assumptions.assumeFalse(server.isEmpty(), jar.missing());
        try (ZipFile zip = new ZipFile(server)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                Matcher matcher = CRAFT_SERVER.matcher(entries.nextElement().getName());
                if (matcher.matches()) {
                    return matcher.group(1).replace('/', '.');
                }
            }
        }
        throw new IllegalStateException("No CraftServer class in " + server);
    }

    private static List<String> list(String property) {
        return Arrays.stream(System.getProperty(property, "").split(","))
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }

    private static synchronized Properties classpaths() throws IOException {
        if (classpaths == null) {
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(Paths.get(System.getProperty("server.classpaths")))) {
                properties.load(in);
            }
            classpaths = properties;
        }
        return classpaths;
    }
}
