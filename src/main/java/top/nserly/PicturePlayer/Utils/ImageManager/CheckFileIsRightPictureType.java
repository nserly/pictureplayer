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

package top.nserly.PicturePlayer.Utils.ImageManager;

import lombok.Getter;
import top.nserly.PicturePlayer.Utils.ImageManager.Info.GetImageInformation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CheckFileIsRightPictureType {
    ArrayList<File> CheckFileList = new ArrayList<>();
    ArrayList<File> FinishedList = new ArrayList<>();
    ArrayList<File> UnfinishedList = new ArrayList<>();
    @Getter
    ArrayList<File> NotImageList = new ArrayList<>();
    @Getter
    ArrayList<File> ImageList = new ArrayList<>();

    public CheckFileIsRightPictureType(File... files) {
        add(files);
    }

    public CheckFileIsRightPictureType(List<File> files) {
        add(files);
    }

    public void add(File... files) {
        CheckFileList.addAll(Arrays.asList(files));
        UnfinishedList.addAll(Arrays.asList(files));
    }

    public void add(List<File> files) {
        CheckFileList.addAll(files);
        UnfinishedList.addAll(files);
    }

    public void statistics() {
        for (File file : UnfinishedList) {
            FinishedList.add(file);
            if (GetImageInformation.isImageFile(file)) {
                ImageList.add(file);
            } else {
                NotImageList.add(file);
            }
        }
        UnfinishedList.clear();
    }

    public int getImageCount() {
        return ImageList.size();
    }

    public int getNotImageCount() {
        return NotImageList.size();
    }

    public void clear() {
        NotImageList.clear();
        ImageList.clear();
        CheckFileList.clear();
        FinishedList.clear();
        UnfinishedList.clear();
    }

    public boolean isFinished() {
        return UnfinishedList.isEmpty();
    }

    public String filePathToString(String separator, File... files) {
        StringBuilder sb = new StringBuilder();
        boolean isFist = true;
        for (File file : files) {
            if (!isFist) {
                sb.append(separator);
            }
            sb.append(file.getAbsolutePath());
            isFist = false;
        }
        return sb.toString();
    }

    public String filePathToString(String separator, ArrayList<File> files) {
        StringBuilder sb = new StringBuilder();
        boolean isFist = true;
        for (File file : files) {
            if (!isFist) {
                sb.append(separator);
            }
            sb.append(file.getAbsolutePath());
            isFist = false;
        }
        return sb.toString();
    }
}
