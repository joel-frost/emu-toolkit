package com.rom.scraper;

import atlantafx.base.theme.NordDark;
import com.rom.scraper.controller.ApplicationController;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main application class for the ROM Scraper.
 * This launches the JavaFX application and sets up the application controller.
 */
public class RomScraperApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Set application-wide theme
        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
        primaryStage.setTitle("ROM Scraper");

        // Create and initialize the application controller
        ApplicationController appController = new ApplicationController();
        appController.initialize(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}