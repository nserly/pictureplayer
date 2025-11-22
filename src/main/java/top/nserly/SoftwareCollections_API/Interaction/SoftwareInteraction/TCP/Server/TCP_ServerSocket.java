package top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.TCP.Server;


import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.TCP.Interactions;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * 此类为被实例化的类的线程为依据，进行存储数据
 * <p>
 * 它还可以读取实例化的时候线程的对象
 * </p>
 */
@Slf4j
public class TCP_ServerSocket {
    @Getter
    protected Set<String> BlackList;//黑名单列表
    @Setter
    protected CheckClient CheckForClient;//检查客户端是否正确
    @Getter
    @Setter
    protected int MaxConnect;//服务器最大连接数
    @Getter
    protected ExecutorService ThreadPool;//线程池
    protected Class<? extends Interactions> interactions;
    private WaitForConnectClient waitForConnectClient;//用于管理等待程序
    @Getter
    ArrayList<Socket> ClientSockets;//客户端套接字集合
    private ServerSocket serverSocket;
    @Getter
    private String IPv4;//服务器ipv4地址
    @Getter
    private String IPv6;//服务器ipv6地址
    @Getter
    private int port;//服务器端口

    {
        BlackList = new HashSet<>();
        ClientSockets = new ArrayList<>();
    }

