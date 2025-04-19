package com.rom.scraper.model;

import javafx.beans.property.*;
import lombok.Getter;

/**
 * Represents a single download task with observable properties for UI binding.
 */
public class DownloadTask {

    @Getter private final String url;
    @Getter private final String destination;
    private final StringProperty filename;
    private final DoubleProperty progress;
    private final StringProperty status;

    public DownloadTask(String filename, String url, String destination) {
        this.filename = new SimpleStringProperty(filename);
        this.url = url;
        this.destination = destination;
        this.progress = new SimpleDoubleProperty(0.0);
        this.status = new SimpleStringProperty("Queued");
    }

    public String getFilename() {
        return filename.get();
    }

    public StringProperty filenameProperty() {
        return filename;
    }

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public void setProgress(double value) {
        this.progress.set(value);
    }

    public String getStatus() {
        return status.get();
    }

    public StringProperty statusProperty() {
        return status;
    }

    public void setStatus(String value) {
        this.status.set(value);
    }
}