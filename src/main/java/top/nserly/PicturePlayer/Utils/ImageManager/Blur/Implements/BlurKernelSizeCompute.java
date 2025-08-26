package top.nserly.PicturePlayer.Utils.ImageManager.Blur.Implements;

import java.awt.image.BufferedImage;

public class BlurKernelSizeCompute {

    // 动态计算kernelSize（在没必要模糊的场景直接返回1）
    public static int calculateKernelSize(BufferedImage bufferedImage, double scaleFactor) {
        // 输入验证
        if (bufferedImage == null) {
            throw new IllegalArgumentException("Images cannot be null");
        }
        if (scaleFactor <= 0) {
            throw new IllegalArgumentException("The scaling factor must be positive");
        }

        scaleFactor /= 100.0;

        // 获取图像尺寸
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();


        // 核心判断：明确不需要模糊的场景，直接返回1
        // 1. 缩放因子非常接近1.0（±8%以内）
        // 2. 图像本身尺寸很小（宽或高小于50像素）- 小图像模糊后更看不清
        // 3. 高分辨率图像的微小缩放（宽高都大于1000且缩放变化<15%）
        boolean isNearlyOriginalScale = Math.abs(scaleFactor - 1.0) <= 0.08;
        boolean isTinyImage = width < 50 || height < 50;
        boolean isHighResWithSmallChange = (width > 1000 && height > 1000) &&
                (Math.abs(scaleFactor - 1.0) <= 0.15);

        if (isNearlyOriginalScale || isTinyImage || isHighResWithSmallChange || (scaleFactor - 1) >= 0.1) {
            return 1; // 这些情况不需要模糊
        }

        return getBaseKernel(scaleFactor);
    }

    private static int getBaseKernel(double scaleFactor) {
        // 计算缩放差异
        double scaleDiff = Math.abs(scaleFactor - 1.0);

        // 计算基础核大小（只在确实需要模糊时）
        int baseKernel;
        if (scaleFactor < 1.0) { // 缩小图像
            baseKernel = (int) Math.round(scaleDiff * 12);
        } else { // 放大图像
            baseKernel = (int) Math.round(scaleDiff * 8); // 放大对模糊需求更低
        }

        // 确保核大小在有效范围内（3-21之间的奇数）
        baseKernel = Math.max(3, baseKernel); // 最小有效核大小
        baseKernel = Math.min(21, baseKernel); // 最大不超过21，避免过度模糊

        // 确保核大小为奇数
        if (baseKernel % 2 == 0) {
            baseKernel++;
        }
        return baseKernel;
    }
}
