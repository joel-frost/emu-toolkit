package com.rom.scraper.view;

import com.rom.scraper.model.RomFile;
import com.rom.scraper.util.DialogHelper;
import com.rom.scraper.viewmodel.BatchViewModel;
import com.rom.scraper.viewmodel.ConfigViewModel;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * View for batch processing functionality.
 */
public class BatchView {
    private final BatchViewModel batchViewModel;
    private final ConfigViewModel configViewModel;

    public BatchView(BatchViewModel batchViewModel, ConfigViewModel configViewModel) {
        this.batchViewModel = batchViewModel;
        this.configViewModel = configViewModel;
    }

    public ScrollPane createView() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Batch input instructions
        Label instructionLabel = new Label("Enter game titles as a comma-separated list:");

        // Text area for batch input
        TextArea batchInput = new TextArea();
        batchInput.setPromptText("Example: Mario Kart, Super Metroid, Final Fantasy");
        batchInput.setPrefRowCount(5);
        batchInput.textProperty().bindBidirectional(batchViewModel.batchInputProperty());

        // Process batch button
        Button processBatchButton = new Button("Process Batch");
        processBatchButton.setOnAction(e -> handleProcessBatch());
        processBatchButton.disableProperty().bind(batchViewModel.processingProperty());

        // Results header
        Label resultsLabel = new Label("Results:");

        // Results list view
        ListView<String> resultsListView = new ListView<>();
        resultsListView.setItems(batchViewModel.getBatchResults());
        VBox.setVgrow(resultsListView, Priority.ALWAYS);
        resultsListView.setPrefHeight(200);

        // Pending selections section
        Label pendingLabel = new Label("Pending Selections (games with multiple matches):");

        // Pending games list
        ListView<String> pendingListView = new ListView<>();
        pendingListView.setItems(batchViewModel.getPendingGames());
        pendingListView.setPrefHeight(150);
        VBox.setVgrow(pendingListView, Priority.SOMETIMES);

        // Button to handle selected pending game
        HBox pendingButtonBox = new HBox(10);
        Button selectButton = new Button("Select ROM");
        selectButton.setOnAction(e -> handlePendingSelection(pendingListView.getSelectionModel().getSelectedItem()));
        selectButton.disableProperty().bind(pendingListView.getSelectionModel().selectedItemProperty().isNull());

        Button skipButton = new Button("Skip");
        skipButton.setOnAction(e -> handleSkipPending(pendingListView.getSelectionModel().getSelectedItem()));
        skipButton.disableProperty().bind(pendingListView.getSelectionModel().selectedItemProperty().isNull());

        pendingButtonBox.getChildren().addAll(selectButton, skipButton);

        // Add all components to the content
        content.getChildren().addAll(
                instructionLabel,
                batchInput,
                processBatchButton,
                resultsLabel,
                resultsListView,
                pendingLabel,
                pendingListView,
                pendingButtonBox
        );

        // Wrap in scroll pane
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);

        return scrollPane;
    }

    private void handleProcessBatch() {
        // Validate download folder
        if (configViewModel.folderIsInvalid()) {
            showError("Download Folder Required",
                    "Please select a download folder using the Browse button before proceeding.");
            return;
        }

        // Get the download folder path
        String downloadFolder = configViewModel.downloadFolderProperty().get();

        // Process the batch using the region from ConfigViewModel
        batchViewModel.processBatch(downloadFolder);
    }

    private void handlePendingSelection(String game) {
        if (game == null) return;

        // Get the download folder
        String downloadFolder = configViewModel.downloadFolderProperty().get();
        if (downloadFolder == null || downloadFolder.isEmpty()) {
            showError("Download Folder Required",
                    "Please select a download folder before selecting ROMs.");
            return;
        }

        // Get matches for the selected game
        List<RomFile> matches = batchViewModel.getMatchesForGame(game);
        if (matches.isEmpty()) {
            showError("No Matches",
                    "No matches found for " + game);
            return;
        }

        // Show dialog for user to select a ROM
        DialogHelper.showMultipleMatchesDialog(
                null, // Use null for owner to center on screen
                game,
                matches,
                selectedRom -> {
                    if (selectedRom != null) {
                        batchViewModel.selectMatchForDownload(game, selectedRom, downloadFolder);
                    }
                }
        );
    }

    private void handleSkipPending(String game) {
        if (game != null) {
            batchViewModel.skipPendingGame(game);
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}