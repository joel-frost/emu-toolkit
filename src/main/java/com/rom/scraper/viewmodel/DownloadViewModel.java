package com.rom.scraper.viewmodel;

import com.rom.scraper.model.DownloadTask;
import com.rom.scraper.service.DownloadService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;

/**
 * ViewModel for the download manager screen.
 */
public class DownloadViewModel {
    private final DownloadService downloadService;

    // Properties
    private final ObjectProperty<DownloadTask> selectedTaskProperty = new SimpleObjectProperty<>();

    public DownloadViewModel(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    public ObservableList<DownloadTask> getDownloadTasks() {
        return downloadService.getDownloadTasks();
    }

    public void pauseAllDownloads() {
        downloadService.pauseAll();
    }

    public void resumeAllDownloads() {
        downloadService.resumeAll();
    }

    public void cancelSelectedDownload() {
        DownloadTask selectedTask = selectedTaskProperty.get();
        if (selectedTask != null) {
            downloadService.cancelTask(selectedTask);
        }
    }

    // Getter for property
    public ObjectProperty<DownloadTask> selectedTaskProperty() {
        return selectedTaskProperty;
    }
}