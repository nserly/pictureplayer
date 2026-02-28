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

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FontPreservingUIUpdater {
    public static void updateComponentTreeUIWithFontPreservation(Component comp) {
        // 1. 保存所有组件的原始字体
        Map<Component, Font> fontMap = new HashMap<>();
        collectFonts(comp, fontMap);

        // 2. 正常更新UI
        SwingUtilities.updateComponentTreeUI(comp);

        // 3. 恢复原始字体
        restoreFonts(comp, fontMap);
    }

    private static void collectFonts(Component comp, Map<Component, Font> fontMap) {
        fontMap.put(comp, comp.getFont());
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                collectFonts(child, fontMap);
            }
        }
    }

    private static void restoreFonts(Component comp, Map<Component, Font> fontMap) {
        Font originalFont = fontMap.get(comp);
        if (originalFont != null) {
            comp.setFont(originalFont);
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                restoreFonts(child, fontMap);
            }
        }
    }
}