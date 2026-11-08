package config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Configures {@code java.util.logging} for console output and rotating log files.
 */
public final class LoggingConfigurer {

    private static final String PROPERTIES_FILE = "application.properties";
    private static final String DEFAULT_ACTIVE_LOG_PATH = "logs/banking.log";
    private static final String DEFAULT_ARCHIVE_BASE_NAME = "banking";
    private static final String DEFAULT_ARCHIVE_DATE_PATTERN = "yyyy-MM-dd";
    private static final int DEFAULT_FILE_LIMIT_BYTES = 5 * 1024 * 1024;
    private static final int DEFAULT_FILE_COUNT = 5;
    private static final String DEFAULT_LEVEL = "INFO";

    private LoggingConfigurer() {
    }

    public static void configure() {
        System.setProperty(
                "java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS [%4$s] %5$s%6$s%n"
        );

        Properties properties = loadProperties();
        Level level = parseLevel(properties.getProperty("log.level", DEFAULT_LEVEL));

        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(level);

        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        SimpleFormatter formatter = new SimpleFormatter();

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(level);
        consoleHandler.setFormatter(formatter);
        rootLogger.addHandler(consoleHandler);

        Path activeLogPath = attachRotatingFileHandler(rootLogger, formatter, level, properties);
        if (activeLogPath != null) {
            Logger.getLogger(LoggingConfigurer.class.getName()).info(
                    "Writing application logs to " + activeLogPath.toAbsolutePath());
            registerShutdownHook(rootLogger);
        }
    }

    private static Path attachRotatingFileHandler(
            Logger rootLogger,
            SimpleFormatter formatter,
            Level level,
            Properties properties) {
        Path activeLogPath = resolveActiveLogPath(properties);
        String archiveBaseName = properties.getProperty("log.archive.base.name", DEFAULT_ARCHIVE_BASE_NAME)
                .trim();
        DateTimeFormatter archiveDateFormatter = DateRotatingFileHandler.parseArchiveDatePattern(
                properties.getProperty("log.archive.date.pattern", DEFAULT_ARCHIVE_DATE_PATTERN));
        int fileLimit = parsePositiveInt(
                properties.getProperty("log.file.limit.bytes"),
                DEFAULT_FILE_LIMIT_BYTES,
                "log.file.limit.bytes");
        int fileCount = parsePositiveInt(
                properties.getProperty("log.file.count"),
                DEFAULT_FILE_COUNT,
                "log.file.count");

        try {
            if (activeLogPath.getParent() != null) {
                Files.createDirectories(activeLogPath.getParent());
            }
            DateRotatingFileHandler fileHandler = new DateRotatingFileHandler(
                    activeLogPath,
                    archiveBaseName,
                    archiveDateFormatter,
                    fileLimit,
                    fileCount,
                    true);
            fileHandler.setLevel(level);
            fileHandler.setFormatter(formatter);
            rootLogger.addHandler(fileHandler);
            return activeLogPath.toAbsolutePath().normalize();
        } catch (IOException e) {
            System.err.println("[WARN] Rotating file logging disabled: " + e.getMessage());
            return null;
        }
    }

    private static void registerShutdownHook(Logger rootLogger) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Handler handler : rootLogger.getHandlers()) {
                handler.flush();
                handler.close();
            }
        }, "logging-shutdown"));
    }

    private static Path resolveActiveLogPath(Properties properties) {
        Path path = resolveActiveLogPathRaw(properties);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
    }

    private static Path resolveActiveLogPathRaw(Properties properties) {
        String activePath = properties.getProperty("log.file.path");
        if (activePath != null && !activePath.isBlank()) {
            return Paths.get(activePath.trim());
        }

        String legacyPattern = properties.getProperty("log.file.pattern");
        if (legacyPattern != null && !legacyPattern.isBlank()) {
            String resolved = legacyPattern.trim().replace("%g", "0").replace("%u", "0");
            if (resolved.contains("%")) {
                return Paths.get(DEFAULT_ACTIVE_LOG_PATH);
            }
            return Paths.get(resolved);
        }

        return Paths.get(DEFAULT_ACTIVE_LOG_PATH);
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream in = LoggingConfigurer.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            System.err.println("[WARN] Could not read " + PROPERTIES_FILE
                    + " for logging settings; using defaults.");
        }
        return properties;
    }

    private static Level parseLevel(String levelName) {
        try {
            return Level.parse(levelName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("[WARN] Invalid log.level '" + levelName
                    + "'; using " + DEFAULT_LEVEL + ".");
            return Level.INFO;
        }
    }

    private static int parsePositiveInt(String raw, int defaultValue, String propertyName) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value <= 0) {
                throw new NumberFormatException("not positive");
            }
            return value;
        } catch (NumberFormatException e) {
            System.err.println("[WARN] Invalid " + propertyName + " '" + raw
                    + "'; using default " + defaultValue + ".");
            return defaultValue;
        }
    }
}
