package top.nserly.SoftwareCollections_API.Thread;

public class ThreadControl {
    private ThreadControl() {

    }

    public static boolean waitThreadsComplete(Thread... threads) {
        return waitThreadsComplete(0, threads);
    }


    /**
     * Waits for the specified threads to complete their execution within a given waiting time.
     *
     * @param waitingTime The maximum time in milliseconds to wait for the threads to complete. A value of 0 indicates no timeout.
     * @param threads     An array of Thread objects to wait for.
     * @return true if all provided threads have completed within the specified waiting time, false otherwise or if any thread is null and not already finished.
     */
    public static boolean waitThreadsComplete(long waitingTime, Thread... threads) {
        long startTime = System.currentTimeMillis();
        if (threads == null)
            return true; // 无线程需要等待，视为完成

        for (Thread thread : threads) {
            if (thread == null)
                // 若不允许传入null线程，可返回false；否则忽略
                // return false;
                continue;

            // 计算剩余等待时间（当前已消耗时间 = 现在 - 开始时间）
            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = waitingTime - elapsed;
            // 若剩余时间 <=0 且不是无限等待（waitingTime=0视为无限等待），则直接判断线程是否存活
            if (remaining <= 0 && waitingTime != 0)
                return !thread.isAlive();

            try {
                // 若waitingTime=0，remaining可能为负，此时join(0)表示无限等待
                thread.join(Math.max(0, remaining));
            } catch (InterruptedException e) {
                // 恢复中断状态，让上层感知
                Thread.currentThread().interrupt();
                return false;
            }
            // 若线程仍存活，说明未在指定时间内完成
            if (thread.isAlive())
                return false;

        }
        return true;
    }

}
