package top.nserly.PicturePlayer.NComponent.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import top.nserly.GUIStarter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Slf4j
@Getter
public class ThumbnailPreviewOfImage extends JComponent {
    private BufferedImage Image;

    public ThumbnailPreviewOfImage(String filePath) throws IOException {
        this.Image = Thumbnails.of(filePath)
                .size(80, 80)
                .outputQuality(0.8) // 设置JPEG质量（0.8~1.0）
                .imageType(BufferedImage.TYPE_INT_RGB) // 减少颜色深度
                .allowOverwrite(true)
                .asBufferedImage(); // 直接返回BufferedImage
        if (Image != null) {
            setPreferredSize(new Dimension(Image.getWidth(), Image.getHeight())); // 设置首选大小
        } else {
            setPreferredSize(new Dimension(50, 50)); // 设置首选大小
        }
        setToolTipText(filePath);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                GUIStarter.main.textField1.setText(filePath);
                if (e.getClickCount() >= 2) {
                    GUIStarter.main.openPicture(filePath);
                }
            }
        });
    }

    public void dispose() {
        if (Image != null) {
            Image.flush(); // 释放资源
            Image = null; // 清除引用
        }
    }

    @Override
    public synchronized void paint(Graphics g) {
        super.paint(g); // 调用父类方法
        var graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (Image != null) {
            graphics2D.drawImage(Image, 0, 0, getWidth(), getHeight(), null);
        }
    }
}