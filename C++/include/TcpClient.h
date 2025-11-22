#pragma once
#include <winsock2.h>
#include <ws2tcpip.h>
#include <regex>
#include <string>
#include <stdexcept>
#include <future>
#include <chrono>
#include <iostream>

#pragma comment(lib, "ws2_32.lib")

class TCPClient {
private:
    static const std::string IPv4_REGEX;
    static const std::string IPv6_REGEX;
    static const std::string DomainName_REGEX;

    SOCKET clientSocket;
    std::string serverIP;
    int serverPort;
    bool isConnectedOver;

    // 初始化Winsock
    bool initWinsock() {
        WSADATA wsaData;
        int result = WSAStartup(MAKEWORD(2, 2), &wsaData);
        return result == 0;
    }

    // 清理Winsock
    void cleanupWinsock() {
        WSACleanup();
    }

public:
    TCPClient(const std::string& ip, int port)
        : serverIP(ip), serverPort(port), clientSocket(INVALID_SOCKET), isConnectedOver(false) {
        if (!initWinsock()) {
            throw std::runtime_error("Winsock初始化失败");
        }
    }

    ~TCPClient() {
        close();
        cleanupWinsock();
    }

    // 检查端口是否可用
    static bool isPortAvailable(int port) {
        WSADATA wsaData;
        if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
            return false;
        }

        SOCKET testSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
        if (testSocket == INVALID_SOCKET) {
            WSACleanup();
            return false;
        }

        sockaddr_in addr;
        addr.sin_family = AF_INET;
        addr.sin_addr.s_addr = INADDR_ANY;
        addr.sin_port = htons(port);

        bool available = bind(testSocket, (SOCKADDR*)&addr, sizeof(addr)) != SOCKET_ERROR;
        closesocket(testSocket);
        WSACleanup();
        return available;
    }

    // 验证IP地址
    static bool matchIP(const std::string& ip) {
        std::regex ipv4Regex(IPv4_REGEX);
        std::regex ipv6Regex(IPv6_REGEX);
        return std::regex_match(ip, ipv4Regex) ^ std::regex_match(ip, ipv6Regex);
    }

    // 验证域名
    static bool matchDomainName(const std::string& domain) {
        std::regex domainRegex(DomainName_REGEX);
        return std::regex_match(domain, domainRegex);
    }

    // 验证端口
    static bool matchPort(int port) {
        return port > 0 && port <= 65535;
    }

    // 综合验证
    static bool match(const std::string& ipOrDomain, int port) {
        return (matchIP(ipOrDomain) ^ matchDomainName(ipOrDomain)) && matchPort(port);
    }

    // 连接服务器
    void connect() {
        if (isConnected()) {
            throw std::runtime_error("已处于连接状态");
        }

        if (!match(serverIP, serverPort)) {
            throw std::invalid_argument("IP地址或端口无效");
        }

        // 创建socket
        clientSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
        if (clientSocket == INVALID_SOCKET) {
            throw std::runtime_error("创建socket失败: " + std::to_string(WSAGetLastError()));
        }

        // 设置服务器地址
        sockaddr_in serverAddr;
        serverAddr.sin_family = AF_INET;
        serverAddr.sin_port = htons(serverPort);

        // 解析IP地址
        if (inet_pton(AF_INET, serverIP.c_str(), &serverAddr.sin_addr) <= 0) {
            closesocket(clientSocket);
            throw std::runtime_error("解析IP地址失败");
        }

        // 连接服务器
        if (::connect(clientSocket, (SOCKADDR*)&serverAddr, sizeof(serverAddr)) == SOCKET_ERROR) {
            closesocket(clientSocket);
            clientSocket = INVALID_SOCKET;
            throw std::runtime_error("连接服务器失败: " + std::to_string(WSAGetLastError()));
        }

        isConnectedOver = true;
    }

    // 发送消息
    void send(const std::string& message) {
        if (!isConnected()) {
            throw std::runtime_error("未连接到服务器");
        }

        int bytesSent = ::send(clientSocket, message.c_str(), message.length(), 0);
        if (bytesSent == SOCKET_ERROR) {
            throw std::runtime_error("发送数据失败: " + std::to_string(WSAGetLastError()));
        }
    }

    // 接收消息 (简单实现，实际使用可能需要更复杂的处理)
    std::string receive(int bufferSize = 1024) {
        if (!isConnected()) {
            throw std::runtime_error("未连接到服务器");
        }

        char* buffer = new char[bufferSize];
        int bytesRead = recv(clientSocket, buffer, bufferSize - 1, 0);

        if (bytesRead <= 0) {
            delete[] buffer;
            if (bytesRead == 0) {
                throw std::runtime_error("连接已关闭");
            }
            else {
                throw std::runtime_error("接收数据失败: " + std::to_string(WSAGetLastError()));
            }
        }

        buffer[bytesRead] = '\0';
        std::string result(buffer);
        delete[] buffer;
        return result;
    }

    // 获取服务器支持的软件名称
    std::string getServerSupportedSoftwareName() {
        if (!isConnected()) {
            throw std::runtime_error("未连接到服务器");
        }

        send("{getSoftwareName}");

        // 使用异步等待响应，设置5秒超时
        auto future = std::async(std::launch::async, &TCPClient::receive, this, 1024);
        if (future.wait_for(std::chrono::seconds(5)) == std::future_status::timeout) {
            throw std::runtime_error("获取软件名称超时");
        }

        return future.get();
    }

    // 检查服务器是否支持当前软件名称
    bool isServerSupportedSoftwareName(const std::string& currentSoftwareName) {
        return getServerSupportedSoftwareName() == currentSoftwareName;
    }

    // 关闭连接
    void close() {
        if (clientSocket != INVALID_SOCKET) {
            closesocket(clientSocket);
            clientSocket = INVALID_SOCKET;
        }
        isConnectedOver = false;
    }

    // 判断是否已连接
    bool isConnected() const {
        return isConnectedOver;
    }
};

// 正则表达式常量定义
const std::string TCPClient::IPv4_REGEX =
"^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

const std::string TCPClient::IPv6_REGEX =
"^(([0-9a-fA-F]{1,4}(:[0-9a-fA-F]{1,4})*)|::([0-9a-fA-F]{1,4}(:[0-9a-fA-F]{1,4})*)?)$";

const std::string TCPClient::DomainName_REGEX =
"^([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,6}$";