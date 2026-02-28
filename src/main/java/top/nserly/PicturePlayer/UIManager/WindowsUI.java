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
