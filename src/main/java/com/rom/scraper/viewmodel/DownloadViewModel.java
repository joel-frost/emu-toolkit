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

    public void cancelDownload(DownloadTask task) {
        if (task != null) {
            downloadService.cancelTask(task);
        }
    }

    /**
     * Cancels all active and queued downloads.
     */
    public void cancelAllDownloads() {
        downloadService.cancelAllTasks();
    }

    public void clearCompletedDownloads() {
        downloadService.clearCompletedTasks();
    }

    /**
     * Checks if there are any completed, cancelled, or error tasks that can be cleared.
     * This method is used to determine whether the "Clear Completed" button should be enabled.
     */
    public boolean canClearTasks() {
        return downloadService.canClearTasks();
    }

    /**
     * Checks if there are any active or queued downloads.
     * This method is used to determine whether the "Cancel All" button should be enabled.
     */
    public boolean hasActiveDownloads() {
        for (DownloadTask task : getDownloadTasks()) {
            String status = task.getStatus();
            if (status != null && (status.equals("Queued") ||
                    status.equals("Downloading") ||
                    status.startsWith("Downloading:"))) {
                return true;
            }
        }
        return false;
    }

    // Getter for properties
    public ObjectProperty<DownloadTask> selectedTaskProperty() {
        return selectedTaskProperty;
    }
}