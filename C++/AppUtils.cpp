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

#include "AppUtils.h"
#include <windows.h>
#include <vector>
#include <string>
#include <shellapi.h>

// 获取当前可执行文件所在目录
std::wstring getExeDirectory() {
    wchar_t exePath[MAX_PATH];
    GetModuleFileNameW(NULL, exePath, MAX_PATH);

    std::wstring path(exePath);
    size_t pos = path.find_last_of(L"\\/");
    if (pos != std::wstring::npos) {
        return path.substr(0, pos);
    }
    return L".";
}

// 获取宽字符命令行参数
std::vector<std::wstring> getRuntimeArgs() {
    LPWSTR* argv = nullptr;
    int argc = 0;

    argv = CommandLineToArgvW(GetCommandLineW(), &argc);
    std::vector<std::wstring> arguments;

    if (argv == nullptr) {
        return arguments;
    }

    for (int i = 0; i < argc; i++) {
        arguments.push_back(argv[i]);
    }

    LocalFree(argv);

    return arguments;
}