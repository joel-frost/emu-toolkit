package com.rom.scraper.viewmodel;

import com.rom.scraper.model.DownloadTask;
import com.rom.scraper.service.DownloadService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * ViewModel for the download manager screen.
 */
public class DownloadViewModel {
    private final DownloadService downloadService;

    // Properties
    private final ObjectProperty<DownloadTask> selectedTaskProperty = new SimpleObjectProperty<>();
    private final BooleanProperty canClearProperty = new SimpleBooleanProperty(false);

    public DownloadViewModel(DownloadService downloadService) {
        this.downloadService = downloadService;

        // Monitor the download tasks for changes to update the canClear property
        downloadService.getDownloadTasks().addListener((ListChangeListener<DownloadTask>) c -> {
            updateCanClearProperty();
        });

        // Also monitor for status changes in the tasks
        downloadService.getDownloadTasks().addListener((ListChangeListener<DownloadTask>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (DownloadTask task : c.getAddedSubList()) {
                        task.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                            updateCanClearProperty();
                        });
                    }
                }
            }
        });
    }

    private void updateCanClearProperty() {
        canClearProperty.set(downloadService.canClearTasks());
    }

    public ObservableList<DownloadTask> getDownloadTasks() {
        return downloadService.getDownloadTasks();
    }

    public void cancelDownload(DownloadTask task) {
        if (task != null) {
            downloadService.cancelTask(task);
        }
    }

    public void clearCompletedDownloads() {
        downloadService.clearCompletedTasks();
    }

    // Getter for properties
    public ObjectProperty<DownloadTask> selectedTaskProperty() {
        return selectedTaskProperty;
    }

    public BooleanProperty canClearProperty() {
        return canClearProperty;
    }
}