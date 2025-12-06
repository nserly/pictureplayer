package top.nserly.PicturePlayer.Utils.ImageManager.Blur.Implements;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jocl.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jocl.CL.*;

@Slf4j
public class OpenCLSupportChecker {

    private static final int BUFFER_SIZE = 1024; // 扩大缓冲区避免设备名称截断
    private static final byte[] buffer = new byte[BUFFER_SIZE];
    private static final ArrayList<String> Supported_GPU_Device_Name_List = new ArrayList<>();
    private static final ArrayList<String> Supported_CPU_Device_Name_List = new ArrayList<>();
    @Getter
    private static int Supported_GPU_Device = 0; // 初始化为0，用于累加
    @Getter
    private static int Supported_CPU_Device = 0; // 初始化为0，用于累加

    /**
     * 检查系统是否支持OpenCL，同时收集所有平台的设备信息
     */
    public static boolean isOpenCLSupported() {
        // 每次检查前清空历史数据
        Supported_GPU_Device_Name_List.clear();
        Supported_CPU_Device_Name_List.clear();
        Supported_GPU_Device = 0;
        Supported_CPU_Device = 0;

        try {
            CL.setExceptionsEnabled(true);

            int[] numPlatforms = new int[1];
            clGetPlatformIDs(0, null, numPlatforms);
            if (numPlatforms[0] == 0) {
                log.info("No OpenCL platforms found");
                return false;
            }

            cl_platform_id[] platforms = new cl_platform_id[numPlatforms[0]];
            clGetPlatformIDs(platforms.length, platforms, null);

            boolean hasSupportedDevice = false;
            // 遍历所有平台，确保收集全部设备信息
            for (cl_platform_id platform : platforms) {
                boolean platformHasGPU = checkDeviceType(platform, CL_DEVICE_TYPE_GPU);
                boolean platformHasCPU = checkDeviceType(platform, CL_DEVICE_TYPE_CPU);
                if (platformHasGPU || platformHasCPU) {
                    hasSupportedDevice = true;
                }
            }

            if (!hasSupportedDevice) {
                log.info("No OpenCL devices (GPU/CPU) found on any platform");
            }
            return hasSupportedDevice;

        } catch (Exception e) {
            log.error("OpenCL check failed: {}", e.getMessage());
            return false;
        } finally {
            CL.setExceptionsEnabled(false);
        }
    }

    public static List<String> getSupported_GPU_Device_Name_List() {
        return new ArrayList<>(Supported_GPU_Device_Name_List); // 返回不可修改的副本
    }

    public static List<String> getSupported_CPU_Device_Name_List() {
        return new ArrayList<>(Supported_CPU_Device_Name_List); // 返回不可修改的副本
    }

    public static List<String> getSupported_Device_Name() {
        if (!Supported_GPU_Device_Name_List.isEmpty())
            return getSupported_GPU_Device_Name_List();
        return getSupported_CPU_Device_Name_List();
    }

    /**
     * 检查指定平台上的指定类型设备，并累加设备计数
     */
    private static boolean checkDeviceType(cl_platform_id platform, long deviceType) {
        try {
            int[] numDevices = new int[1];
            clGetDeviceIDs(platform, deviceType, 0, null, numDevices);
            if (numDevices[0] <= 0) {
                return false;
            }

            cl_device_id[] devices = new cl_device_id[numDevices[0]];
            clGetDeviceIDs(platform, deviceType, devices.length, devices, null);

            for (cl_device_id device : devices) {
                clearBuffer();
                clGetDeviceInfo(device, CL_DEVICE_NAME, BUFFER_SIZE, Pointer.to(buffer), null);
                String deviceName = new String(buffer).trim();
                if (deviceType == CL_DEVICE_TYPE_GPU) {
                    Supported_GPU_Device_Name_List.add(deviceName);
                } else if (deviceType == CL_DEVICE_TYPE_CPU) {
                    Supported_CPU_Device_Name_List.add(deviceName);
                }
                log.debug("Found {} device: {}",
                        deviceType == CL_DEVICE_TYPE_GPU ? "GPU" : "CPU",
                        deviceName);
            }

            // 累加设备数量（核心修复点）
            if (deviceType == CL_DEVICE_TYPE_GPU) {
                Supported_GPU_Device += numDevices[0];
            } else if (deviceType == CL_DEVICE_TYPE_CPU) {
                Supported_CPU_Device += numDevices[0];
            }

            log.info("Found {} {} devices on platform",
                    numDevices[0],
                    deviceType == CL_DEVICE_TYPE_GPU ? "GPU" : "CPU");
            return true;

        } catch (CLException e) {
            log.warn("Failed to check {} devices on platform (error code: {}): {}",
                    deviceType == CL_DEVICE_TYPE_GPU ? "GPU" : "CPU",
                    e.getStatus(),
                    e.getMessage());
            return false;
        }
    }

