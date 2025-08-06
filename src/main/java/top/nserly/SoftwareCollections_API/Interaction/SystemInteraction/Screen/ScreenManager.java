package top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Screen;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;
import top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Screen.Linux.LinuxScreenProvider;
import top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Screen.Windows.WindowsScreenProvider;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 屏幕信息管理类，提供静态方法供外部使用
 */
@Slf4j
public class ScreenManager {
    private static final ScreenProvider screenProvider;
    private static final List<ScreenInfo> allScreens = new CopyOnWriteArrayList<>();
    /**
     * -- GETTER --
     *  获取主屏幕信息
     * 主屏幕信息
     */
    @Getter
    private static ScreenInfo primaryScreen;

    static {
        // 根据操作系统类型初始化相应的屏幕信息提供者
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            screenProvider = new WindowsScreenProvider();
        }
        else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            screenProvider = new LinuxScreenProvider();
        }
        else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }

        // 初始加载屏幕信息
        reload();
    }

    /**
     * 重新加载所有屏幕信息
     */
    public static synchronized void reload() {
        allScreens.clear();
        allScreens.addAll(screenProvider.reloadScreens());

        // 确定主屏幕
        primaryScreen = allScreens.stream()
                .filter(ScreenInfo::isPrimary)
                .findFirst()
                .orElse(allScreens.isEmpty() ? null : allScreens.getFirst());
    }

    /**
     * 获取所有屏幕信息
     * @return 所有屏幕信息列表
     */
    public static List<ScreenInfo> getAllScreens() {
        return List.copyOf(allScreens);
    }

    /**
     * 获取屏幕个数
     * @return 屏幕个数
     */
    public static int getScreenCount() {
        return allScreens.size();
    }

    /**
     * 根据索引获取屏幕信息
     * @param index 屏幕索引
     * @return 屏幕信息，如果索引无效则返回null
     */
    public static ScreenInfo getScreen(int index) {
        if (index >= 0 && index < allScreens.size()) {
            return allScreens.get(index);
        }
        return null;
    }

    /**
     * 获取指定窗口所在的屏幕
     * @param window Java窗口对象
     * @return 窗口所在的屏幕信息
     */
    public static ScreenInfo getScreenForWindow(Window window) {
        if (window == null) {
            return getPrimaryScreen();
        }

        // 获取窗口句柄
        long windowHandle = getWindowHandle(window);
        return screenProvider.getScreenForWindow(windowHandle);
    }

    /**
     * 获取指定点所在的屏幕
     * @param x X坐标
     * @param y Y坐标
     * @return 该点所在的屏幕信息，如果未找到则返回主屏幕
     */
    public static ScreenInfo getScreenAt(int x, int y) {
        for (ScreenInfo screen : allScreens) {
            if (x >= screen.x() && x < screen.x() + screen.screenSize().width &&
                    y >= screen.y() && y < screen.y() + screen.screenSize().height) {
                return screen;
            }
        }
        return getPrimaryScreen();
    }

    /**
     * 获取所有屏幕的总尺寸（虚拟屏幕尺寸）
     * @return 总屏幕尺寸
     */
    public static Dimension getTotalScreenSize() {
        if (allScreens.isEmpty()) {
            return new Dimension(0, 0);
        }

        int minX = allScreens.stream().mapToInt(ScreenInfo::x).min().orElse(0);
        int minY = allScreens.stream().mapToInt(ScreenInfo::y).min().orElse(0);
        int maxX = allScreens.stream()
                .mapToInt(s -> s.x() + s.screenSize().width)
                .max().orElse(0);
        int maxY = allScreens.stream()
                .mapToInt(s -> s.y() + s.screenSize().height)
                .max().orElse(0);

        return new Dimension(maxX - minX, maxY - minY);
    }

    /**
     * 获取窗口句柄
     * @param window Java窗口对象
     * @return 窗口句柄
     */
    private static long getWindowHandle(Window window) {
        try {
            // 使用反射获取窗口句柄
            java.lang.reflect.Method getPeerMethod = window.getClass().getMethod("getPeer");
            Object peer = getPeerMethod.invoke(window);

            if (peer.getClass().getName().contains("WWindowPeer")) {
                // Windows平台
                java.lang.reflect.Field hwndField = peer.getClass().getDeclaredField("hwnd");
                hwndField.setAccessible(true);
                return hwndField.getLong(peer);
            } else if (peer.getClass().getName().contains("XWindowPeer")) {
                // Linux平台
                java.lang.reflect.Field windowField = peer.getClass().getDeclaredField("window");
                windowField.setAccessible(true);
                return windowField.getLong(peer);
            }
        } catch (Exception e) {
            System.err.println("Failed to get window handle: " + e.getMessage());
            log.error(ExceptionHandler.getExceptionMessage(e));
        }

        return 0;
    }
}
