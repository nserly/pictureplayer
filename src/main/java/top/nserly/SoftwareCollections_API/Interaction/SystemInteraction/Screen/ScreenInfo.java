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
