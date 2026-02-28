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

package top.nserly.PicturePlayer.Utils.ImageManager.Info;

import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;


/**
 * Represents an image handle that encapsulates the path to an image, an ImageReader for reading the image,
 * and an ImageInputStream for input operations on the image.
 * <p>
 * This class provides a method to close the associated ImageInputStream, which is essential for
 * releasing system resources after the image processing is complete.
 */

public record ImageHandle(String path, ImageReader imageReader, ImageInputStream imageInputStream) implements AutoCloseable{

    /**
     * Closes the ImageInputStream associated with this ImageHandle.
     * This method is used to release system resources associated with the stream.
     */
    @Override
    public void close() throws IOException {
        imageInputStream.close();
        imageReader.dispose();
    }
}
