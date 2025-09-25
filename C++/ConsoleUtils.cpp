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