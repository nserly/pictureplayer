package top.nserly.PicturePlayer.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.nserly.PicturePlayer.NComponent.Component.PaintPicturePanel;
import top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Screen.ScreenManager;

import java.awt.*;

public class SizeOperate {
    //默认缩放比例
    private final short Default;
    //设置放大时，引用
    public static final int Enlarge = 0;
    //设置缩小时，引用
    public static final int Reduce = 1;
    //当前缩放比例
    private double percent;
    //最大缩放比例
    public final short MaxPercent = 1500;
    //最小缩放比例
    public final short MinPercent = 2;
    //当前图片渲染器
    private PaintPicturePanel.ImageCanvas imageCanvas;
    //当前组件信息
    private Dimension Component;
    //当前最适合组件的比例
    public double FittestPercent;
    //最适合的调节比例
    private int AdjustPercent;
    //图片信息类
    private Dimension PictureSize;
    //屏幕分辨率
    public static final Dimension ScreenSize;
    //可用屏幕尺寸
    public static final Dimension FreeOfScreenSize;
    private static final Logger logger = LoggerFactory.getLogger(SizeOperate.class);

    static {
        //获取屏幕分辨率
        //注意：此处获取的是主屏幕的分辨率，如果有多屏幕环境，可能需要根据实际情况调整
        //如果使用JNA或其他库获取屏幕信息，请确保已正确配置相关依赖
        ScreenSize = ScreenManager.getPrimaryScreen().screenSize();
        //获取可用屏幕分辨率
        //可用屏幕分辨率通常是指不包括任务栏等占用的区域
        //如果有多屏幕环境，可能需要根据实际情况调整
        //注意：此处获取的是主屏幕的可用分辨率，如果有多屏幕环境，可能需要根据实际情况调整
        //如果使用JNA或其他库获取屏幕信息，请确保已正确配置相关依赖
        FreeOfScreenSize = ScreenManager.getPrimaryScreen().usableScreenSize();
    }

    public SizeOperate(PaintPicturePanel.ImageCanvas imageCanvas, Dimension Component) {
        this.Component = Component;
        this.imageCanvas = imageCanvas;
        FittestPercent = percent = Default = 100;
    }


    public SizeOperate(PaintPicturePanel.ImageCanvas imageCanvas, short defaultPercent, Dimension Component) {
        this.imageCanvas = imageCanvas;
        FittestPercent = percent = Default = defaultPercent;
        this.Component = Component;
    }

    //改变图片渲染器
    public void changeCanvas(PaintPicturePanel.ImageCanvas imageCanvas, boolean isRestoreFittest) {
        this.imageCanvas = imageCanvas;
        if (imageCanvas.getPath() != null) {
            PictureSize = new Dimension(imageCanvas.getImageWidth(), imageCanvas.getImageHeight());
            if (Component != null) {
                AdjustPercent = (int) (((Math.abs(Component.getHeight() - PictureSize.height) / 5.5 / PictureSize.height) + (Math.abs(Component.getWidth() - PictureSize.width) / 5.5 / PictureSize.width)) / 2);
            }
        }
        FittestPercent = getPictureOptimalSize();
        if (isRestoreFittest) {
            setPercent(FittestPercent);
            update(false);
        }
    }

    //获取窗体大小
    public Dimension getWindowSize() {
        return Component;
    }

    //获取窗体高度
    public int getWindowHeight() {
        return Component.height;
    }

    //获取窗体宽度
    public int getWindowWight() {
        return Component.height;
    }

    //传入窗体信息（此类无法直接获取窗体信息）
    public void incomeWindowDimension(Dimension window) {
        if (window != null) {
            this.Component = window;
            FittestPercent = getPictureOptimalSize();
            if (PictureSize == null && imageCanvas != null && window.getWidth() != 0 && window.getHeight() != 0)
                try {
                    PictureSize = new Dimension(imageCanvas.getImageWidth(), imageCanvas.getImageHeight());
                } catch (Exception e) {
                    return;
                }
            AdjustPercent = (int) (((Math.abs(window.getHeight() - PictureSize.height) / 5.5 / PictureSize.height) + (Math.abs(window.getWidth() - PictureSize.width) / 5.5 / PictureSize.width)) / 2);
        }
    }

