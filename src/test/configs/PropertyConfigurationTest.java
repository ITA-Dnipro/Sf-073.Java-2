package org.example.configs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Properties;

class PropertyConfigurationTest {

    Path file = Path.of("orm.properties");
    Properties testProperties = PropertyConfiguration.readPropertiesFromFile(file);

    @Test
    void should_Test_Read_Properties_From_File_As_InputStream() {
        Assertions.assertEquals("H2Dialect", testProperties.getProperty("orm.dialect"));
        Assertions.assertEquals("h2.Driver", testProperties.getProperty("orm.connection.driver"));
        Assertions.assertEquals("jdbc:h2:file:./src/main/resources/db/h2-test", testProperties.getProperty("orm.connection.url"));
        Assertions.assertEquals("orm", testProperties.getProperty("orm.connection.username"));
        Assertions.assertEquals("orm", testProperties.getProperty("orm.connection.password"));
    }
}
