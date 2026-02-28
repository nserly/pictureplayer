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

package top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Screen.Linux;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Screen.ScreenInfo;
import top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Screen.ScreenProvider;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// 定义 X11 库接口，使用IntByReference替代UIntByReference
interface X11 extends Library {
    X11 INSTANCE = Native.load("X11", X11.class);

    Pointer XOpenDisplay(String display_name);
    int XScreenCount(Pointer display);
    Pointer XRootWindow(Pointer display, int screen_number);
    // 全部使用IntByReference适配旧版JNA
    int XGetGeometry(Pointer display, Pointer drawable, PointerByReference root_return,
                     IntByReference x_return, IntByReference y_return,
                     IntByReference width_return, IntByReference height_return,
                     IntByReference border_width_return, IntByReference depth_return);
    void XCloseDisplay(Pointer display);
}

@Slf4j
public class LinuxScreenProvider implements ScreenProvider {
    @Override
    public List<ScreenInfo> reloadScreens() {
        // 获取 X11 显示
        Pointer display = X11.INSTANCE.XOpenDisplay(null);
        if (display == null) {
            throw new RuntimeException("无法打开 X11 显示");
        }

        try {
            // 获取屏幕数量
            int screenCount = X11.INSTANCE.XScreenCount(display);
            List<ScreenInfo> screens = new ArrayList<>(screenCount);

            for (int i = 0; i < screenCount; i++) {
                // 获取屏幕信息
                Pointer rootWindow = X11.INSTANCE.XRootWindow(display, i);

                // 全部使用IntByReference
                PointerByReference rootRef = new PointerByReference();
                IntByReference xRef = new IntByReference();
                IntByReference yRef = new IntByReference();
                IntByReference widthRef = new IntByReference();
                IntByReference heightRef = new IntByReference();
                IntByReference borderWidthRef = new IntByReference();
                IntByReference depthRef = new IntByReference();

                // 调用XGetGeometry
                int result = X11.INSTANCE.XGetGeometry(
                        display,
                        rootWindow,
                        rootRef,
                        xRef,
                        yRef,
                        widthRef,
                        heightRef,
                        borderWidthRef,
                        depthRef
                );

                if (result == 0) {
                    log.error("无法获取屏幕 {} 的几何信息", i);
                    continue;
                }

                // 处理尺寸信息（确保为非负值）
                int width = Math.abs(widthRef.getValue());
                int height = Math.abs(heightRef.getValue());

                // 创建 Dimension 对象
                Dimension screenSize = new Dimension(width, height);
                Dimension usableScreenSize = new Dimension(width, height);

                // 创建 ScreenInfo 对象
                ScreenInfo screenInfo = new ScreenInfo(
                        i,
                        screenSize,
                        usableScreenSize,
                        xRef.getValue(),
                        yRef.getValue(),
                        i == 0
                );
                screens.add(screenInfo);
            }

            return screens;
        } finally {
            // 确保显示被关闭
            X11.INSTANCE.XCloseDisplay(display);
        }
    }

    @Override
    public ScreenInfo getScreenForWindow(long windowHandle) {
        // 获取 X11 显示
        Pointer display = X11.INSTANCE.XOpenDisplay(null);
        if (display == null) {
            throw new RuntimeException("无法打开 X11 显示");
        }

        try {
            // 获取屏幕数量
            int screenCount = X11.INSTANCE.XScreenCount(display);

            for (int i = 0; i < screenCount; i++) {
                // 获取屏幕的根窗口
                Pointer rootWindow = X11.INSTANCE.XRootWindow(display, i);

                // 检查窗口是否属于当前屏幕
                if (windowHandle == Pointer.nativeValue(rootWindow)) {
                    // 使用IntByReference
                    PointerByReference rootRef = new PointerByReference();
                    IntByReference xRef = new IntByReference();
                    IntByReference yRef = new IntByReference();
                    IntByReference widthRef = new IntByReference();
                    IntByReference heightRef = new IntByReference();
                    IntByReference borderWidthRef = new IntByReference();
                    IntByReference depthRef = new IntByReference();

                    int result = X11.INSTANCE.XGetGeometry(
                            display,
                            rootWindow,
                            rootRef,
                            xRef,
                            yRef,
                            widthRef,
                            heightRef,
                            borderWidthRef,
                            depthRef
                    );

                    if (result == 0) {
                        throw new RuntimeException("无法获取屏幕几何信息");
                    }

                    // 处理尺寸信息
                    int width = Math.abs(widthRef.getValue());
                    int height = Math.abs(heightRef.getValue());

                    // 创建 Dimension 对象
                    Dimension screenSize = new Dimension(width, height);
                    Dimension usableScreenSize = new Dimension(width, height);

                    // 创建并返回 ScreenInfo 对象
                    return new ScreenInfo(
                            i,
                            screenSize,
                            usableScreenSize,
                            xRef.getValue(),
                            yRef.getValue(),
                            i == 0
                    );
                }
            }

            throw new RuntimeException("未找到窗口所在的屏幕");
        } finally {
            X11.INSTANCE.XCloseDisplay(display);
        }
    }
}
