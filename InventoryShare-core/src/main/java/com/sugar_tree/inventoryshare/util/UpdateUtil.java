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
package com.sugar_tree.inventoryshare.util;

import java.net.HttpURLConnection;
import java.net.URL;

import static com.sugar_tree.inventoryshare.api.SharedConstants.logger;
import static com.sugar_tree.inventoryshare.api.SharedConstants.plugin;

public class UpdateUtil {
    private static String version;
    public static void checkUpdate() {
        try {
            URL url = new URL("https://github.com/the-sugar-tree/InventoryShare/releases/latest");
            String urls = getFinalURL(url).toString();
            version = urls.substring(urls.lastIndexOf('/') + 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!("v"+plugin.getDescription().getVersion()).equals(version)) {
//            logger.warning(I18nUtil.get("update_able", plugin.getDescription().getVersion(), version));
            logger.warning(I18NUtil.get("update_able", "3.1-beta", "3.0"));
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
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return url;
    }
}
