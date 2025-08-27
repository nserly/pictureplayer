package top.nserly.SoftwareCollections_API.OSInformation;

import com.sun.management.OperatingSystemMXBean;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SystemMonitor {
    //当前程序启动时间
    public static final String PROGRAM_START_TIME;
    //CPU核心数
    public static final int CPU_CORE_COUNT;
    //物理核心数（实际物理CPU核心数量）
    public static final int CPU_Physical_Core_Count;
    //逻辑线程数（包含超线程技术的逻辑处理器数量）
    public static final int CPU_Logical_Thread_Count;
    //操作系统名称
    public static final String OS_NAME;
    //java主目录
    public static final String JAVA_HOME;
    //java版本
    public static final String JAVA_VERSION;
    //用户主目录
    public static final String USER_HOME;
    //用户名
    public static final String USER_NAME;
    //cpu名称
    public static final String CPU_NAME;
    // 初始的程序总内存
    public static long JVM_Initialize_Memory;
    // 最大可用内存
    public static long JVM_Maximum_Free_Memory;
    //JVM已使用的内存
    public static long JVM_Used_Memory;
    //总的物理内存
    public static long Physical_Memory;
    // 剩余的物理内存
    public static long Physical_Free_Memory;
    //当前程序总线程数
    public static int Program_Thread_Count;
    //物理内存总占用
    public static long Physical_Used_Memory;
    //JVM内存使用率（x.xx%）没有%号
    public static double JVM_Memory_Usage;
    //总内存使用率（x.xx%）没有%号
    public static double Physical_Memory_Usage;

    //程序启动时间
    static {
        OS_NAME = System.getProperty("os.name");
        PROGRAM_START_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(ManagementFactory.getRuntimeMXBean().getStartTime()));
        CPU_CORE_COUNT = Runtime.getRuntime().availableProcessors();
        JAVA_HOME = System.getProperty("java.home");
        JAVA_VERSION = System.getProperty("java.version");
        USER_HOME = System.getProperty("user.home");
        USER_NAME = System.getProperty("user.name");
        SystemInfo systemInfo = new SystemInfo();
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        CPU_Physical_Core_Count = processor.getPhysicalProcessorCount();
        CPU_Logical_Thread_Count = processor.getLogicalProcessorCount();
        CPU_NAME = processor.getProcessorIdentifier().getName();
    }


    public static void getInformation() {
        // 获取操作系统和内存信息
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage memoryUsage = memoryMXBean.getHeapMemoryUsage();

        // 设置 JVM 内存信息
        JVM_Initialize_Memory = memoryUsage.getInit();
        JVM_Maximum_Free_Memory = memoryUsage.getMax();
        JVM_Used_Memory = memoryUsage.getUsed();

        // 设置物理内存信息
        Physical_Memory = osBean.getTotalMemorySize();
        Physical_Free_Memory = osBean.getFreeMemorySize();
        Physical_Used_Memory = Physical_Memory - Physical_Free_Memory;

        // 获得线程总数
        ThreadGroup rootThreadGroup = getRootThreadGroup(Thread.currentThread().getThreadGroup());
        Program_Thread_Count = rootThreadGroup.activeCount();

        // 计算内存使用百分比
        DecimalFormat df = new DecimalFormat("#.##");
        if (JVM_Maximum_Free_Memory > 0) {
            JVM_Memory_Usage = Double.parseDouble(df.format(100.0 * JVM_Used_Memory / JVM_Maximum_Free_Memory));
        } else {
            JVM_Memory_Usage = 0.0;
        }

        if (Physical_Used_Memory > 0) {
            Physical_Memory_Usage = Double.parseDouble(df.format(100.0 * Physical_Free_Memory / Physical_Used_Memory));
        } else {
            Physical_Memory_Usage = 0.0;
        }
    }

    // 递归方法来获取根 ThreadGroup
    private static ThreadGroup getRootThreadGroup(ThreadGroup group) {
        if (group.getParent() != null) {
            return getRootThreadGroup(group.getParent());
        }
        return group;
    }

    public static String convertSize(long sizeInBytes) {
        double size = sizeInBytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size = size / 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }
}

