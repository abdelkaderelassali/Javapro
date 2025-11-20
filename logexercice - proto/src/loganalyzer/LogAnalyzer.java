package loganalyzer;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LogAnalyzer {

    public static void main(String[] args) throws InterruptedException {
        Path logDir = Paths.get("C:\\Users\\Anouar\\IdeaProjects\\LogAnalyser-Proto\\logexercice - proto\\logs");

        List<Path> files;
        try (Stream<Path> paths = Files.list(logDir)) {
            files = paths.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            System.err.println("Erreur accès dossier logs : " + e.getMessage());
            return;
        }

        System.out.println("Traitement avec Threads simples...");
        List<LogEntry> allEntries = new CopyOnWriteArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            for (Path path : files) {
                executor.submit(new LogFileThread(path, allEntries));
            }
            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                System.err.println("Les tâches n'ont pas terminé dans le délai imparti");
                executor.shutdownNow();
            }
        }

        afficherStatistiques(allEntries);

    }


    private static void afficherStatistiques(List<LogEntry> entries) {
        System.out.println("Nombre total d’entrées : " + entries.size());


        Map<String, Long> counts = entries.stream()
                .collect(Collectors.groupingBy(LogEntry::getLevel, Collectors.counting()));

        counts.forEach((level, count) ->
                System.out.println("Niveau " + level + " : " + count));


        Optional<Map.Entry<String, Long>> topLogger = entries.stream()
                .collect(Collectors.groupingBy(LogEntry::getLogger, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue());

        topLogger.ifPresent(entry ->
                System.out.println("Logger le plus actif : " +
                        entry.getKey() + " avec " + entry.getValue() + " logs"));
    }
}
