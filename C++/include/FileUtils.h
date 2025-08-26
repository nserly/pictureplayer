#pragma once
#include <string>
#include <vector>

bool FileExists(const std::wstring& filePath);
std::string getFileContent(const std::wstring& filePath);
void EnsureVmOptionsFile(const std::wstring& exeDir);
std::vector<std::string> ReadVmOptions(const std::wstring& exeDir);
bool isJavaInstalled();