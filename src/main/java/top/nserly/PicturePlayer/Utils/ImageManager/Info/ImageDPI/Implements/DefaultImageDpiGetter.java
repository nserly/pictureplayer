package top.nserly.PicturePlayer.Utils.ImageManager.Info.ImageDPI.Implements;

import top.nserly.PicturePlayer.Utils.ImageManager.Info.GetImageInformation;
import top.nserly.PicturePlayer.Utils.ImageManager.Info.ImageDPI.ImageDPI;
import top.nserly.PicturePlayer.Utils.ImageManager.Info.ImageHandle;

import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.io.File;
import java.io.IOException;

public class DefaultImageDpiGetter {
    public static ImageDPI getImageDPI(String path) throws IOException {
        float HorizontalDPI = 72.0f; // 默认水平DPI
        float VerticalDPI = 72.0f; // 默认垂直DPI

        try (ImageHandle imageHandle = GetImageInformation.getImageHandle(new File(path))) {
            // 获取元数据
            ImageReader imageReader = imageHandle.imageReader();
            IIOMetadata metadata = imageReader.getImageMetadata(0);
            if (metadata.isStandardMetadataFormatSupported()) {
                IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_1.0");
                IIOMetadataNode horizontalCR = getNode(root, "HorizontalPixelSize");
                IIOMetadataNode verticalCR = getNode(root, "VerticalPixelSize");

                if (horizontalCR != null) {
                    HorizontalDPI = Float.parseFloat(horizontalCR.getAttribute("value"));
                } else if (verticalCR != null) {
                    VerticalDPI = Float.parseFloat(verticalCR.getAttribute("value"));
                }
            }
            imageReader.dispose();
        }
        return new ImageDPI(HorizontalDPI, VerticalDPI);
    }

    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++) {
            if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) rootNode.item(i);
            }
        }
        return null;
    }
}
