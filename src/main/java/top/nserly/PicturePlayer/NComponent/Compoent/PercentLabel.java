package top.nserly.PicturePlayer.NComponent.Compoent;

import javax.swing.*;

public class PercentLabel extends JLabel {

    public void set(int percent) {
        String text = percent + "%";
        String lastText = getText();
        super.setText(text);
        if (text.length() != lastText.length() && PaintPicturePanel.paintPicture != null && PaintPicturePanel.paintPicture.AboveMainPanel != null) {
            PaintPicturePanel.paintPicture.AboveMainPanel.revalidate();
        }

    }

    public PercentLabel() {
        super();
        super.setText(0 + "%");
        this.setVisible(true);
    }
}