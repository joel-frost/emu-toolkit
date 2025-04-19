package com.rom.scraper.view;

import com.rom.scraper.model.DownloadTask;
import com.rom.scraper.model.RomFile;
import com.rom.scraper.model.RomScraperModel;
import com.rom.scraper.service.DownloadManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main view controller for the application.
 * Handles the UI setup and interaction with the model components.
 */
public class MainViewController {

    // UI Components
    private Stage primaryStage;
    private TextField urlField;
    private TextField downloadFolderField;
    private ComboBox<String> regionSelector;
    private ComboBox<String> fileExtensionSelector;
    private Spinner<Integer> threadCountSpinner;
    private TabPane mainTabPane;
    private ProgressIndicator loadingIndicator;
    private Label statusLabel;

    // Model components
    private RomScraperModel model;
    private DownloadManager downloadManager;

    // Thread management
    private ExecutorService executorService;

    public void initialize(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.executorService = Executors.newCachedThreadPool();
        this.model = new RomScraperModel();
        this.downloadManager = new DownloadManager();

        setupUI();
    }

    private void setupUI() {
        // Create main layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Create configuration panel
        VBox configPanel = createConfigPanel();

        // Create main tab pane for different views
        mainTabPane = new TabPane();
        Tab searchTab = new Tab("Search", createSearchTab());
        Tab batchTab = new Tab("Batch Processing", createBatchTab());
        Tab downloadsTab = new Tab("Downloads", createDownloadsTab());

        mainTabPane.getTabs().addAll(searchTab, batchTab, downloadsTab);
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        statusBar.setAlignment(Pos.CENTER_LEFT);

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(20, 20);
        loadingIndicator.setVisible(false);

        statusLabel = new Label("Ready");

        statusBar.getChildren().addAll(loadingIndicator, statusLabel);

        // Assemble layout
        root.setTop(configPanel);
        root.setCenter(mainTabPane);
        root.setBottom(statusBar);

        // Set the scene
        Scene scene = new Scene(root, 800, 750);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createConfigPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        // URL input
        HBox urlBox = new HBox(10);
        urlBox.setAlignment(Pos.CENTER_LEFT); // Align elements in the URL input
        Label urlLabel = new Label("URL:");
        urlField = new TextField();
        urlField.setPromptText("https://myrient.erista.me/files/...");
        urlField.setPrefWidth(400);
        Button connectButton = new Button("Connect");
        connectButton.setOnAction(e -> connectToUrl());
        urlBox.getChildren().addAll(urlLabel, urlField, connectButton);

        // Download folder selection
        HBox folderBox = new HBox(10);
        folderBox.setAlignment(Pos.CENTER_LEFT); // Align elements in the folder selection
        Label folderLabel = new Label("Download Folder:");
        downloadFolderField = new TextField();
        downloadFolderField.setPromptText("Select download folder using Browse button");
        downloadFolderField.setEditable(false); // Make read-only
        downloadFolderField.setPrefWidth(400);
        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> browseFolder());
        folderBox.getChildren().addAll(folderLabel, downloadFolderField, browseButton);

        // Thread count and file extension
        HBox optionsBox = new HBox(20);
        optionsBox.setAlignment(Pos.CENTER_LEFT); // Align elements in the options box

        // Thread count
        Label threadLabel = new Label("Parallel Downloads:");
        threadCountSpinner = new Spinner<>(1, 20, 5);
        threadCountSpinner.setEditable(true);
        threadCountSpinner.setPrefWidth(80);

        // File extension selector
        Label extLabel = new Label("File Extension:");
        fileExtensionSelector = new ComboBox<>();
        fileExtensionSelector.setItems(FXCollections.observableArrayList(
                ".zip", ".7z", ".rar", ".iso", ".bin", ".rom"
        ));
        fileExtensionSelector.setEditable(true);
        fileExtensionSelector.getSelectionModel().select(0);

        // Add components to options box
        optionsBox.getChildren().addAll(
                threadLabel, threadCountSpinner,
                extLabel, fileExtensionSelector
        );

        // Add all to panel
        panel.getChildren().addAll(urlBox, folderBox, optionsBox);

