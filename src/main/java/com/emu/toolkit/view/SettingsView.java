package com.emu.toolkit.view;

import com.emu.toolkit.model.AppConfig;
import com.emu.toolkit.model.PlatformConfig;
import com.emu.toolkit.service.ConfigPersistenceService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;

/**
 * Dialog for managing application settings.
 */
public class SettingsView {
    private final AppConfig appConfig;
    private final ConfigPersistenceService configService;
    private final BooleanProperty advancedModeProperty = new SimpleBooleanProperty();
    private Stage settingsStage;

    public SettingsView(AppConfig appConfig, ConfigPersistenceService configService) {
        this.appConfig = appConfig;
        this.configService = configService;
        this.advancedModeProperty.set(appConfig.isAdvancedMode());
    }

    /**
     * Show the settings dialog
     */
    public void showSettings(Window owner) {
        settingsStage = new Stage();
        settingsStage.initModality(Modality.APPLICATION_MODAL);
        settingsStage.initOwner(owner);
        settingsStage.setTitle("ROM Scraper Settings");
        settingsStage.setMinWidth(800);
        settingsStage.setMinHeight(600);

        TabPane tabPane = new TabPane();

        // General settings tab
        Tab generalTab = new Tab("General");
        generalTab.setClosable(false);
        generalTab.setContent(createGeneralSettingsPane());

        // Platforms tab
        Tab platformsTab = new Tab("Platforms");
        platformsTab.setClosable(false);
        platformsTab.setContent(createPlatformsPane());

        tabPane.getTabs().addAll(generalTab, platformsTab);

        VBox root = new VBox(tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Button box at the bottom
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button saveButton = new Button("Save");
        saveButton.setDefaultButton(true);
        saveButton.setOnAction(e -> {
            saveSettings();
            settingsStage.close();
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(e -> settingsStage.close());

        buttonBox.getChildren().addAll(cancelButton, saveButton);

        root.getChildren().add(buttonBox);

        Scene scene = new Scene(root);
        settingsStage.setScene(scene);
        settingsStage.showAndWait();
    }

    /**
     * Create the general settings pane
     */
    private Pane createGeneralSettingsPane() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // Advanced mode setting
        CheckBox advancedModeCheckbox = new CheckBox("Enable Advanced Mode");
        advancedModeCheckbox.setSelected(appConfig.isAdvancedMode());
        advancedModeCheckbox.selectedProperty().bindBidirectional(advancedModeProperty);

        Label advancedModeLabel = new Label(
                "Advanced mode allows you to manually enter URLs for each search instead of " +
                        "selecting from configured platforms. This is useful for accessing repositories " +
                        "not in your saved platforms list."
        );
        advancedModeLabel.setWrapText(true);

        // Download folder setting
        Label downloadFolderLabel = new Label("Default Download Folder:");
        HBox folderBox = new HBox(10);
        folderBox.setAlignment(Pos.CENTER_LEFT);

        TextField folderField = new TextField(appConfig.getLastDownloadFolder());
        folderField.setEditable(false);
        HBox.setHgrow(folderField, Priority.ALWAYS);

        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Download Folder");

            // Try to use current path as initial directory
            String currentPath = folderField.getText();
            if (currentPath != null && !currentPath.isEmpty()) {
                File initialDir = new File(currentPath);
                if (initialDir.exists() && initialDir.isDirectory()) {
                    directoryChooser.setInitialDirectory(initialDir);
                }
            }

            File selectedFolder = directoryChooser.showDialog(settingsStage);
            if (selectedFolder != null) {
                folderField.setText(selectedFolder.getAbsolutePath());
            }
        });

        folderBox.getChildren().addAll(folderField, browseButton);

        // Add help text for settings
        TitledPane helpPane = new TitledPane("Settings Help", createHelpText());
        helpPane.setExpanded(false);

        // Add all components to the content pane
        Label titleLabel = new Label("General Settings");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        content.getChildren().addAll(
                titleLabel,
                new Separator(),
                advancedModeCheckbox,
                advancedModeLabel,
                new Separator(),
                downloadFolderLabel,
                folderBox,
                new Separator(),
                helpPane
        );

        // Store the folder field for later access when saving
        content.setUserData(folderField);

        return content;
    }

    /**
     * Create a help text area with instructions
     */
    private TextArea createHelpText() {
        TextArea helpText = new TextArea(
                "ROM Scraper Settings Help\n\n" +
                        "Advanced Mode: When enabled, you'll manually enter URLs for each search instead of selecting " +
                        "from your configured platforms. This gives more flexibility but requires more manual input.\n\n" +
                        "Default Download Folder: This is where ROMs will be saved. You can change this for each download " +
                        "session in the main application.\n\n" +
                        "Platforms: Configure the platforms you want to use with ROM Scraper. Each platform needs a valid " +
                        "URL to a ROM repository. The file extension is optional - if not specified, ROM Scraper will " +
                        "attempt to auto-detect the appropriate extension."
        );
        helpText.setEditable(false);
        helpText.setWrapText(true);
        helpText.setPrefRowCount(10);
        return helpText;
    }

    /**
     * Create the platforms configuration pane
     */
    /**
     * Create the platforms configuration pane
     */
    private Pane createPlatformsPane() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        Label titleLabel = new Label("Configure Platforms");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Create table for platform configuration
        TableView<PlatformConfig> platformTable = new TableView<>();
        platformTable.setEditable(true);
        VBox.setVgrow(platformTable, Priority.ALWAYS);

        // Name column
        TableColumn<PlatformConfig, String> nameColumn = new TableColumn<>("Platform");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(200);

