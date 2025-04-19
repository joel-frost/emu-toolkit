package com.rom.scraper.service;

import com.rom.scraper.model.DownloadTask;
import com.rom.scraper.model.RomFile;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Service responsible for managing downloads.
 */
public class DownloadService {

    private final ObservableList<DownloadTask> downloadTasks;
    private final ExecutorService downloadExecutor;
    private final int BUFFER_SIZE = 8192;
    private final Map<DownloadTask, Future<?>> taskFutures;

    public DownloadService() {
        this.downloadTasks = FXCollections.observableArrayList();
        this.downloadExecutor = Executors.newFixedThreadPool(5);
        this.taskFutures = new HashMap<>();
    }

    public ObservableList<DownloadTask> getDownloadTasks() {
        return downloadTasks;
    }

    public void addToQueue(RomFile romFile, String destinationFolder) {
        // Check if file already exists
        File destFile = new File(destinationFolder, romFile.getName());
        if (destFile.exists() && destFile.length() > 0) {
            // Create a task that's already complete
            DownloadTask task = new DownloadTask(romFile.getName(), romFile.getUrl(), destFile.getPath());
            task.setProgress(1.0);
            task.setStatus("Already exists");

            Platform.runLater(() -> {
                downloadTasks.add(task);
                // Remove it after a short delay
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        Platform.runLater(() -> downloadTasks.remove(task));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            });
            return;
        }

        // Check if already in queue
        for (DownloadTask existingTask : downloadTasks) {
            if (existingTask.getFilename().equals(romFile.getName())) {
                return; // Already in queue
            }
        }

        // Create new download task
        DownloadTask task = new DownloadTask(romFile.getName(), romFile.getUrl(), destFile.getPath());
        Platform.runLater(() -> downloadTasks.add(task));

        // Start download
        Future<?> future = downloadExecutor.submit(() -> downloadFile(task));
        taskFutures.put(task, future);
    }

    private void downloadFile(DownloadTask task) {
        // Make sure the destination directory exists
        File destFile = new File(task.getDestination());
        File parent = destFile.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean created = parent.mkdirs();
            if (!created) {
                Platform.runLater(() -> task.setStatus("Error: Cannot create directory"));
                return;
            }
        }

        HttpURLConnection connection = null;
        try {
            // Set up connection
            URL url = new URL(task.getUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                HttpURLConnection finalConnection = connection;
                Platform.runLater(() -> {
                    try {
                        task.setStatus("Error: HTTP " + finalConnection.getResponseCode());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                return;
            }

            // Get file size
            int contentLength = connection.getContentLength();

            // Set up streams
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(destFile)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long totalBytesRead = 0;
                Platform.runLater(() -> task.setStatus("Downloading"));

                long lastUpdateTime = System.currentTimeMillis();

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) {
                        Platform.runLater(() -> task.setStatus("Cancelled"));
                        return;
                    }

                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    // Update progress - throttled to update at most 5 times per second
                    if (contentLength > 0) {
                        final double progress = (double) totalBytesRead / contentLength;
                        long currentTime = System.currentTimeMillis();

                        if (currentTime - lastUpdateTime > 200 || progress >= 1.0) {
                            lastUpdateTime = currentTime;
                            Platform.runLater(() -> task.setProgress(progress));
                        }
                    }
                }

                // Always ensure the final state is correctly set
                Platform.runLater(() -> {
                    task.setProgress(1.0);
                    task.setStatus("Complete");
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
            Platform.runLater(() -> task.setStatus("Error: " + e.getMessage()));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void pauseAll() {
        // Implementation would depend on a more complex download system
        // that supports pausing. For simplicity, this is left as a stub.
        for (DownloadTask task : downloadTasks) {
            if ("Downloading".equals(task.getStatus())) {
                task.setStatus("Paused");
            }
        }
    }

    public void resumeAll() {
        // Implementation would depend on a more complex download system
        // that supports resuming. For simplicity, this is left as a stub.
        for (DownloadTask task : downloadTasks) {
            if ("Paused".equals(task.getStatus())) {
                task.setStatus("Downloading");
            }
        }
    }

    public void cancelTask(DownloadTask task) {
        if (downloadTasks.contains(task)) {
            // Cancel the future if it exists
            Future<?> future = taskFutures.get(task);
            if (future != null && !future.isDone() && !future.isCancelled()) {
                future.cancel(true);
            }

            Platform.runLater(() -> {
                task.setStatus("Cancelled");
                // Optionally remove from the list
                // downloadTasks.remove(task);
            });
        }
    }

    public void setParallelDownloads(int count) {
        // In a more advanced implementation, we would recreate the ExecutorService
        // with the new thread count. For simplicity, we'll just note it here.
        System.out.println("Setting parallel downloads to: " + count);
    }

    public void shutdown() {
        downloadExecutor.shutdownNow();
    }
}