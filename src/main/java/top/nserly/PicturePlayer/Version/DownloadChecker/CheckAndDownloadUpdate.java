package top.nserly.PicturePlayer.Version.DownloadChecker;


import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.nserly.GUIStarter;
import top.nserly.PicturePlayer.Exception.UpdateException;
import top.nserly.PicturePlayer.Loading.Bundle;
import top.nserly.PicturePlayer.NComponent.Frame.DownloadUpdateFrame;
import top.nserly.PicturePlayer.Version.PicturePlayerVersion;
import top.nserly.PicturePlayer.Version.VersionID;
import top.nserly.SoftwareCollections_API.DownloadFile.FileDownloader;
import top.nserly.SoftwareCollections_API.FileHandle.FileContents;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;

import javax.net.ssl.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class CheckAndDownloadUpdate {
    File f;
    @Setter
    String webSide;
    public List<String> downloadFileWebSite;

    public String MainFileWebSite;

    public ArrayList<String> DependenciesWebSite;
    public int TotalDownloadingFile;
    public int HaveDownloadedFile;
    public FileDownloader CurrentFileDownloader;
    private final static long startTime = System.currentTimeMillis();
    public List<String> FilePath = new ArrayList<>();
    //启用安全连接模式
    private static boolean EnableSecureConnection = true;
    public long NewVersionID = 0;
    public String NewVersionName = "";
    public VersionID versionID;
    private boolean StopToUpdate;
    // 定义选项内容
    private final Object[] options = {Bundle.getMessage("DownloadUpdateOptions_1st"), Bundle.getMessage("DownloadUpdateOptions_2nd"), Bundle.getMessage("DownloadUpdateOptions_3rd")};

    private static boolean isChecked = false;

    public static final ArrayList<String> DependenciesName = new ArrayList<>();
    //key:依赖名 value:依赖路径
    public static final TreeMap<String, String> DependenciesFilePath = new TreeMap<>();


    static {
        File[] files = new File("lib").listFiles();
        if (files != null)
            for (File file : files) {
                String fileName = file.getName();
                if (file.isFile() && fileName.endsWith(".jar")) {
                    String DependencyName = fileName.substring(0, fileName.lastIndexOf(".jar"));
                    DependenciesName.add(DependencyName);
                    DependenciesFilePath.put(DependencyName, file.getPath());
                }
            }
    }

    public CheckAndDownloadUpdate(String DownloadPath, String webSide) {
        this.webSide = webSide;
        f = new File(DownloadPath);
        if (f.exists() || f.isDirectory()) setDefaultDownloadPath();
    }

    public CheckAndDownloadUpdate(String webSide) {
        this.webSide = webSide;
        setDefaultDownloadPath();
    }

    public void setDefaultDownloadPath() {
        f = new File("./download/" + startTime + "/");
    }

    public static void secureConnection(boolean Enable) {
        if (EnableSecureConnection != Enable) {
            if (!Enable) {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }

                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }

                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }
                        }
                };
                try {
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, trustAllCerts, new SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                } catch (KeyManagementException | NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
                log.warn("SSL validation is turned off and your computer may be vulnerable!");
            } else {
                try {
                    //重新创建默认的SSLContext
                    SSLContext sc = SSLContext.getInstance("TLS");
                    sc.init(null, null, null);
                    //设置默认的SSLSocketFactory
                    SSLSocketFactory ssf = sc.getSocketFactory();
                    HttpsURLConnection.setDefaultSSLSocketFactory(ssf);
                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    log.error(ExceptionHandler.getExceptionMessage(e));
                    return;
                }
                log.info("SSL validation is enabled");
            }
        }
        EnableSecureConnection = Enable;
    }

    //更新最新版本
    public List<String> getUpdateWebSide() {
        StopToUpdate = false;
        if (!isChecked) {
            try {
                return checkIfTheLatestVersion() ? downloadFileWebSite : null;
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return downloadFileWebSite;
    }

    //检查是否存在最新版本
    public boolean checkIfTheLatestVersion() throws IOException {
        StopToUpdate = false;
        isChecked = true;
        log.info("Checking version...");

        AtomicReference<IOException> exception = new AtomicReference<>();
        FileDownloader fileDownloader = new FileDownloader(webSide, f.getPath());
        fileDownloader.setDownloadErrorHandler((e, f) -> exception.set(e));
        fileDownloader.startDownload();
        if (exception.get() != null) throw exception.get();
        versionID = VersionID.gson.fromJson(FileContents.read(fileDownloader.getFinalPath()), VersionID.class);
        File file = new File(fileDownloader.getFinalPath());
        if (!file.delete()) log.warn("{} cannot delete", file.getPath());
        if (versionID != null) {
            NewVersionID = Long.parseLong(VersionID.getString(versionID.getNormalVersionID(), versionID.getSpecialFields()));
        }
        if (versionID != null) {
            NewVersionName = VersionID.getString(versionID.getNormalVersion(), versionID.getSpecialFields());
        }

        if (versionID != null) {
            MainFileWebSite = VersionID.getString(versionID.getNormalVersionMainFile(), versionID.getSpecialFields());
        }
        DependenciesWebSite = new ArrayList<>();
        TreeMap<String, String> cache = null;
        if (versionID != null) {
            cache = versionID.getNormalDependencies();
        }

        if (cache != null)
            for (String value : cache.values()) {
                String dependenciesWebsite = VersionID.getString(value, versionID.getSpecialFields());
                String dependenciesName = dependenciesWebsite.replace("\\", "/");
                dependenciesName = dependenciesName.substring(dependenciesName.lastIndexOf("/") + 1, dependenciesName.lastIndexOf(".jar"));
                if (!DependenciesName.contains(dependenciesName))
                    DependenciesWebSite.add(VersionID.getString(dependenciesWebsite, versionID.getSpecialFields()));
            }
        downloadFileWebSite = new ArrayList<>(DependenciesWebSite);
        downloadFileWebSite.add(MainFileWebSite);

        if (NewVersionID <= Long.parseLong(PicturePlayerVersion.getVersionID())) {
            log.info("You are using the latest version: {}", PicturePlayerVersion.getVersionID());
            return false;
        }
        log.info("New version found: {} ({})", NewVersionID, NewVersionName);
        log.info("Download file web site: {}", downloadFileWebSite);
        if (downloadFileWebSite.isEmpty()) {
            log.warn("No files to download!");
            return false;
        }
        return true;
    }

    //一键下载所有文件(Map(Key:下载网站,Value:List[0]:文件存放路径;[1]下载类))[调用此方法时，推进使用新线程，否则窗体可能会无相应]
    public Map<String, ArrayList<?>> download(List<String> downloadWebSide) {
        StopToUpdate = false;
        Map<String, ArrayList<?>> finalA = new HashMap<>();
        if (downloadWebSide == null) return null;
        int index = 0;
        TotalDownloadingFile = downloadWebSide.size();

        for (String down : downloadWebSide) {
            FilePath.add(down);
            HaveDownloadedFile = index;
            switch (download(down, finalA, false)) {
                case 1 -> {
                    TotalDownloadingFile--;
                    continue;
                }
                case 2 -> {
                    return null;
                }
            }
            index++;
        }
        if (finalA.isEmpty()) return null;
        log.info("Download completed!");
        return finalA;
    }

    //返回值：0.下载完成 1.跳过当前文件 2.取消下载
    private int download(String down, Map<String, ArrayList<?>> finalA, boolean isTry) {
        try {
            if (StopToUpdate) {
                throw new UpdateException("Update ended,cause of User terminated software update");
            }
            if (!isTry) CurrentFileDownloader = new FileDownloader(down, f.getPath());
            CurrentFileDownloader.setDownloadErrorHandler((e, fileDownloader) -> {
                if (exceptionHandling(e) == 0) {
                    download(down, finalA, true);
                } else {
                    CurrentFileDownloader.stopDownload();
                    DownloadUpdateFrame.downloadUpdateFrame.dispose();
                    GUIStarter.main.setVisible(true);
                    stopToUpdate();
                }
            });
            log.info("Downloading {}", down);
            if (!EnableSecureConnection) log.warn("The connection is not secure from {}!", down);
            CurrentFileDownloader.startDownload();
            ArrayList<Object> list = new ArrayList<>();
            String cache = CurrentFileDownloader.getFinalPath();
            if (StopToUpdate) {
                throw new UpdateException("Update ended,cause of User terminated software update");
            }
            if (cache != null) {
                list.add(cache);
                list.add(CurrentFileDownloader);
                finalA.put(down, list);
            }
        } catch (UpdateException e) {
            log.error(e.getMessage());
            return 2;
        }
        if (CurrentFileDownloader.isCompleted())
            return 0;
        else
            return 1;

    }

    //一键下载所有文件(Map(Key:下载网站,Value:List[0]:文件存放路径;[1]下载类))[调用此方法时，推进使用新线程，否则窗体可能会无相应]
    public Map<String, ArrayList<?>> download() {
        StopToUpdate = false;
        return download(getUpdateWebSide());
    }

    //下载描述文件List[0]:文件存放路径;[1]下载类[调用此方法时，推进使用新线程，否则窗体可能会无相应]
    public List<?> downloadDescribe() {
        log.info("Start downloading describe version file...");
        StopToUpdate = false;
        String DescribeFileWebSide = VersionID.getString(versionID.getNormalVersionDescribe(), versionID.getSpecialFields());
        if (!(DescribeFileWebSide == null) && !DescribeFileWebSide.isEmpty()) {
            return download(Collections.singletonList(DescribeFileWebSide)).get(DescribeFileWebSide);
        }
        return null;
    }

    //终止更新
    public void stopToUpdate() {
        StopToUpdate = true;
        CurrentFileDownloader.stopDownload();
    }

    private int exceptionHandling(IOException e) {
        log.error(e.getMessage());
        return JOptionPane.showOptionDialog(null, Bundle.getMessage("DownloadUpdateError_Content") + "\n" + e, Bundle.getMessage("DownloadUpdateError_Title"), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options);
    }
}
