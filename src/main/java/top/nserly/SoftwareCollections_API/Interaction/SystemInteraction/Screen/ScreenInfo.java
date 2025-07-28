package top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Screen;

import lombok.Getter;

import java.awt.*;

/**
 * 封装单个屏幕的信息
 */
public class ScreenInfo {
    // 屏幕索引
    @Getter
    private final int index;
    private final Dimension screenSize;
    private final Dimension usableScreenSize;
    // 屏幕左上角X坐标
    @Getter
    private final int x;
    // 屏幕左上角Y坐标
    @Getter
    private final int y;
    // 判断是否为主屏幕
    @Getter
    private final boolean isPrimary;

    public ScreenInfo(int index, Dimension screenSize, Dimension usableScreenSize,
                      int x, int y, boolean isPrimary) {
        this.index = index;
        this.screenSize = screenSize;
        this.usableScreenSize = usableScreenSize;
        this.x = x;
        this.y = y;
        this.isPrimary = isPrimary;
    }

    /**
     * 获取屏幕尺寸（包括任务栏等）
     */
    public Dimension getScreenSize() {
        return new Dimension(screenSize);
    }

    /**
     * 获取可用屏幕尺寸（不包括任务栏等）
     */
    public Dimension getUsableScreenSize() {
        return new Dimension(usableScreenSize);
    }

    @Override
    public String toString() {
        return "Screen " + index + (isPrimary ? " (Primary)" : "") +
                ": Size=" + screenSize.width + "x" + screenSize.height +
                ", Usable=" + usableScreenSize.width + "x" + usableScreenSize.height +
                ", Position=(" + x + "," + y + ")";
    }
}
