package top.nserly.PicturePlayer.NComponent.Component;//导入包

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.nserly.GUIStarter;
import top.nserly.PicturePlayer.NComponent.Frame.FullScreenFrame;
import top.nserly.PicturePlayer.Settings.SettingsInfoHandle;
import top.nserly.PicturePlayer.Size.OperatingCoordinate;
import top.nserly.PicturePlayer.Size.SizeOperate;
import top.nserly.PicturePlayer.Utils.ImageManager.Blur.MultiThreadBlur;
import top.nserly.PicturePlayer.Utils.ImageManager.ImageRotationHelper;
import top.nserly.PicturePlayer.Utils.ImageManager.Info.GetImageInformation;
import top.nserly.PicturePlayer.Utils.ImageManager.PictureInformationStorageManagement;
import top.nserly.PicturePlayer.Version.DownloadChecker.AdvancedDownloadSpeed;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

//创建类
@Slf4j
public class PaintPicturePanel extends JPanel {
    //图片打开面板
    public static PaintPicturePanel paintPicture;
    public final Thread init;
    //上部组件
    public JPanel AboveMainPanel;
    //下部总组件
    public JPanel BelowMainPanel;
    //下部左组件
    public JPanel SouthLeftPanel;
    //下部右组件
    public JPanel SouthRightPanel;
    //图片分辨率
    public JLabel PictureResolution;
    //逆时针旋转按钮
    public JButton counterclockwise;
    //还原图片缩放按钮
    public JButton Reset;
    //全屏按钮
    public JButton FullScreen;
    //顺时针旋转按钮
    public JButton clockwise;
    //图片大小
    public JLabel PictureSize;
    //图片缩小按钮
    public JButton reduceButton;
    //图片缩放调节滑动条
    public JSlider PercentSlider;
    //图片放大按钮
    public JButton enlargeButton;
    //打开上一个图片
    private static final int LastSign = 0x25;
    //打开下一个图片
    private static final int NextSign = 0x27;
    //创建鼠标坐标管理对象
    OperatingCoordinate op;
    //鼠标最小移动坐标位
    Point MinPoint;
    //鼠标最大移动坐标位
    Point MaxPoint;
    //图片显示器尺寸
    Dimension ShowingSize;
    //图片渲染器在屏幕上的坐标
    Point LocationOnScreen;
    //图片信息模板
    public PictureInformationViewer pictureInformationViewer;
    //图片比例管理
    public SizeOperate sizeOperate;
    //图片渲染管理
    public ImageCanvas imageCanvas;
    //缩放比例标签
    PercentLabel percentLabel;
    //移动图片时，鼠标最开始的坐标（对于桌面）
    Point mouseLocation;
    @Getter
    //当前图片路径下所有图片
    ArrayList<String> CurrentPathOfPicture;
    //是否启用硬件加速
    public static boolean isEnableHardwareAcceleration;
    private MouseAdapter mouseAdapter;
    private File lastPicturePathParent;
    private static boolean isMousePressed; // 标记鼠标是否按下

    public PictureInformationStorageManagement pictureInformationStorageManagement;
    //图片全屏窗体
    public FullScreenFrame fullScreenWindow;
    private JPanel MainPanel;
    private JButton MorePictureInfoButton;

    //构造方法（函数）（用于直接显示图片）
    public PaintPicturePanel(String path) {
        this();
        openPicture(path);
    }

