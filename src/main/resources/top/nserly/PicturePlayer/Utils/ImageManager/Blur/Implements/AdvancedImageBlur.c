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

        // Precomputed Gaussian Kernel
        for (int ky = -radius; ky <= radius; ky++) {
            for (int kx = -radius; kx <= radius; kx++) {
                int nx = clamp(x + kx, 0, width - 1);
                int ny = clamp(y + ky, 0, height - 1);
                int pixelIdx = ny * width + nx;
                int pixel = input[pixelIdx];

                // Extract color components and normalize
                float4 color;
                color.w = ((pixel >> 24) & 0xFF) / 255.0f;
                color.x = ((pixel >> 16) & 0xFF) / 255.0f;
                color.y = ((pixel >> 8) & 0xFF) / 255.0f;
                color.z = (pixel & 0xFF) / 255.0f;

                // Process completely transparent pixels
                if (color.w < 0.001f) {
                    color = (float4)(0.0f, 0.0f, 0.0f, 0.0f);
                }

                // Calculate Gaussian weights
                float distanceSq = (float)(kx * kx + ky * ky);
                float weight = exp(-distanceSq / (2.0f * sigma * sigma));

                // Accumulated weighted color
                sum += color * weight;
                weightSum += weight;
            }
        }

        // Normalize and convert back to integer
        if (weightSum > 0.0f) {
            sum /= weightSum;
        }

        int finalA = clamp((int)(sum.w * 255.0f), 0, 255);
        int finalR = clamp((int)(sum.x * 255.0f), 0, 255);
        int finalG = clamp((int)(sum.y * 255.0f), 0, 255);
        int finalB = clamp((int)(sum.z * 255.0f), 0, 255);

        output[y * width + x] = (finalA << 24) | (finalR << 16) | (finalG << 8) | finalB;
    }