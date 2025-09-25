package top.nserly.SoftwareCollections_API.FileHandle;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class JarVersionCleaner {
    // Fixed regular expressions: allow optional suffixes (e.g. platform information) after the version number
    private static final Pattern JAR_PATTERN = Pattern.compile(
            "^(.*?)-([0-9]+(\\.[0-9a-zA-Z\\-]+)*)(-[^.]*)?\\.jar$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Clean up the old version of the JAR file in the specified folder
     *
     * @param folderPath folder path
     *                   If @param isOnlyGetNeedToDeleteFile is true, only the files to be deleted are returned, and no deletion operation is performed
     * @return A collection of file paths to be deleted
     * @throws IOException when IOException accesses folders with errors
     */
    public static HashSet<String> cleanOldVersions(String folderPath, boolean isOnlyGetNeedToDeleteFile) throws IOException {
        HashSet<String> needToDeleteFileCollection = new HashSet<>();

        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            throw new IOException("Invalid folder path: " + folderPath);
        }

        File[] jarFiles = folder.listFiles((_, name) -> name.toLowerCase().endsWith(".jar"));

        if (jarFiles == null || jarFiles.length == 0) {
            log.error("The JAR file is not found in the folder.");
            return needToDeleteFileCollection;
        }

        Map<String, List<JarFileInfo>> jarGroups = new HashMap<>();

        for (File file : jarFiles) {
            JarFileInfo jarInfo = parseJarFileName(file);
            if (jarInfo != null) {
                jarGroups.computeIfAbsent(jarInfo.baseName, k -> new ArrayList<>()).add(jarInfo);
            } else {
                log.error("Unable to resolve file name format: {}", file.getName());
            }
        }

        for (Map.Entry<String, List<JarFileInfo>> entry : jarGroups.entrySet()) {
            String baseName = entry.getKey();
            List<JarFileInfo> jarFilesInGroup = entry.getValue();

            if (jarFilesInGroup.size() <= 1) {
                continue;
            }

            log.info("Processing group: {} ; Locate {} files", baseName, jarFilesInGroup.size());

            jarFilesInGroup.sort((j1, j2) -> compareVersions(j2.version, j1.version));

            for (JarFileInfo jarInfo : jarFilesInGroup) {
                log.debug("- {}{}", jarInfo.file.getName(), (jarInfo == jarFilesInGroup.getFirst() ? " (keep)" : ""));
            }

            for (int i = 1; i < jarFilesInGroup.size(); i++) {
                File fileToDelete = jarFilesInGroup.get(i).file;
                needToDeleteFileCollection.add(fileToDelete.getPath());
                if (!isOnlyGetNeedToDeleteFile) {
                    if (fileToDelete.delete()) {
                        log.debug("Deleted old version: {}", fileToDelete.getName());
                    } else {
                        log.error("File deletion fails: {}", fileToDelete.getName());
                    }
                }
            }
        }
        return needToDeleteFileCollection;
    }

    /**
     * Parse JAR file names, extract base names and version numbers
     *
     * @param file JAR file
     * @return JarFileInfo containing the base name, version number, and file object, returns null after parsing fails
     */
    private static JarFileInfo parseJarFileName(File file) {
        Matcher matcher = JAR_PATTERN.matcher(file.getName());
        if (matcher.matches()) {
            String baseName = matcher.group(1);
            String version = matcher.group(2);
            return new JarFileInfo(baseName, version, file);
        }
        return null;
    }

    /**
     * Compare the two version numbers
     *
     * @param version1 first version number
     * @param version2 second version number
     * @return 1 if version1 > version2, -1 if version1 < version2, 0 if equal
     */
    private static int compareVersions(String version1, String version2) {
        String[] parts1 = version1.split("[.-]");
        String[] parts2 = version2.split("[.-]");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            String part1 = (i < parts1.length) ? parts1[i] : "";
            String part2 = (i < parts2.length) ? parts2[i] : "";

            try {
                long num1 = Long.parseLong(part1);
                long num2 = Long.parseLong(part2);

                if (num1 != num2) {
                    return Long.compare(num1, num2);
                }
            } catch (NumberFormatException e) {
                int stringCompare = part1.compareToIgnoreCase(part2);
                if (stringCompare != 0) {
                    return stringCompare;
                }
            }
        }

        return 0;
    }

    /**
     * Internal class that stores JAR file information
     */
    private record JarFileInfo(String baseName,String version, File file) {

    }
}