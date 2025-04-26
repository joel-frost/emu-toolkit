package com.emu.toolkit;

import atlantafx.base.theme.NordDark;
import com.emu.toolkit.controller.ApplicationController;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application class for the ROM Scraper.
 * This launches the JavaFX application and sets up the application controller.
 */
public class EmuToolkitApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Set application-wide theme
        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
        primaryStage.setTitle("EmuToolkit");

        // Set application icons
        setApplicationIcons(primaryStage);

        // Create and initialize the application controller
        ApplicationController appController = new ApplicationController();
        appController.initialize(primaryStage);
    }

    /**
     * Sets the application icons for different resolutions.
     * @param stage The primary stage
     */
    private void setApplicationIcons(Stage stage) {
        List<Image> icons = new ArrayList<>();

        // Load icons in different resolutions
        int[] sizes = {16, 32, 64, 128};
        for (int size : sizes) {
            String iconPath = "/icons/icon_" + size + ".png";
            try {
                Image icon = new Image(getClass().getResourceAsStream(iconPath));
                icons.add(icon);
            } catch (Exception e) {
                System.err.println("Failed to load icon: " + iconPath);
            }
        }

        // Set icons to the stage
        if (!icons.isEmpty()) {
            stage.getIcons().addAll(icons);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}