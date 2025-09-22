package top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.SoftwareChannel;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;
import top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.TCP.Client.TCP_Client;
import top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.TCP.Server.TCP_ServerSocket;
import top.nserly.SoftwareCollections_API.Thread.ThreadControl;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WindowsAppMutex {

    @Getter
    private final int port;
    private final Thread Init_Thread;
    private TCP_ServerSocket tcpServerSocket;
    private TCP_Client tcpClient;
    @Getter
    private boolean isFirstInstance;
    @Getter
    private HandleSoftwareRequestAction handleSoftwareRequestAction;
    @Getter
    private boolean isSupportedSoftwareName = true;
    private ScheduledExecutorService executor;

    public WindowsAppMutex(int port) {
        this.port = port;
        isFirstInstance = TCP_ServerSocket.isPortAvailable(port);
        Init_Thread = new Thread(() -> {
            if (!isFirstInstance) {
                log.info("Port {} is already in use.", port);
                tcpClient = new TCP_Client("127.0.0.1", port, TCP_Handle.class);
                try {
                    tcpClient.connect();
                    if (!tcpClient.isServerSupportedSoftwareName()) {
                        isSupportedSoftwareName = false;
                        log.error("The server does not support the required software name protocol.");
                        throw new RuntimeException("The server does not support the required software name protocol.");
                    }
                } catch (Exception e) {
                    log.warn(ExceptionHandler.getExceptionMessage(e));
                }
            } else {
                TCP_Handle.setWindowsAppMutex(this);
                tcpServerSocket = new TCP_ServerSocket(port, -1, TCP_Handle.class);
                tcpServerSocket.setCheckForClient(event -> {
                    String hostAddress = event.getInetAddress().getHostAddress();
                    log.info("New client connected: (IP: {},Port: {})", hostAddress, event.getPort());
                    return "127.0.0.1".equals(hostAddress) || "::1".equals(hostAddress);
                });

                executor = Executors.newSingleThreadScheduledExecutor();
                executor.scheduleAtFixedRate(() -> tcpServerSocket.checkConnectState(), 0, 30, TimeUnit.SECONDS);

                try {
                    tcpServerSocket.start();
                } catch (IOException e) {
                    log.warn("Server start failed: {}", e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        });

        Init_Thread.start();
    }

    public void sendFilePathToExistingInstance(String filePath) {
        if (isFirstInstance())
            throw new RuntimeException("This is the first instance, cannot send file path.");

        ThreadControl.waitThreadsComplete(Init_Thread);
        if (!isSupportedSoftwareName)
            throw new RuntimeException("The server does not support the required software name protocol.");
        try {
            tcpClient.send("{newPicturePath} " + filePath);
        } catch (IOException e) {
            log.warn("Send file path failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void sendSoftwareVisibleDirectiveToExistingInstance(boolean visible) {
        if (isFirstInstance())
            throw new RuntimeException("This is the first instance, cannot send software visible directive.");

        ThreadControl.waitThreadsComplete(Init_Thread);
        if (!isSupportedSoftwareName)
            throw new RuntimeException("The server does not support the required software name protocol.");
        try {
            tcpClient.send("{getSoftwareVisibleDirective} " + visible);
        } catch (IOException e) {
            log.warn("Send visible directive failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void addHandleSoftwareRequestAction(HandleSoftwareRequestAction handleSoftwareRequestAction) {
        if (handleSoftwareRequestAction == null)
            throw new IllegalArgumentException("ReceiveSoftwareVisibleDirectiveAction cannot be null");
        this.handleSoftwareRequestAction = handleSoftwareRequestAction;
    }

    public void close() {
        ThreadControl.waitThreadsComplete(Init_Thread);
        try {
            if (tcpClient != null) tcpClient.close();
        } catch (IOException e) {
            log.warn("Error closing TCP client: {}", e.getMessage());
        }
        try {
            if (tcpServerSocket != null) tcpServerSocket.close();
        } catch (IOException e) {
            log.warn("Error closing TCP server: {}", e.getMessage());
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }
}
