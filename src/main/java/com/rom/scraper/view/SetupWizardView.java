package com.rom.scraper.view;

import com.rom.scraper.model.AppConfig;
import com.rom.scraper.model.PlatformConfig;
import com.rom.scraper.service.ConfigPersistenceService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wizard for first-time setup of the application.
 */
public class SetupWizardView {
    private final AppConfig appConfig;
    private final ConfigPersistenceService configService;
    private final List<PlatformConfig> selectedPlatforms = new ArrayList<>();
    private Stage wizardStage;
    private final BooleanProperty skipConfigProperty = new SimpleBooleanProperty(false);

    public SetupWizardView(AppConfig appConfig, ConfigPersistenceService configService) {
        this.appConfig = appConfig;
        this.configService = configService;
    }

    /**
     * Show the setup wizard
     */
    public void showWizard(Window owner) {
        wizardStage = new Stage();
        wizardStage.initModality(Modality.APPLICATION_MODAL);
        wizardStage.initOwner(owner);
        wizardStage.setTitle("ROM Scraper Setup Wizard");
        wizardStage.setMinWidth(600);
        wizardStage.setMinHeight(500);

        // Start with the welcome screen
        showWelcomeScreen();

        wizardStage.showAndWait();
    }

    /**
     * First screen of the wizard with welcome message and option to skip
     */
    private void showWelcomeScreen() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Welcome to ROM Scraper");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label descriptionLabel = new Label(
                "This wizard will help you configure ROM Scraper by setting up platforms " +
                        "you want to use. You'll need URLs for ROM repositories for each platform."
        );
        descriptionLabel.setWrapText(true);

        Label skipLabel = new Label(
                "If you prefer, you can skip this setup and use advanced mode to manually " +
                        "enter URLs each time, or configure platforms later in settings."
        );
        skipLabel.setWrapText(true);

        CheckBox skipCheckbox = new CheckBox("Skip setup and enable advanced mode");
        skipCheckbox.selectedProperty().bindBidirectional(skipConfigProperty);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button nextButton = new Button("Next");
        nextButton.setDefaultButton(true);
        nextButton.setOnAction(e -> {
            if (skipConfigProperty.get()) {
                appConfig.setAdvancedMode(true);
                appConfig.setWizardShown(true);

                try {
                    configService.saveConfig(appConfig);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    showErrorAlert("Failed to save configuration");
                }

                wizardStage.close();
            } else {
                showPlatformSelectionScreen();
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(e -> wizardStage.close());

        buttonBox.getChildren().addAll(cancelButton, nextButton);

        content.getChildren().addAll(
                titleLabel,
                descriptionLabel,
                new Separator(),
                skipLabel,
                skipCheckbox,
                new Separator(),
                buttonBox
        );

        Scene scene = new Scene(content);
        wizardStage.setScene(scene);
    }

    /**
     * Screen for selecting which platforms to configure
     */
    private void showPlatformSelectionScreen() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));

        Label titleLabel = new Label("Select Platforms to Configure");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label instructionLabel = new Label(
                "Select the platforms you want to configure. You'll need to provide a " +
                        "repository URL for each selected platform."
        );
        instructionLabel.setWrapText(true);

        // Create a grid of checkboxes for platform selection
        GridPane platformGrid = new GridPane();
        platformGrid.setHgap(20);
        platformGrid.setVgap(10);

        List<CheckBox> checkBoxes = new ArrayList<>();
        int rowIndex = 0;
        int colIndex = 0;
        int maxColumns = 2;

        for (PlatformConfig platform : appConfig.getPlatforms()) {
            CheckBox checkbox = new CheckBox(platform.getName());
            checkbox.setUserData(platform);
            checkBoxes.add(checkbox);

            platformGrid.add(checkbox, colIndex, rowIndex);

            colIndex++;
            if (colIndex >= maxColumns) {
                colIndex = 0;
                rowIndex++;
            }
        }

        // Button box
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button prevButton = new Button("Back");
        prevButton.setOnAction(e -> showWelcomeScreen());

        Button nextButton = new Button("Next");
        nextButton.setDefaultButton(true);
        nextButton.setOnAction(e -> {
            // Collect selected platforms
            selectedPlatforms.clear();
            for (CheckBox checkbox : checkBoxes) {
                if (checkbox.isSelected()) {
                    selectedPlatforms.add((PlatformConfig) checkbox.getUserData());
                }
            }

            if (selectedPlatforms.isEmpty()) {
                showErrorAlert("Please select at least one platform or go back and skip configuration");
            } else {
                showPlatformConfigScreen(0);
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(e -> wizardStage.close());

        buttonBox.getChildren().addAll(cancelButton, prevButton, nextButton);

        // Add select all/none buttons
        HBox selectionButtonBox = new HBox(10);
        selectionButtonBox.setAlignment(Pos.CENTER_LEFT);

        Button selectAllButton = new Button("Select All");
        selectAllButton.setOnAction(e -> checkBoxes.forEach(cb -> cb.setSelected(true)));

        Button selectNoneButton = new Button("Select None");
        selectNoneButton.setOnAction(e -> checkBoxes.forEach(cb -> cb.setSelected(false)));

        selectionButtonBox.getChildren().addAll(selectAllButton, selectNoneButton);

        // Build the content
        content.getChildren().addAll(
                titleLabel,
                instructionLabel,
                new Separator(),
                selectionButtonBox,
                platformGrid,
                new Separator(),
                buttonBox
        );

        // Set max height for the platform grid
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        Scene scene = new Scene(scrollPane);
        wizardStage.setScene(scene);
    }

    /**
     * Screen for configuring individual platform details
     */
    private void showPlatformConfigScreen(int index) {
        if (index >= selectedPlatforms.size()) {
            // All platforms configured, show download folder selection
            showDownloadFolderScreen();
            return;
        }

        PlatformConfig platform = selectedPlatforms.get(index);

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));

        Label titleLabel = new Label("Configure Platform: " + platform.getName());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label progressLabel = new Label(String.format("Platform %d of %d",
                index + 1, selectedPlatforms.size()));

        // URL input
        Label urlLabel = new Label("Enter the repository URL for " + platform.getName() + ":");
        TextField urlField = new TextField(platform.getUrl());
        urlField.setPromptText("https://myrient.erista.me/files/...");
        urlField.setPrefWidth(500);

        // File extension input
        Label extensionLabel = new Label("Preferred file extension (optional):");
        TextField extensionField = new TextField(platform.getFileExtension());
        extensionField.setPromptText(".zip");

        // Explanation text
        TextArea explanationText = new TextArea(
                "For each platform, you'll need a URL to a ROM repository. " +
                        "Typically, these are directory listings where files can be accessed directly. " +
                        "\n\nThe file extension field is optional - if left blank, the application " +
                        "will attempt to auto-detect the most common extension."
        );
        explanationText.setEditable(false);
        explanationText.setWrapText(true);
        explanationText.setPrefRowCount(4);
        explanationText.setFocusTraversable(false);

        // Button box
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button prevButton = new Button("Back");
        prevButton.setOnAction(e -> {
            if (index > 0) {
                showPlatformConfigScreen(index - 1);
            } else {
                showPlatformSelectionScreen();
            }
        });

        Button nextButton = new Button("Next");
        nextButton.setDefaultButton(true);
        nextButton.setOnAction(e -> {
            String url = urlField.getText().trim();
            if (url.isEmpty()) {
                showErrorAlert("Please enter a valid URL");
                return;
            }

            // Save settings to platform
            platform.setUrl(url);
            platform.setFileExtension(extensionField.getText().trim());

            // Move to next platform
            showPlatformConfigScreen(index + 1);
        });

        Button skipButton = new Button("Skip This Platform");
        skipButton.setOnAction(e -> showPlatformConfigScreen(index + 1));

        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(e -> wizardStage.close());

        buttonBox.getChildren().addAll(cancelButton, skipButton, prevButton, nextButton);

        // Add all components to content
        content.getChildren().addAll(
                titleLabel,
                progressLabel,
                new Separator(),
                explanationText,
                urlLabel,
                urlField,
                extensionLabel,
                extensionField,
                new Separator(),
                buttonBox
        );

        Scene scene = new Scene(content);
        wizardStage.setScene(scene);
    }

    /**
     * Screen for selecting the download folder
     */
    private void showDownloadFolderScreen() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));

        Label titleLabel = new Label("Select Download Folder");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label instructionLabel = new Label(
                "Select a folder where downloaded ROMs will be saved. " +
                        "You can change this later in the application settings."
        );
        instructionLabel.setWrapText(true);

        // Folder selection
        HBox folderBox = new HBox(10);
        folderBox.setAlignment(Pos.CENTER_LEFT);

        TextField folderField = new TextField(appConfig.getLastDownloadFolder());
        folderField.setEditable(false);
        folderField.setPromptText("Click Browse to select a folder");
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

            File selectedFolder = directoryChooser.showDialog(wizardStage);
            if (selectedFolder != null) {
                folderField.setText(selectedFolder.getAbsolutePath());
            }
        });

        folderBox.getChildren().addAll(folderField, browseButton);

        // Button box
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button prevButton = new Button("Back");
        prevButton.setOnAction(e -> showPlatformConfigScreen(selectedPlatforms.size() - 1));

        Button finishButton = new Button("Finish");
        finishButton.setDefaultButton(true);
        finishButton.setOnAction(e -> {
            String folder = folderField.getText().trim();
            if (folder.isEmpty()) {
                showErrorAlert("Please select a download folder");
                return;
            }

            // Save the download folder
            appConfig.setLastDownloadFolder(folder);

            // Set the first configured platform as selected
            List<PlatformConfig> configuredPlatforms = appConfig.getConfiguredPlatforms();
            if (!configuredPlatforms.isEmpty()) {
                appConfig.setSelectedPlatform(configuredPlatforms.get(0));
            }

            // Mark wizard as shown
            appConfig.setWizardShown(true);

            // Save configuration
            try {
                configService.saveConfig(appConfig);
                wizardStage.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                showErrorAlert("Failed to save configuration");
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(e -> wizardStage.close());

        buttonBox.getChildren().addAll(cancelButton, prevButton, finishButton);

        // Add all components to content
        content.getChildren().addAll(
                titleLabel,
                instructionLabel,
                new Separator(),
                folderBox,
                new Separator(),
                buttonBox
        );

        Scene scene = new Scene(content);
        wizardStage.setScene(scene);
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(wizardStage);
        alert.showAndWait();
    }
}