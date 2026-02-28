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

package top.nserly.SoftwareCollections_API.FileHandle;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class JarFileRenamer {

    // 匹配模式的正则表达式
    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("(.+?)-(\\d+\\.\\d+\\.\\d+\\.Final)(-.+?)\\.jar$");

    public static HashSet<File> renameJarFile(String dir) throws IOException {
        File dirFile = new File(dir);
        if (!dirFile.exists()) throw new IOException(dir + " is not exists!");
        if (!dirFile.isDirectory()) throw new IOException(dir + " is not a directory!");
        HashSet<File> hashSet = new HashSet<>();
        for (File originalFile : Objects.requireNonNull(dirFile.listFiles())) {
            if (!originalFile.isFile()) continue;
            hashSet.add(renameJarFile(originalFile));
        }
        return hashSet;
    }


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

                if (newFile.exists())
                    if (newFile.delete())
                        log.info("Delete old file successfully: {}", newFileName);
                    else
                        log.error("Failed to delete old file: {}", newFileName);

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