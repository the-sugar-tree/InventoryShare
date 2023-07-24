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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sugar_tree.inventoryshare.utils;

import java.io.IOException;
import java.net.*;

import static com.sugar_tree.inventoryshare.SharedConstants.logger;
import static com.sugar_tree.inventoryshare.SharedConstants.plugin;

public class UpdateUtil {
    private static String version;
    public static void checkUpdate() {
        try {
            URL url = new URL("https://github.com/the-sugar-tree/InventoryShare/releases/latest");
            String urls = getFinalURL(url).toString();
            version = urls.substring(urls.lastIndexOf('/') + 1);
        } catch (MalformedURLException e) {
            logger.severe(I18NUtil.get("update_error", e.getMessage()));
        }

        if (!("v"+plugin.getDescription().getVersion()).equals(version)) {
            logger.warning(I18NUtil.get("update_able", plugin.getDescription().getVersion(), version));
//            logger.warning(I18NUtil.get("update_able", "3.1-beta", "3.0"));
            logger.warning("https://github.com/the-sugar-tree/InventoryShare/releases/latest");
        }
    }

    public static URL getFinalURL(URL url) {
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setInstanceFollowRedirects(false);
            con.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36");
            con.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            con.connect();
            // Header에서 Status Code를 뽑는다.
            int resCode = con.getResponseCode();
            // http코드가 301(영구이동), 302(임시 이동), 303(기타 위치 보기) 이면 또다시 이 함수를 태운다. 재귀함수.
            if (resCode == HttpURLConnection.HTTP_SEE_OTHER || resCode == HttpURLConnection.HTTP_MOVED_PERM
                    || resCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                String Location = con.getHeaderField("Location");
                if (Location.startsWith("/")) {
                    Location = url.getProtocol() + "://" + url.getHost() + Location;
                }
                return getFinalURL(new URL(Location));
            }
        } catch (UnknownHostException | SocketTimeoutException | ConnectException e) {
            logger.severe(I18NUtil.get("update_error", e.getMessage()));
            System.out.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return url;
    }
}
