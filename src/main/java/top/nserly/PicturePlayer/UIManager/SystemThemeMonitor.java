package top.nserly.PicturePlayer.UIManager;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 跨平台系统主题监听器 - 使用 JNA 实现
 */
@Slf4j
public class SystemThemeMonitor {
    private static final String DARK_MODE_KEY = "AppleInterfaceStyle";
    private final Consumer<Boolean> themeChangeCallback;
    private final OSThemeDetector detector;
    private final ScheduledExecutorService executor;
    private Boolean currentTheme;

    public SystemThemeMonitor(Consumer<Boolean> callback) {
        this.themeChangeCallback = callback;
        this.executor = Executors.newSingleThreadScheduledExecutor();

        // 根据操作系统选择对应的检测器
        if (Platform.isWindows()) {
            detector = new WindowsThemeDetector();
        } else if (Platform.isLinux()) {
            detector = new LinuxThemeDetector();
        } else {
            detector = () -> false; // 默认返回 false
        }

        // 启动定期检查任务
        executor.scheduleAtFixedRate(this::checkTheme, 0, 1, TimeUnit.SECONDS);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public void startListener() {
        if (executor.isShutdown()) {
            executor.scheduleAtFixedRate(this::checkTheme, 0, 1, TimeUnit.SECONDS);
        }
    }

    private void checkTheme() {
        try {
            Boolean newTheme = detector.isDarkMode();
            if (newTheme != null && !newTheme.equals(currentTheme)) {
                currentTheme = newTheme;
                themeChangeCallback.accept(newTheme);
            }
        } catch (Exception e) {
            log.error(ExceptionHandler.getExceptionMessage(e));
        }
    }

    // 主题检测器接口
    private interface OSThemeDetector {
        Boolean isDarkMode() throws Exception;
    }

    // Linux 主题检测器
    private static class LinuxThemeDetector implements OSThemeDetector {
        @Override
        public Boolean isDarkMode() throws IOException {
            // 检查 GNOME 桌面环境
            if (isGnome()) {
                Process process = Runtime.getRuntime().exec(
                        new String[]{"gsettings", "get", "org.gnome.desktop.interface color-scheme"}
                );

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {

                    String line = reader.readLine();
                    return line != null && line.contains("dark");
                }
            }

            // 可以添加其他桌面环境的检测逻辑
            return false;
        }

        private boolean isGnome() {
            try {
                Process process = Runtime.getRuntime().exec(
                        new String[]{"echo", "$XDG_CURRENT_DESKTOP"}
                );

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {

                    String line = reader.readLine();
                    return line != null && line.contains("GNOME");
                }
            } catch (Exception e) {
                return false;
            }
        }
    }

    public static class WindowsThemeDetector implements OSThemeDetector {
        // 注册表路径
        private static final String PERSONALIZE_KEY =
                "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
        private static final String APPS_USE_LIGHT_THEME = "AppsUseLightTheme";
        private static final String SYSTEM_USE_LIGHT_THEME = "SystemUsesLightTheme";

        // 检测当前主题状态
        private boolean detectCurrentTheme() {
            try {
                // 尝试使用 UxTheme API (Windows 10+)
                if (Advapi32Util.registryKeyExists(
                        WinReg.HKEY_CURRENT_USER, PERSONALIZE_KEY)) {

                    // 优先检查应用主题设置
                    if (Advapi32Util.registryValueExists(
                            WinReg.HKEY_CURRENT_USER, PERSONALIZE_KEY, APPS_USE_LIGHT_THEME)) {

                        int value = Advapi32Util.registryGetIntValue(
                                WinReg.HKEY_CURRENT_USER, PERSONALIZE_KEY, APPS_USE_LIGHT_THEME);

                        return value == 0; // 0 = 暗模式, 1 = 亮模式
                    }
                }

                // 回退到系统主题设置
                if (Advapi32Util.registryValueExists(
                        WinReg.HKEY_CURRENT_USER, PERSONALIZE_KEY, SYSTEM_USE_LIGHT_THEME)) {

                    int value = Advapi32Util.registryGetIntValue(
                            WinReg.HKEY_CURRENT_USER, PERSONALIZE_KEY, SYSTEM_USE_LIGHT_THEME);

                    return value == 0;
                }

                // 更旧版本的 Windows
                return false;

            } catch (Exception e) {
                // 记录错误但继续运行
                SystemThemeMonitor.log.error("Topic detection failed: {}", e.getMessage());
                return false; // 默认返回亮模式
            }
        }

        @Override
        public Boolean isDarkMode() {
            return detectCurrentTheme();
        }

    }
}