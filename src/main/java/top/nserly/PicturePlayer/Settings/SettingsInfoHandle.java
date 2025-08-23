package top.nserly.PicturePlayer.Settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.nserly.GUIStarter;
import top.nserly.PicturePlayer.Loading.DefaultArgs;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class SettingsInfoHandle {
    public static final Map<String, String> DefaultData = new HashMap<>();
    public final HashMap<String, String> CurrentData = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(SettingsInfoHandle.class);

    //初始化
    static {
        try {
            Class<?> clazz = DefaultArgs.class;
            // 获取注解中所有成员的方法
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (!method.getName().equals("annotationType") && !method.isDefault()) { // 忽略内部的annotationType方法
                    String defaultValue = method.getDefaultValue().toString();
                    DefaultData.put(method.getName(), defaultValue);
                }
            }
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
        }
    }

    public SettingsInfoHandle() {
        reFresh();
    }

    //重置默认设置
    public void setDefault() {
        CurrentData.clear();
        CurrentData.putAll(DefaultData);
    }

    //恢复之前设置（保存时）
    public void reFresh() {
        setDefault();
        try {
            GUIStarter.init.run();
            Properties properties = GUIStarter.init.getProperties();
            for (Object obj : properties.keySet()) {
                if (DefaultData.containsKey((String) obj)) {
                    CurrentData.replace((String) obj, (String) properties.get(obj));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    //获取某键的对应布尔值
    public static boolean getBoolean(String Description, Map<?, ?> map) {
        if (map == null || !map.containsKey(Description)) {
            if (DefaultData.containsKey(Description)) return Boolean.parseBoolean(DefaultData.get(Description));
            return false;
        }
        return Boolean.parseBoolean(map.get(Description).toString().trim().toLowerCase());
    }

    //获取某键的对应浮点值
    public static double getDouble(String Description, Map<?, ?> map) {
        return getDouble(Description, map, -65, 150);
    }

    //获取某键的对应int值
    public static int getInt(String Description, Map<?, ?> map) {
        if (map == null || !map.containsKey(Description)) {
            if (DefaultData.containsKey(Description)) return Integer.parseInt(DefaultData.get(Description));
            return -1;
        }
        return Integer.parseInt(map.get(Description).toString().trim().toLowerCase());
    }

    //获取某建的对应布尔值
    private static double getDouble(String Description, Map<?, ?> map, double min, double max) {
        if (min > max) {
            double temp = max;
            max = min;
            min = temp;
        }
        String cache = map.get(Description).toString().replace(" ", "");
        double result = 0;
        try {
            result = Double.parseDouble(cache);
        } catch (NumberFormatException e) {
            logger.error(e.getMessage());
        }
        if (result > max) {
            result = max;
        }
        if (result < min) {
            result = min;
        }
        return result;
    }

    //保存设置
    public void save() {
        GUIStarter.init.writer(CurrentData);
    }

}
