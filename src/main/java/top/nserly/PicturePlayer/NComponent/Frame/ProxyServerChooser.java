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

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import top.nserly.GUIStarter;
import top.nserly.PicturePlayer.Loading.Bundle;
import top.nserly.PicturePlayer.Utils.ProxyServer.ProxyServerHandle;
import top.nserly.PicturePlayer.Utils.Window.WindowLocation;
import top.nserly.SoftwareCollections_API.String.StringFormation;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

public class ProxyServerChooser extends JDialog {
    private final AddProxyServerFrame addProxyServer;
    private final StringFormation formation = new StringFormation(Bundle.getMessage("AddProxyServer_ErrorMessage_Content"));
    private JPanel contentPane;
    private JButton ChooseThisProxyServerButton;
    private JButton AddProxyServerButton;
    private JButton EditProxyServerButton;
    private JButton DeleteProxyServerButton;
    private JButton CancelProxyServerButton;
    private JButton RefreshProxyServerButton;
    private JTable ProxyServerTable;
    // 表格的数据模型
    DefaultTableModel tableModel = new DefaultTableModel();
    //表格的列
    public static final String[] columnNames = {Bundle.getMessage("ProxyServer_TableFirst"), Bundle.getMessage("ProxyServer_TableSecond")};
    public ProxyServerHandle handle;

    public ProxyServerChooser() {
        $$$setupUI$$$();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(ChooseThisProxyServerButton);
        handle = new ProxyServerHandle("data\\ProxyServerMenu.pxs");
        addProxyServer = new AddProxyServerFrame(this);
        refresh(false);

        ChooseThisProxyServerButton.addActionListener(e -> choice());

        AddProxyServerButton.addActionListener(e -> add());

        EditProxyServerButton.addActionListener(e -> {
            //获取被选中的行号
            int row = ProxyServerTable.getSelectedRow();
            edit(row);
        });

        DeleteProxyServerButton.addActionListener(e -> {
            //获取被选中的行号
            int[] rows = ProxyServerTable.getSelectedRows();
            if (rows.length > 0) {
                if (JOptionPane.showConfirmDialog(this, Bundle.getMessage("ProxyServer_Delete_Content"), Bundle.getMessage("ProxyServer_Delete_Title"), JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
                    delete(rows);
                }

            }
        });

        CancelProxyServerButton.addActionListener(e -> cancel());

        RefreshProxyServerButton.addActionListener(e -> refresh(true));

        // 点击 X 时调用 onCancel()
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });

