package top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Screen;

import java.util.List;

/**
 * 屏幕信息获取接口，定义平台无关的方法
 */
public interface ScreenProvider {
    /**
     * 重新加载所有屏幕信息
     * @return 所有屏幕信息列表
     */
    List<ScreenInfo> reloadScreens();

    /**
     * 获取窗口所在的屏幕
     * @param windowHandle 窗口句柄
     * @return 窗口所在的屏幕信息，若未找到则返回主屏幕
     */
    ScreenInfo getScreenForWindow(long windowHandle);
}