    //获取默认缩放比例
    public short getDefaultPercent() {
        return Default;
    }


    private double decide(double size) {
        double result;
        if (size > MaxPercent) {
            result = MaxPercent;
        } else {
            result = size;
        }
        if (size < MinPercent) {
            result = MinPercent;
        }
        return result;
    }

    //获取图片最佳比例
    public double getPictureOptimalSize() {
        if (Component == null || Component.width == 0 || Component.height == 0) {
            logger.warn("Could not get window optimal size");
            return Default;
        }
        int PictureWidth = imageCanvas.getImageWidth();
        int PictureHeight = imageCanvas.getImageHeight();
        if (imageCanvas.getDegrees() / 90 % 2 == 1) {
            int temp = PictureHeight;
            PictureHeight = PictureWidth;
            PictureWidth = temp;
        }
        if (PictureHeight == 0 && PictureWidth == 0) {
            return Default;
        }
        double WindowsWidth = Component.getWidth();
        double WindowsHeight = Component.getHeight();
        return Math.min(WindowsWidth * 100 / PictureWidth, WindowsHeight * 100 / PictureHeight);
    }

    //恢复默认缩放比例
    public void restoreTheDefaultPercent() {
        percent = Default;
    }


    //是否当前图片显示比例是否是最大比例
    public boolean isTheBiggestRatio() {
        return getPercent() >= MaxPercent;
    }

    //是否当前图片显示比例是否是最小比例
    public boolean isTheSmallestRatio() {
        return getPercent() <= MinPercent;
    }

    //获取缩放比例
    public synchronized double getPercent() {
        return percent;
    }

    //设置缩放比例
    public void setPercent(double percent) {
        if (percent > MaxPercent) {
            this.percent = MaxPercent;
        } else if (percent < MinPercent) {
            this.percent = MinPercent;
        } else this.percent = percent;
    }

    //调节比例（如果返回值为true表示需要刷新，反之不需刷新）
    public boolean adjustPercent(int operate) {
        if ((operate == Enlarge && percent == MaxPercent) || (operate == Reduce && percent == MinPercent)) {
            return false;
        }
        double result = 0;
        switch (operate) {
            case Enlarge -> {
                if (AdjustPercent <= 0) {
                    if (percent < FittestPercent) result = decide(4 + percent);
                    else {
                        result = decide(11 + percent);
                    }
                } else if (percent < FittestPercent) {
                    result = decide(AdjustPercent + percent);
                } else if (percent > FittestPercent) {
                    result = decide(2 * AdjustPercent + percent);
                }
                percent = result;
                if (percent > MaxPercent) {
                    percent = MaxPercent;
                }
            }
            case Reduce -> {
                if (AdjustPercent <= 0) {
                    if (percent < FittestPercent) result = decide(-4 + percent);
                    else {
                        result = decide(-11 + percent);
                    }
                } else if (percent < FittestPercent) result = decide(-AdjustPercent + percent);
                else if (percent > FittestPercent) {
                    result = decide(-2 * AdjustPercent + percent);
                }
                percent = result;
                if (percent < MinPercent) {
                    percent = MaxPercent;
                }
            }
        }
        return true;
    }

    //刷新图片
    public void update(boolean isMove) {
        imageCanvas.setWindowSize(Component);
        imageCanvas.setIsMove(isMove || imageCanvas.getIsMove());
        imageCanvas.repaint();
    }

    //关闭管理
    public void close() {
        percent = 0;
        Component = null;
        imageCanvas.close();
    }
}
