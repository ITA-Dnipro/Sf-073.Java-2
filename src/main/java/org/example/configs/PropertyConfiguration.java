package org.example.configs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

import static java.lang.System.err;

public class PropertyConfiguration {

    private PropertyConfiguration() {}

    public static Properties readPropertiesFromFile(Path file){
        Properties properties = new Properties();
        try(InputStream inputStream = getFileFromResourceAsInputStream(file.toString())) {
            properties.load(inputStream);
        } catch (IOException ioException) {
            err.println("Error when reading properties from file");
            ioException.printStackTrace();
        }
        return properties;
    }

    private static InputStream getFileFromResourceAsInputStream(String fileName){

        ClassLoader classLoader = PropertyConfiguration.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }
}
