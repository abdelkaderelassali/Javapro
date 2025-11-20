package loganalyzer;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Path logDir = Paths.get("logs");

        if (!Files.exists(logDir)) {
            System.err.println("Le dossier 'logs' n'existe pas dans le répertoire courant.");
            System.err.println("Répertoire courant : " + Paths.get("").toAbsolutePath());
            return;
        }

        List<Path> files;
        try (Stream<Path> paths = Files.list(logDir)) {
            files = paths.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            System.err.println("Erreur accès dossier logs : " + e.getMessage());
            return;
        }

        if (files.isEmpty()) {
            System.err.println("Aucun fichier trouvé dans le dossier logs");
            return;
        }

        System.out.println("=== Analyse des fichiers de logs ===");
        System.out.println("Nombre de fichiers à traiter : " + files.size());
        System.out.println();

        long startTime = System.currentTimeMillis();
        List<LogEntry> allEntries = new CopyOnWriteArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            System.out.println("Traitement avec ExecutorService (" + Runtime.getRuntime().availableProcessors() + " threads)...");

            for (Path path : files) {
                executor.submit(new LogFileThread(path, allEntries));
            }

            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                System.err.println("Les tâches n'ont pas terminé dans le délai imparti");
                executor.shutdownNow();
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Temps d'exécution : " + (endTime - startTime) + " ms");
        System.out.println();

        afficherStatistiques(allEntries);
    }

    private static void afficherStatistiques(List<LogEntry> entries) {
        System.out.println("=== Statistiques ===");
        System.out.println("Nombre total d'entrées : " + entries.size());
        System.out.println();

        Map<String, Long> counts = entries.stream()
                .collect(Collectors.groupingBy(LogEntry::getLevel, Collectors.counting()));

        System.out.println("Répartition par niveau :");
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> System.out.println("  " + entry.getKey() + " : " + entry.getValue()));
        System.out.println();

        Optional<Map.Entry<String, Long>> topLogger = entries.stream()
                .collect(Collectors.groupingBy(LogEntry::getLogger, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue());

        topLogger.ifPresent(entry ->
                System.out.println("Logger le plus actif : " + entry.getKey() + " avec " + entry.getValue() + " logs"));

        System.out.println("\nTop 5 des loggers les plus actifs :");
        entries.stream()
                .collect(Collectors.groupingBy(LogEntry::getLogger, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> System.out.println("  " + entry.getKey() + " : " + entry.getValue()));
    }
}

