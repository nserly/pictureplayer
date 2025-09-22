package top.nserly.SoftwareCollections_API.Thread;

public class ThreadControl {
    private ThreadControl() {

    }

    public static void waitThreadsComplete(Thread... threads) {
        if (threads != null)
            for (Thread thread : threads) {
                if (thread != null) {
                    try {
                        thread.join();
                    } catch (InterruptedException ignored) {

                    }
                }
            }
    }

}
