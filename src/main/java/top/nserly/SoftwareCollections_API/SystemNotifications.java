package top.nserly.SoftwareCollections_API;

import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;

import javax.swing.*;
import java.awt.*;

@Slf4j
public class SystemNotifications {
    //是否支持系统通知
    public static final boolean isSupportedSystemNotifications;
    private static final SystemTray tray = SystemTray.getSystemTray();
    private static final TrayIcon DefaultIcon = new TrayIcon(new ImageIcon("icon.png").getImage());

    static {
        isSupportedSystemNotifications = SystemTray.isSupported();
        try {
            tray.add(DefaultIcon);
        } catch (AWTException e) {
            log.error(ExceptionHandler.getExceptionMessage(e));
        }
    }

    public static void send(String caption, String text, TrayIcon.MessageType messageType) {
        if (!isSupportedSystemNotifications) throw new RuntimeException("System Notifications is not supported!");
        DefaultIcon.displayMessage(caption, text, messageType);
    }
}