    /**
     * 获取OpenCL平台和设备的详细信息
     */
    public static String getOpenCLInfo() {
        StringBuilder info = new StringBuilder();
        try {
            CL.setExceptionsEnabled(true);

            int[] numPlatforms = new int[1];
            clGetPlatformIDs(0, null, numPlatforms);
            if (numPlatforms[0] == 0) {
                return "No OpenCL platforms found";
            }

            cl_platform_id[] platforms = new cl_platform_id[numPlatforms[0]];
            clGetPlatformIDs(platforms.length, platforms, null);

            info.append("Found ").append(numPlatforms[0]).append(" OpenCL platform(s)\n");

            for (int i = 0; i < platforms.length; i++) {
                info.append("\nPlatform ").append(i).append(":\n");
                appendPlatformInfo(info, platforms[i]);
                appendDeviceInfo(info, platforms[i], CL_DEVICE_TYPE_GPU, "GPU Devices");
                appendDeviceInfo(info, platforms[i], CL_DEVICE_TYPE_CPU, "CPU Devices");
            }

            return info.toString();

        } catch (Exception e) {
            return "Error getting OpenCL info: " + e.getMessage();
        } finally {
            CL.setExceptionsEnabled(false);
        }
    }

    private static void clearBuffer() {
        Arrays.fill(buffer, (byte) 0);
    }

    private static void appendPlatformInfo(StringBuilder info, cl_platform_id platform) {
        clearBuffer();
        clGetPlatformInfo(platform, CL_PLATFORM_NAME, BUFFER_SIZE, Pointer.to(buffer), null);
        info.append("  Name: ").append(new String(buffer).trim()).append("\n");

        clearBuffer();
        clGetPlatformInfo(platform, CL_PLATFORM_VENDOR, BUFFER_SIZE, Pointer.to(buffer), null);
        info.append("  Vendor: ").append(new String(buffer).trim()).append("\n");

        clearBuffer();
        clGetPlatformInfo(platform, CL_PLATFORM_VERSION, BUFFER_SIZE, Pointer.to(buffer), null);
        info.append("  Version: ").append(new String(buffer).trim()).append("\n");
    }

    private static void appendDeviceInfo(StringBuilder info, cl_platform_id platform, long deviceType, String deviceTypeName) {
        int[] numDevices = new int[1];
        try {
            clGetDeviceIDs(platform, deviceType, 0, null, numDevices);
            if (numDevices[0] > 0) {
                cl_device_id[] devices = new cl_device_id[numDevices[0]];
                clGetDeviceIDs(platform, deviceType, devices.length, devices, null);

                info.append("  ").append(deviceTypeName).append(": ").append(numDevices[0]).append("\n");
                for (cl_device_id device : devices) {
                    clearBuffer();
                    clGetDeviceInfo(device, CL_DEVICE_NAME, BUFFER_SIZE, Pointer.to(buffer), null);
                    info.append("    ").append(new String(buffer).trim()).append("\n");
                }
            }
        } catch (CLException e) {
            info.append("  ").append(deviceTypeName).append(": Error checking devices (code: ").append(e.getStatus()).append(")\n");
        }
    }
}