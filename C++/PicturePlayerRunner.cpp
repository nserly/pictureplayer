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
    std::wstring exeDir = getExeDirectory();

    // 优化：先检查关键文件是否存在，减少无效操作
    if (!isJavaInstalled()) {
        JOptionPane::showError(L"If the Java runtime is not detected, install Java and configure the system PATH first.");
        return 1;
    }
    std::wstring jarPath = exeDir + L"\\PicturePlayer.jar";
    if (!FileExists(jarPath)) {
        JOptionPane::showError(L"Startup files cannot be found!\nPath: " + jarPath);
        return 1;
    }

    // 优化：只在需要时创建 vmoptions 文件
    EnsureVmOptionsFile(exeDir);

    // 优化：只读取一次配置文件
    std::wstring configPath = exeDir + L"\\StartupConfig.properties";
    if (FileExists(configPath)) {
        PropertiesReader propertiesReader(configPath);
        std::wstring enableConsole = PropertiesReader::toLower(propertiesReader.get(L"EnableConsole"));
        if (enableConsole == L"true") isConsole = true;
    }

    // 优化：预分配命令字符串，减少多次扩容
    std::vector<std::wstring> arguments = getRuntimeArgs();
    std::vector<std::string> vmOptions = ReadVmOptions(exeDir);
    std::wstring baseCommand;
    baseCommand.reserve(256 + vmOptions.size() * 32 + arguments.size() * 64);

    baseCommand = L"java ";
    for (const auto& opt : vmOptions) {
        // 去除前导空格
        size_t first = opt.find_first_not_of(" \t\r\n");
        if (first != std::string::npos) {
            if (opt.size() - first >= 2 && opt[first] == '/' && opt[first + 1] == '/')
                continue;
        }
        baseCommand += std::wstring(opt.begin(), opt.end()) + L" ";
    }
    baseCommand += L"-cp \"" + jarPath + L";" + exeDir + L"\\lib\\*\" top.nserly.GUIStarter";

    for (size_t i = 1; i < arguments.size(); ++i) {
        if (PathFileExistsW(arguments[i].c_str())) {
            baseCommand += L" \"" + arguments[i] + L"\"";
        }
    }

    STARTUPINFO si = { sizeof(si) };
    PROCESS_INFORMATION pi;
    std::vector<wchar_t> commandLine(baseCommand.begin(), baseCommand.end());
    commandLine.push_back(L'\0');

    BOOL success = CreateProcess(
        NULL,
        commandLine.data(),
        NULL,
        NULL,
        FALSE,
        isConsole ? NULL : CREATE_NO_WINDOW,
        NULL,
        exeDir.c_str(),
        &si,
        &pi
    );

    if (!success) {
        JOptionPane::showError(L"Failed to start Java application. Please ensure that Java is installed and added to your system PATH.");
        return 1;
    }

    WaitForSingleObject(pi.hProcess, INFINITE);
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    return 0;
}
