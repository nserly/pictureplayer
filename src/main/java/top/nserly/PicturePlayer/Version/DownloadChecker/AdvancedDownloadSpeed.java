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

package top.nserly.PicturePlayer.Version.DownloadChecker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.nserly.GUIStarter;
import top.nserly.PicturePlayer.Command.CommandHandle;
import top.nserly.PicturePlayer.Loading.Bundle;
import top.nserly.PicturePlayer.NComponent.Frame.DownloadUpdateFrame;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;
import top.nserly.SoftwareCollections_API.String.StringFormation;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class AdvancedDownloadSpeed {
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    public Thread DaemonUpdate;
    private final JProgressBar totalProgress;
    private final JProgressBar currentFileProgress;
    private final JLabel speedLabel;
    private final JLabel DownloadCounting;
    private final StringFormation TotalFormation = new StringFormation("{Speed} - {FinishedSize}/{TotalSize},{NeedTime}");
    private final StringFormation totalFormation;
    private final CheckAndDownloadUpdate downloadUpdate;
    private StringFormation currentFormation;
    private static final Logger logger = LoggerFactory.getLogger(AdvancedDownloadSpeed.class);

    public AdvancedDownloadSpeed(CheckAndDownloadUpdate downloadUpdate, JProgressBar totalProgress, JProgressBar currentFileProgress, JLabel speedLabel, JLabel DownloadCounting) {
        this.totalProgress = totalProgress;
        this.currentFileProgress = currentFileProgress;
        this.speedLabel = speedLabel;
        this.DownloadCounting = DownloadCounting;
        this.downloadUpdate = downloadUpdate;
        // 初始进度条
        totalProgress.setMaximum(downloadUpdate.getUpdateWebSide().size());
        totalProgress.setValue(0);
        totalProgress.setStringPainted(true);
        StringFormation formation = new StringFormation(DownloadCounting.getText());
        formation.add("total", String.valueOf(downloadUpdate.getUpdateWebSide().size()));
        totalFormation = new StringFormation(formation.getProcessingString());

        // 初始另一个进度条，用于显示当前文件的进度
        currentFileProgress.setMaximum(100);
        currentFileProgress.setValue(0);
        currentFileProgress.setStringPainted(true);

        DaemonUpdate = new Thread(() -> {
            var map = downloadUpdate.download();
            if (map == null) {
                DownloadUpdateFrame.downloadUpdateFrame.dispose();
                return;
            }
            String website = "";
            try {
                boolean isFound = false;
                for (String websites : downloadUpdate.FilePath) {
                    if (map.get(websites) != null) {
                        isFound = true;
                        website = websites;
                    }
                }
                if (!isFound) return;
                ArrayList<String> arrayList = new ArrayList<>();
                for (String s : map.keySet()) {
                    arrayList.add((String) map.get(s).getFirst());
                }
                arrayList.remove((String) map.get(downloadUpdate.MainFileWebSite).getFirst());
                CommandHandle.moveFileToLibDirectory(arrayList);
                CommandHandle.moveFileToDirectory((String) map.get(downloadUpdate.MainFileWebSite).getFirst());
                String osType = CommandHandle.detectOSType();
                String OpenedPicturePath = null;
                if (GUIStarter.main != null && GUIStarter.paintPicture != null && GUIStarter.paintPicture.imageCanvas != null) {
                    OpenedPicturePath = GUIStarter.paintPicture.imageCanvas.getPath();
                }

                CommandHandle.executeOSSpecificCommands(osType, (String) map.get(website).getFirst(), OpenedPicturePath);
            } catch (IOException | NoClassDefFoundError | ExceptionInInitializerError e) {
                logger.error(ExceptionHandler.getExceptionMessage(e));
                JOptionPane.showMessageDialog(DownloadUpdateFrame.downloadUpdateFrame, Bundle.getMessage("UpdateError_Content") + "\nCaused by:" + e, Bundle.getMessage("UpdateError_Title"), JOptionPane.ERROR_MESSAGE);
                DownloadUpdateFrame.downloadUpdateFrame.dispose();
                GUIStarter.main.setVisible(true);
            }
        });
        DaemonUpdate.start();
        simulateDownload();
    }


    private void simulateDownload() {
        final Timer totalTimer = new Timer(200, this::updateTotalProgress);
        final Timer fileTimer = new Timer(200, this::updateFileProgress);

        totalTimer.start();
        fileTimer.start();
    }

    private void updateTotalProgress(ActionEvent actionEvent) {
        int totalFiles = downloadUpdate.TotalDownloadingFile;
        int currentProgressFile = downloadUpdate.HaveDownloadedFile;

        totalProgress.setMaximum(downloadUpdate.getUpdateWebSide().size() * 100);

        if (downloadUpdate.CurrentFileDownloader.isCompleted()) {
            totalFormation.add("current", String.valueOf(totalFiles));
            DownloadCounting.setText(totalFormation.getProcessingString());
            totalProgress.setValue(totalProgress.getMaximum());
            stopTimer((Timer) actionEvent.getSource());
        } else {
            totalProgress.setValue((100 * currentProgressFile + (int) downloadUpdate.CurrentFileDownloader.getProgress()));
            totalFormation.add("current", String.valueOf(currentProgressFile + 1));
            DownloadCounting.setText(totalFormation.getProcessingString());
        }
    }

    private void updateFileProgress(ActionEvent actionEvent) {
        if (downloadUpdate.CurrentFileDownloader == null || downloadUpdate.CurrentFileDownloader.getProgress() >= 100) {
            handleFileCompletion(actionEvent);
        } else {
            currentFileProgress.setValue((int) downloadUpdate.CurrentFileDownloader.getProgress());
            updateSpeedAndSizeLabels();
        }
    }


    private void handleFileCompletion(ActionEvent actionEvent) {
        currentFileProgress.setValue(100);
        speedLabel.setText("0B/s - " + formatBytes(downloadUpdate.CurrentFileDownloader.getBytesRead()));
        stopTimer((Timer) actionEvent.getSource());
    }

    private void updateSpeedAndSizeLabels() {
        if (currentFormation == null) {
            currentFormation = TotalFormation;
        }

        currentFormation.add("Speed", formatSpeed(downloadUpdate.CurrentFileDownloader.getSpeedBytesPerSecond()));
        currentFormation.add("FinishedSize", formatBytes(downloadUpdate.CurrentFileDownloader.getBytesRead()));

        if (downloadUpdate.CurrentFileDownloader.getFileSize() != -1) {
            currentFormation.add("TotalSize", formatBytes(downloadUpdate.CurrentFileDownloader.getFileSize()));
            currentFormation.add("NeedTime", formatTimes(downloadUpdate.CurrentFileDownloader.getRemainingSeconds()));
        }

        speedLabel.setText(currentFormation.getProcessingString());
    }

    private void stopTimer(Timer timer) {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
    }


    private String formatSpeed(double bytesPerSecond) {
        return formatBytes(bytesPerSecond);
    }

    public static String formatBytes(double bytes) {
        if (bytes >= 1099511627776L) {
            return decimalFormat.format(bytes / 1099511627776L) + "TB";
        } else if (bytes >= 1073741824) {
            return decimalFormat.format(bytes / (1073741824)) + "GB";
        } else if (bytes >= 1048576) {
            return decimalFormat.format(bytes / (1048576)) + "MB";
        } else if (bytes >= 1024) {
            return decimalFormat.format(bytes / 1024) + "KB";
        } else {
            return decimalFormat.format(bytes) + "B";
        }
    }

    private String formatTimes(long seconds) {
        if (seconds >= 2592000) {
            return decimalFormat.format(seconds / 2592000) + "months" + formatTimes(seconds % 2592000);
        } else if (seconds >= 86400) {
            return decimalFormat.format(seconds / (86400)) + "days" + formatTimes(seconds % 86400);
        } else if (seconds >= 3600) {
            return decimalFormat.format(seconds / (3600)) + "h" + formatTimes(seconds % 3600);
        } else if (seconds >= 60) {
            return decimalFormat.format(seconds / 60) + "min" + formatTimes(seconds % 60);
        } else {
            return seconds + "s";
        }
    }
}