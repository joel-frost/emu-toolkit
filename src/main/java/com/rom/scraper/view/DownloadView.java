package com.rom.scraper.view;

import com.rom.scraper.model.DownloadTask;
import com.rom.scraper.viewmodel.DownloadViewModel;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * View for the downloads tab.
 */
public class DownloadView {
    private final DownloadViewModel viewModel;

    public DownloadView(DownloadViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public ScrollPane createView() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Create header
        Label headerLabel = new Label("Download Queue:");

        // Create download table
        TableView<DownloadTask> downloadsTable = createDownloadsTable();

        // Create buttons for the bottom
        HBox buttonBox = createButtonBox();

        // Add components to content
        content.getChildren().addAll(headerLabel, downloadsTable, buttonBox);

        // Wrap in scroll pane
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);

        return scrollPane;
    }

    private TableView<DownloadTask> createDownloadsTable() {
        TableView<DownloadTask> downloadsTable = new TableView<>();
        downloadsTable.setItems(viewModel.getDownloadTasks());
        downloadsTable.setPrefHeight(400);

        // Bind selection to view model
        downloadsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldValue, newValue) -> viewModel.selectedTaskProperty().set(newValue));

        // Create columns
        TableColumn<DownloadTask, String> filenameCol = new TableColumn<>("Filename");
        filenameCol.setCellValueFactory(cellData -> cellData.getValue().filenameProperty());
        filenameCol.prefWidthProperty().bind(downloadsTable.widthProperty().multiply(0.45));

        TableColumn<DownloadTask, Double> progressCol = new TableColumn<>("Progress");
        progressCol.setCellValueFactory(cellData -> cellData.getValue().progressProperty().asObject());
        progressCol.setCellFactory(ProgressBarTableCell.forTableColumn());
        progressCol.prefWidthProperty().bind(downloadsTable.widthProperty().multiply(0.25));

        TableColumn<DownloadTask, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.prefWidthProperty().bind(downloadsTable.widthProperty().multiply(0.2));

        // Create a cancel column with a button
        TableColumn<DownloadTask, DownloadTask> actionCol = new TableColumn<>("");
        actionCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        actionCol.setCellFactory(param -> new CancelButtonCell(viewModel));
        actionCol.prefWidthProperty().bind(downloadsTable.widthProperty().multiply(0.1));

        // Add columns to table
        downloadsTable.getColumns().addAll(filenameCol, progressCol, statusCol, actionCol);

        // Make table fill available space
        VBox.setVgrow(downloadsTable, Priority.ALWAYS);

        return downloadsTable;
    }

    private HBox createButtonBox() {
        HBox buttonBox = new HBox(10);

        // Clear button - enabled only when all downloads are complete/cancelled
        Button clearButton = new Button("Clear Completed");
        clearButton.setOnAction(e -> viewModel.clearCompletedDownloads());
        clearButton.disableProperty().bind(viewModel.canClearProperty().not());

        buttonBox.getChildren().add(clearButton);

        return buttonBox;
    }

    /**
     * Custom table cell with a cancel button.
     */
    private static class CancelButtonCell extends TableCell<DownloadTask, DownloadTask> {
        private final Button cancelButton;
        private final DownloadViewModel viewModel;

        public CancelButtonCell(DownloadViewModel viewModel) {
            this.viewModel = viewModel;

            // Create a small button with an X
            cancelButton = new Button("✕");
            cancelButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 5px;");
            cancelButton.setOnAction(event -> {
                DownloadTask task = getItem();
                if (task != null) {
                    viewModel.cancelDownload(task);
                }
            });
        }

        @Override
        protected void updateItem(DownloadTask task, boolean empty) {
            super.updateItem(task, empty);

            if (empty || task == null) {
                setGraphic(null);
            } else {
                // Only show button if task is still downloading
                String status = task.getStatus();
                if ("Downloading".equals(status) || "Queued".equals(status)) {
                    setGraphic(cancelButton);
                } else {
                    setGraphic(null);
                }
            }
        }
    }
}