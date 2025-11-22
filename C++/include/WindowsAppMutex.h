#pragma once
#include <winsock2.h>
#include <ws2tcpip.h>
#include <thread>
#include <future>
#include <chrono>
#include <iostream>
#include <stdexcept>
#include <memory>
#include <functional>
#include <atomic>

#pragma comment(lib, "ws2_32.lib")

#include "TcpClient.h" // 需要 TCPClient 的声明/定义

// 函数对象接口，对应Java的HandleSoftwareRequestAction
using HandleSoftwareRequestAction = std::function<void(const std::string&)>;

class WindowsAppMutex {
private:
    const int port;
    std::thread initThread;
    std::unique_ptr<TCPClient> tcpClient_;
    std::atomic<bool> isFirstInstance_;
    std::atomic<bool> isSupportedSoftwareName_;
    HandleSoftwareRequestAction handleAction_;
    std::atomic<bool> isInitialized_;

    // 检查端口是否被占用（复用TCPClient的静态方法）
    bool checkPortAvailability() const {
        return TCPClient::isPortAvailable(port);
    }

    // 初始化逻辑（运行在单独线程）
    void init() {
        // 先检查端口并设置标志
        isFirstInstance_.store(checkPortAvailability());

        if (!isFirstInstance_.load()) {
            std::cout << "Port " << port << " is already in use." << std::endl;
            try {
                // 作为客户端连接到已存在的实例（本地回环地址）
                tcpClient_ = std::make_unique<TCPClient>("127.0.0.1", port);
                tcpClient_->connect();

                // 检查服务器是否支持所需的软件名称协议
                std::string serverName = extractSoftwareName(tcpClient_->getServerSupportedSoftwareName());
                std::string currentSoftwareName = getSoftwareName(); // 需要实现获取当前软件名称的逻辑
                isSupportedSoftwareName_.store(serverName == currentSoftwareName);

                if (!isSupportedSoftwareName_.load()) {
                    std::cerr << "The server does not support the required software name protocol." << std::endl;
                }
            }
            catch (const std::exception& e) {
                std::cerr << "Client initialization error: " << e.what() << std::endl;
            }
        }
        else {
            // 第一个实例逻辑（原Java中启动服务器，此处因需求不实现服务器，仅做提示）
            std::cout << "First instance, port " << port << " is available." << std::endl;
            // 注意：服务器相关逻辑未实现（根据需求仅实现TCP客户端）
        }

        // 初始化完成标志设置在最后，保证外部等待时能看到完整的初始化结果
        isInitialized_.store(true);
    }

    // 获取当前软件名称（需根据实际情况实现）
    std::string getSoftwareName() const {
        // 示例：从系统环境变量或配置中获取
        return "PicturePlayer";
    }

public:
    static std::string substring(const std::string& str, int beginIndex) {
        if (beginIndex < 0) throw std::out_of_range("Negative index");
        size_t pos = static_cast<size_t>(beginIndex);
        if (pos > str.size()) throw std::out_of_range("Index out of range");
        return str.substr(pos);
    }

    static std::string substring(const std::string& str, int beginIndex, int endIndex) {
        if (beginIndex < 0 || endIndex < 0) throw std::out_of_range("Negative index");
        size_t pos = static_cast<size_t>(beginIndex);
        size_t end = static_cast<size_t>(endIndex);
        if (pos > end || end > str.size()) throw std::out_of_range("Index out of range");
        return str.substr(pos, end - pos);
    }

    // 提取软件名称（用 Java 风格 substring）
    static std::string extractSoftwareName(const std::string& input) {
        std::string prefix = "{SoftwareName} ";
        int beginIndex = static_cast<int>(prefix.size());
        return substring(input, beginIndex); // 和 Java 写法完全一样
    }

    // 构造函数
    WindowsAppMutex(int port)
        : port(port),
        tcpClient_(nullptr),
        isFirstInstance_(false),
        isSupportedSoftwareName_(false),
        handleAction_(nullptr),
        isInitialized_(false) {
        // 启动初始化线程
        initThread = std::thread(&WindowsAppMutex::init, this);
    }

    // 析构函数
    ~WindowsAppMutex() {
        close();
    }

    // 禁止拷贝构造和赋值
    WindowsAppMutex(const WindowsAppMutex&) = delete;
    WindowsAppMutex& operator=(const WindowsAppMutex&) = delete;

    // 获取端口
    int getPort() const {
        return port;
    }

    // 判断是否为第一个实例
    bool isFirstInstance() const {
        waitForInitialization();
        return isFirstInstance_.load();
    }

    // 判断服务器是否支持当前软件名称协议
    bool isSupportedSoftwareName() const {
        return isSupportedSoftwareName_.load();
    }

    // 获取处理请求的回调
    HandleSoftwareRequestAction getHandleSoftwareRequestAction() const {
        return handleAction_;
    }

    // 发送文件路径到已存在的实例
    void sendFilePathToExistingInstance(const std::string& filePath) {
        if (isFirstInstance()) {
            throw std::runtime_error("This is the first instance, cannot send file path.");
        }

        waitForInitialization();

        if (!isSupportedSoftwareName()) {
            throw std::runtime_error("The server does not support the required software name protocol.");
        }

        if (!tcpClient_) {
            throw std::runtime_error("TCP client not initialized.");
        }

        try {
            tcpClient_->send("{newPicturePath} " + filePath);
        }
        catch (const std::exception& e) {
            std::cerr << "Send file path failed: " << e.what() << std::endl;
            throw;
        }
    }

    // 发送软件可见性指令到已存在的实例
    void sendSoftwareVisibleDirectiveToExistingInstance(bool visible) {
        if (isFirstInstance()) {
            throw std::runtime_error("This is the first instance, cannot send visible directive.");
        }

        waitForInitialization();

        if (!isSupportedSoftwareName()) {
            throw std::runtime_error("The server does not support the required software name protocol.");
        }

        if (!tcpClient_) {
            throw std::runtime_error("TCP client not initialized.");
        }

        try {
            tcpClient_->send("{getSoftwareVisibleDirective} " + std::string(visible ? "true" : "false"));
        }
        catch (const std::exception& e) {
            std::cerr << "Send visible directive failed: " << e.what() << std::endl;
            throw;
        }
    }

    // 设置请求处理回调
    void addHandleSoftwareRequestAction(HandleSoftwareRequestAction action) {
        if (!action) {
            throw std::invalid_argument("HandleSoftwareRequestAction cannot be null.");
        }
        handleAction_ = std::move(action);
    }

    // 关闭资源
    void close() {
        // 等待初始化线程完成
        if (initThread.joinable()) {
            initThread.join();
        }

        // 关闭TCP客户端
        if (tcpClient_) {
            try {
                tcpClient_->close();
            }
            catch (const std::exception& e) {
                std::cerr << "Error closing TCP client: " << e.what() << std::endl;
            }
        }
    }

private:
    // 等待初始化完成
    void waitForInitialization() const {
        while (!isInitialized_.load()) {
            std::this_thread::sleep_for(std::chrono::milliseconds(10));
        }
    }
};