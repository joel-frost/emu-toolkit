package com.rom.scraper.view;

import com.rom.scraper.viewmodel.BatchViewModel;
import com.rom.scraper.viewmodel.ConfigViewModel;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

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
        resultsListView.setPrefHeight(300);

        // Add all components to the content
        content.getChildren().addAll(
                instructionLabel,
                batchInput,
                processBatchButton,
                resultsLabel,
                resultsListView
        );

        // Wrap in scroll pane
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

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

        // Get selected region
        String region = configViewModel.selectedExtensionProperty().get();

        // Process the batch
        batchViewModel.processBatch(downloadFolder, region);
    }

    private void showError(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}