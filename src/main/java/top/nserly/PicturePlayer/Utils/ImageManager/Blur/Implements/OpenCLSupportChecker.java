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

    private static final int BUFFER_SIZE = 128;
    private static final byte[] buffer = new byte[BUFFER_SIZE];
    private static final ArrayList<String> Supported_GPU_Device_Name_List = new ArrayList<>();
    private static final ArrayList<String> Supported_CPU_Device_Name_List = new ArrayList<>();
    @Getter
    private static int Supported_GPU_Device;
    @Getter
    private static int Supported_CPU_Device;

    /**
     * 检查系统是否支持OpenCL
     */
    public static boolean isOpenCLSupported() {
        Supported_GPU_Device_Name_List.clear();
        Supported_CPU_Device_Name_List.clear();
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

            for (cl_platform_id platform : platforms) {
                if (checkDeviceType(platform, CL_DEVICE_TYPE_GPU) || checkDeviceType(platform, CL_DEVICE_TYPE_CPU)) {
                    return true;
                }
            }

            log.info("No OpenCL devices found on any platform");
            return false;
        } catch (Exception e) {
            log.info("OpenCL not supported: {}", e.getMessage());
            return false;
        } finally {
            CL.setExceptionsEnabled(false);
        }
    }

    public static List<String> getSupported_GPU_Device_Name_List() {
        return Supported_GPU_Device_Name_List.stream().toList();
    }

    public static List<String> getSupported_CPU_Device_Name_List() {
        return Supported_CPU_Device_Name_List.stream().toList();
    }

    private static boolean checkDeviceType(cl_platform_id platform, long deviceType) {
        try {
            int[] numDevices = new int[1];
            clGetDeviceIDs(platform, deviceType, 0, null, numDevices);
            if (numDevices[0] > 0) {
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
                }

                if (deviceType == CL_DEVICE_TYPE_GPU) {
                    Supported_GPU_Device = numDevices[0];
                } else if (deviceType == CL_DEVICE_TYPE_CPU) {
                    Supported_CPU_Device = numDevices[0];
                }

                log.info("Found OpenCL {} device on platform", deviceType == CL_DEVICE_TYPE_GPU ? "GPU" : "CPU");
                return true;
            }
        } catch (CLException e) {
            // 忽略错误，继续检查下一个设备类型
        }
        return false;
    }


    /**
     * 获取OpenCL平台和设备信息
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
        clGetPlatformInfo(platform, CL_PLATFORM_NAME, BUFFER_SIZE, Pointer.to(buffer), null);
        info.append("  Name: ").append(new String(buffer).trim()).append("\n");
        clearBuffer();

        clGetPlatformInfo(platform, CL_PLATFORM_VENDOR, BUFFER_SIZE, Pointer.to(buffer), null);
        info.append("  Vendor: ").append(new String(buffer).trim()).append("\n");
        clearBuffer();

        clGetPlatformInfo(platform, CL_PLATFORM_VERSION, BUFFER_SIZE, Pointer.to(buffer), null);
        info.append("  Version: ").append(new String(buffer).trim()).append("\n");
        clearBuffer();
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
                    clGetDeviceInfo(device, CL_DEVICE_NAME, BUFFER_SIZE, Pointer.to(buffer), null);
                    info.append("    ").append(new String(buffer).trim()).append("\n");
                    clearBuffer();
                }
            }
        } catch (CLException e) {
            // 忽略错误
        }
    }
}