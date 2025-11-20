package loganalyzer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogParser {

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static LogEntry parseLogLine(String line) {
        try {
            if (line == null || line.length() < 50) {
                return null;
            }

            String[] parts = line.split(" ", 4);

            if (parts.length < 4) {
                return null;
            }

            LocalDateTime timestamp = LocalDateTime.parse(parts[0] + " " + parts[1], dtf);

            String level = parts[2].replaceAll("[\\[\\]]", "");

            String message = parts[3];
            String[] rest = parts[3].split("-", 2);
            String logger = rest[0].trim();

            return new LogEntry(timestamp, level, logger, message);
        } catch (Exception e) {
            return null;
        }
    }
}
