package top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.TCP;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.String.GetString;

import java.net.Socket;
import java.util.concurrent.Callable;

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

    public Interactions(Socket socket) {
        this.socket = socket;
    }

    @Override
    public final Integer call() throws Exception {
        while (true) {
            if (socket.isClosed() || tryCount >= 3) {
                log.info("Socket is closed, interaction thread is terminating.(IP: {},Port: {})", socket.getInetAddress().getHostAddress(), socket.getPort());
                return 1; // 返回1表示线程结束
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
                    Thread.sleep(500 * (12 - elapsedTime) * (tryCount + 1)); // 如果间隔小于10毫秒，则休眠一段时间
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