        return panel;
    }


    private ScrollPane createSearchTab() {
        VBox tabContent = new VBox(10);
        tabContent.setPadding(new Insets(10));

        // Create search row with region selector first
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        // Region selector (now positioned first)
        Label regionLabel = new Label("Region:");
        regionSelector = new ComboBox<>();
        regionSelector.setItems(FXCollections.observableArrayList(
                "Any", "USA", "EUR", "JPN"
        ));
        regionSelector.getSelectionModel().select(0);
        regionSelector.setPrefWidth(100);

        // Search field
        TextField searchField = new TextField();
        searchField.setPrefWidth(400);
        searchField.setPromptText("Enter game title to search");

        Button searchButton = new Button("Search");
        searchButton.setDefaultButton(true);

        // Add all components to search box in the desired order
        searchBox.getChildren().addAll(regionLabel, regionSelector, searchField, searchButton);

        // Results view
        TableView<RomFile> resultsTable = new TableView<>();
        TableColumn<RomFile, String> nameColumn = new TableColumn<>("ROM Name");
        nameColumn.prefWidthProperty().bind(resultsTable.widthProperty());
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());

        resultsTable.getColumns().add(nameColumn);
        resultsTable.setPlaceholder(new Label("No results yet. Enter a search term above."));

        // Make the TableView fill available space
        VBox.setVgrow(resultsTable, Priority.ALWAYS);

        // Download button
        Button downloadButton = new Button("Download Selected");
        downloadButton.setDisable(true);

        resultsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> downloadButton.setDisable(newSelection == null));

        // Search action
        searchButton.setOnAction(e -> {
            String searchTerm = searchField.getText().trim();
            if (!searchTerm.isEmpty()) {
                updateStatus("Searching for: " + searchTerm);

                executorService.submit(() -> {
                    String region = getSelectedRegion();
                    List<RomFile> results = model.searchRoms(searchTerm, region);

                    Platform.runLater(() -> {
                        resultsTable.setItems(FXCollections.observableArrayList(results));
                        updateStatus("Found " + results.size() + " results for: " + searchTerm);
                    });
                });
            }
        });

        // Download action
        downloadButton.setOnAction(e -> {
            RomFile selectedRom = resultsTable.getSelectionModel().getSelectedItem();
            if (selectedRom != null) {
                String folder = downloadFolderField.getText().trim();
                if (validateFolder(folder)) {
                    downloadManager.addToQueue(selectedRom, folder);
                    updateStatus("Added to download queue: " + selectedRom.getName());
                }
            }
        });

        // Add components to the VBox
        tabContent.getChildren().addAll(searchBox, resultsTable, downloadButton);

        // Wrap the VBox in a ScrollPane
        ScrollPane scrollPane = new ScrollPane(tabContent);
        scrollPane.setFitToWidth(true);

        return scrollPane;
    }

    private ScrollPane createBatchTab() {
        VBox tabContent = new VBox(10);
        tabContent.setPadding(new Insets(10));

        // Batch search input
        Label instructionLabel = new Label("Enter game titles as a comma-separated list:");
        TextArea batchInput = new TextArea();
        batchInput.setPromptText("Example: Mario Kart, Super Metroid, Final Fantasy");
        batchInput.setPrefRowCount(1);

        Button processBatchButton = new Button("Process Batch");

        // Results area
        ListView<String> batchResultsView = new ListView<>();

        // Make the ListView fill the available space
        VBox.setVgrow(batchResultsView, Priority.ALWAYS);
        batchResultsView.setPrefHeight(300); // Initial height, will expand as needed

        // Process batch action
        processBatchButton.setOnAction(e -> {
            String input = batchInput.getText().trim();
            if (!input.isEmpty()) {
                String[] games = input.split(",");
                List<String> gamesList = Arrays.stream(games)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

                if (!gamesList.isEmpty()) {
                    updateStatus("Processing batch of " + gamesList.size() + " games");
                    processBatchGames(gamesList, batchResultsView);
                }
            }
        });

        // Create a label and HBox for the results header
        Label resultsLabel = new Label("Results:");

        // Add components to the VBox with proper spacing
        tabContent.getChildren().addAll(
                instructionLabel, batchInput, processBatchButton,
                resultsLabel, batchResultsView
        );

        // Create a ScrollPane and set it to fit the width
        ScrollPane scrollPane = new ScrollPane(tabContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true); // Make it fit the height as well

        return scrollPane;
    }

    private ScrollPane createDownloadsTab() {
        VBox tabContent = new VBox(10);
        tabContent.setPadding(new Insets(10));

        // Create downloads table
        TableView<DownloadTask> downloadsTable = new TableView<>();
        downloadsTable.setPrefHeight(400);

        // Create columns and bind their widths
        TableColumn<DownloadTask, String> filenameCol = new TableColumn<>("Filename");
        filenameCol.setCellValueFactory(cellData -> cellData.getValue().filenameProperty());
        filenameCol.prefWidthProperty().bind(downloadsTable.widthProperty().multiply(0.5)); // 50% of table width

        TableColumn<DownloadTask, Double> progressCol = new TableColumn<>("Progress");
        progressCol.setCellValueFactory(cellData -> cellData.getValue().progressProperty().asObject());
        progressCol.setCellFactory(ProgressBarTableCell.forTableColumn());
        progressCol.prefWidthProperty().bind(downloadsTable.widthProperty().multiply(0.25)); // 25% of table width

        TableColumn<DownloadTask, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.prefWidthProperty().bind(downloadsTable.widthProperty().multiply(0.25)); // 25% of table width

        // Add columns to the table
        downloadsTable.getColumns().addAll(filenameCol, progressCol, statusCol);

        // Bind the downloads manager's observable list
        downloadsTable.setItems(downloadManager.getDownloadTasks());

        // Buttons
        HBox buttonBox = new HBox(10);
        Button pauseButton = new Button("Pause All");
        Button resumeButton = new Button("Resume All");
        Button cancelButton = new Button("Cancel Selected");
        buttonBox.getChildren().addAll(pauseButton, resumeButton, cancelButton);

        // Actions
        pauseButton.setOnAction(e -> downloadManager.pauseAll());
        resumeButton.setOnAction(e -> downloadManager.resumeAll());
        cancelButton.setOnAction(e -> {
            DownloadTask selected = downloadsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                downloadManager.cancelTask(selected);
            }
        });

        tabContent.getChildren().addAll(
                new Label("Download Queue:"),
                downloadsTable,
                buttonBox
        );

        // Wrap the VBox in a ScrollPane
        ScrollPane scrollPane = new ScrollPane(tabContent);
        scrollPane.setFitToWidth(true); // Ensure the ScrollPane content fills the width
        return scrollPane;
    }

    // Helper methods
    private void connectToUrl() {
        String url = urlField.getText().trim();
        if (url.isEmpty() || !url.contains("myrient")) {
            showError("Invalid URL", "Please enter a valid Myrient URL.");
            return;
        }

        loadingIndicator.setVisible(true);
        updateStatus("Connecting to " + url + "...");

        executorService.submit(() -> {
            boolean success = model.connectToUrl(url, fileExtensionSelector.getValue());

            Platform.runLater(() -> {
                loadingIndicator.setVisible(false);
                if (success) {
                    updateStatus("Connected. Found " + model.getRomFilesCount() + " files.");
                } else {
                    updateStatus("Connection failed. Check the URL and try again.");
                    showError("Connection Failed", "Could not connect to the specified URL or no matching files found.");
                }
            });
        });
    }

    private void browseFolder() {
        DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("Select Download Folder");

        // Set initial directory based on current value if valid
        String currentPath = downloadFolderField.getText().trim();
        if (!currentPath.isEmpty()) {
            File initialDir = new File(currentPath);
            if (initialDir.exists() && initialDir.isDirectory()) {
                directoryChooser.setInitialDirectory(initialDir);
            }
        }

        // Show dialog and get result
        File selectedFolder = directoryChooser.showDialog(primaryStage);

        if (selectedFolder != null) {
            downloadFolderField.setText(selectedFolder.getAbsolutePath());
            updateStatus("Download folder set to: " + selectedFolder.getAbsolutePath());
        }
    }

    private void processBatchGames(List<String> games, ListView<String> resultsView) {
        String folder = downloadFolderField.getText().trim();
        if (!validateFolder(folder)) {
            return;
        }

        ObservableList<String> results = FXCollections.observableArrayList();
        resultsView.setItems(results);

        executorService.submit(() -> {
            String region = getSelectedRegion();

            for (String game : games) {
                Platform.runLater(() -> results.add("Searching for: " + game));
                List<RomFile> matches = model.searchRoms(game, region);

                if (matches.isEmpty()) {
                    Platform.runLater(() -> results.add("  - No matches found for: " + game));
                } else if (matches.size() == 1) {
                    RomFile rom = matches.get(0);
                    downloadManager.addToQueue(rom, folder);
                    Platform.runLater(() -> results.add("  + Added to queue: " + rom.getName()));
                } else {
                    // For multiple matches, show dialog for selection
                    Platform.runLater(() -> {
                        showMultipleMatchesDialog(game, matches, folder, selected -> {
                            if (selected != null) {
                                downloadManager.addToQueue(selected, folder);
                                results.add("  + Added to queue: " + selected.getName());
                            } else {
                                results.add("  - Skipped: " + game);
                            }
                        });
                    });

                    // Wait for dialog result
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}
                }
            }

            Platform.runLater(() -> {
                results.add("Batch processing complete.");
                updateStatus("Batch processing complete. " + downloadManager.getDownloadTasks().size() + " files in queue.");
            });
        });
    }

    private void showMultipleMatchesDialog(String game, List<RomFile> matches, String folder,
                                           java.util.function.Consumer<RomFile> callback) {
        // Create custom dialog
        Dialog<RomFile> dialog = new Dialog<>();
        dialog.setTitle("Multiple Matches Found");
        dialog.setHeaderText("Select a ROM file for: " + game);

        // Buttons
        ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        ButtonType skipButtonType = new ButtonType("Skip", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, skipButtonType);

        // Create content
        ListView<RomFile> listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(matches));
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(RomFile item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getName());
            }
        });

        dialog.getDialogPane().setContent(listView);

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        // Show dialog and process result
        Optional<RomFile> result = dialog.showAndWait();
        callback.accept(result.orElse(null));
    }

    private String getSelectedRegion() {
        String regionText = regionSelector.getValue();
        return "Any".equals(regionText) ? null : regionText;
    }

    private boolean validateFolder(String folder) {
        if (folder.isEmpty()) {
            showError("Download Folder Required", "Please select a download folder using the Browse button before proceeding.");
            return false;
        }

        File dir = new File(folder);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                showError("Error", "Could not create directory: " + folder);
                return false;
            }
        } else if (!dir.isDirectory()) {
            showError("Error", folder + " is not a directory.");
            return false;
        }

        return true;
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void shutdown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        downloadManager.shutdown();
    }
}