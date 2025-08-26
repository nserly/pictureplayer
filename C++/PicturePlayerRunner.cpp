#include "PicturePlayerRunner.h"


/**
 * 应用程序入口点
 * @param hInstance 当前实例句柄
 * @param hPrevInstance 以前的实例句柄（未使用）
 * @param lpCmdLine 命令行参数
 * @param nCmdShow 显示窗口的方式
 * @return int 返回值
 */

int WINAPI WinMain(
    _In_ HINSTANCE hInstance,
    _In_opt_ HINSTANCE hPrevInstance,
    _In_ LPSTR lpCmdLine,
    _In_ int nCmdShow
) {

	bool isConsole = false;
    // 获取当前可执行文件所在的目录
    std::wstring exeDir = getExeDirectory();


    // 判断当前是否存在java运行环境
    if (!isJavaInstalled()) {
        JOptionPane::showError(L"If the Java runtime is not detected, install Java and configure the system PATH first.");
        return 1;
    }

    // 判断PicturePlayer.jar是否存在
    if (!FileExists(exeDir + L"\\PicturePlayer.jar")) {
        JOptionPane::showError(L"Startup files cannot be found!\nPath: " + std::wstring(exeDir + L"\\PicturePlayer.jar"));
        return 1;
    }

    // 检查并创建 vmoptions 文件
    EnsureVmOptionsFile(exeDir);

    // 检查是否存在配置文件并读取配置
    if (FileExists(exeDir + L"\\StartupConfig.properties")) {
        PropertiesReader propertiesReader = PropertiesReader(exeDir + L"\\StartupConfig.properties");
        if (PropertiesReader::toLower(propertiesReader.get(L"EnableConsole")) == L"true") {
            isConsole = true;
        }
    }

    // 获取命令行参数
    std::vector<std::wstring> arguments = getRuntimeArgs();

    // 读取 vmoptions 配置
    std::vector<std::string> vmOptions = ReadVmOptions(exeDir);

    // 构建基础Java命令
    std::wstring baseCommand = L"java ";

    // 添加 vmoptions 参数
    for (const auto& opt : vmOptions) {
        baseCommand += std::wstring(opt.begin(), opt.end()) + L" ";
    }

    baseCommand += L"-cp \"" + exeDir + L"\\PicturePlayer.jar;" +
        exeDir + L"\\lib\\*\" top.nserly.GUIStarter";

    // 添加额外参数（跳过第一个参数即程序自身路径）
    for (size_t i = 1; i < arguments.size(); ++i) {
        // 检查参数是否是文件路径
        if (PathFileExistsW(arguments[i].c_str())) {
            // 用引号包裹文件路径（处理空格问题）
            baseCommand += L" \"";
            baseCommand += arguments[i];
            baseCommand += L"\"";
        }
    }

    // 准备进程启动信息
    STARTUPINFO si = { sizeof(si) };
    PROCESS_INFORMATION pi;

    // 创建可修改的命令行缓冲区
    std::vector<wchar_t> commandLine(baseCommand.begin(), baseCommand.end());
    commandLine.push_back(L'\0'); // 添加字符串终止符

	// 重定向标准输入输出到控制台
    BOOL success;
    if (isConsole) {
        success = CreateProcess(
            NULL,                   // 不使用模块名
            commandLine.data(),     // 动态构建的命令行
            NULL,                   // 进程句柄不可继承
            NULL,                   // 线程句柄不可继承
            FALSE,                  // 不继承句柄
            NULL,                   // 无特殊标志
            NULL,                   // 使用父进程环境
            exeDir.c_str(),         // 设置工作目录为程序所在目录
            &si,                    // 启动信息
            &pi                     // 进程信息
        );
    }
    else {
        success = CreateProcess(
            NULL,                   // 不使用模块名
            commandLine.data(),     // 动态构建的命令行
            NULL,                   // 进程句柄不可继承
            NULL,                   // 线程句柄不可继承
            FALSE,                  // 不继承句柄
            CREATE_NO_WINDOW,       // 无特殊标志
            NULL,                   // 使用父进程环境
            exeDir.c_str(),         // 设置工作目录为程序所在目录
            &si,                    // 启动信息
            &pi                     // 进程信息
        );
    }

    // 检查进程是否创建成功
    if (!success) {
		JOptionPane::showError(L"Failed to start Java application. Please ensure that Java is installed and added to your system PATH.");
        return 1;
    }

    // 等待程序结束
    WaitForSingleObject(pi.hProcess, INFINITE);

    // 清理资源
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);

    return 0;
}
