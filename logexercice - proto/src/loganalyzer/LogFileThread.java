package loganalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class LogFileThread implements Runnable {
    private final Path file;
    private final List<LogEntry> globalLogList;

    public LogFileThread(Path file, List<LogEntry> globalLogList) {
        this.file = file;
        this.globalLogList = globalLogList;
    }

    @Override
    public void run() {
        try (Stream<String> lines = Files.lines(file)) {
            List<LogEntry> local = lines
                    .map(LogParser::parseLogLine)
                    .filter(Objects::nonNull)
                    .toList();
            synchronized (globalLogList) {
                globalLogList.addAll(local);
            }
        } catch (IOException e) {
            System.err.println("Error reading file " + file.getFileName() + ": " + e.getMessage());
        }
    }
}