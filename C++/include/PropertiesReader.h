#pragma once
#include <string>
#include <unordered_map>
#include <fstream>
#include <algorithm>

class PropertiesReader {
public:
    // 构造函数，加载properties文件
    PropertiesReader(const std::wstring& filePath) {
        std::wifstream fin(filePath);
        if (!fin.is_open()) return;
        std::wstring line;
        while (std::getline(fin, line)) {
            // 去除首尾空白
            trim(line);
            // 跳过空行和注释
            if (line.empty() || line[0] == L'#' || line[0] == L'!') continue;
            size_t pos = line.find(L'=');
            if (pos == std::wstring::npos) continue;
            std::wstring key = line.substr(0, pos);
            std::wstring value = line.substr(pos + 1);
            trim(key);
            trim(value);
            properties_[key] = value;
        }
    }

    // 获取key对应的value，不存在则返回空字符串
    std::wstring get(const std::wstring& key) const {
        auto it = properties_.find(key);
        if (it != properties_.end()) return it->second;
        return L"";
    }

    // 字符串转大写
    static std::wstring toUpper(const std::wstring& str) {
        std::wstring result = str;
        std::transform(result.begin(), result.end(), result.begin(), ::towupper);
        return result;
    }

    // 字符串转小写
    static std::wstring toLower(const std::wstring& str) {
        std::wstring result = str;
        std::transform(result.begin(), result.end(), result.begin(), ::towlower);
        return result;
    }

private:
    std::unordered_map<std::wstring, std::wstring> properties_;

    // 去除首尾空白
    static void trim(std::wstring& s) {
        size_t first = s.find_first_not_of(L" \t\r\n");
        size_t last = s.find_last_not_of(L" \t\r\n");
        if (first == std::wstring::npos || last == std::wstring::npos) {
            s.clear();
        } else {
            s = s.substr(first, last - first + 1);
        }
    }
};