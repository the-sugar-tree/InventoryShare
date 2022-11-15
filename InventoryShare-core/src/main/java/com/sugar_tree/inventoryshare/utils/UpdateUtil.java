package com.sugar_tree.inventoryshare.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static com.sugar_tree.inventoryshare.api.Variables.logger;
import static com.sugar_tree.inventoryshare.api.Variables.plugin;

public class UpdateUtil {
    public static String version;
    public static void checkUpdate() {
        try {
            URL url = new URL("https://github.com/the-sugar-tree/InventoryShare/releases/latest");
            String line;
            HttpURLConnection httpURLConnection = ((HttpURLConnection) url.openConnection());
            httpURLConnection.setRequestMethod("GET");
            BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), StandardCharsets.UTF_8));
            while ((line = br.readLine()) != null) {
                if (line.contains("<a aria-current=\"page\" href=\"/the-sugar-tree/InventoryShare/releases/tag/")) {
                    version = line.substring(line.lastIndexOf(" ") + 1);
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!plugin.getDescription().getVersion().equals(version)) {
            logger.warning("플러그인 업데이트가 가능합니다! 현재 버전: v" + plugin.getDescription().getVersion() + "새 버전: " + version);
            logger.warning("https://github.com/the-sugar-tree/InventoryShare/releases/latest");
        }
    }
}
