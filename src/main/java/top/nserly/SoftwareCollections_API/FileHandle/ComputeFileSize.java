package top.nserly.SoftwareCollections_API.FileHandle;

import java.io.File;

public class ComputeFileSize {
    /**
     * Computes the file size in a human-readable format based on the given file and precision.
     *
     * @param file      the File object representing the file to compute the size of
     * @param precision the number of decimal places to round the result
     * @return a FileSize object representing the computed file size with the appropriate unit and rounded to the specified precision, or a FileSize of 0 B if the file is null or does
     * not exist
     */
    public static FileSize computeFileSize(File file, int precision) {
        if (file == null || !file.exists()) {
            return new FileSize(0, FileSize.UNIT_B);
        }
        long sizeInBytes = file.length();
        return computeFileSize(sizeInBytes, precision);
    }

    /**
     * Computes the file size in a human-readable format based on the given size in bytes and precision.
     *
     * @param sizeInBytes the size of the file in bytes
     * @param precision   the number of decimal places to round the result
     * @return a FileSize object representing the computed file size with the appropriate unit and rounded to the specified precision
     */
    public static FileSize computeFileSize(float sizeInBytes, int precision) {
        if (sizeInBytes < 0) {
            return new FileSize(0, FileSize.UNIT_B);
        }
        int unit = FileSize.UNIT_B;
        float size = sizeInBytes;

        while (size >= 1024 && unit < FileSize.UNIT_YB) {
            size /= 1024;
            unit++;
        }

        // Round to the specified precision
        size = Math.round(size * Math.pow(10, precision)) / (float) Math.pow(10, precision);

        return new FileSize(size, unit);
    }

    public static String formatFileSize(FileSize fileSize) {
        return fileSize.size + " " + FileSize.UNIT_SYMBOLS[fileSize.unit];
    }

    public record FileSize(float size, int unit) {
        public static final int UNIT_B = 0;
        public static final int UNIT_KB = 1;
        public static final int UNIT_MB = 2;
        public static final int UNIT_GB = 3;
        public static final int UNIT_TB = 4;
        public static final int UNIT_PB = 5;
        public static final int UNIT_EB = 6;
        public static final int UNIT_ZB = 7;
        public static final int UNIT_YB = 8;
        public static final String[] UNIT_SYMBOLS = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
    }
}
