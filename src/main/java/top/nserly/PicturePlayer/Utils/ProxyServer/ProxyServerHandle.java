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

package top.nserly.PicturePlayer.Utils.ProxyServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;

import java.io.*;
import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;

public class ProxyServerHandle {
    public final File savePath;
    private static final Logger log = LoggerFactory.getLogger(ProxyServerHandle.class);
    private ProxyServerFileManage fileManager;

    //推荐路径：data\\ProxyServerMenu.pxs
    public ProxyServerHandle(String savePath) {
        this.savePath = new File(savePath);
        fileManager = new ProxyServerFileManage();
        if (!this.savePath.exists()) {
            try {
                if(!this.savePath.createNewFile())
                    log.warn("{} cannot create",this.savePath.getPath());
            } catch (IOException e) {
                log.error(ExceptionHandler.getExceptionMessage(e));
            }
        }
    }

    public void refresh() {
        try {
            FileInputStream fileIn = new FileInputStream(savePath);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            fileManager = (ProxyServerFileManage) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException e) {
            log.error(ExceptionHandler.getExceptionMessage(e));
            fileManager = new ProxyServerFileManage();
            save();
        }

    }

    /**
     * 获取当前选中的代理服务器名称
     *
     * @return 代理服务器名称
     */
    public String getCurrentSelectionProxyServerName() {
        return fileManager.currentSelectedProxyServerName;
    }

    /**
     * 设置当前选中的代理服务器名称
     *
     * @param ProxyServerName 代理服务器名称
     */
    public void setCurrentSelectionProxyServerName(String ProxyServerName) {
        if (containsProxyServerName(ProxyServerName))
            fileManager.currentSelectedProxyServerName = ProxyServerName;
    }

    public void delete(String ProxyServerName) {
        fileManager.treemap.remove(ProxyServerName);
    }

    public void add(String ProxyServerName, String ProxyServerAddress) {
        if (fileManager.treemap.containsKey(ProxyServerName))
            fileManager.treemap.replace(ProxyServerName, ProxyServerAddress);
        else
            fileManager.treemap.put(ProxyServerName, ProxyServerAddress);
    }

    public boolean containsProxyServerAddress(String ProxyServerAddress) {
        return fileManager.treemap.containsValue(ProxyServerAddress);
    }

    public boolean containsProxyServerName(String ProxyServerName) {
        return fileManager.treemap.containsKey(ProxyServerName);
    }

    public TreeMap<String, String> getAllProxyServers() {
        return fileManager.treemap;
    }

    public Set<String> getAllProxyServerNames() {
        return fileManager.treemap.keySet();
    }

    public Collection<String> getAllProxyServerAddress() {
        return fileManager.treemap.values();
    }

    public String getProxyServerAddress(String ProxyServerName) {
        return fileManager.treemap.get(ProxyServerName);
    }

    public void save() {
        if (!containsProxyServerName(fileManager.currentSelectedProxyServerName))
            fileManager.currentSelectedProxyServerName = "";
        try {
            FileOutputStream fileOut = new FileOutputStream(savePath);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(fileManager);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            log.error(ExceptionHandler.getExceptionMessage(e));
        }
    }
}
