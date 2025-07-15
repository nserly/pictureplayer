package top.nserly.PicturePlayer.UIManager;

import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.HMODULE;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

    public void shutdown() {
        executor.shutdownNow();
        if (detector instanceof AutoCloseable) {
            try {
                ((AutoCloseable) detector).close();
            } catch (Exception e) {
                log.error(ExceptionHandler.getExceptionMessage(e));
            }
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
                        "gsettings get org.gnome.desktop.interface color-scheme"
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
                        "echo $XDG_CURRENT_DESKTOP"
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

    public class WindowsThemeDetector implements AutoCloseable, OSThemeDetector {
        // Windows 消息常量
        private static final int WM_SETTINGCHANGE = 0x001A;

        // 注册表路径
        private static final String PERSONALIZE_KEY =
                "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
        private static final String APPS_USE_LIGHT_THEME = "AppsUseLightTheme";
        private static final String SYSTEM_USE_LIGHT_THEME = "SystemUsesLightTheme";

        private final User32 user32;
        private final WinUser.HHOOK hook;
        private final AtomicBoolean isDarkMode;

        public WindowsThemeDetector() {
            this.user32 = User32.INSTANCE;
            this.isDarkMode = new AtomicBoolean(detectCurrentTheme());

            // 创建并安装 CallWndProc 钩子
            CallWndProc hookProc = new CallWndProc() {
                @Override
                public LRESULT callback(int nCode, WPARAM wParam, Pointer pointer) {
                    if (nCode >= 0 && pointerToCWPSTRUCT(pointer).message == WM_SETTINGCHANGE) {
                        String setting = pointer.getString(0);
                        if (setting != null && (
                                setting.contains("ImmersiveColorSet") ||
                                        setting.contains("AppsUseLightTheme"))) {
                            checkThemeChange();
                        }
                    }
                    return user32.CallNextHookEx(hook, nCode, wParam, new LPARAM(pointer.getLong(0))); // 将 Pointer 中的内容读取为 long 并传递给 LPARAM
                }

                private WinUser.CWPSTRUCT pointerToCWPSTRUCT(Pointer p) {
                    WinUser.CWPSTRUCT cwp = new WinUser.CWPSTRUCT();
                    cwp.read();
                    return cwp;
                }
            };

            HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);
            hook = user32.SetWindowsHookEx(
                    WinUser.WH_CALLWNDPROC,  // 窗口过程钩子
                    hookProc,
                    hMod,
                    0
            );

//            if (hook == null) {
//                // 钩子安装失败，尝试使用轮询方式
//                log.warn("Warning: The Windows Theme Change Hook cannot be installed, it will be polled");
//                new Thread(this::pollThemeChanges).start();
//            }
        }

        // 轮询主题变更（备用方法）
        private void pollThemeChanges() {
            while (true) {
                try {
                    Thread.sleep(10000); // 每10秒检查一次
                    checkThemeChange();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // 检查当前主题是否变更
        private void checkThemeChange() {
            boolean newTheme = detectCurrentTheme();
            if (newTheme != isDarkMode.getAndSet(newTheme)) {
                themeChangeCallback.accept(newTheme);
            }
        }

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
        public Boolean isDarkMode() throws Exception {
            return detectCurrentTheme();
        }

        @Override
        public void close() {
            // 卸载钩子
            if (hook != null) {
                user32.UnhookWindowsHookEx(hook);
            }
        }

        // 定义 CallWndProc 回调接口，直接实现 WinUser.HOOKPROC
        public interface CallWndProc extends WinUser.HOOKPROC {
            LRESULT callback(int nCode, WPARAM wParam, Pointer lParam);
        }
    }
}