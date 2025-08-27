package top.nserly.PicturePlayer.Utils.ImageManager.Blur.Implements;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jocl.*;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.jocl.CL.*;

@Slf4j
public class OpenCLBlurProcessor implements AutoCloseable {
    private static boolean isSupportedOpenCL;
    // 同步控制
    private static final ReentrantLock staticLock = new ReentrantLock();
    private static final AtomicBoolean staticInitialized = new AtomicBoolean(false);
    private static final AtomicInteger instanceCount = new AtomicInteger(0);
    private static final AtomicBoolean staticInitializing = new AtomicBoolean(false);
    private static final Thread GetIsSupportedOpenCL = new Thread(() -> isSupportedOpenCL = OpenCLSupportChecker.isOpenCLSupported());
    // 用于同步初始化过程
    private static final CountDownLatch initLatch = new CountDownLatch(1);

    // 实例状态
    private final AtomicBoolean instanceClosed = new AtomicBoolean(false);

    // 优化后的 OpenCL 内核代码
    private static final String KERNEL_SOURCE = """
            __kernel void gaussianBlur(
                __global const int* input,
                __global int* output,
                const int width,
                const int height,
                const int kernelSize,
                const float sigma
            ) {
                int x = get_global_id(0);
                int y = get_global_id(1);
            
                if (x >= width || y >= height) {
                    return;
                }
            
                int radius = kernelSize / 2;
                float4 sum = (float4)(0.0f, 0.0f, 0.0f, 0.0f);
                float weightSum = 0.0f;
            
                // 预计算高斯核
                for (int ky = -radius; ky <= radius; ky++) {
                    for (int kx = -radius; kx <= radius; kx++) {
                        int nx = clamp(x + kx, 0, width - 1);
                        int ny = clamp(y + ky, 0, height - 1);
                        int pixelIdx = ny * width + nx;
                        int pixel = input[pixelIdx];
            
                        // 提取颜色分量并归一化
                        float4 color;
                        color.w = ((pixel >> 24) & 0xFF) / 255.0f;
                        color.x = ((pixel >> 16) & 0xFF) / 255.0f;
                        color.y = ((pixel >> 8) & 0xFF) / 255.0f;
                        color.z = (pixel & 0xFF) / 255.0f;
            
                        // 处理完全透明像素
                        if (color.w < 0.001f) {
                            color = (float4)(0.0f, 0.0f, 0.0f, 0.0f);
                        }
            
                        // 计算高斯权重
                        float distanceSq = (float)(kx * kx + ky * ky);
                        float weight = exp(-distanceSq / (2.0f * sigma * sigma));
            
                        // 累积加权颜色
                        sum += color * weight;
                        weightSum += weight;
                    }
                }
            
                // 归一化并转换回整数
                if (weightSum > 0.0f) {
                    sum /= weightSum;
                }
            
                int finalA = clamp((int)(sum.w * 255.0f), 0, 255);
                int finalR = clamp((int)(sum.x * 255.0f), 0, 255);
                int finalG = clamp((int)(sum.y * 255.0f), 0, 255);
                int finalB = clamp((int)(sum.z * 255.0f), 0, 255);
            
                output[y * width + x] = (finalA << 24) | (finalR << 16) | (finalG << 8) | finalB;
            }
            """;

    // 静态共享资源
    private static cl_context clContext = null;
    private static cl_command_queue clCommandQueue = null;
    private static cl_program clProgram = null;
    private static cl_kernel blurKernel = null;

    // 实例特定资源
    private cl_mem inputBuffer = null;
    private cl_mem outputBuffer = null;
    private int currentWidth = 0;
    private int currentHeight = 0;
    private int pixelCount = 0;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final ReentrantLock instanceLock = new ReentrantLock();

    // 图像获取接口
    @Setter
    private BlurProcessorHandleAction handleAction;
    static {
        GetIsSupportedOpenCL.start();
    }

    public static boolean getIsSupportedOpenCL() {
        try {
            GetIsSupportedOpenCL.join();
        } catch (InterruptedException ignored) {

        }
        return isSupportedOpenCL;
    }

    /**
     * 构造函数 - 不带图像
     */
    public OpenCLBlurProcessor() {
        instanceCount.incrementAndGet();
        log.debug("OpenCLBlurProcessor instance created. Total instances: {}", instanceCount.get());
    }

