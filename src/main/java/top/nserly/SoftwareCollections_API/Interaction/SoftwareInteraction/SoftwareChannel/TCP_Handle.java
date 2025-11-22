package top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.SoftwareChannel;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Interaction.SoftwareInteraction.TCP.Interactions;

import java.net.Socket;
import java.util.ArrayList;

@Slf4j
public final class TCP_Handle extends Interactions {
    @Setter
    @Getter
    public static WindowsAppMutex windowsAppMutex;

    public TCP_Handle(Socket socket, ArrayList<Socket> ClientSockets) {
        super(socket, ClientSockets);
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
            if (windowsAppMutex.getHandleSoftwareRequestAction() != null) {
                windowsAppMutex.getHandleSoftwareRequestAction().receiveFile(filePath.trim());
            } else {
                log.warn("No action defined to handle received file path.");
            }
        } else if (message.startsWith("{getSoftwareVisibleDirective} ")) {
            String softwareVisibleDirective = message.trim();
            softwareVisibleDirective = softwareVisibleDirective.substring(softwareVisibleDirective.indexOf(" "));
            if (windowsAppMutex.getHandleSoftwareRequestAction() != null) {
                windowsAppMutex.getHandleSoftwareRequestAction().setVisible(Boolean.parseBoolean(softwareVisibleDirective.trim()));
            } else {
                log.warn("No action defined to handle software visible directive.");
            }
        } else {
            log.warn("Unknown message received: {}", message);
        }
    }
}
