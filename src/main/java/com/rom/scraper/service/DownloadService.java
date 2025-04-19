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
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

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
            task.setStatus("Complete");

            Platform.runLater(() -> {
                downloadTasks.add(task);
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

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
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
                    // Check if we should cancel
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

    // Removed pauseAll(), resumeAll(), pauseTask(), and resumeTask() as requested

    public void cancelTask(DownloadTask task) {
        if (downloadTasks.contains(task)) {
            // Cancel the future if it exists
            Future<?> future = taskFutures.get(task);
            if (future != null && !future.isDone() && !future.isCancelled()) {
                future.cancel(true);
            }

            Platform.runLater(() -> {
                task.setStatus("Cancelled");
            });
        }
    }

    public void clearCompletedTasks() {
        // Create a list to store tasks to be removed
        // (to avoid ConcurrentModificationException)
        ObservableList<DownloadTask> tasksToRemove = FXCollections.observableArrayList();

        for (DownloadTask task : downloadTasks) {
            String status = task.getStatus();
            if ("Complete".equals(status) || "Cancelled".equals(status) ||
                    status.startsWith("Error")) {
                tasksToRemove.add(task);
            }
        }

        // Now remove all tasks in the secondary list
        Platform.runLater(() -> downloadTasks.removeAll(tasksToRemove));
    }

    public boolean canClearTasks() {
        // Can clear if all tasks are in a final state
        for (DownloadTask task : downloadTasks) {
            String status = task.getStatus();
            if (!"Complete".equals(status) && !"Cancelled".equals(status) &&
                    !status.startsWith("Error")) {
                return false;
            }
        }
        return !downloadTasks.isEmpty();
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