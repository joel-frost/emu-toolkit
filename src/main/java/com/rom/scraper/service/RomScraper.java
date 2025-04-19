package com.rom.scraper.service;

import atlantafx.base.theme.NordDark;
import com.rom.scraper.view.MainViewController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Main application class for the Scraper.
 * This launches the JavaFX application and sets up the primary stage.
 */
public class RomScraper extends Application {

    @Override
    public void start(Stage primaryStage) {
        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
        primaryStage.setTitle("ROM Scraper");

        // Create and configure the main UI controller
        MainViewController mainViewController = new MainViewController();
        mainViewController.initialize(primaryStage);

        // Set up proper shutdown handling
        primaryStage.setOnCloseRequest(event -> {
            mainViewController.shutdown();
            Platform.exit();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}