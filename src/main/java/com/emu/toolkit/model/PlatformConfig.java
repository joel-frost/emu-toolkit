package com.emu.toolkit.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

/**
 * Represents a gaming platform configuration
 */
public class PlatformConfig {
    @Getter private final String id;
    @Getter private final String name;
    private final StringProperty url = new SimpleStringProperty("");
    private final StringProperty fileExtension = new SimpleStringProperty("");
    private final StringProperty defaultRegion = new SimpleStringProperty("Any");

    public PlatformConfig(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public PlatformConfig(String id, String name, String url, String fileExtension) {
        this.id = id;
        this.name = name;
        this.url.set(url);
        this.fileExtension.set(fileExtension);
    }

    public PlatformConfig(String id, String name, String url, String fileExtension, String defaultRegion) {
        this.id = id;
        this.name = name;
        this.url.set(url);
        this.fileExtension.set(fileExtension);
        this.defaultRegion.set(defaultRegion);
    }

    public String getUrl() {
        return url.get();
    }

    public void setUrl(String url) {
        this.url.set(url);
    }

    public StringProperty urlProperty() {
        return url;
    }

    public String getFileExtension() {
        return fileExtension.get();
    }

    public void setFileExtension(String extension) {
        this.fileExtension.set(extension);
    }

    public StringProperty fileExtensionProperty() {
        return fileExtension;
    }

    public String getDefaultRegion() {
        return defaultRegion.get();
    }

    public void setDefaultRegion(String region) {
        this.defaultRegion.set(region);
    }

    public StringProperty defaultRegionProperty() {
        return defaultRegion;
    }

    public boolean isConfigured() {
        return url.get() != null && !url.get().isEmpty();
    }
}