package top.nserly.PicturePlayer.Size;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;

public interface User32 extends StdCallLibrary {
    User32 INSTANCE = Native.load("user32", User32.class);

    // 定义 SystemParametersInfo 函数
    boolean SystemParametersInfoA(int uiAction, int uiParam, WinDef.RECT pvParam, int fWinIni);

    // 定义多显示器枚举函数
    int GetSystemMetrics(int nIndex);

    boolean EnumDisplayMonitors(WinNT.HANDLE hdc, WinDef.RECT lprcClip, MONITORENUMPROC lpfnEnum, Pointer dwData);

    boolean GetMonitorInfo(WinNT.HANDLE hMonitor, MONITORINFO lpmi);

    // 定义多显示器枚举回调
    interface MONITORENUMPROC extends StdCallCallback {
        boolean callback(WinNT.HANDLE hMonitor, WinNT.HANDLE hdcMonitor, WinDef.RECT lprcMonitor, Pointer dwData);
    }

    // 定义 MONITORINFO 结构
    class MONITORINFO extends WinUser.MONITORINFO {
        public MONITORINFO() {
            cbSize = size();
        }
    }
}
