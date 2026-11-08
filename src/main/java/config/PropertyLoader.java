package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Loads {@code application.properties} from the classpath.
 *
 * <p>Singleton so the file is only parsed once per JVM run.
 *
 * <pre>
 *     String url = PropertyLoader.getInstance().getProperty("db.url");
 * </pre>
 */
public class PropertyLoader {

    private static final Logger LOGGER = Logger.getLogger(PropertyLoader.class.getName());
    private static final String PROPERTIES_FILE = "application.properties";

    private static volatile PropertyLoader instance;

    private final Properties properties;

    private PropertyLoader() {
        properties = new Properties();
        loadProperties();
    }

    /**
     * Returns the singleton instance (double-checked locking).
     */
    public static PropertyLoader getInstance() {
        if (instance == null) {
            synchronized (PropertyLoader.class) {
                if (instance == null) {
                    instance = new PropertyLoader();
                }
            }
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new RuntimeException(
                        "Configuration file '" + PROPERTIES_FILE + "' not found on the classpath.\n" +
                                "Copy 'application.properties.example' to " +
                                "'src/main/resources/application.properties' and fill in your credentials."
                );
            }
            properties.load(in);
            LOGGER.info("Application properties loaded successfully.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to read '" + PROPERTIES_FILE + "': " + e.getMessage(), e);
        }
    }

    /**
     * Returns the value for the given key.
     *
     * @throws RuntimeException if the key is absent
     */
    public String getProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException(
                    "Required property '" + key + "' not found in " + PROPERTIES_FILE
            );
        }
        return value.trim();
    }

    /**
     * Returns the value for the given key, or {@code defaultValue} if absent.
     */
    public String getProperty(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? value.trim() : defaultValue;
    }
}
