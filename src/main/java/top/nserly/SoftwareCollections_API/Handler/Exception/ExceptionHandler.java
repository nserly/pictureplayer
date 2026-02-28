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

package top.nserly.SoftwareCollections_API.Handler.Exception;

import org.slf4j.Logger;

public class ExceptionHandler {
    public static void setUncaughtExceptionHandler(Logger log) {
        Thread.setDefaultUncaughtExceptionHandler((e1, e2) -> log.error(getExceptionMessage(e2)));
    }

    public static String getExceptionMessage(Throwable e) {
        if (e == null) return null;
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
                .append((e.getMessage() != null && !e.getMessage().isBlank()) ? e.getMessage() : "No Exception Message!")
                .append("\n")
                .append(e.getClass().getName())
                .append(":")
                .append(e.getMessage())
                .append("\n");

        StackTraceElement[] stackTraceElements = e.getStackTrace();
        if (stackTraceElements != null) {
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                stringBuilder
                        .append("at ")
                        .append(stackTraceElement.getClassName())
                        .append("(line:")
                        .append(stackTraceElement.getLineNumber())
                        .append(")\n");
            }
        }
        Throwable throwable = e.getCause();
        if (throwable != null) stringBuilder
                .append("Caused by:")
                .append(getExceptionMessage(throwable));
        return stringBuilder.toString();
    }
}
