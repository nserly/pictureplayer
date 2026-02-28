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

package top.nserly.SoftwareCollections_API.Queue;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 支持设置最大线程数的任务队列
 * 任务按添加顺序执行，并发度由最大线程数控制
 */
@Slf4j
public class ThreadPoolTaskQueue {
    private final ThreadPoolExecutor threadPool;
    /**
     * -- GETTER --
     *  获取最大线程数
     */
    @Getter
    private final int maxThreadCount;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    /**
     * 构造任务队列
     * @param corePoolSize 核心线程数
     * @param maxThreadCount 最大线程数
     */
    public ThreadPoolTaskQueue(int corePoolSize, int maxThreadCount) {
        if (corePoolSize <= 0 || maxThreadCount <= 0 || corePoolSize > maxThreadCount) {
            throw new IllegalArgumentException("The thread number parameter is not legitimate");
        }
        this.maxThreadCount = maxThreadCount;

        // 使用有界队列，避免内存溢出（容量可根据实际需求调整）
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(1024);

        // 拒绝策略：当队列满时阻塞提交者，直到有空间
        RejectedExecutionHandler handler = (r, executor) -> {
            try {
                // 等待队列有空间（最多等1秒，避免无限阻塞）
                if (!executor.getQueue().offer(r, 1, TimeUnit.SECONDS)) {
                    throw new RejectedExecutionException("The task queue is full, and adding tasks fails");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Task addition is interrupted", e);
            }
        };

        this.threadPool = new ThreadPoolExecutor(
                corePoolSize,
                maxThreadCount,
                60,
                TimeUnit.SECONDS,
                workQueue,
                Executors.defaultThreadFactory(),
                handler
        );

        // 允许核心线程超时关闭，节省资源
        this.threadPool.allowCoreThreadTimeOut(true);
    }

    /**
     * 添加任务并返回Future，用于取消任务
     * @param task 待执行的任务
     * @return 任务的Future对象，可用于取消或获取结果
     */
    public Future<?> addTask(Runnable task) {
        if (!isRunning.get() || task == null) {
            return null;
        }
        try {
            // 使用submit而非execute，获取Future对象
            return threadPool.submit(task);
        } catch (RejectedExecutionException e) {
            log.error("Adding a task failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 停止任务队列
     * @param waitForCompletion 是否等待所有任务完成
     */
    public void stop(boolean waitForCompletion) {
        if (isRunning.compareAndSet(true, false)) {
            if (waitForCompletion) {
                threadPool.shutdown();
                try {
                    if (!threadPool.awaitTermination(1, TimeUnit.MINUTES)) {
                        threadPool.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    threadPool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            } else {
                threadPool.shutdownNow();
            }
        }
    }

    /**
     * 获取当前活跃线程数
     */
    public int getActiveThreadCount() {
        return threadPool.getActiveCount();
    }

    /**
     * 获取等待中的任务数
     */
    public int getPendingTaskCount() {
        return threadPool.getQueue().size();
    }

    /**
     * 检查队列是否正在运行
     */
    public boolean isRunning() {
        return isRunning.get();
    }
}