    {
        //获取ipv6地址
        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddress = inetAddresses.nextElement();
                if (inetAddress instanceof Inet6Address && !inetAddress.isLinkLocalAddress()) {
                    IPv6 = inetAddress.getHostAddress();
                }
            }
        }
        //获取ipv4地址
        InetAddress localhost;
        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        IPv4 = localhost.getHostAddress();
    }

    /**
     * @param port         服务器端口
     * @param MaxConnect   服务器最大连接数
     * @param interactions 验证成功后和客户端交互
     */
    public TCP_ServerSocket(int port, int MaxConnect, Class<? extends Interactions> interactions) {
        this.port = port;
        this.MaxConnect = MaxConnect;
        this.interactions = interactions;
    }

    public static boolean isPortAvailable(int port) {
        try (java.nio.channels.ServerSocketChannel channel = java.nio.channels.ServerSocketChannel.open()) {
            channel.socket().setReuseAddress(true);
            channel.socket().bind(new java.net.InetSocketAddress(port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    public static void send(Socket socket, String message) throws IOException {
        if (socket == null || !socket.isConnected()) {
            throw new IllegalArgumentException("Socket is null or not connected");
        }
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        socket.getOutputStream().write(message.getBytes());
        socket.getOutputStream().flush();
    }

    public void changeBlackList(Set<String> BlackIP) {
        BlackList = BlackIP;
    }

    public void changeBlackList(List<String> BlackIP) {
        BlackList = new HashSet<>();
        BlackList.addAll(BlackIP);
    }

    public void changeBlackList(String... BlackIP) {
        BlackList = new HashSet<>();
        BlackList.addAll(Arrays.asList(BlackIP));
    }

    public ArrayList<Socket> checkAndGetClientSockets() {
        checkConnectState();
        return ClientSockets;
    }

    public Set<String> getClientIP() {
        if (ClientSockets.isEmpty()) {
            return null;
        }
        Set<String> cache = new HashSet<>();
        ClientSockets.forEach(e -> cache.add(e.getInetAddress().getHostAddress()));

        return cache;
    }

    public void disconnect(String IP) {
        ArrayList<Socket> arrayList = ClientSockets;
        for (Socket i : arrayList) {
            if (i.getInetAddress().getHostAddress().equals(IP)) {
                try {
                    i.close();
                    arrayList.remove(i);
                } catch (IOException ignored) {

                }
            }
        }
    }

    public void addBlackListByCurrentSocket(Socket client) {
        String IP = client.getInetAddress().getHostAddress();
        disconnect(IP);
        addBlackList(IP);
    }

    public void cleanBlacklist() {
        BlackList.clear();
    }

    public synchronized void checkConnectState() {
        if (ClientSockets == null) return;
        Set<Socket> cache = new HashSet<>(ClientSockets);
        for (Socket i : cache) {
            if (i.isClosed()) {
                try {
                    i.close();
                } catch (IOException ignored) {
                    ClientSockets.remove(i);
                }
            }
            try {
                i.sendUrgentData(1);
            } catch (IOException e) {
                try {
                    i.close();
                } catch (IOException ignored) {

                }
                ClientSockets.remove(i);
            }
        }
    }

    public void close() throws IOException {
        if (serverSocket != null)
            serverSocket.close();
        Thread closeConnectClient = null;
        if (waitForConnectClient != null) {
            closeConnectClient = new Thread(waitForConnectClient::stop);
            closeConnectClient.start();
        }
        if (ThreadPool != null) {
            ThreadPool.close();
        }
        if (closeConnectClient != null)
            try {
                // 让其静默1秒钟 (doge)
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {

            }

    }

    public void removeBlackIP(String BlackIP) {
        BlackList.remove(BlackIP);
    }

    public boolean containsBlack(String BlackIP) {
        return BlackList.contains(BlackIP);
    }

    public void addBlackList(String BlackIP) {
        BlackList.add(BlackIP);
    }

    public void addBlackList(List<String> BlackIP) {
        BlackList.addAll(BlackIP);
    }

    public void addBlackList(String... BlackIP) {
        BlackList.addAll(Arrays.asList(BlackIP));
    }

    public void addBlackList(Set<String> BlackIP) {
        BlackList.addAll(BlackIP);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        waitForConnectClient = new WaitForConnectClient(serverSocket, this);
        new Thread(waitForConnectClient).start();
    }
}

@Slf4j
class WaitForConnectClient implements Runnable {
    private final ServerSocket ServerSocket;
    private final TCP_ServerSocket tcpServerSocket;
    private boolean end;

    public WaitForConnectClient(ServerSocket serverSocket, TCP_ServerSocket tcpServerSocket) {
        this.ServerSocket = serverSocket;
        this.tcpServerSocket = tcpServerSocket;
    }

    public void stop() {
        end = true;
    }

    @Override
    public void run() {
        a:
        while (true) {
            // 等待客户端连接
            if (end) break;
            try {
                Socket socket = ServerSocket.accept();
                log.info("new connection request from {}", socket.getInetAddress().getHostAddress());
                //查看当前连接数是否达到极限
                if (tcpServerSocket.MaxConnect != -1 && tcpServerSocket.ClientSockets.size() > tcpServerSocket.MaxConnect) {
                    socket.close();
                    log.info("connection refused ({}),Caused by:Current Connection counts has been over MaxConnect({})", socket.getInetAddress().getHostAddress(), tcpServerSocket.MaxConnect);
                    continue;
                }
                //查看是否在黑名单列表中
                if ((tcpServerSocket.BlackList != null) && (!tcpServerSocket.BlackList.isEmpty())) {
                    for (String s : tcpServerSocket.BlackList) {
                        if (s.equals(socket.getInetAddress().getHostAddress())) {
                            socket.close();
                            log.info("connection refused ({}),Caused by:Address is in BlackList", socket.getInetAddress().getHostAddress());
                            continue a;
                        }
                    }
                }
                //查看是否通过最后检查开发者设置的验证是否通过，如果通过就进行连接
                if (tcpServerSocket.CheckForClient != null)
                    if (!tcpServerSocket.CheckForClient.Check(socket)) {
                        socket.close();
                        log.info("connection refused ({}),Caused by:Developer's definition", socket.getInetAddress().getHostAddress());
                        continue;
                    }

                //将客户端添加到链接列表中
                ArrayList<Socket> set = tcpServerSocket.ClientSockets;
                if (set == null) set = new ArrayList<>();
                set.add(socket);
                tcpServerSocket.ClientSockets = set;

                if (tcpServerSocket.ThreadPool == null) {
                    //如果线程池为空，则创建一个新的线程池
                    tcpServerSocket.ThreadPool = Executors.newFixedThreadPool(tcpServerSocket.MaxConnect == -1 ? 999 : tcpServerSocket.MaxConnect);
                }
                //创建多线程，用来与用户交互
                tcpServerSocket.ThreadPool.execute(new FutureTask<>(Objects.requireNonNull(Interactions.getInstance(tcpServerSocket.interactions, socket, tcpServerSocket.ClientSockets))));
            } catch (IOException e) {
                if (end) break;
                for (Socket socket : tcpServerSocket.ClientSockets) {
                    if (socket.isClosed()) {
                        tcpServerSocket.ClientSockets.remove(socket);
                        log.info("Closed socket: {}", socket.getInetAddress().getHostAddress());
                    }
                }
            }
        }
    }
}