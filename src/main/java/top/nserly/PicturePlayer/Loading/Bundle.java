/*
 * Copyright 2026 PicturePlayer;Nserly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.nserly.PicturePlayer.Loading;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

@Slf4j
public class Bundle {
    private static ResourceBundle bundle;
    @Getter
    private static Locale currentLocale;

    static {
        // 初始化时使用系统默认区域设置
        setLocale(Locale.getDefault());
    }

    public static void setLocale(Locale locale) {
        currentLocale = locale;
        bundle = ResourceBundle.getBundle("messages", locale, new UTF8Control());
    }

    public static String getMessage(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            // 键不存在时返回占位符
            log.error("Resource bundle read failed:{}", key);
            return key;
        }
    }

    // 自定义资源包控制器，支持 UTF-8
    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale,
                                        String format, ClassLoader loader,
                                        boolean reload)
                throws IOException {

            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");

            try (InputStream stream = loader.getResourceAsStream(resourceName)) {
                if (stream == null) return null;

                // 使用 UTF-8 编码读取资源文件
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    return new PropertyResourceBundle(reader);
                }
            }
        }
    }
}