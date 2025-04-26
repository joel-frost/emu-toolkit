package com.emu.toolkit.viewmodel;

import com.emu.toolkit.service.DownloadService;
import com.emu.toolkit.service.RomScraperService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * ViewModel for configuration settings.
 */
public class ConfigViewModel {
    private final RomScraperService romScraperService;
    private final DownloadService downloadService;

    // Properties
    private final StringProperty urlProperty = new SimpleStringProperty("");
    private final StringProperty downloadFolderProperty = new SimpleStringProperty("");
    private final ObjectProperty<String> selectedExtensionProperty = new SimpleObjectProperty<>("(Auto Select)");
    private final BooleanProperty customExtensionEnabledProperty = new SimpleBooleanProperty(false);
    private final StringProperty customExtensionProperty = new SimpleStringProperty("");
    private final BooleanProperty loadingProperty = new SimpleBooleanProperty(false);
    private final StringProperty statusMessageProperty = new SimpleStringProperty("Ready");
    private final ListProperty<String> availableExtensionsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());

    // Region selection properties
    private final ObjectProperty<String> selectedRegionProperty = new SimpleObjectProperty<>("Any");
    private final ListProperty<String> availableRegionsProperty = new SimpleListProperty<>(
            FXCollections.observableArrayList(Arrays.asList("Any", "USA", "EUR", "JPN")));

    public ConfigViewModel(RomScraperService romScraperService, DownloadService downloadService) {
        this.romScraperService = romScraperService;
        this.downloadService = downloadService;

        // Initialize available extensions with default values
        List<String> initialExtensions = new ArrayList<>();
        initialExtensions.add("(Auto Select)");
        initialExtensions.addAll(Arrays.asList(
                ".zip", ".7z", ".rar", ".iso", ".bin", ".rom"
        ));
        availableExtensionsProperty.set(FXCollections.observableArrayList(initialExtensions));

        // Bind service loading/status to view model properties
        romScraperService.loadingProperty().addListener((obs, oldVal, newVal) ->
                Platform.runLater(() -> this.loadingProperty.set(newVal)));
        romScraperService.statusMessageProperty().addListener((obs, oldVal, newVal) ->
                Platform.runLater(() -> this.statusMessageProperty.set(newVal)));
    }

    public void connectToUrl(Consumer<Boolean> callback) {
        String url = urlProperty.get();
        if (url == null || url.isEmpty() || !url.contains("myrient")) {
            statusMessageProperty.set("Invalid URL. Please enter a valid Myrient URL.");
            callback.accept(false);
            return;
        }

        // Get the extension to use for connection
        String extension = getSelectedExtension();
        loadingProperty.set(true);

        // If using auto-select, try to detect the most common extension
        if (extension.isEmpty() || "(Auto Select)".equals(extension)) {
            // Run in background thread to avoid UI freezing
            Thread detectionThread = new Thread(() -> {
                String detectedExtension = romScraperService.detectMostCommonExtension(url);

                // Connect with the detected extension
                if (!detectedExtension.isEmpty()) {
                    connectWithExtension(url, detectedExtension, success -> {
                        // Update the extensions list on the JavaFX application thread
                        if (success) {
                            Platform.runLater(() -> {
                                updateExtensionsWithDetected(detectedExtension);
                                callback.accept(true);
                            });
                        } else {
                            Platform.runLater(() -> callback.accept(false));
                        }
                    });
                } else {
                    // If no extension detected, connect with empty extension
                    connectWithExtension(url, "", result -> {
                        Platform.runLater(() -> callback.accept(result));
                    });
                }
            });
            detectionThread.setDaemon(true); // Mark as daemon thread to not block application shutdown
            detectionThread.start();
        } else {
            // Connect with the selected/custom extension
            connectWithExtension(url, extension, result -> {
                Platform.runLater(() -> callback.accept(result));
            });
        }
    }

    private void connectWithExtension(String url, String extension, Consumer<Boolean> callback) {
        romScraperService.connectToUrl(url, extension, success -> {
            callback.accept(success);
        });
    }

    private void updateExtensionsWithDetected(String detectedExtension) {
        // This method should only be called from the JavaFX application thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateExtensionsWithDetected(detectedExtension));
            return;
        }

        List<String> currentExtensions = new ArrayList<>(availableExtensionsProperty.get());
        // Add the detected extension with a label
        currentExtensions.add(1, detectedExtension + " (Auto Detected)");

        // Remove duplicates while preserving order
        List<String> uniqueExtensions = new ArrayList<>(new LinkedHashSet<>(currentExtensions));

        // Update the list
        availableExtensionsProperty.set(FXCollections.observableArrayList(uniqueExtensions));

        // Select the detected extension
        selectedExtensionProperty.set(detectedExtension + " (Auto Detected)");
    }

    public String getSelectedExtension() {
        // If custom extension is enabled, use that
        if (customExtensionEnabledProperty.get()) {
            String customExt = customExtensionProperty.get().trim();
            if (!customExt.startsWith(".") && !customExt.isEmpty()) {
                customExt = "." + customExt;
            }
            return customExt;
        }

        // Otherwise, use the selected extension from the dropdown
        String selected = selectedExtensionProperty.get();

        // Handle auto select
        if (selected == null || selected.equals("(Auto Select)")) {
            return "";
        }

        // Extract just the extension part from the dropdown value
        if (selected.contains(" (Auto Detected)")) {
            selected = selected.replace(" (Auto Detected)", "");
        }

        return selected;
    }

    /**
     * Gets the currently selected region, or null if "Any" is selected.
     */
    public String getSelectedRegion() {
        String region = selectedRegionProperty.get();
        return "Any".equals(region) ? null : region;
    }

    public boolean folderIsInvalid() {
        String folder = downloadFolderProperty.get();
        if (folder == null || folder.isEmpty()) {
            return true;
        }

        File dir = new File(folder);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            return !created;
        } else return !dir.isDirectory();
    }

    // Getters for properties
    public StringProperty urlProperty() {
        return urlProperty;
    }

    public StringProperty downloadFolderProperty() {
        return downloadFolderProperty;
    }

    public ObjectProperty<String> selectedExtensionProperty() {
        return selectedExtensionProperty;
    }

    public BooleanProperty customExtensionEnabledProperty() {
        return customExtensionEnabledProperty;
    }

    public StringProperty customExtensionProperty() {
        return customExtensionProperty;
    }

    public BooleanProperty loadingProperty() {
        return loadingProperty;
    }

    public StringProperty statusMessageProperty() {
        return statusMessageProperty;
    }

    public ListProperty<String> availableExtensionsProperty() {
        return availableExtensionsProperty;
    }

    public ObjectProperty<String> selectedRegionProperty() {
        return selectedRegionProperty;
    }

    public ListProperty<String> availableRegionsProperty() {
        return availableRegionsProperty;
    }
}