    //构造方法（无参函数）（用于初始化）
    public PaintPicturePanel() {
        paintPicture = this;
        $$$setupUI$$$();
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("data/PictureCacheManagement.obj"))) {
            pictureInformationStorageManagement = (PictureInformationStorageManagement) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.error(ExceptionHandler.getExceptionMessage(e));
            pictureInformationStorageManagement = new PictureInformationStorageManagement();
        }
        init = new Thread(() -> {
            ExceptionHandler.setUncaughtExceptionHandler(log);
            setLayout(new BorderLayout());
            fullScreenWindow = new FullScreenFrame();
            pictureInformationViewer = new PictureInformationViewer(GUIStarter.main);
            //创建画布
            imageCanvas = new ImageCanvas();
            sizeOperate = new SizeOperate(imageCanvas, null);
            //添加画布至组件中
            MainPanel.add(imageCanvas, BorderLayout.CENTER);
            add(MainPanel, BorderLayout.CENTER);
        });
        init.setPriority(Thread.MAX_PRIORITY);
        init.start();
        new Thread(() -> {
            try {
                init.join();
            } catch (InterruptedException ignored) {

            }

            PercentSlider.setMaximum(sizeOperate.MaxPercent);
            PercentSlider.setMinimum(sizeOperate.MinPercent);

            //设置文本中显示的图片缩放比例
            percentLabel.set((int) sizeOperate.getPercent());
            //设置图片缩放滑动条
            PercentSlider.setValue((int) sizeOperate.getPercent());
            init_Listener();
        }).start();
    }

    public void fitComponent() {
        sizeOperate.incomeWindowDimension(imageCanvas.getSize());
        sizeOperate.setPercent(sizeOperate.getPictureOptimalSize());
        sizeOperate.update(false);
    }

    private void loadPictureInTheParent(String picturePath) {
        File pictureParent = new File(picturePath).getParentFile();
        if (pictureParent == null) {
            picturePath = System.getProperty("user.dir");
            pictureParent = new File(picturePath);
        }
        if (!pictureParent.equals(lastPicturePathParent)) {
            //获取当前图片路径下所有图片
            CurrentPathOfPicture = GetImageInformation.getCurrentPathOfPicture(picturePath);
            lastPicturePathParent = pictureParent;
            GUIStarter.main.reviewPictureList(CurrentPathOfPicture);
        }
    }

    public void openPicture(String path) {
        changePicturePath(path);
    }

    public void openPicture(BufferedImage bufferedImage, String path) {
        changePicturePath(bufferedImage, path);
    }

    private void init_Listener() {
        FullScreen.addActionListener(e -> {
            GUIStarter.main.paintPicture.imageCanvas.setFullScreen(true);
        });
        //点击时间
        AtomicLong ClickedTime = new AtomicLong();
        //规定时间内点击次数
        AtomicInteger times = new AtomicInteger();
        //上次点击reset按钮时的角度
        AtomicReference<Byte> lastRotationDegrees = new AtomicReference<>((byte) 0);
        //创建还原按钮监听器
        Reset.addActionListener(e -> {
            aaa:
            if (System.currentTimeMillis() - ClickedTime.get() < 1500) {
                //若上次点击reset按钮时的角度与当前的旋转角度不否
                if (imageCanvas.RotationDegrees != 0 && lastRotationDegrees.getAndSet(imageCanvas.RotationDegrees) != imageCanvas.RotationDegrees) {
                    //将大小调到合适的比例
                    sizeOperate.setPercent(sizeOperate.getPictureOptimalSize());
                    sizeOperate.update(false);
                    times.set(1);
                    break aaa;
                }
                if (times.get() == 1) {
                    //恢复默认大小（100%）
                    sizeOperate.restoreTheDefaultPercent();
                    sizeOperate.update(false);
                } else if (times.get() == 2 && imageCanvas.RotationDegrees != 0) {
                    //旋转回来，并将大小调到合适的比例
                    imageCanvas.reSetDegrees();
                    sizeOperate.setPercent(sizeOperate.getPictureOptimalSize());
                    sizeOperate.update(false);
                } else if (times.get() == 3) {
                    sizeOperate.restoreTheDefaultPercent();
                    sizeOperate.update(false);
                }
                times.getAndIncrement();
            } else {
                //将大小调到合适的比例
                sizeOperate.setPercent(sizeOperate.getPictureOptimalSize());
                sizeOperate.update(false);
                times.set(1);
            }
            ClickedTime.set(System.currentTimeMillis());
        });
        //创建图片顺时针按钮监听器
        clockwise.addActionListener(e -> {
            imageCanvas.turnLeft();
        });
        //创建图片顺时针按钮监听器
        counterclockwise.addActionListener(e -> {
            imageCanvas.turnRight();
        });
        //添加面板大小改变监听器
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (fullScreenWindow.isShowing() || imageCanvas.getPath() == null) return;
                sizeOperate.incomeWindowDimension(imageCanvas.getSize());
                sizeOperate.update(false);
            }
        });


        MorePictureInfoButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (imageCanvas.getPath() != null && !imageCanvas.getPath().equals(pictureInformationViewer.getPicturePath())) {
                    try {
                        pictureInformationViewer.update(GetImageInformation.getImageHandle(new File(imageCanvas.getPath())));
                    } catch (IOException ex) {
                        log.error(ExceptionHandler.getExceptionMessage(ex));
                    }
                }
                pictureInformationViewer.setVisible(true);
            }
        });

        PercentSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isMousePressed = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isMousePressed = false;
            }
        });

        PercentSlider.addChangeListener(e -> {
            if (isMousePressed) {
                percentLabel.set(PercentSlider.getValue());
                sizeOperate.setPercent(PercentSlider.getValue());
                sizeOperate.update(false);
            }
        });
        reduceButton.addMouseListener(new MouseAdapter() {
            // 创建一个 ScheduledExecutorService 来执行定时任务
            private ScheduledExecutorService executor = null;

            // 点击、长按触发
            @Override
            public void mousePressed(MouseEvent e) {
                if (executor != null && !executor.isShutdown()) {
                    // 如果 executor 已经存在且未关闭，则先关闭它
                    executor.shutdownNow();
                    executor.close();
                }

                // 创建一个新的 ScheduledExecutorService
                executor = Executors.newSingleThreadScheduledExecutor();

                // 每 16 毫秒执行一次任务
                executor.scheduleAtFixedRate(() -> {
                    if (!reduceButton.isEnabled()) {
                        return;
                    }
                    if (sizeOperate.adjustPercent(SizeOperate.Reduce)) {
                        sizeOperate.update(false);
                    }
                }, 0, 16, TimeUnit.MILLISECONDS);
            }

            // 鼠标释放触发
            @Override
            public void mouseReleased(MouseEvent e) {
                if (executor != null && !executor.isShutdown()) {
                    // 关闭 ScheduledExecutorService
                    executor.shutdownNow();
                    executor.close();
                }
            }
        });

        enlargeButton.addMouseListener(new MouseAdapter() {
            // 创建一个 ScheduledExecutorService 来执行定时任务
            private ScheduledExecutorService executor = null;

            // 点击、长按触发
            @Override
            public void mousePressed(MouseEvent e) {
                if (executor != null && !executor.isShutdown()) {
                    // 如果 executor 已经存在且未关闭，则先关闭它
                    executor.shutdownNow();
                    executor.close();
                }

                // 创建一个新的 ScheduledExecutorService
                executor = Executors.newSingleThreadScheduledExecutor();

                // 每 16 毫秒执行一次任务
                executor.scheduleAtFixedRate(() -> {
                    if (!enlargeButton.isEnabled()) {
                        return;
                    }
                    if (sizeOperate.adjustPercent(SizeOperate.Enlarge)) {
                        sizeOperate.update(false);
                    }
                }, 0, 16, TimeUnit.MILLISECONDS);
            }

            // 鼠标释放触发
            @Override
            public void mouseReleased(MouseEvent e) {
                if (executor != null && !executor.isShutdown()) {
                    // 关闭 ScheduledExecutorService
                    executor.shutdownNow();
                    executor.close();
                }
            }
        });
    }

    //显示图片大小、分辨率等信息
    private void setPictureInformationOnComponent(BufferedImage bufferedImage, String path) {
        if (PictureSize == null) return;
        File PictureFile = new File(path);
        Dimension dimension = new Dimension(bufferedImage.getWidth(), bufferedImage.getHeight());
        PictureSize.setText(AdvancedDownloadSpeed.formatBytes(PictureFile.length()));
        PictureResolution.setText((int) dimension.getWidth() + "x" + (int) dimension.getHeight());
    }

    //改变图片路径
    public void changePicturePath(String path) {
        log.info("Opened:\"{}\"", path);
        try {
            init.join();
        } catch (InterruptedException ignored) {

        }
        validate();
        sizeOperate.incomeWindowDimension(imageCanvas.getSize());
        imageCanvas.changePicturePath(path);
    }

    //改变图片路径
    public void changePicturePath(BufferedImage bufferedImage, String path) {
        log.info("Opened:\"{}\"", path);
        try {
            init.join();
        } catch (InterruptedException ignored) {

        }
        validate();
        sizeOperate.incomeWindowDimension(imageCanvas.getSize());
        imageCanvas.changePicturePath(bufferedImage, path, GetImageInformation.getHashcode(new File(path)));
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        MainPanel = new JPanel();
        MainPanel.setLayout(new BorderLayout(0, 0));
        MainPanel.setRequestFocusEnabled(false);
        AboveMainPanel = new JPanel();
        AboveMainPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        AboveMainPanel.setRequestFocusEnabled(false);
        MainPanel.add(AboveMainPanel, BorderLayout.NORTH);
        counterclockwise = new JButton();
        counterclockwise.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(counterclockwise, this.$$$getMessageFromBundle$$$("messages", "Display_RotateCounterclockwise"));
        AboveMainPanel.add(counterclockwise);
        Reset = new JButton();
        Reset.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(Reset, this.$$$getMessageFromBundle$$$("messages", "Display_reset"));
        AboveMainPanel.add(Reset);
        FullScreen = new JButton();
        FullScreen.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(FullScreen, this.$$$getMessageFromBundle$$$("messages", "Display_FullScreenButton"));
        AboveMainPanel.add(FullScreen);
        clockwise = new JButton();
        clockwise.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(clockwise, this.$$$getMessageFromBundle$$$("messages", "Display_RotateClockwiseButton"));
        AboveMainPanel.add(clockwise);
        percentLabel = new PercentLabel();
        percentLabel.setText("0%");
        AboveMainPanel.add(percentLabel);
        BelowMainPanel = new JPanel();
        BelowMainPanel.setLayout(new BorderLayout(1, 2));
        BelowMainPanel.setRequestFocusEnabled(false);
        MainPanel.add(BelowMainPanel, BorderLayout.SOUTH);
        SouthLeftPanel = new JPanel();
        SouthLeftPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 5, 5));
        SouthLeftPanel.setRequestFocusEnabled(false);
        BelowMainPanel.add(SouthLeftPanel, BorderLayout.WEST);
        PictureResolution = new JLabel();
        Font PictureResolutionFont = this.$$$getFont$$$(null, -1, 15, PictureResolution.getFont());
        if (PictureResolutionFont != null) PictureResolution.setFont(PictureResolutionFont);
        PictureResolution.setRequestFocusEnabled(false);
        PictureResolution.setText("");
        SouthLeftPanel.add(PictureResolution);
        PictureSize = new JLabel();
        Font PictureSizeFont = this.$$$getFont$$$(null, -1, 15, PictureSize.getFont());
        if (PictureSizeFont != null) PictureSize.setFont(PictureSizeFont);
        PictureSize.setRequestFocusEnabled(false);
        PictureSize.setText("");
        SouthLeftPanel.add(PictureSize);
        MorePictureInfoButton = new JButton();
        this.$$$loadButtonText$$$(MorePictureInfoButton, this.$$$getMessageFromBundle$$$("messages", "PictureViewer_MorePictureInfoButton"));
        SouthLeftPanel.add(MorePictureInfoButton);
        SouthRightPanel = new JPanel();
        SouthRightPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        SouthRightPanel.setRequestFocusEnabled(false);
        BelowMainPanel.add(SouthRightPanel, BorderLayout.EAST);
        reduceButton = new JButton();
        reduceButton.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(reduceButton, this.$$$getMessageFromBundle$$$("messages", "Display_reduce"));
        SouthRightPanel.add(reduceButton);
        PercentSlider = new JSlider();
        PercentSlider.setRequestFocusEnabled(false);
        SouthRightPanel.add(PercentSlider);
        enlargeButton = new JButton();
        enlargeButton.setRequestFocusEnabled(false);
        this.$$$loadButtonText$$$(enlargeButton, this.$$$getMessageFromBundle$$$("messages", "Display_enlarge"));
        SouthRightPanel.add(enlargeButton);
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
        return MainPanel;
    }

    public class ImageCanvas extends JComponent {
        //图片路径
        @Getter
        String path;
        //判断是否启用硬件加速（针对本图片渲染）
        private boolean isEnableHardware;
        //上次图片路径
        String lastPath;
        //获取图片hashcode值
        //图片hashcode
        @Getter
        String picture_hashcode;
        //当前图片X坐标
        private double X;
        //当前图片Y坐标
        private double Y;
        //点击时，鼠标X坐标
        private double mouseX;
        //点击时，鼠标Y坐标
        private double mouseY;
        //上次图片缩放比
        private double LastPercent;
        //上次图片宽度
        private double lastWidth;
        //上次图片高度
        private double lastHeight;
        //当前图片
        private BufferedImage image;
        //模糊后的bufferedImage
        private BufferedImage BlurBufferedImage;
        //当前组件信息
        private Dimension NewWindow;
        //上次组件信息
        private Dimension LastWindow;
        //旋转度数（逆时针）
        private byte RotationDegrees;
        //上次旋转度数
        private byte lastRotationDegrees;
        //是否为移动
        private boolean isMove;

        //是否要模糊显示
        private boolean isNeedBlurToView;

        //创建时间计时器，（图片模糊）
        private Timer timer;
        //模糊化类
        final MultiThreadBlur multiThreadBlur;

        //是否为低占用模式
        @Getter
        private boolean isLowOccupancyMode = false;

        //构造方法初始化（用于初始化类）
        public ImageCanvas() {
            setDoubleBuffered(true);
            multiThreadBlur = new MultiThreadBlur();
            //初始化监听器
            new Thread(this::init_listener).start();
            new Thread(() -> {
                isEnableHardware = isEnableHardwareAcceleration;
                timer = new Timer(400, e -> {
                    if (!isEnableHardware) return;
                    ((Timer) e.getSource()).stop(); // 停止计时器
                    new Thread(() -> {
                        if (image != null && multiThreadBlur.getSrc() != null) {
                            int KernelSize = multiThreadBlur.calculateKernelSize(sizeOperate.getPercent());
                            if (KernelSize == 1) {
                                BlurBufferedImage = multiThreadBlur.getSrc();
                            } else {
                                BlurBufferedImage = multiThreadBlur.applyOptimizedBlur(KernelSize);
                            }
                            isNeedBlurToView = true;
                            repaint();
                        }
                    }).start();
                });
            }).start();
        }

        //构造方法初始化（用于直接显示图片）
        public ImageCanvas(String path) {
            this();
            //加载图片
            changePicturePath(path);
        }

        public ImageCanvas(String path, String picture_hashcode) {
            this();
            //加载图片
            changePicturePath(path, picture_hashcode);
        }

        //设置低占用模式
        public synchronized void setLowOccupancyMode(boolean isLowOccupancyMode) {
            if (this.isLowOccupancyMode == isLowOccupancyMode) return;
            this.isLowOccupancyMode = isLowOccupancyMode;
            if (isLowOccupancyMode) {
                if (timer != null)
                    timer.stop();
                multiThreadBlur.flushSrc();
                multiThreadBlur.flushDest();
                MultiThreadBlur.clearTable();
                if (image != null)
                    image.flush();
                if (BlurBufferedImage != null)
                    BlurBufferedImage.flush();
                image = BlurBufferedImage = null;
                GUIStarter.main.reviewPictureList(new ArrayList<>());
            } else {
                image = GetImageInformation.getImage(
                        pictureInformationStorageManagement.getCachedPicturePath(path, picture_hashcode));
                if (isEnableHardware) {
                    new Thread(() -> {
                        BlurBufferedImage = GetImageInformation.castToTYPEINTRGB(image);
                        synchronized (multiThreadBlur) {
                            multiThreadBlur.changeImage(BlurBufferedImage);
                        }
                        GUIStarter.main.reviewPictureList(CurrentPathOfPicture);
                    }).start();
                }
            }
        }

        //图片左转
        public void turnLeft() {
            addDegrees((byte) 1);
        }


        //图片右转
        public void turnRight() {
            addDegrees((byte) -1);
        }

        //重置旋转度数
        public void reSetDegrees() {
            RotationDegrees = 0;
            if (lastRotationDegrees != RotationDegrees) sizeOperate.changeCanvas(this, true);
        }


        //改变度数
        private void addDegrees(byte addDegrees) {
            RotationDegrees += addDegrees;
            RotationDegrees = (byte) (RotationDegrees % 4);
            if (RotationDegrees < 0) RotationDegrees = (byte) (4 + RotationDegrees);
            sizeOperate.update(false);
        }

        //设置度数
        private void setDegrees(int Degrees) {
            RotationDegrees = (byte) (Degrees % 4);
            if (RotationDegrees < 0) RotationDegrees = (byte) (4 + RotationDegrees);
            sizeOperate.changeCanvas(this, true);
        }

        //获取度数
        public int getDegrees() {
            return Math.abs(RotationDegrees) * 90;
        }

        //改变图片路径
        public void changePicturePath(String path) {
            changePicturePath(path, GetImageInformation.getHashcode(new File(path)));
        }

        public void changePicturePath(String path, String picture_hashcode) {
            BufferedImage image = GetImageInformation.getImage(
                    pictureInformationStorageManagement.getCachedPicturePath(path, picture_hashcode));
            changePicturePath(image, path, picture_hashcode);
        }

        public void changePicturePath(final BufferedImage image, String path, String picture_hashcode) {
            if (image == null) throw new RuntimeException("Image is null, cannot change picture path.");
            //如果字符串前缀与后缀包含"，则去除其中的"
            if (path.startsWith("\"") && path.endsWith("\"")) {
                path = path.substring(1, path.length() - 1);
            }

            timer.stop();
            boolean isRepeat = path.equals(this.path) && picture_hashcode.equals(this.picture_hashcode);

            if (!isRepeat) {
                this.path = path;
                this.picture_hashcode = picture_hashcode;
                LastPercent = lastWidth = lastHeight = X = Y = mouseX = mouseY = RotationDegrees = lastRotationDegrees = 0;
                NewWindow = LastWindow = null;
                if (this.image != null) this.image.flush();
                if (BlurBufferedImage != null) BlurBufferedImage.flush();
            }
            this.image = image;
            if (sizeOperate != null) sizeOperate.changeCanvas(this, !isRepeat);

            String finalPath = path;

            if (isLowOccupancyMode)
                isLowOccupancyMode = false;


            new Thread(() -> {
                //设置当前图片路径下图片信息
                if (!isRepeat)
                    setPictureInformationOnComponent(image, finalPath);
                //若这两个文件父目录不相同
                //则加载当前图片路径下所有图片
                loadPictureInTheParent(finalPath);
            }).start();
            if (isEnableHardware && !isRepeat) {
                new Thread(() -> {
                    BlurBufferedImage = GetImageInformation.castToTYPEINTRGB(image);
                    synchronized (multiThreadBlur) {
                        multiThreadBlur.changeImage(BlurBufferedImage);
                    }
                    sizeOperate.update(false);
                }).start();
            }
        }

        public void changePicturePath(final BufferedImage image, String path) {
            changePicturePath(image, path, GetImageInformation.getHashcode(new File(path)));
        }


        public void close() {
            removeAll();
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(
                    new FileOutputStream("data/PictureCacheManagement.obj"))) {
                objectOutputStream.writeObject(pictureInformationStorageManagement);
                objectOutputStream.flush();
            } catch (IOException e) {
                log.error(ExceptionHandler.getExceptionMessage(e));
            }
            timer.stop();
            image = BlurBufferedImage = null;
            path = lastPath = picture_hashcode = null;
            LastPercent = lastWidth = lastHeight = X = Y = mouseX = mouseY = lastRotationDegrees = RotationDegrees = 0;
            NewWindow = LastWindow = null;
            isNeedBlurToView = isMove = false;
            timer = null;
            System.gc();
        }

        //设置已知组件长度
        public void setWindowSize(Dimension window) {
            this.NewWindow = window;
        }

        //获取图片高度
        public int getImageHeight() {
            if (image == null) return 0;
            return image.getHeight();
        }

        //获取图片宽度
        public int getImageWidth() {
            if (image == null) return 0;
            return image.getWidth();
        }

        //打开上一个/下一个图片
        public void openLONPicture(int sign) {
            for (String path : CurrentPathOfPicture) {
                File file = new File(path);
                if (!file.exists()) CurrentPathOfPicture.remove(path);
            }
            int CurrentIndex = CurrentPathOfPicture.indexOf(imageCanvas.path);
            switch (sign) {
                case LastSign -> {
                    if (CurrentIndex > 0) {
                        //向控制台输出打开文件路径
                        log.info("Opened:\"{}\"", CurrentPathOfPicture.get(CurrentIndex - 1));
                        imageCanvas.changePicturePath(CurrentPathOfPicture.get(CurrentIndex - 1));
                        sizeOperate.update(false);
                    }
                }
                case NextSign -> {
                    if (CurrentIndex + 1 < CurrentPathOfPicture.size()) {
                        //向控制台输出打开文件路径
                        log.info("Opened:\"{}\"", CurrentPathOfPicture.get(CurrentIndex + 1));
                        imageCanvas.changePicturePath(CurrentPathOfPicture.get(CurrentIndex + 1));
                        sizeOperate.update(false);
                    }
                }
            }
        }

        //判断是否有下一个图片
        public boolean hasNext(int sign) {
            int CurrentIndex = CurrentPathOfPicture.indexOf(imageCanvas.path);
            switch (sign) {
                case LastSign -> {
                    if (CurrentIndex > 0) {
                        return true;
                    }
                }
                case NextSign -> {
                    if (CurrentIndex + 1 < CurrentPathOfPicture.size()) {
                        return true;
                    }
                }
            }
            return false;
        }


        @Override
        public synchronized void paint(Graphics g) {
            if (isLowOccupancyMode) {
                setLowOccupancyMode(false);
            }
            //如果没有图片，直接返回
            if (isLowOccupancyMode || NewWindow == null || NewWindow.width == 0 || NewWindow.height == 0
                    || (LastPercent == sizeOperate.getPercent() && RotationDegrees == LastPercent
                    && !isMove && lastPath.equals(path)) || getImageWidth() == 0 || getImageHeight() == 0) {
                g.dispose();
                return;
            }

            // 获取当前图形环境配置
            timer.stop();
            double FinalX = X, FinalY = Y;
            var graphics2D = (Graphics2D) g;
            graphics2D.rotate(Math.toRadians(RotationDegrees * 90));
            // 1. 启用图形抗锯齿（对线条、形状有效）
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 2. 启用图像插值（对缩放后的图像有效）
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (isNeedBlurToView && BlurBufferedImage != null) {
                // 3. 可选：更高质量但更慢的插值
                graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics2D.drawImage(BlurBufferedImage, (int) X, (int) Y, (int) lastWidth, (int) lastHeight, null);
                isNeedBlurToView = false;
                g.dispose();
                return;
            }
            //如果转动角度为0或180度，请无使用此值，此值为旋转其他度数的设计
            double tempHeight = 0, tempWidth = 0;
            tempWidth = lastHeight;
            tempHeight = lastWidth;
            //如果旋转角度不为0度，则转换x,y值（因为坐标轴）
            if (RotationDegrees == 1) {
                double temp = mouseX;
                mouseX = mouseY;
                mouseY = -temp;
            } else if (RotationDegrees == 2) {
                mouseX = -mouseX;
                mouseY = -mouseY;
            } else if (RotationDegrees == 3) {
                double temp = mouseX;
                mouseX = -mouseY;
                mouseY = temp;
            }

            double width;
            double WindowHeight = 0, WindowWidth = 0, LastWindowHeight = 0, LastWindowWidth = 0;
            //尝试获取之前和现在的组件大小信息
            WindowWidth = NewWindow.getWidth();
            WindowHeight = NewWindow.getHeight();
            if (LastWindow == null || LastWindow.width == 0 || LastWindow.height == 0) {
                LastWindow = NewWindow;
            }
            LastWindowWidth = LastWindow.getWidth();
            LastWindowHeight = LastWindow.getHeight();
            //判断是否为移动（若移动，则执行本代码）;窗体、图片缩放比例相比于之前是否存在改变（如果没有，则执行本代码）
            if (isMove && RotationDegrees == lastRotationDegrees && LastPercent == sizeOperate.getPercent()
                    && LastWindow != null && LastWindow.equals(NewWindow)) {
                X += mouseX;
                Y += mouseY;
                if (RotationDegrees == 0) {
                    if (X > WindowWidth) X = WindowWidth;
                    if (Y > WindowHeight) Y = WindowHeight;
                    if (X + lastWidth < 0) X = -lastWidth;
                    if (Y + lastHeight < 0) Y = -lastHeight;
                } else if (RotationDegrees == 1) {
                    if (X > WindowHeight) X = WindowHeight;
                    if (Y > 0) Y = 0;
                    if (X + tempHeight < 0) X = -tempHeight;
                    if (Y + tempWidth < -WindowWidth) Y = (-WindowWidth - tempWidth);
                } else if (RotationDegrees == 2) {
                    if (X + lastWidth < -WindowWidth) X = (-WindowWidth - lastWidth);
                    if (Y > 0) Y = 0;
                    if (X > 0) X = 0;
                    if (Y + lastHeight < -WindowHeight) Y = (-WindowHeight - lastHeight);
                } else if (RotationDegrees == 3) {
                    if (X > 0) X = 0;
                    if (X + tempHeight < -WindowHeight) X = (-WindowHeight - tempHeight);
                    if (Y > WindowWidth) Y = WindowWidth;
                    if (Y + tempWidth < 0) Y = -tempWidth;
                }
                graphics2D.drawImage(image, (int) X, (int) Y, (int) lastWidth, (int) lastHeight, null);
                mouseX = mouseY = 0;
                lastRotationDegrees = RotationDegrees;
                isMove = false;
                lastPath = path;
                if (isEnableHardware) {
                    timer.start();
                }
                g.dispose();
                return;
            }
            //判断图片缩放比例是否与上次相同
            if (RotationDegrees != lastRotationDegrees) {
                sizeOperate.setPercent(sizeOperate.getPictureOptimalSize());
                Point point = ImageRotationHelper.getRotatedCord((int) FinalX, (int) FinalY,
                        360 - 90 * RotationDegrees, (int) lastWidth, (int) lastHeight);
                FinalX = point.getX();
                FinalY = point.getY();
            }

            if (RotationDegrees == lastRotationDegrees && mouseX == 0 && mouseY == 0) {
                if (RotationDegrees == 0) {
                    mouseX = (WindowWidth / 2);
                    mouseY = (WindowHeight / 2);
                } else if (RotationDegrees == 1) {
                    mouseX = (WindowHeight / 2);
                    mouseY = -(WindowWidth / 2);
                } else if (RotationDegrees == 2) {
                    mouseX = -(WindowWidth / 2);
                    mouseY = -(WindowHeight / 2);
                } else if (RotationDegrees == 3) {
                    mouseX = -(WindowHeight / 2);
                    mouseY = (WindowWidth / 2);
                }
            }
            double WidthRatio = 0;
            double HeightRatio = 0;
            double PictureChangeRatio = 1;
            if (WindowWidth != 0 && WindowHeight != 0) {
                WidthRatio = LastWindowWidth / WindowWidth;
                HeightRatio = LastWindowHeight / WindowHeight;
                if (WidthRatio != 1 && HeightRatio != 1) PictureChangeRatio = (HeightRatio + WidthRatio) / 2;
            }
            if (PictureChangeRatio == 0) {
                g.dispose();
                return;
            }
            width = (getImageWidth() * sizeOperate.getPercent() / 100 * (1 / PictureChangeRatio));
            double height = width * getImageHeight() / getImageWidth();
            sizeOperate.setPercent(width * 100.0 / getImageWidth());

            if (RotationDegrees % 2 == 0 && NewWindow != null && NewWindow.equals(LastWindow)
                    && lastWidth != 0 && lastHeight != 0) {
                FinalX = width * (FinalX - mouseX) / lastWidth + mouseX;
                FinalY = height * (FinalY - mouseY) / lastHeight + mouseY;
            } else if (RotationDegrees % 2 == 1 && NewWindow != null && NewWindow.equals(LastWindow)
                    && lastWidth != 0 && lastHeight != 0) {
                FinalX = height * (FinalX - mouseX) / lastHeight + mouseX;
                FinalY = width * (FinalY - mouseY) / lastWidth + mouseY;
            }

            if (RotationDegrees == 0) {
                if (WindowWidth <= width) {
                    if (FinalX > 0) FinalX = 0;
                    if (FinalX + width < WindowWidth) FinalX = (WindowWidth - width);
                } else FinalX = ((WindowWidth - width) / 2);

                if (WindowHeight <= height) {
                    if (FinalY > 0) FinalY = 0;
                    if (FinalY + height < WindowHeight) FinalY = (WindowHeight - height);
                } else FinalY = ((WindowHeight - height) / 2);
            } else if (RotationDegrees == 1) {
                if (WindowHeight <= width) {
                    if (FinalX > 0) FinalX = 0;
                    if (FinalX + width < WindowHeight) FinalX = (WindowHeight - width);
                } else FinalX = ((WindowHeight - width) / 2);

                if (WindowWidth <= height) {
                    if (FinalY > -WindowWidth) FinalY = -WindowWidth;
                    if (FinalY < -height) FinalY = -height;
                } else FinalY = ((-height - WindowWidth) / 2);
            } else if (RotationDegrees == 2) {
                if (WindowWidth <= width) {
                    if (FinalX > -WindowWidth) FinalX = -WindowWidth;
                    if (FinalX < -width) FinalX = -width;
                } else FinalX = ((-width - WindowWidth) / 2);

                if (WindowHeight <= height) {
                    if (FinalY > -WindowHeight) FinalY = -WindowHeight;
                    if (FinalY < -height) FinalY = -height;
                } else FinalY = ((-height - WindowHeight) / 2);
            } else if (RotationDegrees == 3) {
                if (WindowHeight <= width) {
                    if (FinalX > -WindowHeight) FinalX = -WindowHeight;
                    if (FinalX < -width) FinalX = -width;
                } else FinalX = ((-width - WindowHeight) / 2);

                if (WindowWidth <= height) {
                    if (FinalY > 0) FinalY = 0;
                    if (FinalY + height < WindowWidth) FinalY = (WindowWidth - height);
                } else FinalY = ((WindowWidth - height) / 2);

            }

            //显示图像
            graphics2D.drawImage(image, (int) FinalX, (int) FinalY, (int) width, (int) height, null);
            //检查比例是否为最大值，如果为最大就把放大按钮禁用
            if (paintPicture.enlargeButton != null) {
                paintPicture.enlargeButton.setEnabled(!paintPicture.sizeOperate.isTheBiggestRatio());
                if (!paintPicture.enlargeButton.isEnabled()) imageCanvas.requestFocus();
            }
            //检查比例是否为最小值，如果为最小就把放大按钮禁用
            if (paintPicture.reduceButton != null) {
                paintPicture.reduceButton.setEnabled(!paintPicture.sizeOperate.isTheSmallestRatio());
                if (!paintPicture.reduceButton.isEnabled()) imageCanvas.requestFocus();
            }

            //设置文本中显示的图片缩放比例
            percentLabel.set((int) sizeOperate.getPercent());
            //设置图片缩放滑动条
            if (PercentSlider != null) PercentSlider.setValue((int) sizeOperate.getPercent());
            this.X = FinalX;
            this.Y = FinalY;
            this.LastWindow = this.NewWindow;
            this.LastPercent = sizeOperate.getPercent();
            this.lastWidth = width;
            this.lastHeight = height;
            this.lastRotationDegrees = RotationDegrees;
            mouseX = mouseY = 0;
            lastPath = path;
            if (isEnableHardware) {
                timer.start();
            }
            g.dispose();
        }

        //设置是否图片移动（不应该改变图片大小）
        public void setIsMove(boolean isMove) {
            this.isMove = isMove;
        }

        //获取是否图片将要移动
        public boolean getIsMove() {
            return isMove;
        }

        //添加坐标值
        public void addCoordinate(int x, int y) {
            this.X += x;
            this.Y += y;
        }

        //恢复默认坐标
        public void setDefaultCoordinate() {
            this.X = this.Y = 0;
        }

        //设置X值大小
        public void setX(int x) {
            this.X = x;
        }

        //设置鼠标坐标值
        public void setMouseCoordinate(int mouseX, int mouseY) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        //设置Y值大小
        public void setY(int y) {
            this.Y = y;
        }

        //初始化监听器
        private void init_listener() {
            Robot robot = null;
            try {
                robot = new Robot();
            } catch (AWTException e) {
                log.warn("Couldn't get Mouse Information");
            }
            Robot finalRobot = robot;
            Image image = Toolkit.getDefaultToolkit().createImage(
                    new MemoryImageSource(0, 0, new int[0], 0, 0));
            final boolean[] EnableCursorDisplay = new boolean[1];
            addMouseListener(new MouseAdapter() {
                //鼠标一按下就触发
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() != MouseEvent.BUTTON1) return;
                    op = new OperatingCoordinate(e.getX(), e.getY());
                    EnableCursorDisplay[0] = SettingsInfoHandle.getBoolean("EnableCursorDisplay",
                            GUIStarter.main.centre.CurrentData);
                    if (EnableCursorDisplay[0]) return;
                    setCursor(Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), null));
                    mouseLocation = MouseInfo.getPointerInfo().getLocation();
                    if (ShowingSize != null && LocationOnScreen != null && ShowingSize.equals(sizeOperate.getWindowSize())
                            && LocationOnScreen.equals(imageCanvas.getLocationOnScreen()))
                        return;
                    LocationOnScreen = imageCanvas.getLocationOnScreen();
                    ShowingSize = sizeOperate.getWindowSize();

                    int minX = -LocationOnScreen.x, minY = -LocationOnScreen.y;
                    if (minX < 0) minX = 0;
                    if (minY < 0) minY = 0;
                    MinPoint = new Point(minX, minY);

                    int maxX = ShowingSize.width, maxY = ShowingSize.height;
                    int x = ShowingSize.width + LocationOnScreen.x;
                    int y = ShowingSize.height + LocationOnScreen.y;
                    if (x > SizeOperate.FreeOfScreenSize.width)
                        maxX = SizeOperate.FreeOfScreenSize.width - LocationOnScreen.x;
                    if (y > SizeOperate.FreeOfScreenSize.height)
                        maxY = SizeOperate.FreeOfScreenSize.height - LocationOnScreen.y;
                    MaxPoint = new Point(maxX, maxY);
                }


                //鼠标放出触发
                public void mouseReleased(MouseEvent e) {
                    if (e.getButton() != MouseEvent.BUTTON1) return;
                    if (!EnableCursorDisplay[0]) {
                        setCursor(Cursor.getDefaultCursor());
                        if (finalRobot != null) finalRobot.mouseMove(mouseLocation.x, mouseLocation.y);
                    }
                }
            });
            paintPicture.mouseAdapter = new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e)) return;
                    int x = e.getX(), y = e.getY();
                    if (!EnableCursorDisplay[0]) {
                        boolean NeedToMove = false;
                        if (x <= MinPoint.x) {
                            x = MaxPoint.x - 2;
                            NeedToMove = true;
                        } else if (x >= MaxPoint.x - 1) {
                            x = MinPoint.x + 1;
                            NeedToMove = true;
                        }
                        if (y <= MinPoint.y) {
                            y = MaxPoint.y - 2;
                            NeedToMove = true;
                        } else if (y >= MaxPoint.y - 1) {
                            y = MinPoint.y + 1;
                            NeedToMove = true;
                        }

                        if (NeedToMove) {
                            op = new OperatingCoordinate(x, y);
                            Point point = imageCanvas.getLocationOnScreen();
                            if (finalRobot != null) {
                                finalRobot.mouseMove(x + point.x, y + point.y);
                            }
                            return;
                        }
                    }
                    //增加坐标值
                    imageCanvas.setMouseCoordinate((int) ((1 + SettingsInfoHandle.getDouble("MouseMoveOffsets",
                                    GUIStarter.main.centre.CurrentData) / 100.0) * (x - op.x())),
                            (int) ((1 + SettingsInfoHandle.getDouble("MouseMoveOffsets",
                                    GUIStarter.main.centre.CurrentData) / 100.0) * (y - op.y())));
                    sizeOperate.update(true);
                    op = new OperatingCoordinate(x, y);
                }
            };

            addMouseMotionListener(paintPicture.mouseAdapter);
            addMouseWheelListener(new MouseAdapter() {
                //鼠标滚轮事件
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    //滚轮向后
                    if (e.getWheelRotation() == 1) {
                        if (sizeOperate.adjustPercent(SizeOperate.Reduce) || sizeOperate.adjustPercent(SizeOperate.Reduce)) {
                            imageCanvas.setMouseCoordinate(e.getX(), e.getY());
                            sizeOperate.update(false);
                        }
                    }//滚轮向前
                    else if (e.getWheelRotation() == -1) {
                        if (sizeOperate.adjustPercent(SizeOperate.Enlarge) || sizeOperate.adjustPercent(SizeOperate.Enlarge)) {
                            imageCanvas.setMouseCoordinate(e.getX(), e.getY());
                            sizeOperate.update(false);
                        }
                    }
                }
            });
            addKeyListener(new KeyAdapter() {
                @Override
                public synchronized void keyReleased(KeyEvent e) {
                    int KeyCode = e.getKeyCode();
                    switch (KeyCode) {
                        case KeyEvent.VK_ESCAPE -> {
                            if (fullScreenWindow.isShowing()) {
                                setFullScreen(false);
                                return;
                            }
                        }
                        case KeyEvent.VK_F11 -> {
                            setFullScreen(!fullScreenWindow.isShowing());
                            return;
                        }
                    }

                    imageCanvas.openLONPicture(KeyCode);
                }
            });
        }

        public void setFullScreen(boolean fullScreen) {
            if (imageCanvas == null || fullScreenWindow == null || GUIStarter.main == null || sizeOperate == null
                    || PaintPicturePanel.paintPicture == null)
                return;
            if (fullScreen == fullScreenWindow.isShowing() && fullScreen != GUIStarter.main.isShowing()) {
                return;
            }
            if (fullScreen) {
                fullScreenWindow.setImageCanvas(imageCanvas);
                GUIStarter.main.getGraphics().dispose();
            } else {
                PaintPicturePanel.paintPicture.MainPanel.add(imageCanvas, BorderLayout.CENTER);
            }
            GUIStarter.main.setVisible(!fullScreen);
            fullScreenWindow.setVisible(fullScreen);
            sizeOperate.incomeWindowDimension(imageCanvas.getSize());
            sizeOperate.update(false);

            setCursor(Cursor.getDefaultCursor());
        }
    }
}