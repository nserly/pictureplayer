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
