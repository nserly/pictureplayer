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
