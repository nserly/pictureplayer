/*
 * Copyright 2026 PicturePlayer;Nserly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

﻿#include "PicturePlayerRunner.h"
#include "WindowsAppMutex.h"

/**
 * 应用程序入口点
 * @param hInstance 当前实例句柄
 * @param hPrevInstance 以前的实例句柄（未使用）
 * @param lpCmdLine 命令行参数
 * @param nCmdShow 显示窗口的方式
 * @return int 返回值
 */

static std::string wstringToUtf8(const std::wstring& w) {
    if (w.empty()) return {};
    int size_needed = ::WideCharToMultiByte(CP_UTF8, 0, w.data(), (int)w.size(), NULL, 0, NULL, NULL);
    if (size_needed <= 0) return {};
    std::string str(size_needed, 0);
    ::WideCharToMultiByte(CP_UTF8, 0, w.data(), (int)w.size(), &str[0], size_needed, NULL, NULL);
    return str;
}

// 假设 tcp.sendFilePathToExistingInstance 需要 std::wstring 或 const std::wstring& 参数
// 你当前传递的是 std::string 类型的 extraPaths，需将其转换为 std::wstring

// 1. 添加 stringToWstring 工具函数
static std::wstring stringToWstring(const std::string& str) {
    if (str.empty()) return {};
    int size_needed = ::MultiByteToWideChar(CP_UTF8, 0, str.data(), (int)str.size(), NULL, 0);
    if (size_needed <= 0) return {};
    std::wstring wstr(size_needed, 0);
    ::MultiByteToWideChar(CP_UTF8, 0, str.data(), (int)str.size(), &wstr[0], size_needed);
    return wstr;
}

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

    // 先读取运行时参数（后面构建命令和用于 TCP 消息的图片路径）
    std::vector<std::wstring> arguments = getRuntimeArgs();

    // 读取配置文件（包括 EnableConsole、ConnectPort、ConnectHost）
    int connectPort = 22357;
    std::string connectHost = "127.0.0.1";
    std::wstring configPath = exeDir + L"\\StartupConfig.properties";
    if (FileExists(configPath)) {
        PropertiesReader propertiesReader(configPath);
        std::wstring enableConsole = PropertiesReader::toLower(propertiesReader.get(L"EnableConsole"));
        if (enableConsole == L"true") isConsole = true;

        std::wstring portStr = propertiesReader.get(L"ConnectPort");
        if (!portStr.empty()) {
            try {
                connectPort = std::stoi(std::string(portStr.begin(), portStr.end()));
            }
            catch (...) {
                connectPort = 22357;
            }
        }
        std::wstring hostW = propertiesReader.get(L"ConnectHost");
        if (!hostW.empty()) {
            connectHost = wstringToUtf8(hostW);
        }
    }

    // 构建要发送的图片路径字符串（与 later 拼接命令中添加的文件路径一致）
    std::string extraPaths;
    for (size_t i = 1; i < arguments.size(); ++i) {
        if (PathFileExistsW(arguments[i].c_str())) {
            if (!extraPaths.empty()) extraPaths += " ";
            extraPaths += wstringToUtf8(arguments[i]);
        }
    }

    // 如果配置了连接端口，先检测并尝试连接发送信息，然后退出程序
    if (connectPort > 0) {
        WindowsAppMutex tcp(connectPort); // 默认本地连接，500ms超时

        // 先检查是否有监听者
        if (!tcp.isFirstInstance()) {
            tcp.sendSoftwareVisibleDirectiveToExistingInstance(true);
            tcp.sendFilePathToExistingInstance(extraPaths);
            tcp.close();
            return 0;
        }
    }

    // 优化：预分配命令字符串，减少多次扩容
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