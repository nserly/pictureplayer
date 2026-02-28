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

package top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Notifications;

import lombok.extern.slf4j.Slf4j;
import top.nserly.GUIStarter;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;
import top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.SoftwareChannel.SystemTrayEvent;

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
    public static TrayIcon DefaultIcon;
    public static int createdSystemTrayCount;
    public static BufferedImage bufferedImage;

    static {
        isSupportedSystemNotifications = SystemTray.isSupported();
        try {
            bufferedImage = ImageIO.read(Objects.requireNonNull(GUIStarter.class.getResource("tray.png")));
            if (isSupportedSystemNotifications)
                DefaultIcon = new TrayIcon(bufferedImage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendMessage(TrayIcon trayIcon, String caption, String text, TrayIcon.MessageType messageType) {
        if (!isSupportedSystemNotifications) throw new RuntimeException("System Notifications is not supported!");
        trayIcon.displayMessage(caption, text, messageType);
    }

    public static SystemTray getSystemTray(TrayIcon trayIcon, MenuItem[] menuItems, SystemTrayEvent systemTrayEvent) {
        if (!isSupportedSystemNotifications) throw new RuntimeException("System Notifications is not supported!");
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            log.warn("System tray is not supported!Because your system is Linux or Unix-like system.Because they don't offer this type of support by default!");
            return null;
        }
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
        createdSystemTrayCount++;
        return tray;
    }

}
