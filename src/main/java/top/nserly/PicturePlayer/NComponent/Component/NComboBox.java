package top.nserly.PicturePlayer.NComponent.Component;

import javax.swing.*;

public class NComboBox extends JComboBox<String> {
    {
        this.setModel(new DefaultComboBoxModel<>());
        setRequestFocusEnabled(false);
    }
}