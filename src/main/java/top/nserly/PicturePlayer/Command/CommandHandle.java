package top.nserly.PicturePlayer.Command;

import lombok.extern.slf4j.Slf4j;
import top.nserly.GUIStarter;
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
    private static final String CURRENT_JAR_PARENT_PATH;

    private static final String MainFileSuffix;

    static {
        ClassLoader classLoader = CommandHandle.class.getClassLoader();
        URL url = classLoader.getResource(CommandHandle.class.getName().replace('.', '/') + ".class");
        if (url == null) throw new RuntimeException("Can't find the current JAR file");
        String path = url.getPath();
        // 获取当前JAR文件的路径
        CURRENT_JAR_PATH = path.substring(path.indexOf("/") + 1, path.lastIndexOf("!"));
        // 获取当前JAR文件的名称
        CURRENT_JAR_NAME = CURRENT_JAR_PATH.substring(CURRENT_JAR_PATH.lastIndexOf("/") + 1);
        // 生成一个随机的主文件后缀
        MainFileSuffix = RandomString.getRandomString(5);
        // 获取当前JAR文件的父路径
        CURRENT_JAR_PARENT_PATH = CURRENT_JAR_PATH.substring(0, CURRENT_JAR_PATH.lastIndexOf("/"));
        // 输出当前JAR文件的路径和名称
        log.info("Current JAR file path: {}", CURRENT_JAR_PATH);
        // 输出当前JAR文件的名称
        log.info("Current JAR file name: {}", CURRENT_JAR_NAME);
        // 输出当前JAR文件的父路径
        log.info("Current JAR file parent path: {}", CURRENT_JAR_PARENT_PATH);
        // 输出主文件后缀
        log.info("Main file suffix: {}", MainFileSuffix);
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

    public static void createAndRunWindowsBatchFile(String downloadFilePath, String openedPicturePath) throws IOException {
        // 用File类可靠提取文件名（自动处理Windows/Linux分隔符）
        String fileName = new File(downloadFilePath).getName();

        // 构建批处理内容（修复路径、命令参数）
        String batchContent = "@echo off\n"
                + "timeout /t 3 /nobreak >nul 2>&1\n"  // 禁止用户中断，隐藏输出
                + "if exist \".\\" + CURRENT_JAR_NAME + "\" (\n"  // 检查旧文件是否存在
                + "  del /f /q \".\\" + CURRENT_JAR_NAME + "\"\n"  // 强制删除
                + ")\n"
                + "ren \"" + fileName + MainFileSuffix + "\" \"" + CURRENT_JAR_NAME + "\"\n"
                + "cls\n"
                // 使用java.home获取可靠的Java路径
                + "\"" + System.getProperty("java.home") + "\\bin\\java.exe\" "
                + "-Dsun.java2d.opengl=true -DNUpdate=true "
                + "-cp \"" + CURRENT_JAR_NAME + ";lib\\*\" top.nserly.GUIStarter ";

        if (openedPicturePath != null && !openedPicturePath.isBlank()) {
            batchContent += "\"" + openedPicturePath + "\"";  // 带空格参数需引号
        }

        // Windows批处理默认编码为GBK（中文系统）
        Charset ansiCharset = Charset.forName("GBK");

        // 用File.separator统一路径分隔符
        Path batchPath = Path.of(CURRENT_JAR_PARENT_PATH + File.separator + "replace.bat");
        Files.writeString(batchPath, batchContent, ansiCharset);

        log.info("The script file is created!");
        log.info("Start running the script file and end the current software...");

        // 执行批处理时，路径带空格需用引号包裹
        String batchFullPath = batchPath.toString();
        Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "\"\"", batchFullPath});
        // 注意："start"命令后加"\"\""是为了避免将路径当作窗口标题

        log.info("Program Termination!");
        GUIStarter.exitAndRecord();
    }

    public static void createAndRunLinuxShellScript(String DownloadFilePath) throws IOException {
        createAndRunLinuxShellScript(DownloadFilePath, null);
    }

    public static void createAndRunLinuxShellScript(String downloadFilePath, String openedPicturePath) throws IOException {
        // 提取下载文件的文件名（处理路径分隔符）
        String fileName = new File(downloadFilePath).getName();

        String shellContent =
                "sleep 1\n"
                        + "rm " + CURRENT_JAR_NAME + "\n"
                        + "mv " + fileName + MainFileSuffix + " " + CURRENT_JAR_NAME + "\n"
                        + "java -Dsun.java2d.opengl=true -DNUpdate=true -cp " + CURRENT_JAR_NAME + ":lib/* top.nserly.GUIStarter ";

        if (openedPicturePath != null && !openedPicturePath.isBlank()) {
            shellContent += openedPicturePath;
        }

        Path shellPath = Path.of("./replace.sh");
        Files.writeString(shellPath, shellContent);
        Files.setPosixFilePermissions(shellPath, PosixFilePermissions.fromString("rwx------"));

        log.info("The script file is created");
        log.info("Start running the script file and end the current software...");
        // 执行脚本（nohup输出重定向到/dev/null避免生成nohup.out）
        Runtime.getRuntime().exec(new String[]{"sh", "-c", "nohup sh ./replace.sh >/dev/null 2>&1 &"});
        log.info("Program Termination!");
        GUIStarter.exitAndRecord();
    }

}