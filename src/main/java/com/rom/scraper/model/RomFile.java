package com.rom.scraper.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a ROM file with name and URL.
 */
public class RomFile {
    private final StringProperty name;
    private final String url;

    public RomFile(String name, String url) {
        this.name = new SimpleStringProperty(name);
        this.url = url;
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return getName();
    }
}