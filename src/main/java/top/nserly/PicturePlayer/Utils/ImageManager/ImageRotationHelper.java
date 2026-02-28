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

package top.nserly.PicturePlayer.Utils.ImageManager;

import java.awt.*;

public class ImageRotationHelper {
    //获取未旋转状态下的坐标
    public static Point getOriginalCord(int rotatedX, int rotatedY, int angle, int width, int height) {
        int originalX = rotatedX;
        int originalY = rotatedY;
        switch (angle / 90 % 4) {
            case 1 -> {
                originalX = rotatedY;
                originalY = height - rotatedX;
            }
            case 2 -> {
                originalX = width - rotatedX;
                originalY = height - rotatedY;
            }
            case 3 -> {
                originalX = height - rotatedY;
                originalY = rotatedX;
            }
        }
        return new Point(originalX, originalY);
    }

    //获取旋转状态下的坐标
    public static Point getRotatedCord(int originalX, int originalY, int angle, int width, int height) {
        int newX = originalX;
        int newY = originalY;
        switch (angle / 90 % 4) {
            case 1 -> {
                newX = height - originalY;
                newY = originalX;
            }
            case 2 -> {
                newX = height - originalY;
                newY = height - originalY;
            }
            case 3 -> {
                newX = originalY;
                newY = height - originalX;
            }
        }
        return new Point(newX, newY);
    }

}