        // 遇到 ESCAPE 时调用 onCancel()
        contentPane.registerKeyboardAction(e -> cancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    /**
     * 选择代理服务器
     */

    private void choice() {
        //获取被选中的行号
        int row = ProxyServerTable.getSelectedRow();
        if (row != -1) {
            String str = (String) tableModel.getValueAt(row, 0);
            if (str != null && !str.trim().isEmpty()) {
                setVisible(false);
                handle.setCurrentSelectionProxyServerName(str);
                GUIStarter.main.setProxyServerOfInit(handle.getProxyServerAddress(str));
                handle.save();
            }
        }
    }

    /**
     * 关闭界面
     */
    private void cancel() {
        setVisible(false);
    }

    /**
     * 刷新代理服务器列表
     *
     * @param isSetRowSelectionInterval 是否选中当前已配置代理服务器的那一行
     */

    public void refresh(boolean isSetRowSelectionInterval) {
        ProxyServerTable.getSelectionModel().clearSelection();
        handle.refresh();
        tableModel.setRowCount(0);
        handle.getAllProxyServerNames().forEach(this::addElement);
        if (isSetRowSelectionInterval) {
            setProxyServerSelected();
        }
    }

    /**
     * 选中当前选择代理服务器的行
     */
    public void setProxyServerSelected() {
        String ProxyServerName = handle.getCurrentSelectionProxyServerName();
        if (handle.containsProxyServerName(ProxyServerName)) {
            setRowSelectionInterval(findRowIndexByColumnContent(0, ProxyServerName));
        }
    }

    /**
     * 添加服务器列表
     * 自动从handle中获取服务器地址（位于第二列）
     *
     * @param ProxyServerName 服务器名称（位于第一列）
     */

    private void addElement(String ProxyServerName) {
        tableModel.addRow(new String[]{ProxyServerName, handle.getProxyServerAddress(ProxyServerName)});
    }

    /**
     * 添加服务器列表
     *
     * @param ProxyServerName    服务器名称（位于第一列）
     * @param ProxyServerAddress 服务器地址（位于第二列）
     */

    private void addElement(String ProxyServerName, String ProxyServerAddress) {
        tableModel.addRow(new String[]{ProxyServerName, ProxyServerAddress});
    }

    /**
     * 添加代理服务器（运行添加界面）
     */

    private void add() {
        addProxyServer.pack();
        addProxyServer.loadInformationAndVisible(-1);
    }

    private void edit(int rowIndex) {
        if (rowIndex == -1) return;
        String ProxyServerName = (String) tableModel.getValueAt(rowIndex, 0);
        addProxyServer.ProxyServerNameTextField.setText(ProxyServerName);
        addProxyServer.ProxyServerAddressTextField.setText(handle.getProxyServerAddress(ProxyServerName));
        addProxyServer.pack();
        addProxyServer.loadInformationAndVisible(rowIndex);
    }

    /**
     * 添加代理服务器列表
     *
     * @param ProxyServerName    服务器名称
     * @param ProxyServerAddress 服务器地址
     * @param rowIndex           表格中的行号（若行数小于0则代表是添加新的行）
     */
    public void addNewProxyServer(String ProxyServerName, String ProxyServerAddress, int rowIndex) {
        if (ProxyServerAddress.isBlank()) return;
        boolean isExist = handle.containsProxyServerName(ProxyServerName);
        ProxyServerName = ProxyServerName.isBlank() ? ProxyServerAddress : ProxyServerName;
        if (rowIndex < 0) {
            if (!isExist)
                addElement(ProxyServerName, ProxyServerAddress);
            else {
                if (handle.getProxyServerAddress(ProxyServerName).equals(ProxyServerAddress)) return;
                formation.add("originalProxyServerAddress", handle.getProxyServerAddress(ProxyServerName));
                formation.add("newProxyServerAddress", ProxyServerAddress);
                if (JOptionPane.showConfirmDialog(this, formation.getProcessingString(), Bundle.getMessage("AddProxyServer_ErrorMessage_Title"), JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
                    handle.delete(ProxyServerName);
                    tableModel.setValueAt(ProxyServerAddress, findRowIndexByColumnContent(0, ProxyServerName), 1);
                } else return;
            }
        } else {
            handle.delete((String) tableModel.getValueAt(rowIndex, 0));
            tableModel.setValueAt(ProxyServerName, rowIndex, 0);
            tableModel.setValueAt(ProxyServerAddress, rowIndex, 1);
        }
        handle.add(ProxyServerName, ProxyServerAddress);
        handle.save();
        addProxyServer.clear();
    }

    /**
     * 删除行
     *
     * @param rowIndex 表格中的行
     */

    public void delete(int rowIndex) {
        if (rowIndex >= tableModel.getRowCount()) return;
        handle.delete((String) tableModel.getValueAt(rowIndex, 0));
        tableModel.removeRow(rowIndex);
        handle.save();
    }

    /**
     * 删除当前所需删除的行
     *
     * @param rowIndex 表格中的行
     */
    public void delete(int[] rowIndex) {
        int temp;
        for (int i = 0; i < rowIndex.length - 1; i++) {
            for (int j = 0; j < rowIndex.length - 1 - i; j++) {
                if (rowIndex[j] < rowIndex[j + 1]) {
                    temp = rowIndex[j];
                    rowIndex[j] = rowIndex[j + 1];
                    rowIndex[j + 1] = temp;
                }
            }
        }
        for (int i : rowIndex) {
            delete(i);
        }
    }


    /**
     * 查找指定列（列索引为columnIndex）中内容等于content的行索引
     *
     * @param columnIndex 目标列的索引
     * @param content     要匹配的内容
     * @return 找到匹配内容的行索引，未找到返回-1
     */
    public int findRowIndexByColumnContent(int columnIndex, String content) {
        var dataVector = tableModel.getDataVector();
        if (columnIndex < 0 || dataVector.isEmpty()) return -1;

        for (int rowIdx = 0; rowIdx < dataVector.size(); rowIdx++) {
            var rowData = dataVector.get(rowIdx);
            // 检查列索引是否在有效范围内
            if (columnIndex >= rowData.size()) continue;
            Object cellValue = rowData.get(columnIndex);
            if (content.equals(cellValue)) {
                return rowIdx;
            }
        }
        return -1;
    }

    /**
     * 设置选中哪一行
     *
     * @param rowIndex 行
     */
    private void setRowSelectionInterval(int rowIndex) {
        if (rowIndex < 0 || rowIndex > ProxyServerTable.getRowCount() - 1) return;
        ProxyServerTable.setRowSelectionInterval(rowIndex, rowIndex); // 单选模式
        ProxyServerTable.scrollRectToVisible(ProxyServerTable.getCellRect(rowIndex, 0, true));
    }

    public void setVisible(boolean visible) {
        if (visible) setProxyServerSelected();
        setLocation(WindowLocation.componentCenter(GUIStarter.main, getWidth(), getHeight()));
        super.setVisible(visible);
    }

    private void createUIComponents() {
        //重写isCellEditable方法，设置为不可编辑
        tableModel = new DefaultTableModel(new String[0][], columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ProxyServerTable = new JTable(tableModel);
//        RowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
//        ProxyServerTable.setRowSorter(sorter);
        // 覆盖 JTable 默认的 Enter 键行为（非编辑状态）
        KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        ProxyServerTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enterKey, "none");

        // 覆盖编辑状态下的 Enter 键行为（通过监听编辑器）
        ProxyServerTable.addPropertyChangeListener("tableCellEditor", evt -> {
            if (ProxyServerTable.isEditing()) {
                Component editorComp = ProxyServerTable.getEditorComponent();
                if (editorComp instanceof JTextComponent textComp) {
                    // 移除原本 Enter 键的提交动作，替换为新的Enter动作
                    textComp.getInputMap().put(enterKey, new KeyAdapter() {
                        @Override
                        public void keyPressed(KeyEvent e) {
                            choice();
                        }
                    });
                }
            }
        });
        ProxyServerTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) choice();
            }
        });
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setToolTipText("");
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        ChooseThisProxyServerButton = new JButton();
        ChooseThisProxyServerButton.setHorizontalTextPosition(0);
        ChooseThisProxyServerButton.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(ChooseThisProxyServerButton, this.$$$getMessageFromBundle$$$("messages", "ChooseThisProxyServerButton"));
        panel2.add(ChooseThisProxyServerButton, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        AddProxyServerButton = new JButton();
        AddProxyServerButton.setHorizontalTextPosition(0);
        AddProxyServerButton.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(AddProxyServerButton, this.$$$getMessageFromBundle$$$("messages", "AddProxyServerButton"));
        panel2.add(AddProxyServerButton, new GridConstraints(0, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        EditProxyServerButton = new JButton();
        EditProxyServerButton.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(EditProxyServerButton, this.$$$getMessageFromBundle$$$("messages", "EditProxyServerButton"));
        panel2.add(EditProxyServerButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        DeleteProxyServerButton = new JButton();
        DeleteProxyServerButton.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(DeleteProxyServerButton, this.$$$getMessageFromBundle$$$("messages", "DeleteProxyServerButton"));
        panel2.add(DeleteProxyServerButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        CancelProxyServerButton = new JButton();
        CancelProxyServerButton.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(CancelProxyServerButton, this.$$$getMessageFromBundle$$$("messages", "CancelProxyServerButton"));
        panel2.add(CancelProxyServerButton, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        RefreshProxyServerButton = new JButton();
        RefreshProxyServerButton.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(RefreshProxyServerButton, this.$$$getMessageFromBundle$$$("messages", "RefreshProxyServerButton"));
        panel2.add(RefreshProxyServerButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setRequestFocusEnabled(false);
        scrollPane1.setToolTipText("");
        panel3.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setViewportView(ProxyServerTable);
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    /**
     * @noinspection ALL
     */
    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
