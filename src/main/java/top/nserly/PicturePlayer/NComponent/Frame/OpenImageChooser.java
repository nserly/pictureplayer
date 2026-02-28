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

package top.nserly.PicturePlayer.NComponent.Frame;

import top.nserly.PicturePlayer.Loading.Bundle;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class OpenImageChooser {
    public static File openImageWithChoice(Component component, List<File> options) {
        // 创建一个JList模型并添加打开选项
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (File option : options) {
            listModel.addElement(option.getPath());
        }

        // 创建JList并设置模型
        JList<String> list = new JList<>(listModel);

        // 使用JOptionPane显示包含JList的对话框，不显示图标
        int choice = JOptionPane.showOptionDialog(component, new Object[]{new JScrollPane(list)}, Bundle.getMessage("ChoosePictureInManyPictures_Title"),
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (choice == JOptionPane.OK_OPTION) {
            String selectedValue = list.getSelectedValue();
            if (selectedValue != null) {
                return new File(selectedValue);
            }
        }
        return null;
    }
}