    /**
     * 构造函数 - 带图像
     */
    public OpenCLBlurProcessor(BufferedImage bufferedImage) {
        this();
        changeImage(bufferedImage);
    }

    /**
     * 静态初始化方法
     */
    public static void init() {
        if (staticInitialized.get()) {
            log.info("OpenCL is already initialized");
            return;
        }

        if (!OpenCLSupportChecker.isOpenCLSupported()) {
            log.error("OpenCL is not supported on this system");
            return;
        }

        if (!staticInitializing.compareAndSet(false, true)) {
            log.info("OpenCL initialization is already in progress, waiting...");
            try {
                // 等待初始化完成
                initLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for OpenCL initialization", e);
            }

            // 注意：这里直接返回，不再重复执行后续逻辑
            return;

        }

        staticLock.lock();
        try {
            if (staticInitialized.get()) {
                return;
            }

            log.info("Initializing OpenCL static resources...");
            CL.setExceptionsEnabled(true);

            // 获取平台和设备
            int[] numPlatforms = new int[1];
            clGetPlatformIDs(0, null, numPlatforms);
            if (numPlatforms[0] == 0) {
                throw new RuntimeException("No OpenCL platforms found");
            }

            cl_platform_id[] platforms = new cl_platform_id[numPlatforms[0]];
            clGetPlatformIDs(platforms.length, platforms, null);
            cl_platform_id platform = platforms[0];

            // 获取设备
            int[] numDevices = new int[1];
            clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, 0, null, numDevices);
            if (numDevices[0] == 0) {
                log.warn("No GPU devices found, trying CPU");
                clGetDeviceIDs(platform, CL_DEVICE_TYPE_CPU, 0, null, numDevices);
                if (numDevices[0] == 0) {
                    throw new RuntimeException("No OpenCL devices found");
                }
            }

            cl_device_id[] devices = new cl_device_id[numDevices[0]];
            clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, devices.length, devices, null);
            cl_device_id clDevice = devices[0];

            // 创建上下文
            cl_context_properties contextProperties = new cl_context_properties();
            contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
            clContext = clCreateContext(contextProperties, 1, new cl_device_id[]{clDevice},
                    null, null, null);

            // 创建命令队列
            clCommandQueue = clCreateCommandQueueWithProperties(clContext, clDevice,
                    new cl_queue_properties(), null);

            // 创建和构建程序
            clProgram = clCreateProgramWithSource(clContext, 1, new String[]{KERNEL_SOURCE},
                    null, null);

            // 编译并检查错误
            try {
                clBuildProgram(clProgram, 1, new cl_device_id[]{clDevice}, null, null, null);
            } catch (CLException e) {
                // 获取构建日志
                byte[] buildLog = new byte[8192];
                clGetProgramBuildInfo(clProgram, clDevice, CL_PROGRAM_BUILD_LOG,
                        buildLog.length, Pointer.to(buildLog), null);
                String logMsg = new String(buildLog).trim();
                log.error("OpenCL build error: {}", logMsg);
                throw new RuntimeException("OpenCL build failed: " + logMsg, e);
            }

            // 创建内核
            blurKernel = clCreateKernel(clProgram, "gaussianBlur", null);

