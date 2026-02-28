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

package top.nserly.PicturePlayer.Utils.ImageManager.Info.ImageDPI.Implements;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import top.nserly.PicturePlayer.Utils.ImageManager.Info.ImageDPI.ImageDPI;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 静态方法实现的GDI+ DPI获取工具类
 * 全局只初始化一次GDI+，适合高频次调用
 */
public class WindowsImageDpiGetter {
    // 静态锁 - 保证线程安全
    private static final Lock operationLock = new ReentrantLock();
    // 静态变量 - GDI+初始化令牌
    private static Pointer gdiToken;
    // 初始化状态标记
    private static volatile boolean isInitialized = false;

    // 静态初始化块 - 注册JVM关闭钩子
    static {
        // 程序退出时关闭GDI+
        Runtime.getRuntime().addShutdownHook(new Thread(WindowsImageDpiGetter::shutdownGDIPlus));
    }

    /**
     * 初始化GDI+（仅首次调用时执行）
     */
    private static void initializeGDIPlus() {
        if (isInitialized) return;

        operationLock.lock();
        try {
            if (!isInitialized) {
                GDIPlus.GdiplusStartupInput startupInput = new GDIPlus.GdiplusStartupInput();
                PointerByReference tokenRef = new PointerByReference();
                int status = GDIPlus.INSTANCE.GdiplusStartup(tokenRef, startupInput, null);

                if (status != GDIPlus.Ok) {
                    throw new RuntimeException("GDI+ initialization failed, error code: " + status);
                }

                gdiToken = tokenRef.getValue();
                isInitialized = true;
            }
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * 关闭GDI+（由JVM关闭钩子调用）
     */
    private static void shutdownGDIPlus() {
        if (!isInitialized) return;

        operationLock.lock();
        try {
            if (isInitialized && gdiToken != null) {
                GDIPlus.INSTANCE.GdiplusShutdown(gdiToken);
                gdiToken = null;
                isInitialized = false;
            }
        } finally {
            operationLock.unlock();
        }
    }

    /**
     * 静态方法：获取图像DPI
     * @param filePath 图像文件路径
     * @return ImageDPI对象
     * @throws IOException 异常信息
     */
    public static ImageDPI getImageDPI(String filePath) throws IOException {
        // 确保GDI+已初始化
        initializeGDIPlus();
        if (!isInitialized) {
            throw new IllegalStateException("GDI+ is not initialized properly");
        }

        operationLock.lock();
        PointerByReference image = new PointerByReference();
        float[] horizontalDpi = new float[1];
        float[] verticalDpi = new float[1];

        try {
            // 加载图像
            int status = GDIPlus.INSTANCE.GdipLoadImageFromFile(new WString(filePath), image);
            if (status != GDIPlus.Ok) {
                throw new IOException("Failed to load image [" + filePath + "], error code: " + status);
            }

            // 获取水平DPI
            status = GDIPlus.INSTANCE.GdipGetImageHorizontalResolution(image.getValue(), horizontalDpi);
            if (status != GDIPlus.Ok) {
                throw new IOException("Failed to get horizontal DPI, error code: " + status);
            }

            // 获取垂直DPI
            status = GDIPlus.INSTANCE.GdipGetImageVerticalResolution(image.getValue(), verticalDpi);
            if (status != GDIPlus.Ok) {
                throw new IOException("Failed to get vertical DPI, error code: " + status);
            }

            return new ImageDPI(horizontalDpi[0], verticalDpi[0]);
        } finally {
            // 释放图像资源
            if (image.getValue() != null) {
                GDIPlus.INSTANCE.GdipDisposeImage(image.getValue());
            }
            operationLock.unlock();
        }
    }

    // GDI+库接口
    private interface GDIPlus extends StdCallLibrary {
        GDIPlus INSTANCE = Native.load("gdiplus", GDIPlus.class, W32APIOptions.DEFAULT_OPTIONS);

        int Ok = 0;

        // GDI+核心方法
        int GdiplusStartup(PointerByReference token, GdiplusStartupInput input, Pointer output);

        void GdiplusShutdown(Pointer token);

        int GdipLoadImageFromFile(WString filename, PointerByReference image);

        int GdipGetImageHorizontalResolution(Pointer image, float[] resolution);

        int GdipGetImageVerticalResolution(Pointer image, float[] resolution);

        int GdipDisposeImage(Pointer image);

        class GdiplusStartupInput extends Structure {
            public int GdiplusVersion = 1;
            public Pointer DebugEventCallback;
            public boolean SuppressBackgroundThread = false;
            public boolean SuppressExternalCodecs = false;

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList("GdiplusVersion", "DebugEventCallback",
                        "SuppressBackgroundThread", "SuppressExternalCodecs");
            }
        }
    }
}
