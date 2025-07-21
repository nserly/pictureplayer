package top.nserly.PicturePlayer.Loading;

public @interface DefaultArgs {
    //启用历史路径加载
    boolean EnableHistoryLoader() default true;

    //鼠标移动补偿
    double MouseMoveOffsets() default 0.0;

    //启用代理服务器
    boolean EnableProxyServer() default false;

    //代理服务器
    String ProxyServer() default "";

    //启用安全连接模式
    boolean EnableSecureConnection() default true;

    //启用自动检测更新
    boolean AutoCheckUpdate() default true;

    //启动光标显示
    boolean EnableCursorDisplay() default false;

    //启用硬件加速
    boolean EnableHardwareAcceleration() default true;

    //主题模式（0:跟随系统 1:Dark主题 2:Light主题）
    int ThemeMode() default 0;

    //关闭主窗口时的控制方式（0:显示退出确定窗口 1:直接退出 2:最小化到系统托盘）
    int CloseMainFrameControl() default 0;
}
