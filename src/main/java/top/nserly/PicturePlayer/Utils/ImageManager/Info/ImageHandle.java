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

public record ImageHandle(String path, ImageReader imageReader, ImageInputStream imageInputStream) {

    /**
     * Closes the ImageInputStream associated with this ImageHandle.
     * This method is used to release system resources associated with the stream.
     */
    public void close() throws IOException {
        imageInputStream.close();
        imageReader.dispose();
    }
}
