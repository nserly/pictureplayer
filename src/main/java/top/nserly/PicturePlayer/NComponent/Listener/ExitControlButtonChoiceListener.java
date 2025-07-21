package top.nserly.PicturePlayer.NComponent.Listener;

public interface ExitControlButtonChoiceListener {
    //最小化到系统托盘
    int EXIT_TO_SYSTEM_TRAY = 0;
    //直接退出
    int EXIT_DIRECTLY = 1;
    //取消退出
    int EXIT_CANCEL = 2;

    void exit(int choice, boolean doNotAppear);
}
