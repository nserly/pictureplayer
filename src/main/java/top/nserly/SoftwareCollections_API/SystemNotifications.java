package top.nserly.SoftwareCollections_API;

import lombok.extern.slf4j.Slf4j;
import top.nserly.GUIStarter;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

@Slf4j
public class SystemNotifications {
    //是否支持系统通知
    public static final boolean isSupportedSystemNotifications;
    public static TrayIcon DefaultIcon = null;

    static {
        isSupportedSystemNotifications = SystemTray.isSupported();
        try {
            new SystemNotifications();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SystemNotifications() throws IOException {
        BufferedImage bufferedImage = ImageIO.read(Objects.requireNonNull(GUIStarter.class.getResource("icon.png")));
        DefaultIcon = new TrayIcon(bufferedImage);
    }

    public static void sendMessage(TrayIcon trayIcon, String caption, String text, TrayIcon.MessageType messageType) {
        if (!isSupportedSystemNotifications) throw new RuntimeException("System Notifications is not supported!");
        trayIcon.displayMessage(caption, text, messageType);
    }

    public static SystemTray getSystemTray(TrayIcon trayIcon, MenuItem[] menuItems, SystemTrayEvent systemTrayEvent) {
        if (!isSupportedSystemNotifications) throw new RuntimeException("System Notifications is not supported!");
        // 获取系统托盘实例
        SystemTray tray = SystemTray.getSystemTray();

        trayIcon.setImageAutoSize(true); // 自动调整图标大小

        // 创建右键菜单
        PopupMenu popup = new PopupMenu();

        for (int i = 0; i < menuItems.length; i++) {
            if (menuItems[i] == null) throw new RuntimeException("MenuItem cannot be null");
            popup.add(menuItems[i]);
            if (i + 1 != menuItems.length) {
                popup.addSeparator();
            }
        }
        trayIcon.setPopupMenu(popup);

        // 添加双击事件监听器
        if (systemTrayEvent != null)
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    systemTrayEvent.mouseClicked(e);
                }
            });

        // 添加托盘图标到系统托盘
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            log.error(ExceptionHandler.getExceptionMessage(e));
        }
        return tray;
    }

}
