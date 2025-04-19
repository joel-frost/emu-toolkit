package com.rom.scraper.controller;

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

    // ViewModels
    private ConfigViewModel configViewModel;
    private SearchViewModel searchViewModel;
    private BatchViewModel batchViewModel;
    private DownloadViewModel downloadViewModel;

    // Main view
    private MainView mainView;

    public void initialize(Stage primaryStage) {
        // Create services
        this.executorService = Executors.newCachedThreadPool();
        this.romScraperService = new RomScraperService(executorService);
        this.downloadService = new DownloadService();

        // Create view models
        this.configViewModel = new ConfigViewModel(romScraperService);
        this.searchViewModel = new SearchViewModel(romScraperService, downloadService);
        this.batchViewModel = new BatchViewModel(romScraperService, downloadService);
        this.downloadViewModel = new DownloadViewModel(downloadService);

        // Create and set up main view
        this.mainView = new MainView(
                configViewModel,
                searchViewModel,
                batchViewModel,
                downloadViewModel
        );

        // Initialize and show main view
        mainView.initialize(primaryStage);

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
    }
}