            staticInitialized.set(true);
            log.info("OpenCL static resources initialized successfully with device: {}", getDeviceName(clDevice));

        } catch (Exception e) {
            staticInitializing.set(false);
            log.error("OpenCL initialization failed: {}", e.getMessage());
            log.error(ExceptionHandler.getExceptionMessage(e));
            releaseStaticResources();
            throw new RuntimeException("OpenCL initialization failed", e);
        } finally {
            staticInitializing.set(false);
            staticLock.unlock();
            // 释放所有等待中的线程
            initLatch.countDown();
        }
    }

    /**
     * 关闭模糊处理器（静态方法）
     */
    public static void closeBlurProcessor() {
        staticLock.lock();
        try {
            if (!staticInitialized.get()) {
                log.info("OpenCL is not initialized, nothing to close");
                return;
            }

            log.info("Closing OpenCL static resources...");
            releaseStaticResources();
            staticInitialized.set(false);
            log.info("OpenCL static resources closed successfully");
        } finally {
            staticLock.unlock();
        }
    }

    /**
     * 释放静态资源
     */
    private static void releaseStaticResources() {
        if (blurKernel != null) {
            try {
                clReleaseKernel(blurKernel);
            } catch (Exception e) {
                log.warn("Error releasing kernel: {}", e.getMessage());
            }
            blurKernel = null;
        }
        if (clProgram != null) {
            try {
                clReleaseProgram(clProgram);
            } catch (Exception e) {
                log.warn("Error releasing program: {}", e.getMessage());
            }
            clProgram = null;
        }
        if (clCommandQueue != null) {
            try {
                clReleaseCommandQueue(clCommandQueue);
            } catch (Exception e) {
                log.warn("Error releasing command queue: {}", e.getMessage());
            }
            clCommandQueue = null;
        }
        if (clContext != null) {
            try {
                clReleaseContext(clContext);
            } catch (Exception e) {
                log.warn("Error releasing context: {}", e.getMessage());
            }
            clContext = null;
        }
    }

    /**
     * 获取设备名称
     */
    private static String getDeviceName(cl_device_id device) {
        try {
            byte[] nameBytes = new byte[1024];
            clGetDeviceInfo(device, CL_DEVICE_NAME, nameBytes.length, Pointer.to(nameBytes), null);
            return new String(nameBytes).trim();
        } catch (Exception e) {
            log.warn("Failed to get device name: {}", e.getMessage());
            return "Unknown Device";
        }
    }

    /**
     * 检查OpenCL是否可用
     */
    public static boolean isOpenCLAvailable() {
        return staticInitialized.get();
    }

    /**
     * 更改当前处理的图像
     */
    public void changeImage(BufferedImage image) {
        instanceLock.lock();
        try {
            if (instanceClosed.get()) {
                log.warn("Attempted to change image on a closed processor");
                return;
            }

            releaseInstanceResources();

            if (image != null) {
                updateImageBuffer(image);
            }

            log.debug("Image changed successfully");
        } finally {
            instanceLock.unlock();
        }
    }

    /**
     * 应用模糊效果：自动检查数据，不存在则通过接口获取
     */
    public BufferedImage applyBlur(int kernelSize) {
        instanceLock.lock();
        try {
            // 检查处理器状态
            if (instanceClosed.get() || !staticInitialized.get()) {
                log.info("Processor is closed or OpenCL not initialized, attempting to reinitialize...");
                try {
                    init();
                    instanceClosed.set(false);
                } catch (Exception e) {
                    log.error("Failed to reinitialize OpenCL: {}", e.getMessage());
                    return null;
                }
            }

            // 检查图像数据是否存在，不存在则通过接口获取
            if (!hasValidImageData()) {
                log.debug("No valid image data, trying to get from handle action");
                if (handleAction == null) {
                    log.error("BlurProcessorHandleAction is not set");
                    return null;
                }

                BufferedImage image = handleAction.getSrcBlurBufferedImage();
                if (image == null) {
                    log.error("Failed to get image from handle action");
                    return null;
                }
                changeImage(image);
            }

            // 验证内核大小
            if (kernelSize < 1 || kernelSize % 2 == 0) {
                throw new IllegalArgumentException("Kernel size must be positive and odd");
            }

            if (!isProcessing.compareAndSet(false, true)) {
                log.warn("OpenCL processing already in progress");
                return null;
            }

            // 计算 sigma 值
            float sigma = Math.max(1.0f, kernelSize / 3.0f);

            // 设置内核参数
            clSetKernelArg(blurKernel, 0, Sizeof.cl_mem, Pointer.to(inputBuffer));
            clSetKernelArg(blurKernel, 1, Sizeof.cl_mem, Pointer.to(outputBuffer));
            clSetKernelArg(blurKernel, 2, Sizeof.cl_int, Pointer.to(new int[]{currentWidth}));
            clSetKernelArg(blurKernel, 3, Sizeof.cl_int, Pointer.to(new int[]{currentHeight}));
            clSetKernelArg(blurKernel, 4, Sizeof.cl_int, Pointer.to(new int[]{kernelSize}));
            clSetKernelArg(blurKernel, 5, Sizeof.cl_float, Pointer.to(new float[]{sigma}));

            // 执行内核
            long[] globalWorkSize = new long[]{currentWidth, currentHeight};
            clEnqueueNDRangeKernel(clCommandQueue, blurKernel, 2, null,
                    globalWorkSize, null, 0, null, null);
            clFinish(clCommandQueue);

            // 读取结果
            int[] resultPixels = new int[pixelCount];
            IntBuffer resultBuffer = IntBuffer.wrap(resultPixels);
            clEnqueueReadBuffer(clCommandQueue, outputBuffer, CL_TRUE, 0,
                    (long) pixelCount * Sizeof.cl_int, Pointer.to(resultBuffer),
                    0, null, null);

            // 创建结果图像
            BufferedImage result = new BufferedImage(currentWidth, currentHeight,
                    BufferedImage.TYPE_INT_ARGB);
            result.setRGB(0, 0, currentWidth, currentHeight, resultPixels, 0, currentWidth);

            return result;

        } catch (CLException e) {
            log.error("OpenCL processing failed with error code: {}", e.getStatus());
            log.error(ExceptionHandler.getExceptionMessage(e));
            instanceClosed.set(true);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error during OpenCL processing: {}", e.getMessage());
            log.error(ExceptionHandler.getExceptionMessage(e));
            instanceClosed.set(true);
            return null;
        } finally {
            instanceLock.unlock();
            isProcessing.set(false);
        }
    }

    /**
     * 检查是否有有效的图像数据
     */
    private boolean hasValidImageData() {
        return currentWidth > 0 && currentHeight > 0 &&
                pixelCount == currentWidth * currentHeight &&
                inputBuffer != null && outputBuffer != null;
    }

    /**
     * 更新图像缓冲区（基于传入的图像）
     */
    private void updateImageBuffer(BufferedImage image) {
        // 释放旧缓冲区
        releaseInstanceResources();

        // 从传入的图像获取尺寸
        currentWidth = image.getWidth();
        currentHeight = image.getHeight();
        pixelCount = currentWidth * currentHeight;

        // 创建新缓冲区
        inputBuffer = clCreateBuffer(clContext, CL_MEM_READ_ONLY,
                (long) pixelCount * Sizeof.cl_int, null, null);
        outputBuffer = clCreateBuffer(clContext, CL_MEM_WRITE_ONLY,
                (long) pixelCount * Sizeof.cl_int, null, null);

        // 更新输入数据
        updateInputBufferData(image);

        log.debug("Image buffer updated for size: {}x{}", currentWidth, currentHeight);
    }

    /**
     * 更新输入缓冲区数据（基于传入的图像）
     */
    private void updateInputBufferData(BufferedImage image) {
        if (inputBuffer == null || pixelCount == 0 || image == null) return;

        int[] pixels = image.getRGB(0, 0, currentWidth, currentHeight, null, 0, currentWidth);
        IntBuffer pixelBuffer = IntBuffer.wrap(pixels);
        clEnqueueWriteBuffer(clCommandQueue, inputBuffer, CL_TRUE, 0,
                (long) pixelCount * Sizeof.cl_int, Pointer.to(pixelBuffer),
                0, null, null);
    }

    /**
     * 释放实例特定资源
     */
    private void releaseInstanceResources() {
        if (outputBuffer != null) {
            try {
                clReleaseMemObject(outputBuffer);
            } catch (Exception e) {
                log.warn("Error releasing output buffer: {}", e.getMessage());
            }
            outputBuffer = null;
        }
        if (inputBuffer != null) {
            try {
                clReleaseMemObject(inputBuffer);
            } catch (Exception e) {
                log.warn("Error releasing input buffer: {}", e.getMessage());
            }
            inputBuffer = null;
        }

        currentWidth = 0;
        currentHeight = 0;
        pixelCount = 0;
    }

    /**
     * 关闭实例
     */
    @Override
    public void close() {
        instanceLock.lock();
        try {
            if (instanceClosed.get()) {
                return;
            }

            releaseInstanceResources();
            instanceClosed.set(true);

            int count = instanceCount.decrementAndGet();
            log.debug("OpenCLBlurProcessor instance closed. Remaining instances: {}", count);
        } finally {
            instanceLock.unlock();
        }
    }

    /**
     * 获取处理器名称
     */
    public String getName() {
        return "OpenCL_Accelerated_Blur_Processor";
    }

    /**
     * 检查实例是否可用
     */
    public boolean isAvailable() {
        return !instanceClosed.get() && staticInitialized.get() &&
                inputBuffer != null && outputBuffer != null;
    }
}