package top.nserly.SoftwareCollections_API.FileHandle;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class JarFileRenamer {

    // 匹配模式的正则表达式
    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("(.+?)-(\\d+\\.\\d+\\.\\d+\\.Final)(-.+?)\\.jar$");


    public static File renameJarFile(File file) {
        // Check if file exists and is a valid file (not a directory)
        if (file == null || !file.exists() || !file.isFile()) {
            log.warn("File does not exist or is not a valid file: {}", (file != null ? file.getAbsolutePath() : "null"));
            return file;
        }

        String originalName = file.getName();
        Matcher matcher = FILENAME_PATTERN.matcher(originalName);

        if (matcher.matches()) {
            try {
                String baseName = matcher.group(1);
                String version = matcher.group(2);
                String suffix = matcher.group(3);

                // Construct new filename
                String newFileName = baseName + suffix + "-" + version + ".jar";

                // Get parent directory and create new file object
                File parentDir = file.getParentFile();
                File newFile = new File(parentDir, newFileName);

                // Perform rename operation
                boolean renamed = file.renameTo(newFile);

                if (renamed) {
                    log.info("Renamed successfully: {} -> {}", originalName, newFileName);
                    return newFile;
                } else {
                    log.error("Failed to rename: {}", originalName);
                    return file;
                }
            } catch (Exception e) {
                log.error("Error occurred while processing file: {}", originalName, e);
                return file;
            }
        } else {
            // Filename doesn't match pattern, no renaming performed
            log.debug("Filename does not match pattern, no renaming performed: {}", originalName);
            return file;
        }
    }
}