package com.emu.toolkit;

/**
 * Launcher class for the application.
 * This is needed to work around the JavaFX runtime components
 * when launching from an executable jar.
 */
public class Launcher {
    public static void main(String[] args) {
        EmuToolkitApp.main(args);
    }
}