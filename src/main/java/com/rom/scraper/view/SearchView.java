package com.rom.scraper.view;

import com.rom.scraper.model.RomFile;
import com.rom.scraper.viewmodel.ConfigViewModel;
import com.rom.scraper.viewmodel.SearchViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * View for the search tab.
 */
public class SearchView {
    private final SearchViewModel searchViewModel;
    private final ConfigViewModel configViewModel;

    public SearchView(SearchViewModel searchViewModel, ConfigViewModel configViewModel) {
        this.searchViewModel = searchViewModel;
        this.configViewModel = configViewModel;
    }

    public ScrollPane createView() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Create search row
        HBox searchBox = createSearchBox();

        // Create results table
        TableView<RomFile> resultsTable = createResultsTable();

        // Create download button
        Button downloadButton = new Button("Download Selected");
        downloadButton.disableProperty().bind(searchViewModel.downloadButtonEnabledProperty().not());
        downloadButton.setOnAction(e -> handleDownload());

        // Add all to content
        content.getChildren().addAll(searchBox, resultsTable, downloadButton);

        // Wrap in scroll pane
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);

        return scrollPane;
    }

    private HBox createSearchBox() {
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        // Search field
        TextField searchField = new TextField();
        searchField.setPrefWidth(400);
        searchField.setPromptText("Enter game title to search");
        searchField.textProperty().bindBidirectional(searchViewModel.searchTermProperty());

        // Search button
        Button searchButton = new Button("Search");
        searchButton.setDefaultButton(true);
        searchButton.setOnAction(e -> searchViewModel.performSearch());

        searchBox.getChildren().addAll(searchField, searchButton);
        return searchBox;
    }

    private TableView<RomFile> createResultsTable() {
        TableView<RomFile> resultsTable = new TableView<>();

        // Set up name column
        TableColumn<RomFile, String> nameColumn = new TableColumn<>("ROM Name");
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameColumn.prefWidthProperty().bind(resultsTable.widthProperty());

        resultsTable.getColumns().add(nameColumn);
        resultsTable.setPlaceholder(new Label("No results yet. Enter a search term above."));

        // Bind to results
        resultsTable.itemsProperty().bind(searchViewModel.searchResultsProperty());

        // Bind selection
        resultsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> searchViewModel.selectedRomProperty().set(newSelection));

        // Make the table fill available space
        VBox.setVgrow(resultsTable, Priority.ALWAYS);
        resultsTable.setPrefHeight(400);

        return resultsTable;
    }

    private void handleDownload() {
        // Validate download folder
        if (configViewModel.folderIsInvalid()) {
            showError("Download Folder Required",
                    "Please select a download folder using the Browse button before proceeding.");
            return;
        }

        // Proceed with download
        searchViewModel.downloadSelectedRom(configViewModel.downloadFolderProperty().get());
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}