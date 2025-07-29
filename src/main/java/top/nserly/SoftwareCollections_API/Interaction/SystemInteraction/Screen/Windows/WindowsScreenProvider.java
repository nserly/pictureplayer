package top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Screen.Windows;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;
import top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Screen.ScreenInfo;
import top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Screen.ScreenManager;
import top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Screen.ScreenProvider;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Windows平台的屏幕信息获取实现
 */
@Slf4j
public class WindowsScreenProvider implements ScreenProvider {
    private static final User32 user32 = User32.INSTANCE;

    @Override
    public List<ScreenInfo> reloadScreens() {
        List<ScreenInfo> screens = new ArrayList<>();
        AtomicInteger index = new AtomicInteger();

        // 枚举所有显示设备
        user32.EnumDisplayMonitors(null, null, (hMonitor, hdcMonitor, rect, lParam) -> {
            WinUser.MONITORINFOEX monitorInfo = new WinUser.MONITORINFOEX();
            monitorInfo.cbSize = monitorInfo.size();

            if (user32.GetMonitorInfo(hMonitor, monitorInfo).booleanValue()) {
                // 获取屏幕尺寸
                int width = monitorInfo.rcMonitor.right - monitorInfo.rcMonitor.left;
                int height = monitorInfo.rcMonitor.bottom - monitorInfo.rcMonitor.top;

                // 获取可用屏幕尺寸
                int usableWidth = monitorInfo.rcWork.right - monitorInfo.rcWork.left;
                int usableHeight = monitorInfo.rcWork.bottom - monitorInfo.rcWork.top;

                // 判断是否为主屏幕
                boolean isPrimary = (monitorInfo.dwFlags & User32.MONITORINFOF_PRIMARY) != 0;

                screens.add(new ScreenInfo(
                        index.getAndIncrement(),
                        new Dimension(width, height),
                        new Dimension(usableWidth, usableHeight),
                        monitorInfo.rcMonitor.left,
                        monitorInfo.rcMonitor.top,
                        isPrimary
                ));
            }
            // 返回 1（true）继续枚举下一个显示器
            return 1;
        }, null);

        return screens;
    }

    @Override
    public ScreenInfo getScreenForWindow(long windowHandle) {
        if (windowHandle == 0) {
            return ScreenManager.getPrimaryScreen();
        }

        try {
            WinDef.HWND hwnd = new WinDef.HWND();
            hwnd.setPointer(Pointer.createConstant(windowHandle));

            WinUser.HMONITOR hMonitor = user32.MonitorFromWindow(hwnd, User32.MONITOR_DEFAULTTONEAREST);

            WinDef.RECT rect = new WinDef.RECT();
            user32.GetWindowRect(hwnd, rect);

            List<ScreenInfo> screens = ScreenManager.getAllScreens();
            for (ScreenInfo screen : screens) {
                if (rect.left >= screen.x() && rect.left < screen.x() + screen.screenSize().width &&
                        rect.top >= screen.y() && rect.top < screen.y() + screen.screenSize().height) {
                    return screen;
                }
            }
        } catch (Exception e) {
            log.error("Error getting screen for window: {}", e.getMessage());
            log.error(ExceptionHandler.getExceptionMessage(e));
        }

        return ScreenManager.getPrimaryScreen();
    }
}
