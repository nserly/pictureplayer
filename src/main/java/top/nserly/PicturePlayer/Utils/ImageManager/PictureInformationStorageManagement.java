package top.nserly.PicturePlayer.Utils.ImageManager;

import lombok.extern.slf4j.Slf4j;
import top.nserly.PicturePlayer.Loading.Init;
import top.nserly.PicturePlayer.Utils.ImageManager.Info.GetImageInformation;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;
import top.nserly.SoftwareCollections_API.String.RandomString;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public record PictureInformationStorageManagement(TreeMap<String, ArrayList<String>> treeMap) implements Serializable {
    //图片缓存存储位置
    private static final String saveDir = "cache/PictureCache/";
    //保存图片缓存类型
    private static final String saveType = "png";
    //保存图片缓存后缀
    private static final String FileSuffix = ".png";


    //初始化
    public PictureInformationStorageManagement() {
        this(new TreeMap<>());
    }

    //获取特定图片缓存路径
    public String getCachedPicturePath(String OriginalPicturePath) {
        return getCachedPicturePath(OriginalPicturePath, GetImageInformation.getHashcode(new File(OriginalPicturePath)));
    }

    // 获取特定图片缓存路径
    public String getCachedPicturePath(String originalPicturePath, String originalPictureHashCode) {
        File originalPicture = new File(originalPicturePath);

        // 检查原图片是否存在
        if (!originalPicture.exists()) {
            return originalPicturePath;
        }

        // 检查是否支持 Java 图片类型
        if (GetImageInformation.isOriginalJavaSupportedPictureType(originalPicturePath)) {
            return originalPicturePath;
        }

        // 检查缓存树中是否存在该图片
        if (treeMap.containsKey(originalPicturePath)) {
            ArrayList<String> pictureInformation = treeMap.get(originalPicturePath);
            File cachedPicture = new File(pictureInformation.get(1));

            // 检查缓存文件是否存在且哈希码匹配
            if (pictureInformation.get(0).equals(originalPictureHashCode) &&
                    cachedPicture.exists() &&
                    hashcodeEquals(cachedPicture.getPath(), pictureInformation.get(2))) {
                return cachedPicture.getPath();
            }

            // 缓存信息不匹配，移除缓存
            treeMap.remove(originalPicturePath);
        }

        // 生成新的缓存路径并保存图片
        ArrayList<String> pictureInformation = new ArrayList<>();
        pictureInformation.add(originalPictureHashCode);
        File savePath = new File(saveDir + RandomString.getRandomString(10) + FileSuffix);
        pictureInformation.add(savePath.getPath());

        try {
            BufferedImage bufferedImage = GetImageInformation.getImage(originalPicturePath);
            GetImageInformation.writeImage(bufferedImage, savePath.getPath(), saveType);
            pictureInformation.add(GetImageInformation.getHashcode(savePath));
            treeMap.put(originalPicturePath, pictureInformation);
            if (bufferedImage != null)
                bufferedImage.flush();

        } catch (Exception e) {
            // 处理异常情况
            log.error(ExceptionHandler.getExceptionMessage(e));
            return null;
        }

        return savePath.getPath();
    }

    //移除并删除特定图片缓存
    public void removePictureCache(String OriginalPicturePath) {
        if (treeMap == null) return;
        String cachedPicture = treeMap.get(OriginalPicturePath).get(1);
        File cachedPictureFile = new File(cachedPicture);
        if (cachedPictureFile.exists())
            if (!cachedPictureFile.delete())
                throw new RuntimeException("Deleting the image cache failed: " + cachedPicture);

        treeMap.remove(OriginalPicturePath);
    }

    //清除所有的图片缓存
    public void clear() {
        treeMap.clear();
        Init.clearDirectory(new File(saveDir));
    }

    // 清除原图片不存在的图片缓存（返回清除的缓存图片数量）（建议在新进程中调用）
    public void optimize() {
        if (treeMap == null || treeMap.isEmpty()) {
            clear();
            return;
        }

        // 使用 ConcurrentHashMap 来存储需要删除的缓存路径
        ConcurrentHashMap<String, ArrayList<String>> cacheToDelete = new ConcurrentHashMap<>();

        // 创建一个线程池来并行处理文件删除任务
        try (ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            // 遍历缓存树
            for (Map.Entry<String, ArrayList<String>> entry : treeMap.entrySet()) {
                final String originalPicturePath = entry.getKey();
                final ArrayList<String> cache = entry.getValue();
                final File currentProcessingFile = new File(originalPicturePath);
                final File currentProcessingCachedFile = new File(cache.get(1));

                // 提交任务到线程池
                executorService.submit(() -> {
                    if (!currentProcessingFile.exists() ||
                            !currentProcessingCachedFile.exists() ||
                            GetImageInformation.isOriginalJavaSupportedPictureType(originalPicturePath) ||
                            !hashcodeEquals(originalPicturePath, cache.get(0)) ||
                            !hashcodeEquals(currentProcessingCachedFile.getPath(), cache.get(2))) {

                        synchronized (cacheToDelete) {
                            cacheToDelete.put(originalPicturePath, cache);
                        }
                    }
                });
            }
        }

        // 删除需要删除的缓存
        for (Map.Entry<String, ArrayList<String>> entry : cacheToDelete.entrySet()) {
            removePictureCache(entry.getKey());
        }
    }


    //判断文件hashcode值是否与提供的hashcode值相等
    public static boolean hashcodeEquals(String filePath, String hashcode) {
        File file = new File(filePath);
        if (!file.exists()) return false;
        return Objects.equals(GetImageInformation.getHashcode(file), hashcode);
    }
}
