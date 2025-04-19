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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ViewModel for batch processing functionality.
 */
public class BatchViewModel {
    private final RomScraperService romScraperService;
    private final DownloadService downloadService;
    private final ConfigViewModel configViewModel;

    // Properties
    private final StringProperty batchInputProperty = new SimpleStringProperty("");
    private final BooleanProperty processingProperty = new SimpleBooleanProperty(false);
    private final ObservableList<String> batchResultsProperty = FXCollections.observableArrayList();

    // Storage for multiple matches that need user selection
    private final Map<String, List<RomFile>> pendingSelections = new HashMap<>();
    private final ObservableList<String> pendingGames = FXCollections.observableArrayList();

    public BatchViewModel(RomScraperService romScraperService, DownloadService downloadService, ConfigViewModel configViewModel) {
        this.romScraperService = romScraperService;
        this.downloadService = downloadService;
        this.configViewModel = configViewModel;
    }

    public void processBatch(String downloadFolder) {
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
        pendingSelections.clear();
        pendingGames.clear();
        batchResultsProperty.add("Processing batch of " + gamesList.size() + " games");

        // Get the region from ConfigViewModel
        String region = configViewModel.getSelectedRegion();

        // Create a CompletableFuture chain to process games sequentially
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        for (String game : gamesList) {
            future = future.thenCompose(v -> processGame(game, downloadFolder, region));
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
                    if (!pendingSelections.isEmpty()) {
                        batchResultsProperty.add("Found " + pendingSelections.size() + " games with multiple matches. Please manually select them from the pending list.");
                        pendingGames.addAll(pendingSelections.keySet());
                    }
                    processingProperty.set(false);
                });
            }
        });
    }

    private CompletableFuture<Void> processGame(String game, String downloadFolder, String region) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Platform.runLater(() -> batchResultsProperty.add("Searching for: " + game));

        // Convert to CompletableFuture pattern
        romScraperService.searchRoms(game, region, matches -> {
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
                // Multiple matches - store for later user selection
                Platform.runLater(() -> {
                    batchResultsProperty.add("  ! Multiple matches found for: " + game + " (skipped for manual selection)");
                    pendingSelections.put(game, new ArrayList<>(matches));
                });
                future.complete(null);
            }
        });

        return future;
    }

    /**
     * Gets a list of games that require manual selection.
     */
    public ObservableList<String> getPendingGames() {
        return pendingGames;
    }

    /**
     * Gets the matches for a specific game.
     */
    public List<RomFile> getMatchesForGame(String game) {
        return pendingSelections.getOrDefault(game, Collections.emptyList());
    }

    /**
     * Adds a selected ROM to the download queue and removes it from pending selections.
     */
    public void selectMatchForDownload(String game, RomFile selectedRom, String downloadFolder) {
        if (selectedRom != null && pendingSelections.containsKey(game)) {
            downloadService.addToQueue(selectedRom, downloadFolder);
            Platform.runLater(() -> {
                batchResultsProperty.add("  + Added to queue: " + selectedRom.getName());
                pendingSelections.remove(game);
                pendingGames.remove(game);
            });
        }
    }

    /**
     * Skips a game from the pending selections without downloading it.
     */
    public void skipPendingGame(String game) {
        if (pendingSelections.containsKey(game)) {
            Platform.runLater(() -> {
                batchResultsProperty.add("  - Skipped: " + game);
                pendingSelections.remove(game);
                pendingGames.remove(game);
            });
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