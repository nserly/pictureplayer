package top.nserly.PicturePlayer.Utils.ImageManager.Info;

import lombok.extern.slf4j.Slf4j;
import top.nserly.PicturePlayer.Size.GetSystemSize;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

@Slf4j
public class GetImageInformation {
    public static final boolean isHardwareAccelerated;
    public static final String[] readFormats = ImageIO.getReaderFormatNames();

    private static final ArrayList<String> SupportPictureExtension = new ArrayList<>();

    static {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        isHardwareAccelerated = gc.getBufferCapabilities().isPageFlipping();

        for (String readFormat : readFormats) {
            SupportPictureExtension.add("." + readFormat.toLowerCase());
        }
    }


    //算法实现：获取图片Image对象
    public static BufferedImage getImage(String path) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new File(path))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis, true, true);
                BufferedImage image = reader.read(0); // 只读取第一帧
                reader.dispose();
                return image;
            }
        } catch (Exception e) {
            log.error(ExceptionHandler.getExceptionMessage(e));
        }
        return null;
    }

    //算法实现：写入图片
    public static void writeImage(BufferedImage image, String path, String type) {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path))) {
            ImageIO.write(image, type, bos);
        } catch (IOException e) {
            log.error(ExceptionHandler.getExceptionMessage(e));
        }
    }

    //判断文件路径是否正确、是否为文件（非文件夹）
    public static boolean isRightFilePath(String path) {
        path = path.trim();
        File file = new File(path);
        return file.exists() && file.isFile();
    }

    //算法实现：获取文件是否为受Java支持的图片格式
    public static boolean isImageFile(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return false;
        }

        String fileName = file.getName().toLowerCase();
        return SupportPictureExtension.contains(fileName.substring(fileName.lastIndexOf(".")));
    }

    //算法实现：将byte数组转化为十六进制字符串
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    //算法实现：将普通的BufferedImage转化为TYPE_INT_RGB BufferedImage
    public static BufferedImage castToTYPEINTRGB(BufferedImage src) {
        // 新增：统一图像格式
        BufferedImage convertedImage = new BufferedImage(
                src.getWidth(),
                src.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        convertedImage.getGraphics().drawImage(src, 0, 0, null);
        return convertedImage;
    }

    //算法实现：将Image转换成VolatileImage
    public static VolatileImage convert(BufferedImage source) {
        // 获取当前图形环境配置
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();

        // 创建兼容的 VolatileImage（尺寸与 BufferedImage 一致）
        VolatileImage volatileImage = null;
        do {
            volatileImage = gc.createCompatibleVolatileImage(
                    source.getWidth(),
                    source.getHeight(),
                    VolatileImage.OPAQUE // 根据需求选择透明度模式
            );
            // 验证 VolatileImage 有效性
        } while (volatileImage.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE);

        // 绘制 BufferedImage 到 VolatileImage
        Graphics2D g = volatileImage.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();

        return volatileImage;
    }


    //算法实现：获取图片大小
    public static Dimension getImageSize(File file) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                reader.dispose();
                return new Dimension(width, height);
            }
        }
        return null;
    }

    //算法实现：获取图片hashcode值（CRC32）
    public static String getHashcode(File file) {
        Checksum crc = new CRC32();
        byte[] buffer = new byte[81920];
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                crc.update(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
        // 补零填充至 8 位，保证格式统一
        return String.format("%08x", crc.getValue());
    }

    // 算法实现：多线程获取多个文件的hashcode值（key:文件 ; value:该文件hashcode值）
    public static HashMap<String, String> getHashcode(String[] files) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Map<String, Future<String>> futures = new HashMap<>();

        for (String filePath : files) {
            File file = new File(filePath);
            if (file.exists() && !file.isDirectory()) {
                Future<String> future = executor.submit(() -> getHashcode(file));
                futures.put(filePath, future);
            } else {
                log.warn("File does not exist or is a directory: {}", filePath);
            }
        }

        HashMap<String, String> results = new HashMap<>();
        for (Map.Entry<String, Future<String>> entry : futures.entrySet()) {
            try {
                String hashcode = entry.getValue().get();
                if (hashcode != null) {
                    results.put(entry.getKey(), hashcode);
                }
            } catch (Exception e) {
                log.error("Failed to compute hashcode for file: {}", entry.getKey(), e);
            }
        }

        executor.shutdown();
        return results;
    }


    public static String getPictureType(File file) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis);
                    return reader.getFormatName();
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException e) {
            log.error("Failed to get picture type for file: {}", file.getAbsolutePath(), e);
        }
        return null;
    }

    // 获取位深度的方法
    public static int getBitDepth(String imagePath) throws IOException {
        File file = new File(imagePath);
        BufferedImage image = ImageIO.read(file);
        return image.getColorModel().getPixelSize();
    }

    //判断图片是否是java原本支持的（一般java原本支持的打开、操作image通常比较快，不需要进行图片转换）
    public static boolean isOriginalJavaSupportedPictureType(String path) {
        return path.endsWith(".png") || path.endsWith(".jpeg") || path.endsWith(".jpg");
    }


    //算法实现：获取最佳大小、坐标
    public static Rectangle getBestSize(String path) throws IOException {
        //如果字符串前缀与后缀包含"，则去除其中的"
        if (path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1);
        }
        //初始化宽度、高度
        int Width, Height, X, Y = 0;
        Dimension PictureSize = getImageSize(new File(path));
        //初始化变量
        int SystemWidth = GetSystemSize.width;
        int SystemHeight = GetSystemSize.height;
        int PictureWidth = PictureSize.width;
        int PictureHeight = PictureSize.height;
        //算法实现
        Width = PictureWidth > SystemWidth * 0.8 ? (int) (SystemWidth * 0.8) : PictureWidth;
        Height = PictureHeight > SystemHeight * 0.8 ? (int) (SystemHeight * 0.8) : PictureHeight;
        Height += 70;
        Width += 20;
        if (Height < SystemWidth * 0.3) Height = (int) (SystemWidth * 0.3);
        if (Width < SystemHeight * 0.5) Width = (int) (SystemHeight * 0.5);
        X = Math.abs((int) ((SystemWidth * 0.9 - PictureWidth) / 2));
        Y = Math.abs((int) ((SystemHeight * 0.9 - PictureHeight) / 2));
        //返回结果
        return new Rectangle(X, Y, Width, Height);
    }

    //获取当前图片路径下所有图片
    public static ArrayList<String> getCurrentPathOfPicture(String path) {
        ArrayList<String> arrayList = new ArrayList<>();
        for (File file : Objects.requireNonNull(new File(path).getParentFile().listFiles())) {
            if (GetImageInformation.isImageFile(file)) arrayList.add(file.getPath());
        }
        return arrayList;
    }
}
