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

package com.sugar_tree.inventoryshare.utils;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

import static com.sugar_tree.inventoryshare.SharedConstants.logger;
import static com.sugar_tree.inventoryshare.SharedConstants.plugin;

public final class UpdateUtil {
    private UpdateUtil() {}

    private static String version;
    public static void showUpdateInformation() {
        try {
            String url = "https://api.github.com/repos/the-sugar-tree/InventoryShare/releases/latest";
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            if (con != null) {
                con.setConnectTimeout(1000);
                con.setRequestMethod("GET");

                int resCode = con.getResponseCode();

                if (resCode == HttpURLConnection.HTTP_OK) {
                    JSONObject value = (JSONObject) new JSONParser().parse(new InputStreamReader(con.getInputStream()));
                    version = (String) value.get("tag_name");
                }
                con.disconnect();
            }
        } catch (IOException | ParseException e) {
            logger.severe(I18NUtil.get(false, false, "update_error", e.getMessage()));
            return;
        }
        if (!("v"+plugin.getDescription().getVersion()).equals(version)) {
            logger.warning(I18NUtil.get(false, false, "update_able", plugin.getDescription().getVersion(), version));
            logger.warning("https://github.com/the-sugar-tree/InventoryShare/releases/latest");
        }
    }

}
