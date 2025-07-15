package top.nserly.PicturePlayer.UIManager;

public class LinuxUI extends UIManager {
    protected LinuxUI() {
        super();
        System.setProperty("flatlaf.gtk", "true");
        System.setProperty("flatlaf.gtk2", "true");
        System.setProperty("flatlaf.gtk.allowQuit", "true");
    }

    @Override
    public int getSystemTheme() {
        return 0;
    }

    @Override
    public void setupThemeListener(int theme) {
        if (getTheme() == UIManager.FollowSystemTheme) {
            applyTheme(theme);
            updateAllWindows();
        }
    }


}
