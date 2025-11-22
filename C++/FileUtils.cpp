#include "FileUtils.h"
#include <shlwapi.h>
#include <fstream>

// 检查文件是否存在
bool FileExists(const std::wstring& filePath) {
    return PathFileExistsW(filePath.c_str()) != 0;
}

// 获取文件内容
std::string getFileContent(const std::wstring& filePath) {
    FILE* file = nullptr;
    if (_wfopen_s(&file, filePath.c_str(), L"rb") != 0 || !file) {
        return {};
    }
    fseek(file, 0, SEEK_END);
    long fileSize = ftell(file);
    fseek(file, 0, SEEK_SET);
    std::string content;
    if (fileSize > 0) {
        content.resize(fileSize);
        fread(&content[0], 1, fileSize, file);
    }
    fclose(file);
    return content;
}

// 检查并创建 PicturePlayer.vmoptions 文件
void EnsureVmOptionsFile(const std::wstring& exeDir) {
    std::wstring vmOptionsPath = exeDir + L"\\PicturePlayer.vmoptions";
    if (!FileExists(vmOptionsPath)) {
        std::ofstream ofs(vmOptionsPath, std::ios::out | std::ios::binary);
        if (ofs) {
            ofs << "--enable-native-access=ALL-UNNAMED\n";
            ofs << "-Xms64m\n";
            ofs << "-Xmx512m\n";
            ofs << "-XX:+UseZGC\n";
            ofs << "-XX:+UseCompactObjectHeaders\n";
            ofs << "-Dsun.java2d.opengl=true\n";
            ofs << "-Dfile.encoding=UTF-8\n";
            ofs << "-Dflatlaf.uiScale=auto\n";
            ofs.close();
        }
    }
}

// 读取 vmoptions 文件内容，返回每一行
std::vector<std::string> ReadVmOptions(const std::wstring& exeDir) {
    std::vector<std::string> options;
    std::wstring vmOptionsPath = exeDir + L"\\PicturePlayer.vmoptions";
    std::ifstream ifs(vmOptionsPath, std::ios::in | std::ios::binary);
    std::string line;
    while (std::getline(ifs, line)) {
        if (!line.empty() && (line.back() == '\r' || line.back() == '\n')) {
            line.pop_back();
        }
        if (!line.empty()) {
            options.push_back(line);
        }
    }
    return options;
}


bool isJavaInstalled() {
    // 尝试运行 "java -version" 并检查返回值
    STARTUPINFO si = { sizeof(si) };
    PROCESS_INFORMATION pi;
    wchar_t cmd[] = L"java -version";
    BOOL success = CreateProcess(
        NULL,
        cmd,
        NULL,
        NULL,
        FALSE,
        CREATE_NO_WINDOW,
        NULL,
        NULL,
        &si,
        &pi
    );
    if (!success) {
        return false;
    }
    // 等待进程结束
    WaitForSingleObject(pi.hProcess, 3000);
    DWORD exitCode = 0;
    GetExitCodeProcess(pi.hProcess, &exitCode);
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    // java -version 通常返回0或非0都代表已安装，只要能启动即可
    return true;
}