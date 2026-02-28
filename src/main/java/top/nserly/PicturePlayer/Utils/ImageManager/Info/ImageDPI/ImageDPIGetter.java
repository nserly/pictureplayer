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

package top.nserly.PicturePlayer.Utils.ImageManager.Info.ImageDPI;

import top.nserly.PicturePlayer.Utils.ImageManager.Info.ImageDPI.Implements.DefaultImageDpiGetter;
import top.nserly.PicturePlayer.Utils.ImageManager.Info.ImageDPI.Implements.WindowsImageDpiGetter;

import java.io.IOException;

public class ImageDPIGetter {

    /**
     * 通过JNA调用GDI+获取图像DPI
     *
     * @param filePath 图像文件路径
     * @return ImageDPI对象
     */
    public static ImageDPI getImageDPI(String filePath) throws IOException {
        ImageDPI imageDPI;
        if (System.getProperty("os.name").toLowerCase().contains("win"))
            try {
                imageDPI = WindowsImageDpiGetter.getImageDPI(filePath);
            } catch (Exception e) {
                imageDPI = DefaultImageDpiGetter.getImageDPI(filePath);
            }
        else imageDPI = DefaultImageDpiGetter.getImageDPI(filePath);

        return imageDPI;
    }

}
