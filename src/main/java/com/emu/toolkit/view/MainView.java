package com.emu.toolkit.view;

import com.emu.toolkit.model.AppConfig;
import com.emu.toolkit.model.PlatformConfig;
import com.emu.toolkit.service.ConfigPersistenceService;
import com.emu.toolkit.viewmodel.BatchViewModel;
import com.emu.toolkit.viewmodel.ConfigViewModel;
import com.emu.toolkit.viewmodel.DownloadViewModel;
import com.emu.toolkit.viewmodel.SearchViewModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Main view for the application with platform selector.
 * Contains the configuration panel and tab pane with different views.
 */
public class MainView {
    private final ConfigViewModel configViewModel;
    private final SearchViewModel searchViewModel;
    private final BatchViewModel batchViewModel;
    private final DownloadViewModel downloadViewModel;
    private final AppConfig appConfig;
    private final ConfigPersistenceService configService;

    // UI Components
    private ConfigView configView;
    private ProgressIndicator loadingIndicator;
    private Label statusLabel;
    private ComboBox<PlatformConfig> platformSelector;
    private CheckBox advancedModeCheckbox;
    private Label advancedModeExplanation;

    public MainView(
            ConfigViewModel configViewModel,
            SearchViewModel searchViewModel,
            BatchViewModel batchViewModel,
            DownloadViewModel downloadViewModel,
            AppConfig appConfig,
            ConfigPersistenceService configService) {
        this.configViewModel = configViewModel;
        this.searchViewModel = searchViewModel;
        this.batchViewModel = batchViewModel;
        this.downloadViewModel = downloadViewModel;
        this.appConfig = appConfig;
        this.configService = configService;
    }

    public void initialize(Stage primaryStage) {
        // Create main layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Create platform selector and top panel
        VBox topPanel = createTopPanel();

        // Create or reuse configuration panel based on mode
        configView = new ConfigView(configViewModel);
        VBox configPanel = configView.createView();
        configPanel.setVisible(appConfig.isAdvancedMode());
        configPanel.setManaged(appConfig.isAdvancedMode());

        // Create main tab pane for different views
        TabPane mainTabPane = new TabPane();

        // Create tabs
        SearchView searchView = new SearchView(searchViewModel, configViewModel);
        BatchView batchView = new BatchView(batchViewModel, configViewModel);
        DownloadView downloadView = new DownloadView(downloadViewModel);

        Tab searchTab = new Tab("Search", searchView.createView());
        Tab batchTab = new Tab("Batch Processing", batchView.createView());
        Tab downloadsTab = new Tab("Downloads", downloadView.createView());

        mainTabPane.getTabs().addAll(searchTab, batchTab, downloadsTab);
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Create status bar
        HBox statusBar = createStatusBar();

        // Assemble layout
        VBox topContainer = new VBox(10);
        topContainer.getChildren().addAll(topPanel, configPanel);

        root.setTop(topContainer);
        root.setCenter(mainTabPane);
        root.setBottom(statusBar);

        // Set the scene
        Scene scene = new Scene(root, 800, 800);
        primaryStage.setScene(scene);

        // Handle menu items
        Menu fileMenu = new Menu("File");
        MenuItem settingsMenuItem = new MenuItem("Settings");
        settingsMenuItem.setOnAction(e -> {
            SettingsView settingsView = new SettingsView(appConfig, configService);
            settingsView.showSettings(primaryStage);

            // Refresh UI after settings change
            refreshAfterSettingsChange();
        });

        MenuItem exitMenuItem = new MenuItem("Exit");
        exitMenuItem.setOnAction(e -> Platform.exit());

        fileMenu.getItems().addAll(settingsMenuItem, new SeparatorMenuItem(), exitMenuItem);

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().add(fileMenu);

        // Add menu bar to root
        root.setTop(new VBox(menuBar, topContainer));

        // Show setup wizard if needed
        if (!appConfig.isWizardShown()) {
            Platform.runLater(() -> {
                SetupWizardView wizard = new SetupWizardView(appConfig, configService);
                wizard.showWizard(primaryStage);

                // Refresh UI after wizard completes
                refreshAfterSettingsChange();
            });
        }

        primaryStage.show();
    }