        // URL column
        TableColumn<PlatformConfig, String> urlColumn = new TableColumn<>("Repository URL");
        urlColumn.setCellValueFactory(cellData -> cellData.getValue().urlProperty());
        urlColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        urlColumn.setOnEditCommit(event -> {
            PlatformConfig platform = event.getRowValue();
            platform.setUrl(event.getNewValue());
        });
        urlColumn.setPrefWidth(300);

        // Extension column
        TableColumn<PlatformConfig, String> extensionColumn = new TableColumn<>("File Extension");
        extensionColumn.setCellValueFactory(cellData -> cellData.getValue().fileExtensionProperty());
        extensionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        extensionColumn.setOnEditCommit(event -> {
            PlatformConfig platform = event.getRowValue();
            platform.setFileExtension(event.getNewValue());
        });
        extensionColumn.setPrefWidth(100);

        // Region column
        TableColumn<PlatformConfig, String> regionColumn = new TableColumn<>("Default Region");
        regionColumn.setCellValueFactory(cellData -> cellData.getValue().defaultRegionProperty());

        // Create a cell factory for the region column with a ComboBox
        regionColumn.setCellFactory(column -> new TableCell<PlatformConfig, String>() {
            private final ComboBox<String> comboBox = new ComboBox<>();

            {
                comboBox.getItems().addAll("Any", "USA", "EUR", "JPN");
                comboBox.setOnAction(event -> {
                    PlatformConfig platform = getTableRow().getItem();
                    if (platform != null) {
                        platform.setDefaultRegion(comboBox.getValue());
                        commitEdit(comboBox.getValue());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    comboBox.setValue(item);
                    setGraphic(comboBox);
                }
            }
        });
        regionColumn.setPrefWidth(120);

        // Status column - shows if platform is configured
        TableColumn<PlatformConfig, Boolean> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData ->
                new SimpleBooleanProperty(cellData.getValue().isConfigured()));
        statusColumn.setCellFactory(column -> new TableCell<PlatformConfig, Boolean>() {
            @Override
            protected void updateItem(Boolean configured, boolean empty) {
                super.updateItem(configured, empty);
                if (empty || configured == null) {
                    setText(null);
                } else {
                    setText(configured ? "Configured" : "Not Configured");
                    setStyle(configured ?
                            "-fx-text-fill: green; -fx-font-weight: bold;" :
                            "-fx-text-fill: red;");
                }
            }
        });
        statusColumn.setPrefWidth(100);

        platformTable.getColumns().addAll(nameColumn, urlColumn, extensionColumn, regionColumn, statusColumn);
        platformTable.setItems(appConfig.getPlatforms());

        // Add buttons for editing platforms
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        Button testButton = new Button("Test Connection");
        testButton.setOnAction(e -> {
            PlatformConfig selected = platformTable.getSelectionModel().getSelectedItem();
            if (selected != null && selected.isConfigured()) {
                testPlatformConnection(selected);
            } else {
                showAlert(Alert.AlertType.WARNING, "No configured platform selected",
                        "Please select a configured platform to test.");
            }
        });

        Button clearButton = new Button("Clear URL");
        clearButton.setOnAction(e -> {
            PlatformConfig selected = platformTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setUrl("");
                selected.setFileExtension("");
                selected.setDefaultRegion("Any");
                platformTable.refresh();
            }
        });

        // Disable buttons if no row is selected
        testButton.disableProperty().bind(
                platformTable.getSelectionModel().selectedItemProperty().isNull());
        clearButton.disableProperty().bind(
                platformTable.getSelectionModel().selectedItemProperty().isNull());

        buttonBox.getChildren().addAll(testButton, clearButton);

        // Instructions
        Label instructionsLabel = new Label(
                "Double-click URL or File Extension fields to edit. Select a default region for each platform."
        );
        instructionsLabel.setWrapText(true);

        // Add all to content
        content.getChildren().addAll(
                titleLabel,
                instructionsLabel,
                platformTable,
                buttonBox
        );

        return content;
    }

    /**
     * Save all settings
     */
    private void saveSettings() {
        // Update advanced mode setting from property
        appConfig.setAdvancedMode(advancedModeProperty.get());

        // Update download folder from field
        VBox generalPane = (VBox) ((TabPane) settingsStage.getScene().getRoot().getChildrenUnmodifiable().get(0))
                .getTabs().get(0).getContent();
        TextField folderField = (TextField) generalPane.getUserData();
        appConfig.setLastDownloadFolder(folderField.getText());

        // Handle case where advanced mode is being disabled without configured platforms
        if (!appConfig.isAdvancedMode() && appConfig.hasNotConfiguredPlatforms()) {
            showAlert(Alert.AlertType.WARNING, "No Configured Platforms",
                    "You disabled Advanced Mode, but have no configured platforms. " +
                            "Advanced Mode will remain enabled until at least one platform is configured.");
            appConfig.setAdvancedMode(true);
        }

        // Save to config file
        try {
            configService.saveConfig(appConfig);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Save Error",
                    "Failed to save settings: " + e.getMessage());
        }
    }

    /**
     * Test connection to a platform's repository URL
     */
    private void testPlatformConnection(PlatformConfig platform) {
        // This would call the RomScraperService to test the connection
        // For now, we'll just show a message that it's not implemented
        showAlert(Alert.AlertType.INFORMATION, "Test Connection",
                "Connection testing not implemented in this version.");
    }

    /**
     * Show an alert dialog
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(settingsStage);
        alert.showAndWait();
    }
}