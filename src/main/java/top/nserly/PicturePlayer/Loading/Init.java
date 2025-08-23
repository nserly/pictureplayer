package top.nserly.PicturePlayer.Loading;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class Init<KEY, VALUE> {
    private static final AtomicBoolean isInit = new AtomicBoolean();
    private final File f = new File("data/configuration.ch");
    private final Properties properties = new Properties();
    private boolean EnableAutoUpdate;
    private static final String[] createDirectory = {"data", "lib", "cache", "cache/PictureCache", "cache/thum", "download"};
    private static final String[] createFile = {"data/PictureCacheManagement.obj"};

    public static void init() {
        synchronized (isInit) {
            if (isInit.get()) return;
            File dire;
            try {
                for (String directory : createDirectory) {
                    dire = new File(directory);
                    if (!dire.exists()) {
                        if (!dire.mkdir())
                            log.warn("{} cannot mkdir", dire.getPath());
                    }
                }
                for (String file : createFile) {
                    dire = new File(file);
                    if (!dire.exists()) {
                        if (!dire.createNewFile())
                            log.warn("{} cannot create", dire.getPath());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            clearDirectory(new File("./download/"));
            clearDirectory(new File("replace.sh"));
            clearDirectory(new File("replace.bat"));
            clearDirectory(new File("runnable.bat"));
            isInit.set(true);
        }
    }

    public static void clearDirectory(File directory) {
        if (!directory.exists()) return;
        if (directory.isFile()) {
            if (!directory.delete())
                log.warn("{} cannot delete", directory.getPath());
            return;
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    clearDirectory(file);
                    if (!file.delete())
                        log.warn("{} cannot delete", file.getPath());
                    continue;
                }
                if (!file.delete())
                    log.warn("{} cannot delete", file.getPath());
            }
        }
    }

    public void run() {
        if (!isInit.get()) init();
        try {
            if (!f.exists()) {
                writer(setDefault());
            }
            properties.clear();
            properties.load(new BufferedReader(new FileReader(f)));
        } catch (IOException e) {
            log.error("Failed to read the configuration file");
        }
    }

    public boolean containsKey(KEY key) {
        return properties.containsKey(key);
    }

    public void setUpdate(boolean EnableAutoUpdate) {
        this.EnableAutoUpdate = EnableAutoUpdate;
    }

    public Properties getProperties() {
        return (Properties) properties.clone();
    }

    public void changeValue(KEY key, VALUE value) {
        properties.remove(key, value);
        properties.put(key, value);
        if (EnableAutoUpdate) store();
    }

    public void remove(KEY key, VALUE value) {
        properties.remove(key, value);
        if (EnableAutoUpdate) store();
    }

    public void update() {
        store();
    }

    @SafeVarargs
    public final void remove(KEY... key) {
        for (KEY i : key) {
            properties.remove(i);
        }
        if (EnableAutoUpdate) store();
    }

    public void writer(Map<?, ?> map) {
        if (!isInit.get()) init();
        properties.putAll(map);
        store();
    }

    public void writer(KEY key, VALUE value) {
        if (!isInit.get()) init();
        properties.put(key, value);
        store();
    }

    @DefaultArgs
    public Map<String, Object> setDefault() { // 修改这里的泛型为 <String, Object>
        HashMap<String, Object> hashMap = new HashMap<>();
        var method = DefaultArgs.class.getDeclaredMethods();
        for (Method i : method) {
            hashMap.put(i.getName(), i.getDefaultValue()); // 现在应该可以正确地添加键值对
        }
        return hashMap;
    }

    private void store() {
        if (!isInit.get()) init();
        try {
            properties.store(new BufferedWriter(new FileWriter(f)), "");
        } catch (Exception e) {
            log.error("Failed to save the configuration file");
        }
    }
}
