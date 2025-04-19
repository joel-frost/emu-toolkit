package com.rom.scraper.controller;

import com.rom.scraper.model.AppConfig;
import com.rom.scraper.service.ConfigPersistenceService;
import com.rom.scraper.service.DownloadService;
import com.rom.scraper.service.RomScraperService;
import com.rom.scraper.viewmodel.BatchViewModel;
import com.rom.scraper.viewmodel.ConfigViewModel;
import com.rom.scraper.viewmodel.DownloadViewModel;
import com.rom.scraper.viewmodel.SearchViewModel;
import com.rom.scraper.view.MainView;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main controller for the application.
 * Coordinates the interactions between views, view models, and services.
 */
public class ApplicationController {
    private ExecutorService executorService;
    private RomScraperService romScraperService;
    private DownloadService downloadService;
    private ConfigPersistenceService configService;
    private AppConfig appConfig;

    // ViewModels
    private ConfigViewModel configViewModel;
    private SearchViewModel searchViewModel;
    private BatchViewModel batchViewModel;
    private DownloadViewModel downloadViewModel;

    // Main view
    private MainView mainView;

    public void initialize(Stage primaryStage) {
        // Load application configuration
        this.configService = new ConfigPersistenceService();
        this.appConfig = configService.loadConfig();

        // Set the download folder in the app config if already configured
        if (appConfig.getLastDownloadFolder() == null || appConfig.getLastDownloadFolder().isEmpty()) {
            // Use default download folder if not set
            appConfig.setLastDownloadFolder(System.getProperty("user.home") + "/Downloads");
        }

        // Create services
        this.executorService = Executors.newCachedThreadPool();
        this.romScraperService = new RomScraperService(executorService);
        this.downloadService = new DownloadService();

        // Create view models
        this.configViewModel = new ConfigViewModel(romScraperService, downloadService);

        // Set the download folder in the view model
        configViewModel.downloadFolderProperty().set(appConfig.getLastDownloadFolder());

        // Create remaining view models
        this.searchViewModel = new SearchViewModel(romScraperService, downloadService, configViewModel);
        this.batchViewModel = new BatchViewModel(romScraperService, downloadService, configViewModel);
        this.downloadViewModel = new DownloadViewModel(downloadService);

        // Create and set up main view with app config
        this.mainView = new MainView(
                configViewModel,
                searchViewModel,
                batchViewModel,
                downloadViewModel,
                appConfig,
                configService
        );

        // Initialize and show main view
        mainView.initialize(primaryStage);

        // If we're not in advanced mode and there's a selected platform, connect to it
        if (!appConfig.isAdvancedMode() && appConfig.getSelectedPlatform() != null) {
            configViewModel.urlProperty().set(appConfig.getSelectedPlatform().getUrl());
            configViewModel.connectToUrl(success -> {
                // Connection status will be shown in the UI
            });
        }

        // Set up proper shutdown handling
        primaryStage.setOnCloseRequest(event -> {
            shutdown();
            Platform.exit();
        });
    }

    public void shutdown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (downloadService != null) {
            downloadService.shutdown();
        }

        // Save configuration before shutdown
        try {
            configService.saveConfig(appConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}