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

package com.sugar_tree.inventoryshare.utils;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sugar_tree.inventoryshare.SharedConstants.logger;
import static com.sugar_tree.inventoryshare.SharedConstants.plugin;

public final class UpdateUtil {
    private UpdateUtil() {}

    private static final String LATEST_RELEASE_API = "https://api.github.com/repos/the-sugar-tree/InventoryShare/releases/latest";
    private static final Pattern TAG_NAME = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern VERSION = Pattern.compile("^v?(\\d+(?:\\.\\d+)*)(.*)$");
    private static final int TIMEOUT_MILLIS = 3000;

    /**
     * Asks GitHub for the latest release off the main thread, and logs a warning if it is newer than this version.
     * A connection or parse error is logged instead of thrown.
     */
    public static void showUpdateInformation() {
        String current = plugin.getDescription().getVersion();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String latest;
            try {
                latest = fetchLatestTag();
            } catch (IOException e) {
                logger.severe(I18NUtil.get(false, false, "update_error", e.getMessage()));
                return;
            }
            if (latest == null) {
                logger.severe(I18NUtil.get(false, false, "update_error", "no release tag in the response"));
                return;
            }
            if (isNewer(latest, current)) {
                logger.warning(I18NUtil.get(false, false, "update_able", current, latest));
                logger.warning("https://github.com/the-sugar-tree/InventoryShare/releases/latest");
            }
        });
    }

    /**
     * @return the tag of the latest release, e.g. {@code v3.1.1}, or {@code null} if the response has none
     * @throws IOException if the request fails or GitHub does not answer with 200 OK
     */
    private static @Nullable String fetchLatestTag() throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(LATEST_RELEASE_API).openConnection();
        try {
            con.setConnectTimeout(TIMEOUT_MILLIS);
            con.setReadTimeout(TIMEOUT_MILLIS);
            con.setRequestMethod("GET");
            int resCode = con.getResponseCode();
            if (resCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + resCode);
            }
            try (InputStream in = con.getInputStream()) {
                return parseTagName(read(in));
            }
        } finally {
            con.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        char[] buffer = new char[4096];
        for (int n; (n = reader.read(buffer)) != -1; ) {
            sb.append(buffer, 0, n);
        }
        return sb.toString();
    }

    /**
     * @param json body of GitHub's "latest release" response
     * @return the value of {@code tag_name}, or {@code null} if there is none
     */
    static @Nullable String parseTagName(String json) {
        Matcher matcher = TAG_NAME.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Compares the numeric parts ({@code v4.0.1} is newer than {@code 4.0}). With the same numbers, a release is newer
     * than a pre-release such as {@code 4.0-beta}.
     *
     * @param latest  tag of the latest release, e.g. {@code v4.0}
     * @param current this plugin's version, e.g. {@code 4.0-beta}
     * @return whether {@code latest} is newer; {@code false} if either is not a version
     */
    static boolean isNewer(String latest, String current) {
        Matcher l = VERSION.matcher(latest.trim());
        Matcher c = VERSION.matcher(current.trim());
        if (!l.matches() || !c.matches()) return false;
        String[] lp = l.group(1).split("\\.");
        String[] cp = c.group(1).split("\\.");
        for (int i = 0; i < Math.max(lp.length, cp.length); i++) {
            int a = i < lp.length ? Integer.parseInt(lp[i]) : 0;
            int b = i < cp.length ? Integer.parseInt(cp[i]) : 0;
            if (a != b) return a > b;
        }
        return l.group(2).isEmpty() && !c.group(2).isEmpty();
    }
}
