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

#include "ConsoleUtils.h"

void RedirectIOToConsole()
{
    // 获取标准输出句柄
    HANDLE hStdOut = GetStdHandle(STD_OUTPUT_HANDLE);
    if (hStdOut != INVALID_HANDLE_VALUE) {
        int fd = _open_osfhandle((intptr_t)hStdOut, _O_TEXT);
        if (fd != -1) {
            FILE* fp = _fdopen(fd, "w");
            if (fp != nullptr) {
                *stdout = *fp;
                setvbuf(stdout, nullptr, _IONBF, 0);
            }
        }
    }

    // 获取标准错误句柄
    HANDLE hStdErr = GetStdHandle(STD_ERROR_HANDLE);
    if (hStdErr != INVALID_HANDLE_VALUE) {
        int fd = _open_osfhandle((intptr_t)hStdErr, _O_TEXT);
        if (fd != -1) {
            FILE* fp = _fdopen(fd, "w");
            if (fp != nullptr) {
                *stderr = *fp;
                setvbuf(stderr, nullptr, _IONBF, 0);
            }
        }
    }
}