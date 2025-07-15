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