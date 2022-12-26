package configs;

import org.example.configs.PropertyConfiguration;
import org.example.lib.utils.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Properties;

class PropertyConfigurationTest {

    Path file = Path.of(Constants.Connection.PROPERTIES_FILE_NAME);
    Properties testProperties = PropertyConfiguration.readPropertiesFromFile(file);

    @Test
    void should_Test_Read_Properties_From_File_As_InputStream() {
        Assertions.assertEquals("H2Dialect", testProperties.getProperty(Constants.Connection.DIALECT));
        Assertions.assertEquals("h2.Driver", testProperties.getProperty(Constants.Connection.DRIVER));
        Assertions.assertEquals("jdbc:h2:file:./src/main/java/org/example/db/h2-test", testProperties.getProperty(Constants.Connection.URL));
        Assertions.assertEquals("", testProperties.getProperty(Constants.Connection.USERNAME));
        Assertions.assertEquals("", testProperties.getProperty(Constants.Connection.PASSWORD));
    }
}
