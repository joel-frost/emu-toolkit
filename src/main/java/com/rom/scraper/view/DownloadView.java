package com.rom.scraper.view;

import com.rom.scraper.model.DownloadTask;
import com.rom.scraper.viewmodel.DownloadViewModel;
import javafx.geometry.Insets;
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

        // Create buttons
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
        filenameCol.prefWidthProperty().bind(downloadsTable.widthProperty().multiply(0.5));

        TableColumn<DownloadTask, Double> progressCol = new TableColumn<>("Progress");
        progressCol.setCellValueFactory(cellData -> cellData.getValue().progressProperty().asObject());
        progressCol.setCellFactory(ProgressBarTableCell.forTableColumn());
        progressCol.prefWidthProperty().bind(downloadsTable.widthProperty().multiply(0.25));

        TableColumn<DownloadTask, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.prefWidthProperty().bind(downloadsTable.widthProperty().multiply(0.25));

        // Add columns to table
        downloadsTable.getColumns().addAll(filenameCol, progressCol, statusCol);

        // Make table fill available space
        VBox.setVgrow(downloadsTable, Priority.ALWAYS);

        return downloadsTable;
    }

    private HBox createButtonBox() {
        HBox buttonBox = new HBox(10);

        Button pauseButton = new Button("Pause All");
        pauseButton.setOnAction(e -> viewModel.pauseAllDownloads());

        Button resumeButton = new Button("Resume All");
        resumeButton.setOnAction(e -> viewModel.resumeAllDownloads());

        Button cancelButton = new Button("Cancel Selected");
        cancelButton.setOnAction(e -> viewModel.cancelSelectedDownload());
        cancelButton.disableProperty().bind(viewModel.selectedTaskProperty().isNull());

        buttonBox.getChildren().addAll(pauseButton, resumeButton, cancelButton);

        return buttonBox;
    }
}