    private VBox createTopPanel() {
        VBox topPanel = new VBox(10);
        topPanel.setPadding(new Insets(5));

        // Platform selection row
        HBox platformRow = new HBox(10);
        platformRow.setAlignment(Pos.CENTER_LEFT);

        Label platformLabel = new Label("Platform:");

        // Create platform selector
        platformSelector = new ComboBox<>();
        updatePlatformSelector();

        // Set default selection if available
        if (appConfig.getSelectedPlatform() != null) {
            platformSelector.getSelectionModel().select(appConfig.getSelectedPlatform());
        }

        // Handle platform selection change
        platformSelector.setOnAction(e -> {
            PlatformConfig selected = platformSelector.getValue();
            if (selected != null) {
                appConfig.setSelectedPlatform(selected);

                // Auto-connect to the selected platform's URL
                configViewModel.urlProperty().set(selected.getUrl());
                configViewModel.connectToUrl(success -> {
                    if (!success) {
                        showErrorAlert("Connection Failed",
                                "Could not connect to the selected platform. Please check the URL in settings.");
                    }
                });
            }
        });

        Button settingsButton = new Button("Settings");
        settingsButton.setOnAction(e -> {
            SettingsView settingsView = new SettingsView(appConfig, configService);
            settingsView.showSettings(platformSelector.getScene().getWindow());

            // Refresh UI after settings change
            refreshAfterSettingsChange();
        });

        // Advanced mode toggle
        advancedModeCheckbox = new CheckBox("Advanced Mode");
        advancedModeCheckbox.setSelected(appConfig.isAdvancedMode());
        advancedModeCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            // Only allow disabling advanced mode if we have configured platforms
            if (!newVal && appConfig.hasNotConfiguredPlatforms()) {
                advancedModeCheckbox.setSelected(true);
                showErrorAlert("No Configured Platforms",
                        "You must configure at least one platform before disabling advanced mode.");
                return;
            }

            appConfig.setAdvancedMode(newVal);

            // Show/hide the config panel based on advanced mode
            if (configView != null) {
                VBox configPanel = configView.createView();
                configPanel.setVisible(newVal);
                configPanel.setManaged(newVal);
            }

            // Save the config change
            try {
                configService.saveConfig(appConfig);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // Update UI visibility
            updateComponentVisibility();
        });

        // Advanced mode explanation
        advancedModeExplanation = new Label(
                "Advanced mode allows direct URL entry. Configure platforms in Settings to disable."
        );
        advancedModeExplanation.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
        advancedModeExplanation.setVisible(appConfig.isAdvancedMode());
        advancedModeExplanation.setManaged(appConfig.isAdvancedMode());

        platformRow.getChildren().addAll(platformLabel, platformSelector, settingsButton, advancedModeCheckbox);
        topPanel.getChildren().addAll(platformRow, advancedModeExplanation);

        // Update visibility based on current mode
        updateComponentVisibility();

        return topPanel;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        statusBar.setAlignment(Pos.CENTER_LEFT);

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(20, 20);
        loadingIndicator.visibleProperty().bind(configViewModel.loadingProperty());

        statusLabel = new Label();
        statusLabel.textProperty().bind(configViewModel.statusMessageProperty());

        statusBar.getChildren().addAll(loadingIndicator, statusLabel);

        return statusBar;
    }

    /**
     * Update the platform selector with the current list of configured platforms
     */
    private void updatePlatformSelector() {
        List<PlatformConfig> configuredPlatforms = appConfig.getConfiguredPlatforms();

        if (configuredPlatforms.isEmpty()) {
            // If no platforms are configured, show a placeholder
            platformSelector.setItems(FXCollections.observableArrayList(
                    new PlatformConfig("none", "No Platforms Configured")));
            platformSelector.getSelectionModel().selectFirst();
            platformSelector.setDisable(true);
        } else {
            platformSelector.setItems(FXCollections.observableArrayList(configuredPlatforms));
            platformSelector.setDisable(false);

            // Select the previously selected platform if it exists, otherwise the first one
            if (appConfig.getSelectedPlatform() != null &&
                    configuredPlatforms.contains(appConfig.getSelectedPlatform())) {
                platformSelector.getSelectionModel().select(appConfig.getSelectedPlatform());
            } else {
                platformSelector.getSelectionModel().selectFirst();
                appConfig.setSelectedPlatform(platformSelector.getValue());
            }
        }
    }

    /**
     * Update component visibility based on advanced mode setting
     */
    private void updateComponentVisibility() {
        boolean advancedMode = appConfig.isAdvancedMode();

        // Update visibility of components
        if (platformSelector != null) {
            platformSelector.setVisible(!advancedMode);
            platformSelector.setManaged(!advancedMode);
        }

        if (advancedModeExplanation != null) {
            advancedModeExplanation.setVisible(advancedMode);
            advancedModeExplanation.setManaged(advancedMode);
        }

        // Update the config panel if it exists
        if (configView != null) {
            BorderPane root = (BorderPane) advancedModeCheckbox.getScene().getRoot();
            VBox topContainer = (VBox) ((VBox) root.getTop()).getChildren().get(1);

            // Find the config panel
            if (topContainer.getChildren().size() > 1) {
                VBox configPanel = (VBox) topContainer.getChildren().get(1);
                configPanel.setVisible(advancedMode);
                configPanel.setManaged(advancedMode);
            }
        }
    }

    /**
     * Refresh UI components after settings have changed
     */
    private void refreshAfterSettingsChange() {
        // Update advanced mode checkbox
        advancedModeCheckbox.setSelected(appConfig.isAdvancedMode());

        // Update platform selector
        updatePlatformSelector();

        // Update component visibility
        updateComponentVisibility();

        // Update download folder in config view model
        configViewModel.downloadFolderProperty().set(appConfig.getLastDownloadFolder());

        // Connect to the selected platform if available and not in advanced mode
        if (!appConfig.isAdvancedMode() && appConfig.getSelectedPlatform() != null) {
            configViewModel.urlProperty().set(appConfig.getSelectedPlatform().getUrl());
            configViewModel.connectToUrl(success -> {
                if (!success) {
                    showErrorAlert("Connection Failed",
                            "Could not connect to the selected platform. Please check the URL in settings.");
                }
            });
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}