package top.nserly.SoftwareCollections_API.TCP.Server;

import java.net.Socket;

public interface CheckClient {
    /**
     * Socket ClientSocket为客户端的socket
     * 如果检查成功则返回true，反之亦然
     */
    boolean Check(Socket ClientSocket);
}
