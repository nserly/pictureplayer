package top.nserly.PicturePlayer.Command;

import lombok.extern.slf4j.Slf4j;
import top.nserly.PicturePlayer.Version.DownloadChecker.CheckAndDownloadUpdate;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;
import top.nserly.SoftwareCollections_API.String.RandomString;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;

@Slf4j
public class CommandHandle {
    private static final String CURRENT_JAR_PATH; // 当前JAR文件路径
    private static final String CURRENT_JAR_NAME;//当前JAR文件路径

    private static final String MainFileSuffix;

    static {
        ClassLoader classLoader = CommandHandle.class.getClassLoader();
        URL url = classLoader.getResource(CommandHandle.class.getName().replace('.', '/') + ".class");
        if (url == null) throw new RuntimeException("Can't find the current JAR file");
        CURRENT_JAR_PATH = url.getPath().substring(5, url.getPath().lastIndexOf("!"));
        CURRENT_JAR_NAME = CURRENT_JAR_PATH.substring(CURRENT_JAR_PATH.lastIndexOf("/") + 1);
        MainFileSuffix = RandomString.getRandomString(5);
    }


    public static void moveFileToDirectory(String DownloadMainFilePath) throws IOException {
        DownloadMainFilePath = DownloadMainFilePath.replace("\\", "/");
        Path sourcePath = Path.of(DownloadMainFilePath);
        Path destinationPath = Path.of("./" + DownloadMainFilePath.substring(DownloadMainFilePath.lastIndexOf("/") + 1) + MainFileSuffix);
        Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Main File moved successfully.");
    }

    public static void moveFileToLibDirectory(ArrayList<String> DownloadLibFilePath) throws IOException {
        for (String string : DownloadLibFilePath) {
            string = string.replace("\\", "/");
            String fileName = new File(string).getName();
            int LastIndex1 = fileName.lastIndexOf("-");
            if (LastIndex1 == -1) {
                LastIndex1 = fileName.lastIndexOf(".jar");
            }
            String currentHandlerDependencyName = fileName.substring(0, LastIndex1);
            for (String DependencyName : CheckAndDownloadUpdate.DependenciesName) {
                if (DependencyName.contains(currentHandlerDependencyName)) {
                    String deleteFilePath = CheckAndDownloadUpdate.DependenciesFilePath.get(DependencyName);
                    new File(deleteFilePath).delete();
                }
            }

            Path sourcePath = Path.of(string);
            Path destinationPath = Path.of("./lib/" + string.substring(string.lastIndexOf("/") + 1));
            Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Lib File moved successfully.");
    }

    public static String detectOSType() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "windows";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return "linux";
        } else {
            return "unknown";
        }
    }

    public static void executeOSSpecificCommands(String osType, String DownloadFilePath, String OpenedPicturePath) {
        switch (osType) {
            case "windows":
                try {
                    createAndRunWindowsBatchFile(DownloadFilePath, OpenedPicturePath);
                } catch (IOException e) {
                    log.error(ExceptionHandler.getExceptionMessage(e));
                    log.error("Automatic update failed");
                }
                break;
            case "linux":
                try {
                    createAndRunLinuxShellScript(DownloadFilePath, OpenedPicturePath);
                } catch (IOException e) {
                    log.error(ExceptionHandler.getExceptionMessage(e));
                    log.error("Automatic update failed");
                }
                break;
            default:
                log.error("Unsupported OS:\"{}\"", osType);
        }
    }

    public static void executeOSSpecificCommands(String osType, String DownloadFilePath) {
        executeOSSpecificCommands(osType, DownloadFilePath, null);
    }

    public static void createAndRunWindowsBatchFile(String DownloadFilePath) throws IOException {
        createAndRunWindowsBatchFile(DownloadFilePath, null);
    }

    public static void createAndRunWindowsBatchFile(String DownloadFilePath, String OpenedPicturePath) throws IOException {
        String batchContent = "@echo off\n" +
                        "timeout /t 3\n"
                        + "del \".\\" + CURRENT_JAR_NAME + "\"\n"
                        + "ren \"" + DownloadFilePath.substring(DownloadFilePath.lastIndexOf("/") + 1) + MainFileSuffix + "\" \"" + CURRENT_JAR_NAME + "\"\n" +
                        "cls\n"
                        + "\"" + System.getProperty("sun.boot.library.path") + "\\java.exe\" -cp \"" + CURRENT_JAR_NAME + ";lib\\*\" top.nserly.GUIStarter -Dsun.java2d.opengl=true ";
        if (OpenedPicturePath != null && !OpenedPicturePath.isBlank()) {
            batchContent = batchContent + "\"" + OpenedPicturePath + "\"";
        }

        Charset ansiCharset = Charset.forName("GBK");

        Path batchPath = Path.of("./replace.bat");
        Files.writeString(batchPath, batchContent, ansiCharset);

        batchContent = "start replace.bat";
        batchPath = Path.of("./runnable.bat");
        Files.writeString(batchPath, batchContent, ansiCharset);

        log.info("The script file is created!");
        log.info("Start running the script file and end the current software...");

        Runtime.getRuntime().exec(new String[]{"runnable.bat"});
        log.info("Program Termination!");

        System.exit(0);
    }

    public static void createAndRunLinuxShellScript(String DownloadFilePath) throws IOException {
        createAndRunLinuxShellScript(DownloadFilePath, null);
    }

    public static void createAndRunLinuxShellScript(String DownloadFilePath, String OpenedPicturePath) throws IOException {
        String shellContent =
                "sleep 1\n"
                        + "rm " + CURRENT_JAR_NAME + "\n"
                        + "mv " + DownloadFilePath.substring(DownloadFilePath.lastIndexOf("/") + 1) + MainFileSuffix + " " + CURRENT_JAR_NAME + "\n"
                        + "\"" + System.getProperty("sun.boot.library.path") + "\\java.exe\" -cp \"" + CURRENT_JAR_NAME + ";lib\\*\" top.nserly.GUIStarter -Dsun.java2d.opengl=true ";
        if (OpenedPicturePath != null && !OpenedPicturePath.isBlank()) {
            shellContent = shellContent + OpenedPicturePath;
        }
        Path shellPath = Path.of("./replace.sh");
        Files.write(shellPath, shellContent.getBytes());
        Files.setPosixFilePermissions(shellPath, PosixFilePermissions.fromString("rwx------"));
        log.info("The script file is created");
        log.info("Start running the script file and end the current software...");
        Runtime.getRuntime().exec(new String[]{"sh", "-c", "nohup sh ./replace.sh &"});
        log.info("Program Termination!");
        System.exit(0);
    }

}