package com.rom.scraper.viewmodel;

import com.rom.scraper.model.RomFile;
import com.rom.scraper.service.DownloadService;
import com.rom.scraper.service.RomScraperService;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * ViewModel for batch processing functionality.
 */
public class BatchViewModel {
    private final RomScraperService romScraperService;
    private final DownloadService downloadService;

    // Properties
    private final StringProperty batchInputProperty = new SimpleStringProperty("");
    private final BooleanProperty processingProperty = new SimpleBooleanProperty(false);
    private final ObservableList<String> batchResultsProperty = FXCollections.observableArrayList();

    public BatchViewModel(RomScraperService romScraperService, DownloadService downloadService) {
        this.romScraperService = romScraperService;
        this.downloadService = downloadService;
    }

    public void processBatch(String downloadFolder, String selectedRegion) {
        String input = batchInputProperty.get().trim();
        if (input.isEmpty()) {
            return;
        }

        // Split by commas and clean up the list
        String[] games = input.split(",");
        List<String> gamesList = Arrays.stream(games)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (gamesList.isEmpty()) {
            return;
        }

        processingProperty.set(true);
        batchResultsProperty.clear();
        batchResultsProperty.add("Processing batch of " + gamesList.size() + " games");

        // Create a CompletableFuture chain to process games sequentially
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        for (String game : gamesList) {
            future = future.thenCompose(v -> processGame(game, downloadFolder, selectedRegion));
        }

        future.whenComplete((v, error) -> {
            if (error != null) {
                Platform.runLater(() -> {
                    batchResultsProperty.add("Error during batch processing: " + error.getMessage());
                    processingProperty.set(false);
                });
            } else {
                Platform.runLater(() -> {
                    batchResultsProperty.add("Batch processing complete.");
                    processingProperty.set(false);
                });
            }
        });
    }

    private CompletableFuture<Void> processGame(String game, String downloadFolder, String region) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Platform.runLater(() -> batchResultsProperty.add("Searching for: " + game));

        // Convert to CompletableFuture pattern
        romScraperService.searchRoms(game, "Any".equals(region) ? null : region, matches -> {
            if (matches.isEmpty()) {
                Platform.runLater(() -> batchResultsProperty.add("  - No matches found for: " + game));
                future.complete(null);
            } else if (matches.size() == 1) {
                // Single match - add directly to download queue
                RomFile rom = matches.get(0);
                downloadService.addToQueue(rom, downloadFolder);
                Platform.runLater(() -> batchResultsProperty.add("  + Added to queue: " + rom.getName()));
                future.complete(null);
            } else {
                // Multiple matches - need user interaction
                Platform.runLater(() -> {
                    batchResultsProperty.add("  ! Multiple matches found for: " + game + " (first match will be used)");
                    // In a real implementation, we might show a dialog here
                    // For simplicity, we'll just take the first match
                    RomFile firstMatch = matches.get(0);
                    downloadService.addToQueue(firstMatch, downloadFolder);
                    batchResultsProperty.add("  + Added to queue: " + firstMatch.getName());
                    future.complete(null);
                });
            }
        });

        return future;
    }

    // Method to handle user selection for multiple matches
    public void selectMatchForBatch(RomFile selectedRom, String downloadFolder, Consumer<Boolean> callback) {
        if (selectedRom != null) {
            downloadService.addToQueue(selectedRom, downloadFolder);
            Platform.runLater(() -> batchResultsProperty.add("  + Added to queue: " + selectedRom.getName()));
            callback.accept(true);
        } else {
            Platform.runLater(() -> batchResultsProperty.add("  - Selection skipped"));
            callback.accept(false);
        }
    }

    // Getters for properties
    public StringProperty batchInputProperty() {
        return batchInputProperty;
    }

    public BooleanProperty processingProperty() {
        return processingProperty;
    }

    public ObservableList<String> getBatchResults() {
        return batchResultsProperty;
    }
}