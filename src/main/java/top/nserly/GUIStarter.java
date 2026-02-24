package top.nserly;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.nserly.PicturePlayer.Loading.Bundle;
import top.nserly.PicturePlayer.Loading.Init;
import top.nserly.PicturePlayer.NComponent.Component.NComboBox;
import top.nserly.PicturePlayer.NComponent.Component.PaintPicturePanel;
import top.nserly.PicturePlayer.NComponent.Component.ThumbnailPreviewOfImage;
import top.nserly.PicturePlayer.NComponent.Frame.ConfirmUpdateDialog;
import top.nserly.PicturePlayer.NComponent.Frame.ExitControlDialog;
import top.nserly.PicturePlayer.NComponent.Frame.OpenImageChooser;
import top.nserly.PicturePlayer.NComponent.Frame.ProxyServerChooser;
import top.nserly.PicturePlayer.NComponent.Listener.ChangeFocusListener;
import top.nserly.PicturePlayer.NComponent.Listener.ExitControlButtonChoiceListener;
import top.nserly.PicturePlayer.Settings.SettingsInfoHandle;
import top.nserly.PicturePlayer.Size.SizeOperate;
import top.nserly.PicturePlayer.UIManager.FontPreservingUIUpdater;
import top.nserly.PicturePlayer.UIManager.UIManager;
import top.nserly.PicturePlayer.Utils.ImageManager.Blur.Implements.OpenCLBlurProcessor;
import top.nserly.PicturePlayer.Utils.ImageManager.Blur.Implements.OpenCLSupportChecker;
import top.nserly.PicturePlayer.Utils.ImageManager.CheckFileIsRightPictureType;
import top.nserly.PicturePlayer.Utils.ImageManager.Info.GetImageInformation;
import top.nserly.PicturePlayer.Version.DownloadChecker.CheckAndDownloadUpdate;
import top.nserly.PicturePlayer.Version.PicturePlayerVersion;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;
import top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.SoftwareChannel.HandleSoftwareRequestAction;
import top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.SoftwareChannel.WindowsAppMutex;
import top.nserly.SoftwareCollections_API.Interaction.SystemInteraction.Notifications.SystemNotifications;
import top.nserly.SoftwareCollections_API.OSInformation.SystemMonitor;
import top.nserly.SoftwareCollections_API.String.StringFormation;
import top.nserly.SoftwareCollections_API.Thread.ThreadControl;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class GUIStarter extends JFrame {
    //初始化
    public static final Init<String, String> init = new Init<>();
    public static GUIStarter main;
    private JPanel panel1;
    private JTabbedPane tabbedPane1;
    private JCheckBox EnableCursorDisplayCheckBox;
    private JCheckBox EnableHistoryLoaderCheckBox;
    private JLabel MouseMoveOffsetsLabel;
    private JSlider MouseMoveOffsetsSlider;
    private JCheckBox EnableProxyServerCheckBox;
    private JLabel ProxyServerLabel;
    private JButton ProxyServerButton;
    private JCheckBox EnableSecureConnectionCheckBox;
    private JCheckBox AutoCheckUpdateCheckBox;
    private JButton SaveButton;
    private JButton ResetButton;
    private JButton RefreshButton;
    private JLabel JVMVersionLabel;
    private JLabel CurrentSoftwareVersionLabel;
    private JButton CheckVersionButton;
    public JTextField textField1;
    private JButton TurnButton;
    private JPanel SecondPanel;
    private JLabel TopLabel;
    private JLabel VersionView;
    private JLabel CurrentSoftwareInteriorLabel;
    private JLabel OSLabel;
    private JLabel CurrentSoftwareLanguage;
    private JLabel MemUsed;
    private JPanel FileChoosePane;
    private JPanel FirstPanel;
    private JPanel ThirdPanel;
    private JPanel FourthPanel;
    private JLabel TotalThread;
    private JLabel DefaultJVMMem;
    private JLabel ProgramStartTime;
    private JLabel CPUName;
    private JLabel JavaPath;
    private JCheckBox EnableHardwareAccelerationCheckBox;
    private JPanel A;
    private JPanel B;
    private JLabel Display_1st;
    private JLabel Display_2nd;
    private JLabel Display_3rd;
    private JButton FreeUpMemory;
    private static final String[] ThemeComboBoxStringItems = new String[]{"Theme_0", "Theme_1", "Theme_2"};
    private static final String[] CloseMainFrameControlComboBoxStringItems = new String[]{"CloseMainFrameControl_0", "CloseMainFrameControl_1", "CloseMainFrameControl_2"};
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> future;
    private final ChangeFocusListener changeFocusListener;
    //是否启用代理服务器
    private static boolean EnableProxyServer;
    //最新版本下载地址（如果当前是最新版本，则返回null值）
    private static List<String> NewVersionDownloadingWebSide;
    //更新网站（必须指定VersionID.sum下载地址）
    public static String UPDATE_WEBSITE = "https://gitee.com/nserly-huaer/ImagePlayer/raw/master/artifacts/PicturePlayer_jar/VersionID.sum";
    String MouseMoveLabelPrefix;
    String ProxyServerPrefix;
    public SettingsInfoHandle centre;
    private boolean IsFreshen;
    private static final File REPEAT_PICTURE_PATH_LOGOTYPE = new File("???");
    private static ProxyServerChooser proxyServerChooser;
    private static MenuItem[] SystemTrayMenuItems;
    private static SystemTray systemTray;
    private final MouseAdapter mouseAdapter = new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                tabbedPane1.setSelectedIndex(0);
            }
        }
    };

    public static PaintPicturePanel paintPicture;

    private static WindowsAppMutex windowsAppMutex;

    private JLabel BuildView;

    private JComboBox<String> ThemeModeComboBox;
    private JLabel ThemeModeLabel;
    private static final Thread init_PaintPicture = new Thread(() -> {
        try {
            PaintPicturePanel.isEnableHardwareAcceleration =
                    OpenCLBlurProcessor.getIsSupportedOpenCL()
                            && Boolean.parseBoolean(init.getProperties().getProperty("EnableHardwareAcceleration"));

            OpenCLBlurProcessor.setSelectDeviceIndex(Integer.parseInt(init.getProperties().getProperty("OpenCLDeviceIndex")));
        } catch (Exception _) {

        }
        paintPicture = new PaintPicturePanel();
        new Thread(() -> {
            ThreadControl.waitThreadsComplete(paintPicture.init);
            paintPicture.pictureInformationViewer.setOwner(main);
        }).start();
    });

    public static final Image SOFTWARE_FRAME_ICON;

    @Getter
    private final TreeMap<String, ThumbnailPreviewOfImage> thumbnailPreviewOfImages = new TreeMap<>();

    private static void initSystemTrayMenuItems() {
        MenuItem open = new MenuItem(Bundle.getMessage("SystemTrayMenu_Open"));
        MenuItem display = new MenuItem(Bundle.getMessage("SystemTrayMenu_Display"));
        MenuItem settings = new MenuItem(Bundle.getMessage("SystemTrayMenu_Settings"));
        MenuItem about = new MenuItem(Bundle.getMessage("SystemTrayMenu_About"));
        MenuItem checkUpdate = new MenuItem(Bundle.getMessage("SystemTrayMenu_CheckUpdate"));
        MenuItem exit = new MenuItem(Bundle.getMessage("SystemTrayMenu_Exit"));
        open.addActionListener(e -> {
            GUIStarter.main.setVisible(true);
            GUIStarter.main.tabbedPane1.setSelectedIndex(0);
        });
        display.addActionListener(e -> {
            GUIStarter.main.setVisible(true);
            GUIStarter.main.tabbedPane1.setSelectedIndex(1);
        });
        settings.addActionListener(e -> {
            GUIStarter.main.setVisible(true);
            GUIStarter.main.tabbedPane1.setSelectedIndex(2);
        });
        about.addActionListener(e -> {
            GUIStarter.main.setVisible(true);
            GUIStarter.main.tabbedPane1.setSelectedIndex(3);
        });
        checkUpdate.addActionListener(e -> {
            CheckAndDownloadUpdate downloadUpdate = new CheckAndDownloadUpdate(UPDATE_WEBSITE);
            new Thread(() -> {
                ExceptionHandler.setUncaughtExceptionHandler(log);


                try {
                    if (SystemNotifications.isSupportedSystemNotifications && !downloadUpdate.checkIfTheLatestVersion()) {
                        SystemNotifications.sendMessage(SystemNotifications.DefaultIcon,
                                Bundle.getMessage("NoAnyUpdate_Title"),
                                Bundle.getMessage("NoAnyUpdate_Content_First"),
                                TrayIcon.MessageType.INFO);
                        return;
                    }
                } catch (IOException e1) {
                    log.error(ExceptionHandler.getExceptionMessage(e1));
                    SystemNotifications.sendMessage(SystemNotifications.DefaultIcon,
                            Bundle.getMessage("CantGetUpdate_Title"),
                            "Error: " + e1 + "\n" + Bundle.getMessage("CantGetUpdate_Content"),
                            TrayIcon.MessageType.ERROR);
                    return;
                }

                new Thread(() -> {
                    ExceptionHandler.setUncaughtExceptionHandler(log);
                    UpdateForm(downloadUpdate);
                }).start();
            }).start();
        });
        exit.addActionListener(e -> GUIStarter.exitAndRecord());
        SystemTrayMenuItems = new MenuItem[]{
                open, display, settings, about, checkUpdate, exit
        };
    }

    private final DropTargetAdapter dropTargetAdapter = new DropTargetAdapter() {
        public void drop(DropTargetDropEvent dtde) {
            try {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
                Transferable transferable = dtde.getTransferable();

                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    Object obj = transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    // 使用泛型进行类型安全的转换
                    @SuppressWarnings("unchecked")
                    List<File> fileList = (List<File>) obj;
                    File file = checkFileOpen(fileList);
                    if (file != null) {
                        openPicture(file.getPath());
                    }
                }
            } catch (IOException | UnsupportedFlavorException e) {
                log.error(ExceptionHandler.getExceptionMessage(e));
            }
        }
    };

    private JComboBox<String> CloseMainFrameControlComboBox;
    private JLabel OpenCLSelectedDeviceNameLavel;
    private JScrollPane FileChoosePaneScrollPane;
    private JLabel OpenCLDevicesChooserLabel;
    private NComboBox OpenCLDevicesChooser;

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.setRequestFocusEnabled(true);
        tabbedPane1 = new JTabbedPane();
        tabbedPane1.setRequestFocusEnabled(false);
        tabbedPane1.setToolTipText("");
        panel1.add(tabbedPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        FirstPanel = new JPanel();
        FirstPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        FirstPanel.setName("");
        FirstPanel.setToolTipText("");
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("messages", "FirstPanel"), FirstPanel);
        textField1 = new JTextField();
        FirstPanel.add(textField1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        TurnButton = new JButton();
        TurnButton.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(TurnButton, this.$$$getMessageFromBundle$$$("messages", "TurnButton"));
        FirstPanel.add(TurnButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        FirstPanel.add(panel2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_SOUTHEAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        VersionView = new JLabel();
        VersionView.setRequestFocusEnabled(false);
        VersionView.setText("Version:");
        panel2.add(VersionView, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        BuildView = new JLabel();
        BuildView.setText("");
        BuildView.setVerticalAlignment(3);
        BuildView.setVerticalTextPosition(3);
        panel2.add(BuildView, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        FileChoosePaneScrollPane = new JScrollPane();
        FirstPanel.add(FileChoosePaneScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        FileChoosePane = new JPanel();
        FileChoosePane.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        FileChoosePane.setOpaque(true);
        FileChoosePane.setVerifyInputWhenFocusTarget(true);
        FileChoosePane.putClientProperty("html.disable", Boolean.FALSE);
        FileChoosePaneScrollPane.setViewportView(FileChoosePane);
        SecondPanel = new JPanel();
        SecondPanel.setLayout(new GridBagLayout());
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("messages", "SecondPanel"), SecondPanel);
        Display_1st = new JLabel();
        Font Display_1stFont = this.$$$getFont$$$(null, -1, 35, Display_1st.getFont());
        if (Display_1stFont != null) Display_1st.setFont(Display_1stFont);
        Display_1st.setHorizontalTextPosition(11);
        this.$$$loadLabelText$$$(Display_1st, this.$$$getMessageFromBundle$$$("messages", "Display_1st"));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        SecondPanel.add(Display_1st, gbc);
        Display_2nd = new JLabel();
        Font Display_2ndFont = this.$$$getFont$$$(null, -1, 20, Display_2nd.getFont());
        if (Display_2ndFont != null) Display_2nd.setFont(Display_2ndFont);
        this.$$$loadLabelText$$$(Display_2nd, this.$$$getMessageFromBundle$$$("messages", "Display_2nd"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        SecondPanel.add(Display_2nd, gbc);
        Display_3rd = new JLabel();
        Display_3rd.setBackground(new Color(-2104859));
        Font Display_3rdFont = this.$$$getFont$$$(null, -1, 15, Display_3rd.getFont());
        if (Display_3rdFont != null) Display_3rd.setFont(Display_3rdFont);
        Display_3rd.setHorizontalAlignment(0);
        Display_3rd.setHorizontalTextPosition(0);
        this.$$$loadLabelText$$$(Display_3rd, this.$$$getMessageFromBundle$$$("messages", "Display_3rd"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        SecondPanel.add(Display_3rd, gbc);
        ThirdPanel = new JPanel();
        ThirdPanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("messages", "ThirdPanel"), ThirdPanel);
        A = new JPanel();
        A.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        ThirdPanel.add(A, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        A.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        ResetButton = new JButton();
        ResetButton.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(ResetButton, this.$$$getMessageFromBundle$$$("messages", "ResetButton"));
        A.add(ResetButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        RefreshButton = new JButton();
        RefreshButton.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(RefreshButton, this.$$$getMessageFromBundle$$$("messages", "RefreshButton"));
        A.add(RefreshButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        SaveButton = new JButton();
        SaveButton.setHorizontalAlignment(0);
        SaveButton.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(SaveButton, this.$$$getMessageFromBundle$$$("messages", "SaveButton"));
        SaveButton.setVerticalTextPosition(0);
        A.add(SaveButton, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        B = new JPanel();
        B.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        ThirdPanel.add(B, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        B.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(502, 372), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(12, 2, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane1.setViewportView(panel3);
        EnableHistoryLoaderCheckBox = new JCheckBox();
        EnableHistoryLoaderCheckBox.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(EnableHistoryLoaderCheckBox, this.$$$getMessageFromBundle$$$("messages", "EnableHistoryLoader"));
        panel3.add(EnableHistoryLoaderCheckBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(402, 25), null, 0, false));
        EnableHardwareAccelerationCheckBox = new JCheckBox();
        EnableHardwareAccelerationCheckBox.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(EnableHardwareAccelerationCheckBox, this.$$$getMessageFromBundle$$$("messages", "EnableHardwareAcceleration"));
        panel3.add(EnableHardwareAccelerationCheckBox, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(402, 25), null, 0, false));
        EnableCursorDisplayCheckBox = new JCheckBox();
        EnableCursorDisplayCheckBox.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(EnableCursorDisplayCheckBox, this.$$$getMessageFromBundle$$$("messages", "EnableCursorDisplayCheckBox"));
        panel3.add(EnableCursorDisplayCheckBox, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(402, 25), null, 0, false));
        MouseMoveOffsetsLabel = new JLabel();
        MouseMoveOffsetsLabel.setRequestFocusEnabled(false);
        this.$$$loadLabelText$$$(MouseMoveOffsetsLabel, this.$$$getMessageFromBundle$$$("messages", "MouseMoveOffsetsLabel"));
        panel3.add(MouseMoveOffsetsLabel, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(402, 17), null, 0, false));
        MouseMoveOffsetsSlider = new JSlider();
        MouseMoveOffsetsSlider.setMaximum(150);
        MouseMoveOffsetsSlider.setMinimum(-65);
        MouseMoveOffsetsSlider.setRequestFocusEnabled(false);
        panel3.add(MouseMoveOffsetsSlider, new GridConstraints(7, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        EnableProxyServerCheckBox = new JCheckBox();
        EnableProxyServerCheckBox.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(EnableProxyServerCheckBox, this.$$$getMessageFromBundle$$$("messages", "EnableProxyServerCheckBox"));
        panel3.add(EnableProxyServerCheckBox, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(402, 25), null, 0, false));
        ProxyServerLabel = new JLabel();
        ProxyServerLabel.setRequestFocusEnabled(false);
        this.$$$loadLabelText$$$(ProxyServerLabel, this.$$$getMessageFromBundle$$$("messages", "ProxyServerLabel"));
        panel3.add(ProxyServerLabel, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(402, 17), null, 0, false));
        EnableSecureConnectionCheckBox = new JCheckBox();
        EnableSecureConnectionCheckBox.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(EnableSecureConnectionCheckBox, this.$$$getMessageFromBundle$$$("messages", "EnableSecureConnectionCheckBox"));
        panel3.add(EnableSecureConnectionCheckBox, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(402, 25), null, 0, false));
        AutoCheckUpdateCheckBox = new JCheckBox();
        AutoCheckUpdateCheckBox.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(AutoCheckUpdateCheckBox, this.$$$getMessageFromBundle$$$("messages", "AutoCheckUpdateCheckBox"));
        panel3.add(AutoCheckUpdateCheckBox, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(402, 25), null, 0, false));
        ProxyServerButton = new JButton();
        ProxyServerButton.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(ProxyServerButton, this.$$$getMessageFromBundle$$$("messages", "ProxyServerButton"));
        panel3.add(ProxyServerButton, new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ThemeModeLabel = new JLabel();
        ThemeModeLabel.setRequestFocusEnabled(false);
        this.$$$loadLabelText$$$(ThemeModeLabel, this.$$$getMessageFromBundle$$$("messages", "ThemeModeLabel"));
        panel3.add(ThemeModeLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("messages", "Settings_ExitControlLabel"));
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        CloseMainFrameControlComboBox = new NComboBox();
        panel3.add(CloseMainFrameControlComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ThemeModeComboBox = new NComboBox();
        panel3.add(ThemeModeComboBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        OpenCLDevicesChooserLabel = new JLabel();
        this.$$$loadLabelText$$$(OpenCLDevicesChooserLabel, this.$$$getMessageFromBundle$$$("messages", "Settings_OpenCLDevicesChooserLabel"));
        panel3.add(OpenCLDevicesChooserLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        OpenCLDevicesChooser = new NComboBox();
        panel3.add(OpenCLDevicesChooser, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("messages", "FourthPanel"), scrollPane2);
        FourthPanel = new JPanel();
        FourthPanel.setLayout(new GridLayoutManager(12, 7, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane2.setViewportView(FourthPanel);
        JVMVersionLabel = new JLabel();
        Font JVMVersionLabelFont = this.$$$getFont$$$(null, -1, 16, JVMVersionLabel.getFont());
        if (JVMVersionLabelFont != null) JVMVersionLabel.setFont(JVMVersionLabelFont);
        this.$$$loadLabelText$$$(JVMVersionLabel, this.$$$getMessageFromBundle$$$("messages", "JVMVersionLabel"));
        FourthPanel.add(JVMVersionLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        FourthPanel.add(spacer2, new GridConstraints(11, 0, 1, 7, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        CurrentSoftwareVersionLabel = new JLabel();
        Font CurrentSoftwareVersionLabelFont = this.$$$getFont$$$(null, -1, 16, CurrentSoftwareVersionLabel.getFont());
        if (CurrentSoftwareVersionLabelFont != null)
            CurrentSoftwareVersionLabel.setFont(CurrentSoftwareVersionLabelFont);
        this.$$$loadLabelText$$$(CurrentSoftwareVersionLabel, this.$$$getMessageFromBundle$$$("messages", "CurrentSoftwareVersionLabel"));
        FourthPanel.add(CurrentSoftwareVersionLabel, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        CurrentSoftwareInteriorLabel = new JLabel();
        Font CurrentSoftwareInteriorLabelFont = this.$$$getFont$$$(null, -1, 16, CurrentSoftwareInteriorLabel.getFont());
        if (CurrentSoftwareInteriorLabelFont != null)
            CurrentSoftwareInteriorLabel.setFont(CurrentSoftwareInteriorLabelFont);
        this.$$$loadLabelText$$$(CurrentSoftwareInteriorLabel, this.$$$getMessageFromBundle$$$("messages", "CurrentSoftwareInteriorLabel"));
        FourthPanel.add(CurrentSoftwareInteriorLabel, new GridConstraints(8, 2, 1, 5, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        CheckVersionButton = new JButton();
        Font CheckVersionButtonFont = this.$$$getFont$$$(null, -1, 16, CheckVersionButton.getFont());
        if (CheckVersionButtonFont != null) CheckVersionButton.setFont(CheckVersionButtonFont);
        CheckVersionButton.setRequestFocusEnabled(false);
        CheckVersionButton.setRolloverEnabled(false);
        this.$$$loadButtonText$$$(CheckVersionButton, this.$$$getMessageFromBundle$$$("messages", "CheckVersionButton"));
        FourthPanel.add(CheckVersionButton, new GridConstraints(9, 0, 1, 7, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        OSLabel = new JLabel();
        Font OSLabelFont = this.$$$getFont$$$(null, -1, 16, OSLabel.getFont());
        if (OSLabelFont != null) OSLabel.setFont(OSLabelFont);
        this.$$$loadLabelText$$$(OSLabel, this.$$$getMessageFromBundle$$$("messages", "OSLabel"));
        FourthPanel.add(OSLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ProgramStartTime = new JLabel();
        Font ProgramStartTimeFont = this.$$$getFont$$$(null, -1, 16, ProgramStartTime.getFont());
        if (ProgramStartTimeFont != null) ProgramStartTime.setFont(ProgramStartTimeFont);
        this.$$$loadLabelText$$$(ProgramStartTime, this.$$$getMessageFromBundle$$$("messages", "ProgramStartTime"));
        FourthPanel.add(ProgramStartTime, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        CurrentSoftwareLanguage = new JLabel();
        CurrentSoftwareLanguage.setEnabled(true);
        Font CurrentSoftwareLanguageFont = this.$$$getFont$$$(null, -1, 16, CurrentSoftwareLanguage.getFont());
        if (CurrentSoftwareLanguageFont != null) CurrentSoftwareLanguage.setFont(CurrentSoftwareLanguageFont);
        this.$$$loadLabelText$$$(CurrentSoftwareLanguage, this.$$$getMessageFromBundle$$$("messages", "CurrentSoftwareLanguage"));
        FourthPanel.add(CurrentSoftwareLanguage, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        CPUName = new JLabel();
        Font CPUNameFont = this.$$$getFont$$$(null, -1, 16, CPUName.getFont());
        if (CPUNameFont != null) CPUName.setFont(CPUNameFont);
        this.$$$loadLabelText$$$(CPUName, this.$$$getMessageFromBundle$$$("messages", "CPULabel"));
        FourthPanel.add(CPUName, new GridConstraints(1, 0, 1, 7, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        TotalThread = new JLabel();
        Font TotalThreadFont = this.$$$getFont$$$(null, -1, 16, TotalThread.getFont());
        if (TotalThreadFont != null) TotalThread.setFont(TotalThreadFont);
        this.$$$loadLabelText$$$(TotalThread, this.$$$getMessageFromBundle$$$("messages", "TotalThread"));
        FourthPanel.add(TotalThread, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        MemUsed = new JLabel();
        Font MemUsedFont = this.$$$getFont$$$(null, -1, 16, MemUsed.getFont());
        if (MemUsedFont != null) MemUsed.setFont(MemUsedFont);
        this.$$$loadLabelText$$$(MemUsed, this.$$$getMessageFromBundle$$$("messages", "MemUsed"));
        FourthPanel.add(MemUsed, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        DefaultJVMMem = new JLabel();
        Font DefaultJVMMemFont = this.$$$getFont$$$(null, -1, 16, DefaultJVMMem.getFont());
        if (DefaultJVMMemFont != null) DefaultJVMMem.setFont(DefaultJVMMemFont);
        this.$$$loadLabelText$$$(DefaultJVMMem, this.$$$getMessageFromBundle$$$("messages", "DefaultJVMMem"));
        FourthPanel.add(DefaultJVMMem, new GridConstraints(6, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        FourthPanel.add(spacer3, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        FourthPanel.add(spacer4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        FourthPanel.add(spacer5, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        FourthPanel.add(spacer6, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        JavaPath = new JLabel();
        Font JavaPathFont = this.$$$getFont$$$(null, -1, 16, JavaPath.getFont());
        if (JavaPathFont != null) JavaPath.setFont(JavaPathFont);
        this.$$$loadLabelText$$$(JavaPath, this.$$$getMessageFromBundle$$$("messages", "JavaPath"));
        FourthPanel.add(JavaPath, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        FreeUpMemory = new JButton();
        FreeUpMemory.setActionCommand(this.$$$getMessageFromBundle$$$("messages", "FreeUpMemory"));
        Font FreeUpMemoryFont = this.$$$getFont$$$(null, -1, 16, FreeUpMemory.getFont());
        if (FreeUpMemoryFont != null) FreeUpMemory.setFont(FreeUpMemoryFont);
        FreeUpMemory.setRequestFocusEnabled(false);
        FreeUpMemory.setRolloverEnabled(false);
        this.$$$loadButtonText$$$(FreeUpMemory, this.$$$getMessageFromBundle$$$("messages", "FreeUpMemory"));
        FourthPanel.add(FreeUpMemory, new GridConstraints(10, 0, 1, 7, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        OpenCLSelectedDeviceNameLavel = new JLabel();
        Font OpenCLSelectedDeviceNameLavelFont = this.$$$getFont$$$(null, -1, 16, OpenCLSelectedDeviceNameLavel.getFont());
        if (OpenCLSelectedDeviceNameLavelFont != null)
            OpenCLSelectedDeviceNameLavel.setFont(OpenCLSelectedDeviceNameLavelFont);
        this.$$$loadLabelText$$$(OpenCLSelectedDeviceNameLavel, this.$$$getMessageFromBundle$$$("messages", "About_OpenCLSelectedDeviceNameLabel"));
        FourthPanel.add(OpenCLSelectedDeviceNameLavel, new GridConstraints(2, 0, 1, 7, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        TopLabel = new JLabel();
        Font TopLabelFont = this.$$$getFont$$$(null, -1, 20, TopLabel.getFont());
        if (TopLabelFont != null) TopLabel.setFont(TopLabelFont);
        TopLabel.setHorizontalTextPosition(0);
        TopLabel.setText("Picture Player by nserly");
        panel1.add(TopLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
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
    private void $$$loadLabelText$$$(JLabel component, String text) {
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
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
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
        return panel1;
    }

    static {
        String classPath = System.getProperty("java.class.path");
        if (classPath == null || classPath.isBlank()) {
            System.setProperty("java.home", ".");
        }

        ExceptionHandler.setUncaughtExceptionHandler(log);
        SOFTWARE_FRAME_ICON = SystemNotifications.bufferedImage;
        //初始化Init
        new Thread(Init::init).start();
        init.setUpdate(true);
        log.info("The software starts running...");
        System.setProperty("SoftwareName", "PicturePlayer");
        log.info("SoftwareName:{}", System.getProperty("SoftwareName"));
    }

    //更新界面
    public static void UpdateForm(CheckAndDownloadUpdate downloadUpdate) {
        StringFormation stringFormation_title = new StringFormation(Bundle.getMessage("FindUpdateTitle"));
        stringFormation_title.add("version", PicturePlayerVersion.getVersion());
        stringFormation_title.add("version", downloadUpdate.NewVersionName);
        stringFormation_title.add("versionID", String.valueOf(downloadUpdate.NewVersionID));
        if (SystemNotifications.isSupportedSystemNotifications)
            SystemNotifications.sendMessage(SystemNotifications.DefaultIcon,
                    stringFormation_title.getProcessingString(),
                    Bundle.getMessage("SystemTrayMenu_FindNewVersionContent"),
                    TrayIcon.MessageType.INFO);
        ConfirmUpdateDialog confirmUpdateDialog = new ConfirmUpdateDialog(downloadUpdate);
        confirmUpdateDialog.pack();
        confirmUpdateDialog.setVisible(true);
    }

    public GUIStarter(String title) {
        super(title);
        $$$setupUI$$$();

        SwingUtilities.invokeLater(() -> {
            setIconImage(SOFTWARE_FRAME_ICON);
            setContentPane(this.panel1);
            log.info("Start GUI");
            setVisible(true);

            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            Dimension dimension = SizeOperate.FreeOfScreenSize;
            setSize((int) (dimension.getWidth() * 0.5), (int) (dimension.getHeight() * 0.6));
            setLocationRelativeTo(null);
            setMinimumSize(new Dimension(680, 335));
        });

        changeFocusListener = new ChangeFocusListener(this);
        new Thread(() -> {
            ExceptionHandler.setUncaughtExceptionHandler(log);
            ProxyServerPrefix = ProxyServerLabel.getText();
            MouseMoveLabelPrefix = MouseMoveOffsetsLabel.getText();
            centre = new SettingsInfoHandle();
            init();
            if (!OpenCLBlurProcessor.getIsSupportedOpenCL()) {
                centre.CurrentData.replace("EnableHardwareAcceleration", "false");
                EnableHardwareAccelerationCheckBox.setSelected(false);
                EnableHardwareAccelerationCheckBox.setEnabled(false);
                centre.save();
            }
            about();
            if (SettingsInfoHandle.getBoolean("EnableProxyServer", main.centre.CurrentData)) {
                setProxyServerOfInit();
                log.info("Proxy Server is turned on, and all networking activities of the Program will be handled by the proxy server");
                log.info("proxy server ip(or domain):{}", UPDATE_WEBSITE);
            }
            proxyServerChooser = new ProxyServerChooser();
            proxyServerChooser.pack();
            proxyServerChooser.setTitle(Bundle.getMessage("InputProxyServer_Title"));

            CheckAndDownloadUpdate.secureConnection(EnableSecureConnectionCheckBox.isSelected());
            if (init.containsKey("AutoCheckUpdate") && init.getProperties().get("AutoCheckUpdate").equals("true")) {
                CheckAndDownloadUpdate downloadUpdate = new CheckAndDownloadUpdate(UPDATE_WEBSITE);
                new Thread(() -> {
                    ExceptionHandler.setUncaughtExceptionHandler(log);
                    NewVersionDownloadingWebSide = downloadUpdate.getUpdateWebSide();
                    if (NewVersionDownloadingWebSide != null && !NewVersionDownloadingWebSide.isEmpty()) {
                        UpdateForm(downloadUpdate);
                    }
                }).start();
            }

            ThreadControl.waitThreadsComplete(init_PaintPicture);
            paintPicture.pictureInformationStorageManagement.optimize();
        }).start();
    }

    //初始化所有组件设置
    private void init() {
        VersionView.setText(VersionView.getText() + PicturePlayerVersion.getShorterVersion());
        BuildView.setText(PicturePlayerVersion.getBuildVersion());
        TurnButton.addMouseListener(changeFocusListener);
        TurnButton.addActionListener(e -> {
            String path = textField1.getText().trim();
            //如果字符串前缀与后缀包含"，则去除其中的"
            if (path.startsWith("\"") && path.endsWith("\"")) {
                path = path.substring(1, path.length() - 1);
            }
            File file = new File(path);
            if (GetImageInformation.isImageFile(file)) {
                openPicture(path);
                return;
            }

            JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
            fileChooser.setCurrentDirectory(file);
            String picturePath;
            while (true) {
                int returnValue = fileChooser.showOpenDialog(GUIStarter.main);
                File chooseFile = fileChooser.getSelectedFile();
                if (chooseFile == null) return;
                picturePath = chooseFile.getPath();
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    file = checkFileOpen(new File(picturePath));
                    if (file == null) continue;
                    String filePath = file.getAbsolutePath();
                    if (filePath.endsWith("???")) {
                        tabbedPane1.setSelectedIndex(1);
                        break;
                    }
                    openPicture(filePath);
                    paintPicture.sizeOperate.setPercent(paintPicture.sizeOperate.getPictureOptimalSize());
                }
                return;
            }
        });
        SecondPanel.addMouseListener(mouseAdapter);
        // 设置SecondPanel为可接受拖放
        new DropTarget(SecondPanel, DnDConstants.ACTION_COPY_OR_MOVE, dropTargetAdapter, true);
        settings();
        SaveButton.addActionListener(e -> {
            log.info("Saving Settings...");
            centre.save();
            settingRevised(false);
        });
        ResetButton.addActionListener(e -> {
            centre.setDefault();
            reFresh();
            settingRevised(true);
        });
        RefreshButton.addActionListener(e -> {
            centre.reFresh();
            reFresh();
            settingRevised(false);
        });
        ProxyServerButton.addActionListener(e -> proxyServerChooser.setVisible(true));

        new Thread(() -> {
            ExceptionHandler.setUncaughtExceptionHandler(log);
            JavaPath.setText(JavaPath.getText() + System.getProperty("sun.boot.library.path"));
            DefaultJVMMem.setText(DefaultJVMMem.getText() + SystemMonitor.convertSize(SystemMonitor.JVM_Initialize_Memory));
            JVMVersionLabel.setText(JVMVersionLabel.getText() + System.getProperty("java.runtime.version"));
            ProgramStartTime.setText(ProgramStartTime.getText() + SystemMonitor.PROGRAM_START_TIME);
            CurrentSoftwareVersionLabel.setText(CurrentSoftwareVersionLabel.getText() + PicturePlayerVersion.getVersion());
            CurrentSoftwareInteriorLabel.setText(CurrentSoftwareInteriorLabel.getText() + PicturePlayerVersion.getVersionID());
            OSLabel.setText(OSLabel.getText() + SystemMonitor.OS_NAME);
            CPUName.setText(CPUName.getText() + SystemMonitor.CPU_NAME);
            CurrentSoftwareLanguage.setText(CurrentSoftwareLanguage.getText() + System.getProperty("user.language"));
            if (EnableProxyServer)
                CheckVersionButton.setText(CheckVersionButton.getText() + Bundle.getMessage("IsEnableProxyServer"));
            final String JmemI = MemUsed.getText();
            final String TTI = TotalThread.getText();
            final String OpenCLRendererI = OpenCLSelectedDeviceNameLavel.getText();
            OpenCLSelectedDeviceNameLavel.setText(OpenCLRendererI + "<No OpenCL device selected>");
            tabbedPane1.addChangeListener(_ -> {
                request();
                int tabIndex = tabbedPane1.getSelectedIndex();
                if (tabIndex == 3) {
                    future = executor.scheduleAtFixedRate(() -> {
                        SystemMonitor.getInformation();
                        MemUsed.setText(JmemI + SystemMonitor.convertSize(
                                SystemMonitor.JVM_Used_Memory) + "/"
                                + SystemMonitor.convertSize(SystemMonitor.JVM_Maximum_Free_Memory)
                                + "(" + SystemMonitor.JVM_Memory_Usage
                                + "%" + ")");
                        TotalThread.setText(TTI + SystemMonitor.Program_Thread_Count);
                        if (OpenCLBlurProcessor.getIsSupportedOpenCL() && PaintPicturePanel.isEnableHardwareAcceleration)
                            OpenCLSelectedDeviceNameLavel.setText(OpenCLRendererI + OpenCLBlurProcessor.getSelectedDevice());
                    }, 0, 2, TimeUnit.SECONDS);
                } else {
                    if (tabIndex == 0 && paintPicture != null) paintPicture.refreshPictureThumbnail();
                    if (future != null) future.cancel(false);
                }
                if (tabIndex == 2 && !IsFreshen) {
                    IsFreshen = true;
                    reFresh();
                }
            });
        }).start();

        //设置窗体在显示时自动获取焦点
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                request();

            }

            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
    }

    private static void extractedSystemInfoToLog() {
        //获取操作系统版本
        log.info("OS:{}", SystemMonitor.OS_NAME);
        //获取系统语言
        log.info("System Language:{}", System.getProperty("user.language"));
        //获取java版本
        log.info("Java Runtime:{} {} ({})", System.getProperty("java.vm.name"), System.getProperty("java.runtime.version"), System.getProperty("sun.boot.library.path"));
        //获取软件版本
        log.info("Software Version:{}({})", PicturePlayerVersion.getVersion(), PicturePlayerVersion.getVersionID());
        //程序是否启用硬件加速
        log.info("Enable Hardware Acceleration:{}", PaintPicturePanel.isEnableHardwareAcceleration);
        if (!OpenCLBlurProcessor.getIsSupportedOpenCL()) {
            log.warn("OpenCL is not supported, and the image will be rendered using software!");
        } else if (PaintPicturePanel.isEnableHardwareAcceleration) {
            log.info("OpenCL Information:\n{}", OpenCLSupportChecker.getOpenCLInfo());
        }
    }

    public static void initSystemTray() {
        if (systemTray != null || !SystemNotifications.isSupportedSystemNotifications) return;
        systemTray = SystemNotifications.getSystemTray(SystemNotifications.DefaultIcon, SystemTrayMenuItems, e -> {
            if (e.getClickCount() == 2) {
                GUIStarter.main.setVisible(true);
            }
        });

        //每5秒更新一次提示文本
        new Timer(true).schedule(new TimerTask() {
            private String lastToolTip;

            @Override
            public void run() {
                StringBuilder ToolTipText = new StringBuilder();
                ToolTipText
                        .append("Picture Player(")
                        .append(PicturePlayerVersion.getShorterVersion())
                        .append(")\nGUI Hardware acceleration: ")
                        .append(PaintPicturePanel.isEnableHardwareAcceleration)
                        .append("\nOpenCL supported: ")
                        .append(OpenCLBlurProcessor.getIsSupportedOpenCL())
                        .append("\nOpenCL Available: ")
                        .append(OpenCLBlurProcessor.isOpenCLAvailable());

                if (PaintPicturePanel.paintPicture != null &&
                        PaintPicturePanel.paintPicture.imageCanvas != null &&
                        PaintPicturePanel.paintPicture.imageCanvas.getPath() != null)
                    ToolTipText
                            .append("\nOpened: ")
                            .append(PaintPicturePanel.paintPicture.imageCanvas.getPath());

                if (SystemNotifications.isSupportedSystemNotifications && (lastToolTip == null || !lastToolTip.contentEquals(ToolTipText)))
                    SystemNotifications.DefaultIcon.setToolTip(ToolTipText.toString());

                lastToolTip = ToolTipText.toString();
            }
        }, 0, 5000);
    }

    public static void gotoSystemTrayAndDisposeMainFrame() {
        GUIStarter.main.dispose();
        if (GUIStarter.main != null && paintPicture != null && paintPicture.imageCanvas != null)
            paintPicture.imageCanvas.setLowOccupancyMode(true);
        System.gc();
    }

    public static void exitAndRecord() {
        if (main != null) {
            main.dispose();
        }

        // 移除系统托盘
        if (systemTray != null && SystemNotifications.isSupportedSystemNotifications) {
            try {
                systemTray.remove(SystemNotifications.DefaultIcon);
            } catch (Exception e) {
                log.warn("Removing the system tray failed: {}", ExceptionHandler.getExceptionMessage(e));
            }
            systemTray = null;
        }

        // 关闭图片面板资源
        try {
            if (PaintPicturePanel.paintPicture != null && PaintPicturePanel.paintPicture.sizeOperate != null) {
                PaintPicturePanel.paintPicture.sizeOperate.close();
            }
        } catch (Exception e) {
            log.warn("Closing the image panel resource failed: {}", ExceptionHandler.getExceptionMessage(e));
        }

        // 关闭互斥锁
        try {
            if (windowsAppMutex != null) {
                windowsAppMutex.close();
            }
        } catch (Exception e) {
            log.warn("Turning off the mutex failed: {}", ExceptionHandler.getExceptionMessage(e));
        }

        log.info("Program Termination!");
        System.exit(0);
    }


    //设置界面
    private void settings() {
        for (String i : ThemeComboBoxStringItems) {
            ThemeModeComboBox.addItem(Bundle.getMessage(i));
        }
        for (String i : CloseMainFrameControlComboBoxStringItems) {
            CloseMainFrameControlComboBox.addItem(Bundle.getMessage(i));
        }

        OpenCLDevicesChooser.removeAllItems();

        reFresh();
        if (PaintPicturePanel.isEnableHardwareAcceleration)
            new Thread(() -> {
                ThreadControl.waitThreadsComplete(init_PaintPicture);
                ThreadControl.waitThreadsComplete(paintPicture.init);
                for (String i : OpenCLSupportChecker.getSupported_Device_Name()) {
                    OpenCLDevicesChooser.addItem(i);
                }
                OpenCLDevicesChooser.setSelectedIndex(OpenCLBlurProcessor.getSelectDeviceIndex());


                OpenCLDevicesChooser.addItemListener(_ -> {
                    if (!PaintPicturePanel.isEnableHardwareAcceleration ||
                            SettingsInfoHandle.getInt("OpenCLDeviceIndex", centre.CurrentData) ==
                                    OpenCLDevicesChooser.getSelectedIndex()) return;

                    centre.CurrentData.replace("OpenCLDeviceIndex", String.valueOf(OpenCLDevicesChooser.getSelectedIndex()));
                    settingRevised(true);
                    JOptionPane.showConfirmDialog(GUIStarter.main, Bundle.getMessage("OpenCL_Chooser_Action_Setting_Content"), Bundle.getMessage("OpenCL_Chooser_Action_Setting_Title"), JOptionPane.YES_NO_OPTION);
                });
            }).start();

        ThemeModeComboBox.addItemListener(_ -> {
            centre.CurrentData.replace("ThemeMode", String.valueOf(ThemeModeComboBox.getSelectedIndex()));
            UIManager.getUIManager().setTheme(ThemeModeComboBox
                    .getSelectedIndex());
            UIManager.getUIManager().applyThemeOnSetAndRefreshWindows();
            FontPreservingUIUpdater.updateComponentTreeUIWithFontPreservation(paintPicture);
            settingRevised(true);
        });
        CloseMainFrameControlComboBox.addItemListener(_ -> {
            centre.CurrentData.replace("CloseMainFrameControl", String.valueOf(CloseMainFrameControlComboBox.getSelectedIndex()));
            settingRevised(true);
        });
        EnableHistoryLoaderCheckBox.addActionListener(_ -> {
            centre.CurrentData.replace("EnableHistoryLoader", String.valueOf(EnableHistoryLoaderCheckBox.isSelected()));
            settingRevised(true);
        });
        EnableCursorDisplayCheckBox.addActionListener(_ -> {
            centre.CurrentData.replace("EnableCursorDisplay", String.valueOf(EnableCursorDisplayCheckBox.isSelected()));
            settingRevised(true);
        });
        MouseMoveOffsetsSlider.addChangeListener(_ -> {
            centre.CurrentData.replace("MouseMoveOffsets", String.valueOf(MouseMoveOffsetsSlider.getValue()));
            StringBuilder stringBuffer1 = new StringBuilder(MouseMoveLabelPrefix);
            stringBuffer1.insert(MouseMoveLabelPrefix.indexOf(":") + 1, MouseMoveOffsetsSlider.getValue() + "% ");
            MouseMoveOffsetsLabel.setText(stringBuffer1.toString());
            settingRevised(true);
        });
        EnableProxyServerCheckBox.addActionListener(_ -> {
            centre.CurrentData.replace("EnableProxyServer", String.valueOf(EnableProxyServerCheckBox.isSelected()));
            ProxyServerButton.setVisible(EnableProxyServerCheckBox.isSelected());
            ProxyServerLabel.setVisible(EnableProxyServerCheckBox.isSelected());
            settingRevised(true);
        });
        EnableHardwareAccelerationCheckBox.addActionListener(_ -> {
            centre.CurrentData.replace("EnableHardwareAcceleration", String.valueOf(EnableHardwareAccelerationCheckBox.isSelected()));
            PaintPicturePanel.isEnableHardwareAcceleration = EnableHardwareAccelerationCheckBox.isSelected();
            settingRevised(true);
        });
        EnableSecureConnectionCheckBox.addActionListener(_ -> {
            if (!EnableSecureConnectionCheckBox.isSelected()) {
                EnableSecureConnectionCheckBox.setSelected(true);
                int choose = JOptionPane.showConfirmDialog(GUIStarter.main, Bundle.getMessage("ConfirmCloseSecureConnection_Content_1Line") + "\n" + Bundle.getMessage("ConfirmCloseSecureConnection_Content_2Line"), Bundle.getMessage("ConfirmCloseSecureConnection_Title"), JOptionPane.YES_NO_OPTION);
                if (choose != 0) {
                    return;
                }
                EnableSecureConnectionCheckBox.setSelected(false);
            }
            centre.CurrentData.replace("EnableSecureConnection", String.valueOf(EnableSecureConnectionCheckBox.isSelected()));
            settingRevised(true);
        });
        AutoCheckUpdateCheckBox.addActionListener(_ -> {
            centre.CurrentData.replace("AutoCheckUpdate", String.valueOf(AutoCheckUpdateCheckBox.isSelected()));
            settingRevised(true);
        });
    }

    //关于界面设置
    private void about() {
        CheckAndDownloadUpdate downloadUpdate = new CheckAndDownloadUpdate(UPDATE_WEBSITE);
        CheckVersionButton.addActionListener(e -> {
            downloadUpdate.setWebSide(UPDATE_WEBSITE);
            try {
                if (!downloadUpdate.checkIfTheLatestVersion()) {
                    int choice = JOptionPane.showConfirmDialog(GUIStarter.main, Bundle.getMessage("NoAnyUpdate_Content_First") + "\n" + Bundle.getMessage("NoAnyUpdate_Content_Second"), Bundle.getMessage("NoAnyUpdate_Title"), JOptionPane.YES_NO_OPTION);
                    if (choice != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
            } catch (IOException e1) {
                log.error(ExceptionHandler.getExceptionMessage(e1));
                JOptionPane.showMessageDialog(GUIStarter.main, "Error: " + e1 + "\n" + Bundle.getMessage("CantGetUpdate_Content"), Bundle.getMessage("CantGetUpdate_Title"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            new Thread(() -> {
                ExceptionHandler.setUncaughtExceptionHandler(log);
                ConfirmUpdateDialog confirmUpdateDialog = new ConfirmUpdateDialog(downloadUpdate);
                confirmUpdateDialog.pack();
                confirmUpdateDialog.setVisible(true);
            }).start();
        });

        FreeUpMemory.addActionListener(e -> {
            System.gc();
            JOptionPane.showConfirmDialog(GUIStarter.main, Bundle.getMessage("FreeUpMemory_Content"), Bundle.getMessage("FreeUpMemory_Title"), JOptionPane.DEFAULT_OPTION);
        });

    }

    private void reFresh() {
        EnableHardwareAccelerationCheckBox.setSelected(SettingsInfoHandle.getBoolean("EnableHardwareAcceleration", centre.CurrentData));
        ThemeModeComboBox.setSelectedIndex(SettingsInfoHandle.getInt("ThemeMode", centre.CurrentData));
        CloseMainFrameControlComboBox.setSelectedIndex(SettingsInfoHandle.getInt("CloseMainFrameControl", centre.CurrentData));
        EnableHistoryLoaderCheckBox.setSelected(SettingsInfoHandle.getBoolean("EnableHistoryLoader", centre.CurrentData));
        EnableCursorDisplayCheckBox.setSelected(SettingsInfoHandle.getBoolean("EnableCursorDisplay", centre.CurrentData));
        MouseMoveOffsetsSlider.setValue((int) SettingsInfoHandle.getDouble("MouseMoveOffsets", centre.CurrentData));
        StringBuilder stringBuffer = new StringBuilder(MouseMoveLabelPrefix);
        stringBuffer.insert(MouseMoveLabelPrefix.indexOf(":") + 1, MouseMoveOffsetsSlider.getValue() + "% ");
        MouseMoveOffsetsLabel.setText(stringBuffer.toString());
        EnableProxyServerCheckBox.setSelected(SettingsInfoHandle.getBoolean("EnableProxyServer", centre.CurrentData));
        ProxyServerLabel.setText(ProxyServerPrefix + centre.CurrentData.get("ProxyServer"));
        EnableSecureConnectionCheckBox.setSelected(SettingsInfoHandle.getBoolean("EnableSecureConnection", centre.CurrentData));
        AutoCheckUpdateCheckBox.setSelected(SettingsInfoHandle.getBoolean("AutoCheckUpdate", centre.CurrentData));
        ProxyServerButton.setVisible(EnableProxyServerCheckBox.isSelected());
        ProxyServerLabel.setVisible(EnableProxyServerCheckBox.isSelected());
    }

    //代理服务器设置
    private static void setProxyServerOfInit() {
        String website = init.getProperties().getProperty("ProxyServer");
        if (!website.startsWith("http")) {
            website = "http://" + website;
        }
        if (!website.endsWith(".sum")) {
            website = website.trim();
            if (website.endsWith("/")) {
                website += "VersionID.sum";
            } else {
                website += "/VersionID.sum";
            }
        }
        UPDATE_WEBSITE = website;
        EnableProxyServer = true;
    }

    //设置修改
    private void settingRevised(boolean a) {
        if (a && !getTitle().contains("*")) {
            setTitle(getTitle() + "*");
        } else if ((!a) && getTitle().contains("*")) {
            setTitle(getTitle().substring(0, getTitle().lastIndexOf("*")));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        AtomicReference<String> openingFilePath = new AtomicReference<>();
        Thread pictureFileThread = new Thread(() -> {
            for (String arg : args) {
                if (GetImageInformation.isImageFile(new File(arg))) {
                    openingFilePath.set(arg);
                    return;
                }
            }
        });
        if (args.length > 0) {
            pictureFileThread.start();
        }

        windowsAppMutex = new WindowsAppMutex(22357);
        boolean isNUpdate = "true".equals(System.getProperty("NUpdate"));
        log.info("NUpdate:{}", isNUpdate);
        // 尝试读取是否为第一个实例
        if ((!windowsAppMutex.isFirstInstance()) && (!isNUpdate)) {
            windowsAppMutex.sendSoftwareVisibleDirectiveToExistingInstance(true);
            ThreadControl.waitThreadsComplete(pictureFileThread);
            // 发送参数到已有实例
            if (openingFilePath.get() != null && !openingFilePath.get().isBlank()) {
                windowsAppMutex.sendFilePathToExistingInstance(openingFilePath.get());
            }
            exitAndRecord();
        }

        AtomicReference<BufferedImage> image = new AtomicReference<>();
        AtomicReference<String> hashCode = new AtomicReference<>();
        AtomicBoolean isImageLoaded = new AtomicBoolean(false);

        Thread getImageAndHashCode = new Thread(() -> {
            ThreadControl.waitThreadsComplete(pictureFileThread);
            if (openingFilePath.get() != null && !openingFilePath.get().isBlank()) {
                image.set(GetImageInformation.getImage(openingFilePath.get()));
                hashCode.set(GetImageInformation.getHashcode(new File(openingFilePath.get())));
                isImageLoaded.set(true);
            }
        });
        getImageAndHashCode.start();

        init.run();
        UIManager.getUIManager().setTheme(SettingsInfoHandle.getInt("ThemeMode", init.getProperties()));
        UIManager.getUIManager().applyThemeOnSetAndRefreshWindows();

        init_PaintPicture.setPriority(Thread.MAX_PRIORITY);
        init_PaintPicture.start();

        main = new GUIStarter("Picture Player(Version:" + PicturePlayerVersion.getVersion() + ")");

        new Thread(() -> {
            ThreadControl.waitThreadsComplete(getImageAndHashCode);

            if (isImageLoaded.get())
                main.openPicture(image.get(), openingFilePath.get(), hashCode.get());
        }).start();

        windowsAppMutex.addHandleSoftwareRequestAction(new HandleSoftwareRequestAction() {
            @Override
            public void setVisible(boolean visible) {
                main.setVisible(visible);
            }

            @Override
            public void receiveFile(String filePath) {
                main.openPicture(filePath);
            }
        });

        new Thread(GUIStarter::extractedSystemInfoToLog).start();

        initSystemTrayMenuItems();
        initSystemTray();
    }

    private File checkFileOpen(CheckFileIsRightPictureType checkFileIsRightPictureType, boolean isMakeSure) {
        checkFileIsRightPictureType.statistics();
        if (checkFileIsRightPictureType.getNotImageCount() != 0) {
            JOptionPane.showMessageDialog(GUIStarter.main, Bundle.getMessage("OpenPictureError_Content") + "\n\"" + checkFileIsRightPictureType.filePathToString("\n", checkFileIsRightPictureType.getNotImageList()) + "\"", Bundle.getMessage("OpenPictureError_Title"), JOptionPane.ERROR_MESSAGE);
        }
        if (checkFileIsRightPictureType.getImageCount() == 0) return null;
        File choose;
        if (checkFileIsRightPictureType.getImageCount() == 1) {
            choose = checkFileIsRightPictureType.getImageList().getFirst();
            String choose_hashcode = GetImageInformation.getHashcode(choose);
            if (paintPicture != null && paintPicture.imageCanvas.getPath() != null) {
                if (choose_hashcode == null && paintPicture.imageCanvas.getPicture_hashcode() == null) {
                    log.warn("Couldn't get current or opening picture hashcode,this will fake the judgment file path");
                    if (!new File(paintPicture.imageCanvas.getPath()).equals(choose)) return null;
                } else if (Objects.equals(choose_hashcode, paintPicture.imageCanvas.getPicture_hashcode()))
                    return REPEAT_PICTURE_PATH_LOGOTYPE;
                if (isMakeSure && JOptionPane.showConfirmDialog(GUIStarter.main, Bundle.getMessage("OpenPictureExactly_Content") + "\n\"" + choose.getPath() + "\"", Bundle.getMessage("OpenPictureExactly_Title"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                    return REPEAT_PICTURE_PATH_LOGOTYPE;
            }
        } else {
            choose = OpenImageChooser.openImageWithChoice(GUIStarter.main, checkFileIsRightPictureType.getImageList());
        }
        return choose;
    }

    //代理服务器更改
    public void setProxyServerOfInit(String ProxyServerAddress) {
        if (ProxyServerAddress == null || ProxyServerAddress.trim().isEmpty()) return;
        ProxyServerAddress = ProxyServerAddress.trim();
        if (ProxyServerAddress.equals(centre.CurrentData.get("ProxyServer"))) return;
        if (!ProxyServerAddress.equals("proxy server address") && !ProxyServerAddress.isEmpty()) {
            init.changeValue("ProxyServer", ProxyServerAddress);
            setProxyServerOfInit();
            log.info("To enable a new proxy server:{}", ProxyServerAddress);
            EnableProxyServerCheckBox.setSelected(true);
            ProxyServerButton.setVisible(true);
            ProxyServerLabel.setVisible(true);
            centre.CurrentData.replace("ProxyServer", ProxyServerAddress);
            ProxyServerLabel.setText(Bundle.getMessage("ProxyServerLabel") + centre.CurrentData.get("ProxyServer"));
            JOptionPane.showConfirmDialog(GUIStarter.main, Bundle.getMessage("ProxyServerWasModified_Content"), Bundle.getMessage("ProxyServerWasModified_Title"), JOptionPane.YES_NO_OPTION);
            settingRevised(true);
        }
    }

    public void reviewPictureList(ArrayList<String> picturePath) {
        if (picturePath == null || picturePath.isEmpty()) {
            clearAllThumbnails();
            return;
        }

        // 使用Set提高查找效率，并重命名变量以明确含义
        Set<String> currentPicturePaths = new LinkedHashSet<>(picturePath);

        // 存储需要移除和添加的元素，减少同步块内的操作
        List<String> pathsToRemove;
        List<String> pathsToAdd;
        List<ThumbnailPreviewOfImage> newThumbnails = new ArrayList<>();
        List<ThumbnailPreviewOfImage> thumbnailsToRemove;

        synchronized (thumbnailPreviewOfImages) {
            // 1. 找出需要移除的路径（缓存中有但当前列表没有）
            pathsToRemove = thumbnailPreviewOfImages.keySet().stream()
                    .filter(path -> !currentPicturePaths.contains(path))
                    .toList();

            // 收集需要移除的缩略图对象
            thumbnailsToRemove = pathsToRemove.stream()
                    .map(thumbnailPreviewOfImages::get)
                    .toList();

            // 2. 找出需要添加的路径（当前列表有但缓存中没有）
            pathsToAdd = currentPicturePaths.stream()
                    .filter(path -> !thumbnailPreviewOfImages.containsKey(path))
                    .toList();

            // 3. 预创建新的缩略图（可能耗时的操作）
            for (String path : pathsToAdd) {
                ThumbnailPreviewOfImage thumbnail;
                thumbnail = new ThumbnailPreviewOfImage(path);

                newThumbnails.add(thumbnail);
                thumbnailPreviewOfImages.put(path, thumbnail);
            }
        }

        // 4. 批量处理UI更新（确保在EDT线程执行）
        List<ThumbnailPreviewOfImage> finalThumbnailsToRemove = thumbnailsToRemove;
        SwingUtilities.invokeLater(() -> {
            // 移除不需要的缩略图
            finalThumbnailsToRemove.forEach(thumbnail -> {
                FileChoosePane.remove(thumbnail);
                thumbnail.dispose();
            });

            // 添加新的缩略图
            newThumbnails.forEach(FileChoosePane::add);

            FileChoosePane.revalidate();
            FileChoosePane.repaint();
        });

        new Thread(() -> {
            try {
                ThumbnailPreviewOfImage.waitTillCompleteLoading();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            SwingUtilities.invokeLater(() -> {
                // 刷新面板
                FileChoosePane.revalidate();
                FileChoosePane.repaint();
            });
        }).start();

        // 5. 从缓存中移除不再需要的条目（在UI操作后执行，避免并发问题）
        synchronized (thumbnailPreviewOfImages) {
            pathsToRemove.forEach(thumbnailPreviewOfImages::remove);
        }
    }

    // 新增辅助方法：清空所有缩略图
    private void clearAllThumbnails() {
        List<ThumbnailPreviewOfImage> allThumbnails;
        synchronized (thumbnailPreviewOfImages) {
            allThumbnails = new ArrayList<>(thumbnailPreviewOfImages.values());
        }

        SwingUtilities.invokeLater(() -> {
            allThumbnails.forEach(thumbnail -> {
                FileChoosePane.remove(thumbnail);
                thumbnail.dispose();
            });
            FileChoosePane.revalidate();
            FileChoosePane.repaint();
        });

        synchronized (thumbnailPreviewOfImages) {
            thumbnailPreviewOfImages.clear();
        }
    }


    //获取焦点
    private void request() {
        //当选项界面切换时
        if (tabbedPane1.getSelectedIndex() == 0) {
            //让路径输入框获取焦点
            textField1.requestFocusInWindow();
        } else if (tabbedPane1.getSelectedIndex() == 1) {
            //让图片渲染器获取焦点
            if (paintPicture != null && paintPicture.imageCanvas != null) {
                paintPicture.imageCanvas.requestFocusInWindow();
            }
        } else if (tabbedPane1.getSelectedIndex() == 2) {
            //让窗体获取焦点
            tabbedPane1.requestFocusInWindow();
        } else {
            //让窗体获取焦点
            requestFocusInWindow();
        }
    }

    //检查文件打开
    private File checkFileOpen(File... files) {
        return checkFileOpen(new CheckFileIsRightPictureType(files), false);
    }

    private File checkFileOpen(List<File> files) {
        return checkFileOpen(new CheckFileIsRightPictureType(files), true);
    }

    //关闭
    public static void close() {
        if (GUIStarter.main.getTitle().contains("*")) {
            int choose = JOptionPane.showConfirmDialog(GUIStarter.main, Bundle.getMessage("WindowCloseWithSettingsNotSave_Content"), Bundle.getMessage("WindowCloseWithSettingsNotSave_Title"), JOptionPane.YES_NO_CANCEL_OPTION);
            if (choose == JOptionPane.YES_OPTION) {
                log.info("Saving Settings...");
                GUIStarter.main.centre.save();
                GUIStarter.main.settingRevised(false);
            } else if (choose == JOptionPane.NO_OPTION) {
                exitAndRecord();
            } else {
                return;
            }
        }

        if (SystemNotifications.createdSystemTrayCount == 0) {
            log.warn("System Tray is not supported! Program will exit directly when you close the main window!");
            exitAndRecord();
        }
        switch (SettingsInfoHandle.getInt("CloseMainFrameControl", init.getProperties())) {
            //直接退出
            case 1 -> {
                exitAndRecord();
                return;
            }
            //最小化到系统托盘
            case 2 -> {
                log.info("Minimized to the system tray");
                gotoSystemTrayAndDisposeMainFrame();
                return;
            }
        }

        //设置消息对话框面板
        var exitControlDialog = new ExitControlDialog(main, true);
        exitControlDialog.setExitControlButtonChoiceListener(((choice, doNotAppear) -> {
            if (choice == ExitControlButtonChoiceListener.EXIT_CANCEL) return;
            switch (choice) {
                case ExitControlButtonChoiceListener.EXIT_DIRECTLY -> {
                    if (doNotAppear) {
                        GUIStarter.main.centre.CurrentData.replace("CloseMainFrameControl", "1");
                        GUIStarter.main.centre.save();
                        GUIStarter.main.CloseMainFrameControlComboBox.setSelectedIndex(1);
                        GUIStarter.main.settingRevised(false);
                    }
                    exitAndRecord();
                }
                case ExitControlButtonChoiceListener.EXIT_TO_SYSTEM_TRAY -> {
                    if (doNotAppear) {
                        GUIStarter.main.centre.CurrentData.replace("CloseMainFrameControl", "2");
                        GUIStarter.main.centre.save();
                        GUIStarter.main.CloseMainFrameControlComboBox.setSelectedIndex(2);
                        GUIStarter.main.settingRevised(false);
                    }
                    log.info("Minimized to the system tray");
                    gotoSystemTrayAndDisposeMainFrame();
                }
            }

        }));
        exitControlDialog.setVisible(true);
    }

    //打开图片
    public void openPicture(String path) {
        if (path == null || path.endsWith("???")) return;

        AtomicReference<BufferedImage> image = new AtomicReference<>();

        Thread getBufferedImage = new Thread(() -> image.set(GetImageInformation.getImage(path)));
        getBufferedImage.start();

        String hashCode = GetImageInformation.getHashcode(new File(path));

        ThreadControl.waitThreadsComplete(getBufferedImage);

        openPicture(image.get(), path, hashCode);
    }

    public synchronized void openPicture(BufferedImage image, String path, String hashcode) {
        if (image == null || path == null || path.endsWith("???")) return;

        textField1.setText(path);

        new Thread(() -> {
            ExceptionHandler.setUncaughtExceptionHandler(log);

            tabbedPane1.setSelectedIndex(1);

            ThreadControl.waitThreadsComplete(init_PaintPicture);

            if (!tabbedPane1.getComponentAt(1).equals(paintPicture)) {
                tabbedPane1.setComponentAt(1, paintPicture);
                new DropTarget(paintPicture, DnDConstants.ACTION_COPY_OR_MOVE, dropTargetAdapter, true);
            }

            paintPicture.changePicturePath(image, path, hashcode);

        }).start();
    }

}
