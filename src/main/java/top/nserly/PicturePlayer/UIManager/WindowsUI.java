package top.nserly.PicturePlayer.UIManager;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;

@Slf4j
public class WindowsUI extends UIManager {
    protected WindowsUI() {
        super();
        System.setProperty("flatlaf.win.enableSystemThemeChangeListener", "true");
    }

    @Override
    public int getSystemTheme() {
        try {
            final String keyPath = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
            final String valueName = "AppsUseLightTheme";

            if (Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, keyPath)) {
                int value = Advapi32Util.registryGetIntValue(
                        WinReg.HKEY_CURRENT_USER,
                        keyPath,
                        valueName
                );
                return ++value;
            }
        } catch (Exception e) {
            log.error("Registry read failed");
            log.error(ExceptionHandler.getExceptionMessage(e));
        }
        return UIManager.LightTheme;
    }

    @Override
    public void setupThemeListener(int theme) {
        if (getTheme() == UIManager.FollowSystemTheme) {
            applyThemeAndRefreshWindows(theme);
        }

    }

}
