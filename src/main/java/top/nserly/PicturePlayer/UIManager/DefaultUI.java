package top.nserly.PicturePlayer.UIManager;

public class DefaultUI extends UIManager{
    @Override
    public int getSystemTheme() {
        return 1;
    }

    @Override
    public void setupThemeListener(int theme) {
        if (getTheme() == UIManager.FollowSystemTheme) {
            applyTheme(theme);
            updateAllWindows();
        }
    }
}
