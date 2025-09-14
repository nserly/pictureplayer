package top.nserly.SoftwareCollections_API.FileHandle;

import lombok.extern.slf4j.Slf4j;
import top.nserly.SoftwareCollections_API.Handler.Exception.ExceptionHandler;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
public class FileContents {

    public static String read(String path) {
        try {
            return read(new File(path).toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String read(URL url){
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            log.error(ExceptionHandler.getExceptionMessage(e));
            return null;
        }
        return content.toString();
    }


    public static void write(String path, String content) {
        try (FileWriter fileWriter = new FileWriter(path); BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(content);
            bufferedWriter.flush();
        } catch (IOException e) {
            log.error(ExceptionHandler.getExceptionMessage(e));
        }
    }
}
