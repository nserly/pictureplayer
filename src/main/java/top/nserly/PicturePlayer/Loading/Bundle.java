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