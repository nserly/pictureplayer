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

package top.nserly.SoftwareCollections_API.DownloadFile;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Collections.TwoWayMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class FileDownloaderControl {
    // 存储活跃下载任务（任务ID -> 下载器）
    private final TwoWayMap<String, FileDownloader> activeDownloads = new TwoWayMap<>();
    // 存储已完成的下载任务（任务ID -> 保存路径）
    private final Map<String, String> completedDownloads = new ConcurrentHashMap<>();
    // 下载任务队列
    private final Queue<DownloadTask> downloadQueue = new LinkedList<>();
    // 线程池（核心线程数=最大并发数，避免动态创建销毁）
    private final ExecutorService downloadExecutor;
    // 最大并发下载数
    @Getter
    private final int maxConcurrentDownloads;
    // 队列操作锁
    private final ReentrantLock queueLock = new ReentrantLock();
    // 队列条件变量（用于等待/通知任务调度）
    private final Condition queueCondition;
    // 全局速度限制（字节/秒，-1表示无限制）
    private long globalSpeedLimitBytesPerSecond = -1;
    // 控制器运行状态
    private volatile boolean isRunning = true;

    public FileDownloaderControl(int maxConcurrentDownloads) {
        if (maxConcurrentDownloads < 1) {
            throw new IllegalArgumentException("The maximum number of concurrent events cannot be less than 1");
        }
        this.maxConcurrentDownloads = maxConcurrentDownloads;
        this.downloadExecutor = new ThreadPoolExecutor(
                maxConcurrentDownloads,
                maxConcurrentDownloads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                r -> new Thread(r, "download-worker-" + System.currentTimeMillis()),
                new ThreadPoolExecutor.DiscardPolicy() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                        log.warn("The download task is rejected and the current thread pool is full");
                        if (r instanceof DownloadRunnable runnable) {
                            if (runnable.task.errorHandler != null) {
                                runnable.task.errorHandler.handler(
                                        new IOException("There are too many download tasks to handle"), null);
                            }
                        }
                    }
                }
        );
        this.queueCondition = queueLock.newCondition();
        startQueueProcessor();
    }

    /**
     * 删除下载任务（支持删除队列中或活跃的任务）
     *
     * @param taskId 任务ID
     * @return 是否删除成功
     */
    public boolean removeTask(String taskId) {
        if (taskId == null) return false;

        queueLock.lock();
        try {
            // 1. 处理活跃任务
            FileDownloader activeDownloader = activeDownloads.getValue(taskId);
            if (activeDownloader != null) {
                activeDownloader.stopDownload();
                activeDownloads.removeByKey(taskId);
                return true;
            }

            // 2. 处理队列中的任务
            Iterator<DownloadTask> queueIterator = downloadQueue.iterator();
            while (queueIterator.hasNext()) {
                DownloadTask task = queueIterator.next();
                if (task.taskId.equals(taskId)) {
                    // 同步删除子任务
                    if (!task.chunkTaskIds.isEmpty()) {
                        for (String chunkId : task.chunkTaskIds) {
                            removeTask(chunkId);
                        }
                    }
                    queueIterator.remove();
                    return true;
                }
            }

            // 3. 处理分片子任务
            for (DownloadTask task : downloadQueue) {
                if (task.chunkTaskIds.contains(taskId)) {
                    queueIterator = downloadQueue.iterator();
                    while (queueIterator.hasNext()) {
                        DownloadTask subTask = queueIterator.next();
                        if (subTask.taskId.equals(taskId)) {
                            queueIterator.remove();
                            task.chunkTaskIds.remove(taskId);
                            return true;
                        }
                    }
                }
            }

            return false;
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * 阻塞当前线程，直到指定ID的下载任务完成
     */
    public void waitTillDownload(String taskID) throws InterruptedException {
        FileDownloader downloader;
        queueLock.lock();
        try {
            do {
                downloader = activeDownloads.getValue(taskID);
                if (downloader == null) {
                    // 使用 Condition 进行等待
                    if (queueCondition.await(100, TimeUnit.MILLISECONDS))
                        log.warn("Stopped waiting for the download thread to be terminated by other threads");

                }
            } while (downloader == null && isRunning);
        } finally {
            queueLock.unlock();
        }


        if (downloader == null) {
            throw new IllegalArgumentException("The task ID does not exist: " + taskID);
        }

        if (downloader.isCompleted() || downloader.isStopped()) return;

        downloader.getDownloadThread().join();

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Waiting to be interrupted");
        }
    }

    /**
     * 向下载队列添加任务
     *
     * @param url          下载链接
     * @param saveDir      保存目录
     * @param threadCount  线程数（1=单线程，>1=分片下载）
     * @param errorHandler 错误处理器
     * @return 唯一任务ID（用于跟踪和管理）
     */
    public String addDownloadTask(String url, String saveDir, int threadCount, DownloadErrorHandler errorHandler) {
        if (url == null || saveDir == null) {
            throw new IllegalArgumentException("URLs and save directories cannot be empty");
        }
        String taskId = generateTaskId();
        DownloadTask task = new DownloadTask();
        task.url = url;
        task.saveDir = saveDir;
        task.threadCount = Math.max(1, threadCount);
        task.errorHandler = errorHandler;
        task.taskId = taskId;
        task.masterTaskId = null;

        queueLock.lock();
        try {
            downloadQueue.add(task);
            queueCondition.signal();
        } finally {
            queueLock.unlock();
        }
        return taskId;
    }

    /**
     * 阻塞当前线程，直到所有下载任务完成
     */
    public void waitTillDownload() throws InterruptedException {
        queueLock.lock();
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (activeDownloads.isEmpty() && downloadQueue.isEmpty()) {
                    break;
                }
                // 使用 Condition 进行等待
                if (queueCondition.await(100, TimeUnit.MILLISECONDS)) {
                    log.warn("Stopped waiting for the download thread to be terminated by other threads");
                }
            }

            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Waiting to be interrupted");
            }
        } finally {
            queueLock.unlock();
        }
    }

    // 提供获取已完成任务的公共方法
    public Map<String, String> getCompletedDownloads() {
        // 返回不可修改的视图，避免外部直接修改
        return Collections.unmodifiableMap(completedDownloads);
    }

    /**
     * 启动队列处理器线程
     */
    private void startQueueProcessor() {
        Thread processor = new Thread(this::processQueue, "download-queue-processor");
        processor.setDaemon(true);
        processor.start();
    }

    /**
     * 处理任务队列的主循环
     */
    private void processQueue() {
        while (isRunning) {
            try {
                DownloadTask task = takeTaskFromQueue();
                if (task != null) {
                    submitTaskToExecutor(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("The queue processor is interrupted");
                break;
            } catch (Exception e) {
                log.error("Queue processing error", e);
            }
        }
    }

    /**
     * 从队列中获取任务
     */
    private DownloadTask takeTaskFromQueue() throws InterruptedException {
        queueLock.lock();
        try {
            while (downloadQueue.isEmpty() && isRunning) {
                queueCondition.await();
            }
            return isRunning ? downloadQueue.poll() : null;
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * 将任务提交到线程池执行
     */
    private void submitTaskToExecutor(DownloadTask task) {
        if (task.threadCount <= 1) {
            // 单线程下载
            FileDownloader downloader = new FileDownloader(
                    task.url, task.saveDir, task.errorHandler);
            configureDownloader(downloader);
            activeDownloads.put(task.taskId, downloader);
            downloadExecutor.submit(new DownloadRunnable(task, downloader));
        } else {
            // 多线程分片下载
            try {
                startMultiThreadDownload(task);
            } catch (IOException e) {
                log.error("Failed to start the shard download", e);
                if (task.errorHandler != null) {
                    task.errorHandler.handler(e, null);
                }
            }
        }
    }

    /**
     * 多线程分片下载实现
     */
    private void startMultiThreadDownload(DownloadTask task) throws IOException {
        long fileSize = getFileSize(task.url);
        if (fileSize <= 0) {
            log.warn("Unable to get file size, downgraded to single-threaded download");
            submitSingleThreadTask(task);
            return;
        }

        // 计算分片数（最大不超过10，最小分片1MB）
        int threadCount = Math.min(task.threadCount, 10);
        threadCount = Math.min(threadCount, (int) Math.ceil(fileSize / (1024.0 * 1024)));
        threadCount = Math.max(threadCount, 1);

        long chunkSize = fileSize / threadCount;
        String baseFileName = getFileNameFromUrl(task.url);
        String finalFileName = generateUniqueFileName(baseFileName, task.saveDir);
        task.finalFileName = finalFileName; // 保存最终文件名到主任务

        String tempDir = normalizeDirectoryPath(task.saveDir) + ".chunk_" + finalFileName + "_" + System.currentTimeMillis() + "/";
        File tempDirFile = new File(tempDir);
        if (!tempDirFile.mkdirs() && !tempDirFile.exists()) {
            throw new IOException("Unable to create a temporary shard directory: " + tempDir);
        }

        // 将主任务添加到活跃列表（用于跟踪）
        activeDownloads.put(task.taskId, null); // 用null占位，标识主任务

        CountDownLatch chunkLatch = new CountDownLatch(threadCount);
        DownloadErrorHandler chunkErrorHandler = (e, d) -> {
            log.error("Shard download failed", e);
            // 停止所有相关分片下载
            task.chunkTaskIds.forEach(chunkId -> {
                FileDownloader chunkDownloader = activeDownloads.getValue(chunkId);
                if (chunkDownloader != null) {
                    chunkDownloader.stopDownload();
                }
            });
            // 通知主任务错误
            if (task.errorHandler != null) {
                task.errorHandler.handler(new IOException("Shard download failed", e), null);
            }
            // 强制计数器归零
            while (chunkLatch.getCount() > 0) {
                chunkLatch.countDown();
            }
            deleteDirectory(new File(tempDir));
        };

        // 创建分片下载任务
        for (int i = 0; i < threadCount; i++) {
            long start = i * chunkSize;
            long end = (i == threadCount - 1) ? fileSize - 1 : (i + 1) * chunkSize - 1;

            String chunkTaskId = generateTaskId();
            task.chunkTaskIds.add(chunkTaskId);

            // 自定义分片下载器（设置Range头）
            FileDownloader chunkDownloader = new FileDownloader(
                    task.url, tempDir, chunkErrorHandler) {
                @Override
                public HttpURLConnection createConnection(boolean isResume) throws IOException {
                    HttpURLConnection connection = super.createConnection(isResume);
                    connection.setRequestProperty("Range", "bytes=" + start + "-" + end);
                    return connection;
                }
            };
            // 配置分片文件名
            chunkDownloader.setFinalFileName("chunk_" + i + "_" + baseFileName);
            configureDownloader(chunkDownloader);

            activeDownloads.put(chunkTaskId, chunkDownloader);
            downloadExecutor.submit(new DownloadRunnable(
                    new DownloadTask() {{
                        url = task.url;
                        saveDir = tempDir;
                        threadCount = 1;
                        errorHandler = chunkErrorHandler;
                        taskId = chunkTaskId;
                        masterTaskId = task.taskId;
                    }}, chunkDownloader) {
                @Override
                public void run() {
                    super.run();
                    chunkLatch.countDown();
                }
            });
        }

        // 启动合并线程
        startMergeThread(tempDir, task.saveDir, finalFileName, chunkLatch, task);
    }

    /**
     * 启动分片合并线程
     */
    private void startMergeThread(String tempDir, String saveDir, String finalFileName,
                                  CountDownLatch latch, DownloadTask mainTask) {
        Thread mergeThread = new Thread(() -> {
            try {
                latch.await();
                if (isAnyChunkFailed(tempDir)) {
                    log.warn("Some chunks failed, aborting merge");
                    deleteDirectory(new File(tempDir));
                    return;
                }
                // 合并分片文件
                mergeChunks(tempDir, normalizeDirectoryPath(saveDir) + finalFileName);
                log.info("File merged successfully: {}", finalFileName);
            } catch (InterruptedException e) {
                log.info("Merge thread interrupted");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Failed to merge chunks", e);
                if (mainTask.errorHandler != null) {
                    mainTask.errorHandler.handler(new IOException("Failed to merge chunks", e), null);
                }
            } finally {
                // 清理临时目录和主任务
                deleteDirectory(new File(tempDir));
                queueLock.lock();
                try {
                    activeDownloads.removeByKey(mainTask.taskId); // 移除主任务
                    completedDownloads.put(mainTask.taskId, finalFileName);
                    queueCondition.signal();
                } finally {
                    queueLock.unlock();
                }
            }
        }, "merge-thread-" + mainTask.taskId);
        mergeThread.setDaemon(true);
        mergeThread.start();
    }

    /**
     * 合并分片文件
     */
    private void mergeChunks(String tempDir, String targetPath) throws IOException {
        File targetFile = new File(targetPath);
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            File[] chunkFiles = new File(tempDir).listFiles((dir, name) -> name.startsWith("chunk_"));
            if (chunkFiles == null || chunkFiles.length == 0) {
                throw new IOException("No chunk files found for merging");
            }

            // 按分片索引排序
            Arrays.sort(chunkFiles, (f1, f2) -> {
                int idx1 = Integer.parseInt(f1.getName().split("_")[1]);
                int idx2 = Integer.parseInt(f2.getName().split("_")[1]);
                return Integer.compare(idx1, idx2);
            });

            // 合并所有分片
            byte[] buffer = new byte[FileDownloader.getBUFFER_SIZE()];
            for (File chunk : chunkFiles) {
                try (FileInputStream fis = new FileInputStream(chunk)) {
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                if (!chunk.delete()) log.warn("{} cannot delete", chunk.getPath());
            }
        }
    }

    /**
     * 检查是否有分片下载失败
     */
    private boolean isAnyChunkFailed(String tempDir) {
        File[] chunkFiles = new File(tempDir).listFiles((dir, name) -> name.startsWith("chunk_"));
        if (chunkFiles == null) return true;
        for (File chunk : chunkFiles) {
            if (chunk.length() == 0) return true;
        }
        return false;
    }

    /**
     * 生成唯一文件名（避免重复）
     */
    private String generateUniqueFileName(String baseName, String saveDir) {
        String name = baseName;
        int counter = 1;
        File targetFile = new File(normalizeDirectoryPath(saveDir) + name);

        while (targetFile.exists()) {
            int dotIndex = baseName.lastIndexOf('.');
            if (dotIndex > 0) {
                name = baseName.substring(0, dotIndex) + "(" + counter + ")" + baseName.substring(dotIndex);
            } else {
                name = baseName + "(" + counter + ")";
            }
            counter++;
            targetFile = new File(normalizeDirectoryPath(saveDir) + name);
        }
        return name;
    }

    /**
     * 生成唯一任务ID
     */
    private String generateTaskId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 提交单线程任务（分片下载降级用）
     */
    private void submitSingleThreadTask(DownloadTask task) {
        FileDownloader downloader = new FileDownloader(task.url, task.saveDir, task.errorHandler);
        configureDownloader(downloader);
        activeDownloads.put(task.taskId, downloader);
        downloadExecutor.submit(new DownloadRunnable(task, downloader));
    }

    /**
     * 配置下载器（设置全局速度限制等）
     */
    private void configureDownloader(FileDownloader downloader) {
        if (globalSpeedLimitBytesPerSecond > 0) {
            downloader.setMaxSpeedBytesPerSecond(globalSpeedLimitBytesPerSecond);
        }
    }

    /**
     * 删除目录
     */
    private boolean deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return true;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        log.warn("Files cannot be deleted: {}", file.getAbsolutePath());
                    }
                }
            }
        }
        return dir.delete();
    }

    /**
     * 获取文件大小
     */
    private long getFileSize(String url) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL urlObj = URL.of(URI.create(url), null);
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK ||
                    responseCode == HttpURLConnection.HTTP_PARTIAL) {
                return connection.getContentLengthLong();
            }
            throw new IOException("HEAD request fails, response code: " + responseCode);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 从URL获取文件名
     */
    private String getFileNameFromUrl(String url) {
        try {
            URL urlObj = URL.of(URI.create(url), null);
            String path = urlObj.getPath();
            int lastSlash = path.lastIndexOf('/');
            return lastSlash == -1 ? path : path.substring(lastSlash + 1);
        } catch (Exception e) {
            log.error("Failed to resolve file name from URL", e);
            return "unknown_file_" + System.currentTimeMillis();
        }
    }

    /**
     * 标准化目录路径（确保以/结尾）
     */
    private String normalizeDirectoryPath(String path) {
        if (path == null || path.isEmpty()) {
            return "./";
        }
        path = path.replace("\\", "/");
        return path.endsWith("/") ? path : path + "/";
    }

    /**
     * 设置全局下载速度限制（字节/秒）
     */
    public void setGlobalSpeedLimit(long bytesPerSecond) {
        this.globalSpeedLimitBytesPerSecond = bytesPerSecond;
        activeDownloads.values().forEach(this::configureDownloader);
    }

    /**
     * 暂停所有下载任务
     */
    public void pauseAll() {
        activeDownloads.values().forEach(FileDownloader::stopDownload);
    }

    // 公共控制方法

    /**
     * 恢复所有未完成的下载任务
     */
    public void resumeAll() {
        activeDownloads.values().forEach(downloader -> {
            if (downloader != null && !downloader.isCompleted() && !downloader.isStopped()) {
                downloader.startDownloadInNewThread();
            }
        });
    }

    /**
     * 停止所有下载任务并关闭控制器
     */
    public void stopAll() {
        isRunning = false;
        activeDownloads.values().forEach(downloader -> {
            if (downloader != null) downloader.stopDownload();
        });
        downloadExecutor.shutdownNow();
        try {
            if (!downloadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("The thread pool fails to close gracefully");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        queueLock.lock();
        try {
            downloadQueue.clear();
            queueCondition.signal();
        } finally {
            queueLock.unlock();
        }
        activeDownloads.clear();
    }

    /**
     * 获取当前活跃的下载任务
     */
    public List<FileDownloader> getActiveDownloads() {
        return new ArrayList<>(activeDownloads.values());
    }

    /**
     * 获取队列中等待的任务数量
     */
    public int getQueueSize() {
        queueLock.lock();
        try {
            return downloadQueue.size();
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * 根据任务ID获取下载器
     */
    public FileDownloader getDownloaderByTaskId(String taskId) {
        return activeDownloads.getValue(taskId);
    }

    /**
     * 移除队列中的任务（未开始的任务）
     */
    public boolean removeQueuedTask(String taskId) {
        queueLock.lock();
        try {
            return downloadQueue.removeIf(task -> task.taskId.equals(taskId));
        } finally {
            queueLock.unlock();
        }
    }

    // 下载任务封装类
    public static class DownloadTask {
        @Getter
        String url;
        @Getter
        String saveDir;
        @Getter
        int threadCount;
        DownloadErrorHandler errorHandler;
        @Getter
        String taskId;
        List<String> chunkTaskIds = new ArrayList<>();
        @Getter
        String masterTaskId; // 子任务关联的主任务ID（主任务为null）
        @Getter
        String finalFileName; // 最终文件名（用于分片下载合并）
    }

    /**
     * 下载任务执行器
     */
    private class DownloadRunnable implements Runnable {
        private final DownloadTask task;
        private final FileDownloader downloader;

        public DownloadRunnable(DownloadTask task, FileDownloader downloader) {
            this.task = task;
            this.downloader = downloader;
        }

        @Override
        public void run() {
            try {
                downloader.startDownload();
                // 下载完成且未被停止，记录到已完成任务
                if (downloader.isCompleted() && !downloader.isStopped()) {
                    // 添加到已完成任务
                    completedDownloads.put(task.taskId, downloader.getFinalPath());
                    log.info("Task completion and recording: {}", task.taskId);
                }
            } finally {
                queueLock.lock();
                try {
                    activeDownloads.removeByKey(task.taskId);

                    // 从主任务的子列表中移除
                    if (task.masterTaskId != null) {
                        for (DownloadTask masterTask : downloadQueue) {
                            if (masterTask.taskId.equals(task.masterTaskId)) {
                                masterTask.chunkTaskIds.remove(task.taskId);
                                break;
                            }
                        }
                    }

                    queueCondition.signal();
                } finally {
                    queueLock.unlock();
                }
            }
        }
    }
}