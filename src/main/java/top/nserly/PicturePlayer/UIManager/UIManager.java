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

package top.nserly.PicturePlayer.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.SystemInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;

import javax.swing.*;
import java.awt.*;
import java.util.List;

@Setter
@Slf4j
public abstract class UIManager {
    public static final int FollowSystemTheme = 0;
    public static final int DarkTheme = 1;
    public static final int LightTheme = 2;
    @Getter
    private static final UIManager UIManager = createInstance();
    //设置主题（可设值：0，1，2）
    @Getter
    private int Theme = 0;//当前主题：0：跟随系统 1：Dark主题 2：Light主题

    protected UIManager() {
        System.setProperty("flatlaf.useSystemEnvironment", "true");
        SystemThemeMonitor monitor = new SystemThemeMonitor(isDark -> {
            int theme = isDark ? DarkTheme : LightTheme;
            int appliedTheme = getAppliedTheme();
            if (theme == appliedTheme) return;
            setupThemeListener(theme);
        });
    }

    private static UIManager createInstance() {
        if (SystemInfo.isWindows) {
            return new WindowsUI();
        } else if (SystemInfo.isLinux) {
            return new LinuxUI();
        } else {
            log.warn("For unsupported operating systems, the default GUI theme will be used");
            return new DefaultUI();
        }
    }

    //更新所有窗体
    public synchronized static void updateAllWindows() {
        SwingUtilities.invokeLater(() -> {
            for (Window window : Window.getWindows()) {
                if (window.isShowing()) {
                    window.dispose();
                    FontPreservingUIUpdater.updateComponentTreeUIWithFontPreservation(window);
                    window.setVisible(true);
                    continue;
                }
                FontPreservingUIUpdater.updateComponentTreeUIWithFontPreservation(window);
            }
        });
    }

    //更新所有所选窗体
    public synchronized static void updateChoiceWindows(List<Window> windowsCollections) {
        SwingUtilities.invokeLater(() -> {
            for (Window window : windowsCollections) {
                if (window.isShowing()) {
                    window.dispose();
                    FontPreservingUIUpdater.updateComponentTreeUIWithFontPreservation(window);
                    window.setVisible(true);
                } else {
                    FontPreservingUIUpdater.updateComponentTreeUIWithFontPreservation(window);
                }
            }
        });
    }

    // 获取当前已应用的主题信息
    public synchronized int getAppliedTheme() {
        LookAndFeel laf = javax.swing.UIManager.getLookAndFeel();
        if (laf instanceof FlatLaf) {
            return ((FlatLaf) laf).isDark() ? DarkTheme : LightTheme;
        }
        return FollowSystemTheme;
    }

    //应用操作系统主题
    public synchronized void applyThemeOnSystem() {
        int SystemTheme = getSystemTheme();
        if (getAppliedTheme() == SystemTheme) return;
        if (Theme != 0) Theme = SystemTheme;
        applyTheme(SystemTheme);
    }

    //应用操作系统主题并刷新界面
    public synchronized void applyThemeAndRefreshWindows() {
        int SystemTheme = getSystemTheme();
        if (getAppliedTheme() == SystemTheme) return;
        if (Theme != 0) Theme = SystemTheme;
        applyTheme(SystemTheme);
        updateAllWindows();
    }

    //应用主题（可设值：1，2）
    public synchronized void applyTheme(int theme) {
        try {
            switch (theme) {
                case DarkTheme -> javax.swing.UIManager.setLookAndFeel(new FlatDarkLaf());
                case LightTheme -> javax.swing.UIManager.setLookAndFeel(new FlatLightLaf());
                default -> throw new RuntimeException("The theme is set up incorrectly!");
            }
        } catch (Exception e) {
            try {
                javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                log.error(ExceptionHandler.getExceptionMessage(e));
            }
        }
    }

    //应用主题（可设值：1，2）并刷新界面
    public synchronized void applyThemeAndRefreshWindows(int theme) {
        if (theme == getAppliedTheme()) return;
        applyTheme(theme);
        updateAllWindows();
    }

    //应用当前设置的主题
    public synchronized void applyThemeOnSet() {
        if (Theme == FollowSystemTheme) {
            applyTheme(getSystemTheme());
        } else {
            applyTheme(Theme);
        }
    }

    //应用当前设置的主题并刷新界面
    public synchronized void applyThemeOnSetAndRefreshWindows() {
        if (Theme == FollowSystemTheme) {
            applyThemeAndRefreshWindows(getSystemTheme());
        } else {
            applyThemeAndRefreshWindows(Theme);
        }
    }

    //获取系统主题
    abstract public int getSystemTheme();

    //注册主题变换监听器
    abstract public void setupThemeListener(int theme);
}