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

package com.sugar_tree.inventoryshare.nms;

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
 * Spigot server classpaths written by {@code InventoryShare-nms-test/build.gradle}.
 */
final class SpigotJars {

    private static final Pattern CRAFT_SERVER = Pattern.compile("^(org/bukkit/craftbukkit/(?:v[0-9_R]+/)?CraftServer)\\.class$");

    private static Properties classpaths;

    private SpigotJars() {}

    /**
     * A Spigot server jar to check: the runtime jar of a version, or (1.17+) the same version remapped to Mojang's
     * names, which stands in for Paper 1.20.5+.
     */
    static final class Jar {
        final String mc;
        final boolean mojangRemapped;

        private Jar(String mc, boolean mojangRemapped) {
            this.mc = mc;
            this.mojangRemapped = mojangRemapped;
        }

        String key() {
            return mojangRemapped ? mc + "@mojang" : mc;
        }

        ServerVersion version() {
            return ServerVersion.parse(mc);
        }

        /** Remapped jars, and every 26.1+ jar, use Mojang's names */
        Namespace expectedNamespace() {
            return mojangRemapped || version().isAtLeast("26.1") ? Namespace.MOJANG : Namespace.SPIGOT;
        }

        @Override
        public String toString() {
            return mojangRemapped ? mc + " (remapped-mojang)" : mc;
        }
    }

    /**
     * @return every Minecraft version listed in the build script, installed or not
     */
    static Stream<String> versions() {
        return list("spigot.versions").stream();
    }

    /**
     * @return the runtime jar of every listed version, then the remapped-mojang jar of the versions that have one
     */
    static Stream<Jar> jars() {
        Set<String> mojang = new HashSet<>(list("spigot.mojangVersions"));
        return Stream.concat(
                versions().map(mc -> new Jar(mc, false)),
                versions().filter(mojang::contains).map(mc -> new Jar(mc, true)));
    }

    static Stream<Jar> jarsBelow(String version) {
        return jars().filter(jar -> jar.version().isOlderThan(version));
    }

    static Stream<Jar> jarsFrom(String version) {
        return jars().filter(jar -> jar.version().isAtLeast(version));
    }

    static Stream<String> versionsBelow(String version) {
        return versions().filter(mc -> ServerVersion.parse(mc).isOlderThan(version));
    }

    static Stream<String> versionsFrom(String version) {
        return versions().filter(mc -> ServerVersion.parse(mc).isAtLeast(version));
    }

    /**
     * Opens the classpath of the jar in a loader isolated from the test classpath,
     * or skips the test if the jar is not installed.
     */
    static URLClassLoader open(Jar jar) throws IOException {
        String classpath = classpaths().getProperty(jar.key(), "");
        Assumptions.assumeFalse(classpath.isEmpty(), "Spigot " + jar + " is not installed in mavenLocal (BuildTools --rev "
                + jar.mc + (jar.mojangRemapped ? " --remapped" : "") + ")");
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
    static String craftServerClass(Jar jar) throws IOException {
        String server = classpaths().getProperty(jar.key() + ".server", "");
        Assumptions.assumeFalse(server.isEmpty(), "Spigot " + jar + " is not installed in mavenLocal");
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
            try (InputStream in = Files.newInputStream(Paths.get(System.getProperty("spigot.classpaths")))) {
                properties.load(in);
            }
            classpaths = properties;
        }
        return classpaths;
    }
}
