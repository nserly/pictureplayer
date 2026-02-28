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

package top.nserly.PicturePlayer.NComponent.Component;

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