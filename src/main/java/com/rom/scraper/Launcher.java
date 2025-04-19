package com.rom.scraper;

import com.rom.scraper.service.RomScraper;

/**
 * Launcher class for the application.
 * This is needed to work around the JavaFX runtime components
 * when launching from an executable jar.
 */
public class Launcher {
    public static void main(String[] args) {
        RomScraper.main(args);
    }
}