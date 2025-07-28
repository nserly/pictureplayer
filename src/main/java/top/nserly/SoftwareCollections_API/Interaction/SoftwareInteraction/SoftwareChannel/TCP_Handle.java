package top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.SoftwareChannel;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.TCP.Interactions;

import java.io.IOException;
import java.net.Socket;

@Slf4j
public final class TCP_Handle extends Interactions {
    @Setter
    @Getter
    public static WindowsAppMutex windowsAppMutex;

    public TCP_Handle(Socket socket) throws IOException {
        super(socket);
    }

    @Override
    public int messageCall() throws Exception {
        handle(sendMessage);
        return 0;
    }

    private void handle(String message) throws Exception {
        if (windowsAppMutex == null)
            throw new RuntimeException("WindowsAppMutex is not initialized. Please set it before handling messages.");
        if (message.startsWith("{newPicturePath} ")) {
            String filePath = message.trim();
            filePath = filePath.substring(filePath.indexOf(" "));
            log.info("Received file path: {}", filePath);
            if (windowsAppMutex.getReceiveFileAction() != null) {
                windowsAppMutex.getReceiveFileAction().receiveFile(filePath.trim());
            } else {
                log.warn("No action defined to handle received file path.");
            }
        } else if (message.startsWith("{getSoftwareVisibleDirective} ")) {
            String softwareVisibleDirective = message.trim();
            softwareVisibleDirective = softwareVisibleDirective.substring(softwareVisibleDirective.indexOf(" "));
            if (windowsAppMutex.getReceiveSoftwareVisibleDirectiveAction() != null) {
                windowsAppMutex.getReceiveSoftwareVisibleDirectiveAction().setVisible(Boolean.parseBoolean(softwareVisibleDirective.trim()));
            } else {
                log.warn("No action defined to handle software visible directive.");
            }
        } else {
            log.warn("Unknown message received: {}", message);
        }
    }
}
