package top.nserly.PicturePlayer.NComponent.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import top.nserly.GUIStarter;
import top.nserly.SoftwareCollections_API.OSInformation.SystemMonitor;
import top.nserly.SoftwareCollections_API.Queue.ThreadPoolTaskQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Getter
public class ThumbnailPreviewOfImage extends JComponent {
    // 新增：软引用缓存，避免重复生成相同图片的缩略图（减少重复分配）
    private static final Map<String, SoftReference<BufferedImage>> THUMBNAIL_CACHE = new ConcurrentHashMap<>();
    private static final ThreadPoolTaskQueue loadingPictureTaskQueue =
            new ThreadPoolTaskQueue(
                    SystemMonitor.CPU_Physical_Core_Count,
                    SystemMonitor.CPU_Logical_Thread_Count
            );

    static {
        // 定期清理失效缓存（避免缓存膨胀）
        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            scheduler.scheduleAtFixedRate(() -> {
                THUMBNAIL_CACHE.entrySet().removeIf(entry -> entry.getValue().get() == null);
                log.debug("Clean up the invalid thumbnail cache, the current cache size: {}", THUMBNAIL_CACHE.size());
            }, 5, 5, TimeUnit.MINUTES); // 每5分钟清理一次
        }
    }

    private final MouseAdapter mouseAdapter;
    private final AtomicBoolean isDisposed = new AtomicBoolean(false);
    private BufferedImage image;
    private Future<?> loadTask; // 保存任务Future，用于取消

    public ThumbnailPreviewOfImage(String filePath) {
        this.mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isDisposed.get()) return;
                GUIStarter.main.textField1.setText(filePath);
                if (e.getClickCount() >= 2) {
                    GUIStarter.main.openPicture(filePath);
                }
            }
        };
        addMouseListener(mouseAdapter);
        setToolTipText(filePath);

        // 先查缓存，避免重复生成
        SoftReference<BufferedImage> cacheRef = THUMBNAIL_CACHE.get(filePath);
        if (cacheRef != null && cacheRef.get() != null) {
            this.image = cacheRef.get();
            if (image != null) {
                setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            }
            repaint();
            return;
        }

        // 提交任务（优化内存分配逻辑）
        this.loadTask = loadingPictureTaskQueue.addTask(() -> {
            if (isDisposed.get() || (loadTask != null && loadTask.isCancelled())) {
                return;
            }

            BufferedImage tempImage;
            try {
                // 核心优化1：强制JPEG格式，减少内存占用（比PNG小）
                // 核心优化2：使用TYPE_3BYTE_BGR（3字节/像素）替代TYPE_INT_RGB（4字节/像素），节省25%内存
                tempImage = Thumbnails.of(filePath)
                        .size(80, 80)
                        .outputFormat("jpg") // 强制JPEG
                        .outputQuality(0.7f) // 适当降低质量（0.7足够清晰，进一步减少内存）
                        .imageType(BufferedImage.TYPE_3BYTE_BGR) // 关键：减少色彩深度
                        .allowOverwrite(true)
                        .asBufferedImage();

                // 核心优化3：及时释放临时资源（若任务取消）
                if (isDisposed.get() || (loadTask != null && loadTask.isCancelled())) {
                    if (tempImage != null) {
                        tempImage.flush();
                    }
                    return;
                }

                // 存入缓存（软引用：内存不足时自动回收）
                THUMBNAIL_CACHE.put(filePath, new SoftReference<>(tempImage));

                BufferedImage finalTempImage = tempImage;
                SwingUtilities.invokeLater(() -> {
                    if (!isDisposed.get()) {
                        this.image = finalTempImage;
                        setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
                        repaint();
                    } else {
                        if (finalTempImage != null) finalTempImage.flush();
                    }
                });
            } catch (IOException e) {
                log.error("Failed to load the thumbnail: {}", filePath, e);
            }
        });
    }

    // 判断所有图片是否加载完毕
    public static boolean isCompleteLoading() {
        // 正确逻辑： 无活跃线程 且 无等待任务
        return  loadingPictureTaskQueue.getActiveThreadCount() == 0
                && loadingPictureTaskQueue.getPendingTaskCount() == 0;
    }

    // 等待图片加载完毕（带超时和中断处理）
    public static void waitTillCompleteLoading() throws InterruptedException {
        waitTillCompleteLoading(0); // 调用带超时的重载方法，0表示不超时
    }

    /**
     * 等待图片加载完毕（带超时机制）
     * @param timeout 最大等待时间（毫秒），0表示无限等待
     * @return 若加载完成返回true，超时返回false
     * @throws InterruptedException 若线程被中断则抛出
     */
    public static boolean waitTillCompleteLoading(long timeout) throws InterruptedException {
        long start = System.currentTimeMillis();
        long remaining = timeout;

        // 循环检查直到加载完成或超时
        while (true) {
            if (isCompleteLoading()) {
                return true; // 加载完成
            }

            // 检查是否超时
            if (timeout > 0) {
                remaining = timeout - (System.currentTimeMillis() - start);
                if (remaining <= 0) {
                    return false; // 超时未完成
                }
            }

            // 等待一小段时间后再次检查（使用wait而非sleep，减少CPU占用）
            synchronized (loadingPictureTaskQueue) {
                loadingPictureTaskQueue.wait(Math.min(250, remaining > 0 ? remaining : 250));
            }
        }
    }

    // 新增：通知等待线程加载状态变化（在任务队列状态改变时调用）
    public static void notifyLoadingStateChanged() {
        synchronized (loadingPictureTaskQueue) {
            loadingPictureTaskQueue.notifyAll(); // 唤醒所有等待的线程
        }
    }

    public void dispose() {
        if (isDisposed.compareAndSet(false, true)) {
            // 1. 取消任务（如果任务未执行或正在执行，尝试中断）
            if (loadTask != null) {
                loadTask.cancel(true);
                loadTask = null;
            }

            // 2. 释放图片资源
            if (image != null) {
                image.flush();
                image = null;
            }

            // 3. 移除监听器和父容器关联
            removeMouseListener(mouseAdapter);
            removeAll();

            // 4. 清理UI资源
            setUI(null);
            setToolTipText(null);
        }
    }

    @Override
    public synchronized void paint(Graphics g) {
        if (isDisposed.get()) return;
        super.paint(g);
        if (image == null) {
            // 绘制加载占位符
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
            return;
        }
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, getWidth(), getHeight(), null);
    }
}
