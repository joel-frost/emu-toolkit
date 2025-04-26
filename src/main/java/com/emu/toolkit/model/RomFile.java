package com.emu.toolkit.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a ROM file with name and URL.
 */
@ToString(onlyExplicitlyIncluded = true)
public class RomFile {
    private final StringProperty name;
    @Getter private final String url;

    public RomFile(String name, String url) {
        this.name = new SimpleStringProperty(name);
        this.url = url;
    }

    @ToString.Include
    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }
}