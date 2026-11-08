package config;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.stream.Stream;

/**
 * Writes to a single active log file and, on size limit, archives it with a date-based name.
 */
final class DateRotatingFileHandler extends Handler {

    private final Path activeLogPath;
    private final String archiveBaseName;
    private final DateTimeFormatter archiveDateFormatter;
    private final int maxBytes;
    private final int maxArchiveCount;

    private OutputStream outputStream;
    private int writtenBytes;

    DateRotatingFileHandler(
            Path activeLogPath,
            String archiveBaseName,
            DateTimeFormatter archiveDateFormatter,
            int maxBytes,
            int maxArchiveCount,
            boolean append) throws IOException {
        this.activeLogPath = activeLogPath;
        this.archiveBaseName = archiveBaseName;
        this.archiveDateFormatter = archiveDateFormatter;
        this.maxBytes = maxBytes;
        this.maxArchiveCount = maxArchiveCount;

        Path parent = activeLogPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        if (append && Files.exists(activeLogPath)) {
            writtenBytes = (int) Math.min(Integer.MAX_VALUE, Files.size(activeLogPath));
        }

        openOutputStream(append);
    }

    @Override
    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }

        String formatted = getFormatter().format(record);
        byte[] payload = formatted.getBytes(StandardCharsets.UTF_8);

        try {
            if (writtenBytes > 0 && writtenBytes + payload.length > maxBytes) {
                rotate();
            }
            outputStream.write(payload);
            outputStream.flush();
            writtenBytes += payload.length;
        } catch (IOException e) {
            reportError("Failed to write log record.", e, ErrorManager.WRITE_FAILURE);
        }
    }

    @Override
    public synchronized void flush() {
        if (outputStream != null) {
            try {
                outputStream.flush();
            } catch (IOException e) {
                reportError("Failed to flush log file.", e, ErrorManager.FLUSH_FAILURE);
            }
        }
    }

    @Override
    public synchronized void close() throws SecurityException {
        flush();
        closeOutputStream();
    }

    private void rotate() throws IOException {
        closeOutputStream();

        if (Files.exists(activeLogPath) && Files.size(activeLogPath) > 0) {
            Path archivePath = resolveArchivePath();
            Files.move(activeLogPath, archivePath);
            purgeOldArchives();
        }

        writtenBytes = 0;
        openOutputStream(false);
    }

    private Path resolveArchivePath() throws IOException {
        String dateStamp = archiveDateFormatter.format(LocalDateTime.now());
        Path directory = activeLogPath.getParent() != null
                ? activeLogPath.getParent()
                : Path.of(".");
        Path candidate = directory.resolve(archiveBaseName + "-" + dateStamp + ".log");

        if (!Files.exists(candidate)) {
            return candidate;
        }

        int suffix = 1;
        while (true) {
            Path withSuffix = directory.resolve(
                    archiveBaseName + "-" + dateStamp + "-" + suffix + ".log");
            if (!Files.exists(withSuffix)) {
                return withSuffix;
            }
            suffix++;
        }
    }

    private void purgeOldArchives() throws IOException {
        Path directory = activeLogPath.getParent() != null
                ? activeLogPath.getParent()
                : Path.of(".");
        String archivePrefix = archiveBaseName + "-";

        List<Path> archives;
        try (Stream<Path> files = Files.list(directory)) {
            archives = files.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith(archivePrefix)
                                && name.endsWith(".log")
                                && !name.equals(activeLogPath.getFileName().toString());
                    })
                    .sorted(Comparator.comparing(path -> {
                        try {
                            return Files.getLastModifiedTime(path);
                        } catch (IOException e) {
                            return null;
                        }
                    }, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
        }

        int deleteCount = archives.size() - maxArchiveCount;
        for (int i = 0; i < deleteCount; i++) {
            try {
                Files.deleteIfExists(archives.get(i));
            } catch (IOException e) {
                reportError("Failed to delete old log archive.", e, ErrorManager.GENERIC_FAILURE);
            }
        }
    }

    private void openOutputStream(boolean append) throws IOException {
        List<StandardOpenOption> options = new ArrayList<>();
        options.add(StandardOpenOption.CREATE);
        options.add(StandardOpenOption.WRITE);
        if (append) {
            options.add(StandardOpenOption.APPEND);
        } else {
            options.add(StandardOpenOption.TRUNCATE_EXISTING);
        }
        outputStream = new BufferedOutputStream(
                Files.newOutputStream(activeLogPath, options.toArray(StandardOpenOption[]::new)));
    }

    private void closeOutputStream() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                reportError("Failed to close log file.", e, ErrorManager.CLOSE_FAILURE);
            } finally {
                outputStream = null;
            }
        }
    }

    static DateTimeFormatter parseArchiveDatePattern(String pattern) {
        String normalized = pattern == null || pattern.isBlank()
                ? "yyyy-MM-dd"
                : pattern.trim();
        try {
            return DateTimeFormatter.ofPattern(normalized);
        } catch (IllegalArgumentException e) {
            System.err.println("[WARN] Invalid log.archive.date.pattern '" + normalized
                    + "'; using yyyy-MM-dd.");
            return DateTimeFormatter.ofPattern("yyyy-MM-dd");
        }
    }
}
