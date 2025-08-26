#pragma once
#include <string>
#include <windows.h>

class JOptionPane
{
public:
	// 信息提示框
	static void showMessage(const std::wstring& message, const std::wstring& title = L"Message");

	// 错误提示框
	static void showError(const std::wstring& message, const std::wstring& title = L"Error");

	// 确认对话框，返回 true 表示“是”，false 表示“否”
	static bool showConfirm(const std::wstring& message, const std::wstring& title = L"Confirm");
};

