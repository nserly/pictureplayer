package top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.SoftwareChannel;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;
import top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.TCP.Client.TCP_Client;
import top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.TCP.Server.TCP_ServerSocket;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
public class WindowsAppMutex {

    // 常量定义
    @Getter
    private int port;
    private TCP_ServerSocket tcpServerSocket;
    private TCP_Client tcpClient;
    @Getter
    private ReceiveFileAction receiveFileAction;
    @Getter
    private ReceiveSoftwareVisibleDirectiveAction receiveSoftwareVisibleDirectiveAction;

    @Getter
    private boolean isSupportedSoftwareName = true;

    public WindowsAppMutex(int port) {
        this.port = port;
        if (!TCP_ServerSocket.isPortAvailable(port)) {
            log.info("Port {} is already in use.", port);
            tcpClient = new TCP_Client("127.0.0.1", port, TCP_Handle.class);
            try {
                tcpClient.connect();
                if (!tcpClient.isServerSupportedSoftwareName()) {
                    isSupportedSoftwareName = false;
                    log.error("The server does not support the required software name protocol.");
                    throw new RuntimeException("The server does not support the required software name protocol.");
                }
            } catch (IOException | ExecutionException | InterruptedException | TimeoutException e) {
                log.error(ExceptionHandler.getExceptionMessage(e));
            }
        } else {
            TCP_Handle.setWindowsAppMutex(this);
            tcpServerSocket = new TCP_ServerSocket(port, 2, TCP_Handle.class);
            tcpServerSocket.setCheckForClient(event -> {
                String hostAddress = event.getInetAddress().getHostAddress();
                log.info("New client connected: (IP: {},Port: {})", hostAddress, event.getPort());
                return "127.0.0.1".equals(hostAddress) || "::1".equals(hostAddress);
            });
            new Thread(() -> {
                while (true) {
                    tcpServerSocket.checkAndGetClientSockets();
                    try {
                        Thread.sleep(6000); // 每60秒检查一次
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
            try {
                tcpServerSocket.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 判断是否为第一个实例
    public boolean isFirstInstance() {
        // 如果没有客户端实例，则是第一个实例
        // 如果存在客户端实例，则不是第一个实例
        return tcpClient == null;
    }

    // 发送文件路径到已有实例
    public void sendFilePathToExistingInstance(String filePath) {
        if (isFirstInstance()) throw new RuntimeException("This is the first instance, cannot send file path.");
        if (!isSupportedSoftwareName)
            throw new RuntimeException("The server does not support the required software name protocol.");
        try {
            tcpClient.send("{newPicturePath} " + filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //发送软件显示信号到已有实例
    public void sendSoftwareVisibleDirectiveToExistingInstance(boolean visible) {
        if (isFirstInstance())
            throw new RuntimeException("This is the first instance, cannot send software visible directive.");
        if (!isSupportedSoftwareName)
            throw new RuntimeException("The server does not support the required software name protocol.");
        try {
            tcpClient.send("{getSoftwareVisibleDirective} " + visible);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //接收文件路径消息
    public void addGetFilePathFromCreatingInstanceAction(ReceiveFileAction receiveFileAction) {
        if (receiveFileAction == null) throw new IllegalArgumentException("ReceiveFileAction cannot be null");
        this.receiveFileAction = receiveFileAction;
    }

    //接收软件显示信号
    public void addReceiveSoftwareVisibleDirective(ReceiveSoftwareVisibleDirectiveAction receiveSoftwareVisibleDirectiveAction) {
        if (receiveSoftwareVisibleDirectiveAction == null)
            throw new IllegalArgumentException("ReceiveSoftwareVisibleDirectiveAction cannot be null");
        this.receiveSoftwareVisibleDirectiveAction = receiveSoftwareVisibleDirectiveAction;
    }

    //关闭
    public void close() {
        try {
            if (tcpClient != null) tcpClient.close();
            if (tcpServerSocket != null) tcpServerSocket.close();
        } catch (IOException e) {
            log.info("Error closing TCP connections: {}", e.getMessage());
        }
    }


}