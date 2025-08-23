package top.nserly.SoftwareCollections_API.String;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class GetString {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final byte[] bytes = new byte[102400];

    // 保持原始方法不变，以便兼容现有代码
    public static String getString(InputStream inputStream) {
        try {
            int length = inputStream.read(bytes);
            if (length <= 0) {
                log.warn("Received empty input stream or end of stream reached.");
                return null;
            }
            return new String(bytes, 0, length);
        } catch (Exception ignored) {

        }
        return null;
    }

    /**
     * 处理字符串，删除{...}中包含的所有空格
     * @param input 输入字符串
     * @return 处理后的字符串
     */
    public static String removeSpacesInCurlyBraces(String input) {
        // 检查输入是否为null
        if (input == null) {
            return null;
        }

        // 使用StringBuilder构建结果
        StringBuilder result = new StringBuilder();
        // 标记是否在花括号内
        boolean inCurlyBraces = false;

        // 遍历字符串的每个字符
        for (char c : input.toCharArray()) {
            // 遇到左花括号，标记开始
            if (c == '{') {
                inCurlyBraces = true;
                result.append(c);
            }
            // 遇到右花括号，标记结束
            else if (c == '}') {
                inCurlyBraces = false;
                result.append(c);
            }
            // 其他情况直接添加字符
            else if (!(inCurlyBraces && Character.isWhitespace(c))) {
                result.append(c);
            }
        }

        return result.toString();
    }
}