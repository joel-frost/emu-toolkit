package com.rom.scraper;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a single download task with observable properties for UI binding.
 */
public class DownloadTask {

    private final StringProperty filename;
    private final String url;
    private final String destination;
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

    public String getUrl() {
        return url;
    }

    public String getDestination() {
        return destination;
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