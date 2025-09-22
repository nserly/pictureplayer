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
