__kernel void gaussianBlur(
    __global const int* input,
    __global int* output,
    const int width,
    const int height,
    const int kernelSize,
    const float sigma
)
{

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