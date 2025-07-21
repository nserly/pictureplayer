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
}