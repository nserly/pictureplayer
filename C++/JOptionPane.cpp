#include "JOptionPane.h"

void JOptionPane::showMessage(const std::wstring& message, const std::wstring& title) {
    MessageBoxW(NULL, message.c_str(), title.c_str(), MB_OK | MB_ICONINFORMATION);
}

void JOptionPane::showError(const std::wstring& message, const std::wstring& title) {
    MessageBoxW(NULL, message.c_str(), title.c_str(), MB_OK | MB_ICONERROR);
}

bool JOptionPane::showConfirm(const std::wstring& message, const std::wstring& title) {
    int ret = MessageBoxW(NULL, message.c_str(), title.c_str(), MB_YESNO | MB_ICONQUESTION);
    return ret == IDYES;
}