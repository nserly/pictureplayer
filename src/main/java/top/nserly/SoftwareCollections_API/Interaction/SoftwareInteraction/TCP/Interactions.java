package top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.TCP;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;
import top.nserly.SoftwareCollections_API.String.GetString;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * Callable<Integer>中Integer为返回值，如果执行成功返回0，否则返回非0整数
 */
@Slf4j
public abstract class Interactions implements Callable<Integer> {
    public final Socket socket;
    public String sendMessage;
    @Setter
    @Getter
    public ReceviceSoftwareNameInformationAction receviceSoftwareNameInformationAction;
    private long ms;
    private int tryCount = 0;
    public ArrayList<Socket> ClientSockets = null;

    public Interactions(Socket socket, ArrayList<Socket> ClientSockets) {
        this.socket = socket;
        this.ClientSockets = ClientSockets;
    }

    /**
     * 通过反射获取Interactions（或其子类）的实例
     *
     * @param interactions  目标类的Class对象（必须是Interactions或其子类）
     * @param socket        构造方法参数1
     * @param ClientSockets 构造方法参数2
     * @return 反射创建的实例，若失败返回null
     */
    public static Interactions getInstance(Class<? extends Interactions> interactions,
                                           Socket socket,
                                           ArrayList<Socket> ClientSockets) {
        try {
            Constructor<? extends Interactions> constructor =
                    interactions.getDeclaredConstructor(Socket.class, ArrayList.class);

            constructor.setAccessible(true);

            return constructor.newInstance(socket, ClientSockets);

        } catch (Exception e) {
            log.error(ExceptionHandler.getExceptionMessage(e));
        }
        return null;
    }

    @Override
    public final Integer call() throws Exception {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> scheduledFuture = null;

        try {
            while (true) {
                if (socket.isClosed() || tryCount >= 3) {
                    log.info("Socket is closed, interaction thread is terminating. (IP: {}, Port: {})", socket.getInetAddress().getHostAddress(), socket.getPort());
                    ClientSockets.remove(socket);
                    throw new IOException("Socket is closed, interaction thread is terminating.");
                }

                sendMessage = GetString.getString(socket.getInputStream());
                if (sendMessage == null || sendMessage.isBlank()) {
                    tryCount++;
                    if (ms == 0) {
                        ms = System.currentTimeMillis();
                        continue;
                    }
                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - ms;

                    if (elapsedTime <= 10) {
                        long sleepTime = 500 * (12 - elapsedTime) * (tryCount + 1);
                        if (scheduledFuture != null && !scheduledFuture.isDone()) {
                            scheduledFuture.cancel(false);
                        }
                        scheduledFuture = scheduler.schedule(() -> {
                            // 继续执行
                        }, sleepTime, TimeUnit.MILLISECONDS);
                        ms = System.currentTimeMillis();
                        continue;
                    }
                    ms = System.currentTimeMillis();
                    continue;
                }

                tryCount = 0;
                int internalResult = internalInformationProcessing();
                if (internalResult == -1) {
                    messageCall();
                }
            }
        } finally {
            if (scheduledFuture != null && !scheduledFuture.isDone()) {
                scheduledFuture.cancel(false);
            }
            scheduler.shutdown();
        }
    }

    private int internalInformationProcessing() throws Exception {
        if (sendMessage.equals("{getSoftwareName}")) {
            socket.getOutputStream().write(("{SoftwareName} " + System.getProperty("SoftwareName")).getBytes());
            socket.getOutputStream().flush();
            return 0;
        } else if (sendMessage.startsWith("{SoftwareName} ")) {
            if (receviceSoftwareNameInformationAction != null) {
                String softwareName = sendMessage.substring("{SoftwareName} ".length());
                receviceSoftwareNameInformationAction.receiveSoftwareName(softwareName.trim());
            }
            return 0;
        }
        return -1; // 返回-1表示没有处理完毕，继续调用messageCall()
    }

    abstract public int messageCall() throws Exception;
}
