package top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Screen;

import java.awt.*;

/**
 * 封装单个屏幕的信息
 *
 * @param index     屏幕索引
 * @param x         屏幕左上角X坐标
 * @param y         屏幕左上角Y坐标
 * @param isPrimary 判断是否为主屏幕
 */
public record ScreenInfo(int index, Dimension screenSize, Dimension usableScreenSize, int x,
                         int y, boolean isPrimary) {

    /**
     * 获取屏幕尺寸（包括任务栏等）
     */
    @Override
    public Dimension screenSize() {
        return new Dimension(screenSize);
    }

    /**
     * 获取可用屏幕尺寸（不包括任务栏等）
     */
    @Override
    public Dimension usableScreenSize() {
        return new Dimension(usableScreenSize);
    }
}
