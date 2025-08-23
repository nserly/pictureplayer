package top.nserly.PicturePlayer.Utils.ImageManager.Info;

import lombok.extern.slf4j.Slf4j;
import top.nserly.PicturePlayer.Size.GetSystemSize;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
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
        try {
            ImageHandle imageHandle = getImageHandle(new File(path));
            BufferedImage bu = getImage(imageHandle.imageReader());
            imageHandle.close();
            return bu;
        } catch (IOException e) {
            log.error(ExceptionHandler.getExceptionMessage(e));
            return null;
        }

    }

    //算法实现：获取图片BufferedImage对象
    public static BufferedImage getImage(ImageReader imageReader) throws IOException {
        return imageReader.read(0);
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
        return fileName.contains(".") && SupportPictureExtension.contains(fileName.substring(fileName.lastIndexOf(".")));
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
        BufferedImage convertedImage = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
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
        VolatileImage volatileImage;
        do {
            volatileImage = gc.createCompatibleVolatileImage(source.getWidth(), source.getHeight(), VolatileImage.OPAQUE // 根据需求选择透明度模式
            );
            // 验证 VolatileImage 有效性
        } while (volatileImage.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE);

        // 绘制 BufferedImage 到 VolatileImage
        Graphics2D g = volatileImage.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();

        return volatileImage;
    }

    //算法实现：获取图片ImageHandle
    public static ImageHandle getImageHandle(File file) throws IOException {
        return getImageHandle(file, true);
    }

    /**
     * 获取图片的ImageHandle，该对象封装了用于读取和管理图像的ImageReader和ImageInputStream。
     *
     * @param file            文件对象，表示要处理的图像文件。
     * @param EnableExtension 如果为true，则尝试根据文件扩展名直接获取ImageReader；如果为false或文件扩展名无效，则使用自动检测逻辑。
     * @return 返回一个封装了ImageReader和ImageInputStream的ImageHandle对象。
     * @throws IOException 如果在创建ImageInputStream或查找合适的ImageReader过程中发生错误，则抛出此异常。
     */
    public static ImageHandle getImageHandle(File file, boolean EnableExtension) throws IOException {
        ImageInputStream iis = ImageIO.createImageInputStream(file);
        Iterator<ImageReader> readers;
        if (EnableExtension) {
            String extension = getFileExtension(file.getPath());
            if (extension != null) {
                // 尝试根据文件扩展名直接获取 ImageReader (避免自动检测开销)
                readers = ImageIO.getImageReadersBySuffix(extension);
                ImageReader imageReader = getImageReader(readers, iis);
                if (imageReader != null)
                    try {
                        imageReader.getWidth(0); // 触发读取，确保ImageReader有效
                        return new ImageHandle(file.getPath(), imageReader, iis);
                    } catch (Exception e) {
                        log.error(ExceptionHandler.getExceptionMessage(e));
                        return getImageHandle(file, false); // 如果读取失败，回退到自动检测逻辑
                    }
            }
        }

        // 回退到原始自动检测逻辑
        readers = ImageIO.getImageReaders(iis);
        ImageReader imageReader = getImageReader(readers, iis);
        imageReader.getWidth(0); // 触发读取，确保ImageReader有效
        return new ImageHandle(file.getPath(), imageReader, iis);
    }

    private static String getFileExtension(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < path.length() - 1) {
            return path.substring(dotIndex + 1).toLowerCase();
        }
        return null;
    }

    private static ImageReader getImageReader(Iterator<ImageReader> readers, ImageInputStream iis) {
        ImageReader reader = null;
        if (readers.hasNext()) {
            reader = readers.next();
            reader.setInput(iis, true, true);
        }
        return reader;

    }

    //算法实现：获取图片分辨率
    public static Dimension getImageResolution(File file) throws IOException {
        ImageHandle imageHandle = getImageHandle(file);
        Dimension dimension = getImageResolution(imageHandle.imageReader());
        imageHandle.close();
        return dimension;
    }

    //算法实现：获取图片分辨率
    public static Dimension getImageResolution(ImageReader imageReader) throws IOException {
        return new Dimension(imageReader.getWidth(0), imageReader.getHeight(0));
    }

    /**
     * 算法实现：获取图片DPI（每英寸点数）。
     *
     * @param file 图片文件对象。
     * @return 返回图片的DPI值，如果无法从元数据中获取指定方向的DPI，则返回默认值72.0f。
     * @throws IOException 如果找不到合适的ImageReader或读取过程中发生错误，则抛出此异常。
     */
    public static ImageDPI getImageDPI(File file) throws IOException {
        ImageHandle imageReader = getImageHandle(file);
        ImageDPI imageDPI = getImageDPI(imageReader.imageReader());
        imageReader.close();
        return imageDPI;
    }

    public static ImageDPI getImageDPI(ImageReader imageReader) throws IOException {
        float HorizontalDPI = 72.0f; // 默认水平DPI
        float VerticalDPI = 72.0f; // 默认垂直DPI
        // 获取元数据
        IIOMetadata metadata = imageReader.getImageMetadata(0);
        if (metadata.isStandardMetadataFormatSupported()) {
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_1.0");
            IIOMetadataNode horizontalCR = getNode(root, "HorizontalPixelSize");
            IIOMetadataNode verticalCR = getNode(root, "VerticalPixelSize");

            if (horizontalCR != null) {
                HorizontalDPI = Float.parseFloat(horizontalCR.getAttribute("value"));
            } else if (verticalCR != null) {
                VerticalDPI = Float.parseFloat(verticalCR.getAttribute("value"));
            }
        }
        return new ImageDPI(HorizontalDPI, VerticalDPI);
    }

    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++) {
            if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) rootNode.item(i);
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
    // 注意：此方法使用虚拟线程来提高性能
    // 需要确保JDK版本支持虚拟线程（Java 19及以上）
    // 传入的files数组中每个元素为文件的绝对路径
    // 返回一个HashMap，key为文件路径，value为该文件的hashcode
    // 如果文件不存在或是目录，则在日志中记录警告信息
    // 如果计算hashcode失败，则在日志中记录错误信息
    // 注意：此方法会阻塞直到所有任务完成
    // 返回的HashMap中只包含成功计算hashcode的文件
    // 如果没有成功计算的文件，则返回一个空的HashMap
    // 注意：此方法会自动关闭ExecutorService
    // 如果需要处理大量文件，建议使用此方法以提高性能
    // 注意：此方法会在计算hashcode时使用虚拟线程，可能会导致一些性能问题，具体取决于文件大小和数量
    public static HashMap<String, String> getHashcode(String[] files) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
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
                    log.error("Failed to compute hashcode for file: {}\n{}", entry.getKey(), ExceptionHandler.getExceptionMessage(e));
                }
            }

            return results;
        }
    }

    //算法实现：获取图片类型
    public static String getImageType(File file) throws IOException {
        ImageHandle imageHandle = getImageHandle(file);
        String type = getImageType(imageHandle.imageReader());
        imageHandle.close();
        return type;
    }

    //算法实现：获取图片类型
    public static String getImageType(ImageReader imageReader) throws IOException {
        return imageReader.getFormatName();
    }

    // 算法实现：获取位深度
    public static int getBitDepth(String ImagePath) throws IOException {
        BufferedImage image = getImage(ImagePath);
        int bitDepth = getBitDepth(image);
        image.flush();
        return bitDepth;
    }

    //算法实现：获取位深度
    public static int getBitDepth(BufferedImage image) {
        if (image == null) throw new IllegalArgumentException("Image is null or unsupported format");
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
        int Width, Height, X, Y;
        Dimension PictureSize = getImageResolution(new File(path));
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
        File pictureOrDir = new File(path);
        File parentDir = pictureOrDir.isFile() ? pictureOrDir.getParentFile() : pictureOrDir;
        for (File file : Objects.requireNonNull(parentDir.listFiles())) {
            if (GetImageInformation.isImageFile(file)) arrayList.add(file.getPath());
        }
        return arrayList;
    }
}
