package com.emu.toolkit.view;

import com.emu.toolkit.viewmodel.ConfigViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;

/**
 * View for configuration settings.
 */
public class ConfigView {
    private final ConfigViewModel viewModel;

    // UI Components
    private TextField urlField;
    private CheckBox customExtensionCheckbox;

    public ConfigView(ConfigViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public VBox createView() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        // URL input
        HBox urlBox = createUrlBox();

        // Download folder selection
        HBox folderBox = createFolderBox();

        // File extension and region options in one row
        HBox optionsBox = createOptionsBox();

        // Add all components to the panel
        panel.getChildren().addAll(urlBox, folderBox, optionsBox);

        return panel;
    }

    private HBox createUrlBox() {
        HBox urlBox = new HBox(10);
        urlBox.setAlignment(Pos.CENTER_LEFT);

        Label urlLabel = new Label("URL:");
        urlField = new TextField();
        urlField.setPromptText("https://somerepo.me/files/..");
        urlField.setPrefWidth(400);
        urlField.textProperty().bindBidirectional(viewModel.urlProperty());

        Button connectButton = new Button("Connect");
        connectButton.setOnAction(e -> viewModel.connectToUrl(success -> {
            if (!success) {
                showError("Connection Failed",
                        "Could not connect to the specified URL or no matching files found.");
            }
        }));

        urlBox.getChildren().addAll(urlLabel, urlField, connectButton);
        return urlBox;
    }

    private HBox createFolderBox() {
        HBox folderBox = new HBox(10);
        folderBox.setAlignment(Pos.CENTER_LEFT);

        Label folderLabel = new Label("Download Folder:");
        TextField downloadFolderField = new TextField();
        downloadFolderField.setPromptText("Select download folder using Browse button");
        downloadFolderField.setEditable(false);
        downloadFolderField.setPrefWidth(400);
        downloadFolderField.textProperty().bindBidirectional(viewModel.downloadFolderProperty());

        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> browseFolder());

        folderBox.getChildren().addAll(folderLabel, downloadFolderField, browseButton);
        return folderBox;
    }

    private HBox createOptionsBox() {
        HBox optionsBox = new HBox(20);
        optionsBox.setAlignment(Pos.CENTER_LEFT);

        // File extension UI components
        Label extLabel = new Label("File Extension:");

        // Extension selector
        ComboBox<String> fileExtensionSelector = new ComboBox<>();
        fileExtensionSelector.setPrefWidth(150);
        fileExtensionSelector.itemsProperty().bind(viewModel.availableExtensionsProperty());
        fileExtensionSelector.valueProperty().bindBidirectional(viewModel.selectedExtensionProperty());

        // Custom extension controls
        customExtensionCheckbox = new CheckBox("Custom Extension");
        customExtensionCheckbox.selectedProperty().bindBidirectional(viewModel.customExtensionEnabledProperty());

        TextField customExtensionField = new TextField();
        customExtensionField.setPrefWidth(100);
        customExtensionField.setPromptText(".ext");
        customExtensionField.textProperty().bindBidirectional(viewModel.customExtensionProperty());
        customExtensionField.disableProperty().bind(customExtensionCheckbox.selectedProperty().not());

        // Disable selector when custom is enabled
        fileExtensionSelector.disableProperty().bind(customExtensionCheckbox.selectedProperty());

        // Add listener to the extension selector
        fileExtensionSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isAutoSelect = newVal != null && newVal.equals("(Auto Select)");
            customExtensionCheckbox.setDisable(isAutoSelect);

            // If switching to Auto Select, uncheck the custom extension checkbox
            if (isAutoSelect && customExtensionCheckbox.isSelected()) {
                customExtensionCheckbox.setSelected(false);
            }
        });

        // Region selector - moved to the right
        Label regionLabel = new Label("Region:");
        ComboBox<String> regionSelector = new ComboBox<>();
        regionSelector.setPrefWidth(100);
        regionSelector.itemsProperty().bind(viewModel.availableRegionsProperty());
        regionSelector.valueProperty().bindBidirectional(viewModel.selectedRegionProperty());

        // Add all components to options box - region at the end
        optionsBox.getChildren().addAll(extLabel, fileExtensionSelector,
                customExtensionCheckbox, customExtensionField,
                regionLabel, regionSelector);
        return optionsBox;
    }

    private void browseFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Download Folder");

        // Set initial directory based on current value if valid
        String currentPath = viewModel.downloadFolderProperty().get();
        if (currentPath != null && !currentPath.isEmpty()) {
            File initialDir = new File(currentPath);
            if (initialDir.exists() && initialDir.isDirectory()) {
                directoryChooser.setInitialDirectory(initialDir);
            }
        }

        // Show dialog and get result
        File selectedFolder = directoryChooser.showDialog(urlField.getScene().getWindow());

        if (selectedFolder != null) {
            viewModel.downloadFolderProperty().set(selectedFolder.getAbsolutePath());
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