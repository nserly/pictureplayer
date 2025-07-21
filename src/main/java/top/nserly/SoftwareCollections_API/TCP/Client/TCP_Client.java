package top.nserly.SoftwareCollections_API.TCP.Client;

import lombok.Getter;
import top.nserly.SoftwareCollections_API.Collections.SameThreadCollections;
import top.nserly.SoftwareCollections_API.TCP.Interactions;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TCP_Client {
    private static final String IPv4_REGEX =
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final String IPv6_REGEX =
            "^(([0-9a-fA-F]{1,4}(:[0-9a-fA-F]{1,4})*)|::([0-9a-fA-F]{1,4}(:[0-9a-fA-F]{1,4})*)?)$";
    private static final String DomainName_REGEX =
            "^([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,6}$";

    static SameThreadCollections<Socket> sameThreadCollections;

    static {
        sameThreadCollections = new SameThreadCollections<>();
    }

    // 声明一个Class类型的变量interactions，用于存储Runnable的子类
    private final Class<? extends Callable> interactions;
    Interactions interaction = null;
    @Getter
    private String ServerIP;//服务器IP地址
    @Getter
    private int ServerPort;//服务器端口
    private Thread t;

    public TCP_Client(String ServerIP, int ServerPort, Class<? extends Interactions> interactions) {
        this.interactions = interactions;
        this.ServerIP = ServerIP;
        this.ServerPort = ServerPort;
    }

    public static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 查看ip地址是否配置正确
     *
     * @param IP IP地址
     */
    public static boolean matchIP(String IP) {
        Matcher matcherIPv4 = Pattern.compile(IPv4_REGEX).matcher(IP);
        Matcher matcherIPv6 = Pattern.compile(IPv6_REGEX).matcher(IP);
        return matcherIPv4.matches() ^ matcherIPv6.matches();
    }

    /**
     * 检查域名（Domin Name）是否配置正确
     *
     * @param DomainName 域名
     */
    public static boolean matchDomainName(String DomainName) {
        Matcher matcherDomainName = Pattern.compile(DomainName_REGEX).matcher(DomainName);
        return matcherDomainName.matches() ^ matcherDomainName.matches();
    }

    /**
     * 检查port是否配置正确
     *
     * @param port 端口
     */
    public static boolean matchPort(int port) {
        return (port > 0) && (port <= 65535);
    }

    /**
     * 综合检查是否正确
     *
     * @param IPOrDomainName IP或域名地址
     * @param port           端口
     */
    public static boolean match(String IPOrDomainName, int port) {
        return (matchIP(IPOrDomainName) ^ matchDomainName(IPOrDomainName)) && matchPort(port);
    }

    public void connect() throws IOException {
        String IP = ServerIP;
        int Port = ServerPort;
        sameThreadCollections.Add(new Socket(IP, Port));
        Constructor constructor = null;
        try {
            constructor = interactions.getConstructor(Socket.class);
            interaction = (Interactions) constructor.newInstance(sameThreadCollections.Get());
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        FutureTask futureTask = new FutureTask<>(interaction);
        t = new Thread(futureTask);
        t.start();
    }

    public void send(String message) throws IOException {
        getSocket().getOutputStream().write(message.getBytes());
        getSocket().getOutputStream().flush();
    }

    public void close() throws IOException {
        t.interrupt();
        t = null;
        sameThreadCollections.Get().close();
    }

    public String getServerSupportedSoftwareName() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        if (getSocket().isClosed()) throw new RuntimeException("TCP_Client is not connected.");

        CompletableFuture<String> future = new CompletableFuture<>();

        // 设置回调函数，当收到响应时完成 future
        interaction.setReceviceSoftwareNameInformationAction(future::complete);

        // 发送请求
        send("{getSoftwareName}");

        // 等待 future 完成并获取结果
        return future.get(5, java.util.concurrent.TimeUnit.SECONDS);

    }

    public boolean isServerSupportedSoftwareName() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        return getServerSupportedSoftwareName().equals(System.getProperty("SoftwareName"));
    }

    public Socket getSocket() {
        return sameThreadCollections.Get();
    }
}
