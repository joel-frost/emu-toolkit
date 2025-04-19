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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service responsible for managing downloads with strict enforcement of parallel download limits.
 */
public class DownloadService {

    private final ObservableList<DownloadTask> downloadTasks;
    private final int BUFFER_SIZE = 8192;
    private final Map<DownloadTask, Future<?>> taskFutures;
    private final int PROGRESS_UPDATE_INTERVAL_MS = 100;

    // Fixed to 5 parallel downloads
    private final int maxParallelDownloads = 5;

    // Use a single-threaded executor to handle download queue management
    private final ExecutorService queueManagerExecutor = Executors.newSingleThreadExecutor();

    // Use a fixed executor for the actual downloads
    private final ExecutorService downloadExecutor;

    // Use a scheduled executor for delayed removal of cancelled tasks
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    // Queue for pending downloads
    private final Queue<DownloadTask> pendingDownloads = new ConcurrentLinkedQueue<>();

    // Set to track currently active downloads
    private final Set<DownloadTask> activeDownloads = Collections.synchronizedSet(new HashSet<>());

    // Lock to protect queue processing
    private final ReentrantLock queueLock = new ReentrantLock();

    public DownloadService() {
        this.downloadTasks = FXCollections.observableArrayList();
        this.taskFutures = new ConcurrentHashMap<>();

        // Initialize with a fixed thread pool of 5 parallel downloads
        this.downloadExecutor = Executors.newFixedThreadPool(maxParallelDownloads);
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
        final DownloadTask task = new DownloadTask(romFile.getName(), romFile.getUrl(), destFile.getPath());

        Platform.runLater(() -> {
            downloadTasks.add(task);
            task.setStatus("Queued");
        });

        // Add task to pending queue and process queue
        pendingDownloads.add(task);
        processDownloadQueue();
    }

    /**
     * Process the download queue in a single-threaded manner to ensure consistent state
     */
    private void processDownloadQueue() {
        // Use the queue manager executor to handle queue processing
        queueManagerExecutor.submit(() -> {
            try {
                queueLock.lock();

                // While we have capacity and pending downloads
                while (activeDownloads.size() < maxParallelDownloads && !pendingDownloads.isEmpty()) {
                    // Get next task from queue
                    DownloadTask nextTask = pendingDownloads.poll();
                    if (nextTask != null) {
                        // Mark as active before starting
                        activeDownloads.add(nextTask);

                        // Submit the download
                        Future<?> future = downloadExecutor.submit(() -> {
                            try {
                                // Update task status
                                Platform.runLater(() -> nextTask.setStatus("Downloading"));

                                // Perform download
                                downloadFile(nextTask);
                            } finally {
                                // Mark as inactive and process queue again
                                activeDownloads.remove(nextTask);
                                processDownloadQueue();
                            }
                        });

                        // Store future for cancellation
                        taskFutures.put(nextTask, future);
                    }
                }
            } finally {
                queueLock.unlock();
            }
        });
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
            connection.setConnectTimeout(15000); // 15 seconds connect timeout
            connection.setReadTimeout(30000);    // 30 seconds read timeout
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                HttpURLConnection finalConnection = connection;
                Platform.runLater(() -> {
                    try {
                        task.setStatus("Error: HTTP " + finalConnection.getResponseCode());
                    } catch (IOException e) {
                        task.setStatus("Error: " + e.getMessage());
                    }
                });
                return;
            }

            // Get file size
            long contentLength = connection.getContentLengthLong();
            boolean knownFileSize = contentLength > 0;

            // Set up streams
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(destFile)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long totalBytesRead = 0;

                long lastUpdateTime = System.currentTimeMillis();
                long startTime = System.currentTimeMillis();

                // For download speed calculation
                long lastSpeedUpdateTime = startTime;
                long bytesAtLastSpeedUpdate = 0;
                String currentSpeed = "Calculating...";

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    // Check if we should cancel
                    if (Thread.currentThread().isInterrupted()) {
                        Platform.runLater(() -> task.setStatus("Cancelled"));

                        // Close resources manually before returning
                        outputStream.close();

                        // Delete the partial file
                        if (destFile.exists()) {
                            destFile.delete();
                        }

                        // Schedule task removal after delay
                        scheduledExecutor.schedule(() -> {
                            Platform.runLater(() -> {
                                downloadTasks.remove(task);
                            });
                        }, 1, TimeUnit.SECONDS);

                        return;
                    }

                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    // Current time for update calculations
                    long currentTime = System.currentTimeMillis();

                    // Update download speed every 1 second
                    if (currentTime - lastSpeedUpdateTime > 1000) {
                        long bytesInInterval = totalBytesRead - bytesAtLastSpeedUpdate;
                        long timeInterval = currentTime - lastSpeedUpdateTime;

                        // Calculate speed
                        if (timeInterval > 0) {
                            double speedBps = (bytesInInterval * 1000.0) / timeInterval;
                            currentSpeed = formatFileSize(speedBps) + "/s";
                        }

                        lastSpeedUpdateTime = currentTime;
                        bytesAtLastSpeedUpdate = totalBytesRead;
                    }

                    // Update progress based on known file size
                    if (knownFileSize && (currentTime - lastUpdateTime > PROGRESS_UPDATE_INTERVAL_MS)) {
                        lastUpdateTime = currentTime;

                        // Calculate accurate progress
                        final double progress = Math.min(0.99, (double) totalBytesRead / contentLength);
                        final String speedString = currentSpeed;

                        Platform.runLater(() -> {
                            task.setProgress(progress);
                            task.setStatus("Downloading: " + speedString);
                        });
                    }
                    // For unknown file sizes, update status with speed only
                    else if (!knownFileSize && (currentTime - lastUpdateTime > PROGRESS_UPDATE_INTERVAL_MS)) {
                        lastUpdateTime = currentTime;

                        // Use a constant progress display for unknown size files
                        final double indeterminateProgress = 0.15;
                        final String speedString = currentSpeed;
                        final String downloadedSize = formatFileSize(totalBytesRead);

                        Platform.runLater(() -> {
                            task.setProgress(indeterminateProgress);
                            task.setStatus("Downloading: " + downloadedSize + " at " + speedString);
                        });
                    }
                }

                // Always ensure the final state is correctly set
                long finalTotalBytesRead = totalBytesRead;
                Platform.runLater(() -> {
                    task.setProgress(1.0); // Always set to 100% when download is complete
                    String finalSize = formatFileSize(finalTotalBytesRead);
                    task.setStatus("Complete: " + finalSize);
                });
            }

        } catch (IOException e) {
            e.printStackTrace();

            // Delete the partial file on error
            if (destFile.exists()) {
                destFile.delete();
            }

            Platform.runLater(() -> task.setStatus("Error: " + e.getMessage()));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // Helper method to format file size in human-readable format
    private String formatFileSize(double bytes) {
        if (bytes < 1024) {
            return String.format("%.0f B", bytes);
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024 * 1024 * 1024));
        }
    }

    public void cancelTask(DownloadTask task) {
        if (downloadTasks.contains(task)) {
            // Cancel the future if it exists
            Future<?> future = taskFutures.get(task);
            if (future != null && !future.isDone() && !future.isCancelled()) {
                future.cancel(true);
            }

            // Remove from active downloads if present
            activeDownloads.remove(task);

            // Remove from pending queue if present
            pendingDownloads.remove(task);

            Platform.runLater(() -> {
                task.setStatus("Cancelled");
            });

            // Delete the partially downloaded file
            File destFile = new File(task.getDestination());
            if (destFile.exists()) {
                destFile.delete();
            }

            // Schedule removal of the task after 1 second delay
            scheduledExecutor.schedule(() -> {
                Platform.runLater(() -> {
                    downloadTasks.remove(task);
                });
            }, 1, TimeUnit.SECONDS);

            // Process queue to start next download
            processDownloadQueue();
        }
    }

    /**
     * Cancels all downloads that are currently in progress or queued.
     */
    public void cancelAllTasks() {
        try {
            queueLock.lock();

            // Create a list of tasks to cancel to avoid concurrent modification
            List<DownloadTask> tasksToCancel = new ArrayList<>();

            // Find all tasks that are active or queued
            for (DownloadTask task : downloadTasks) {
                String status = task.getStatus();
                if (status.equals("Queued") || status.startsWith("Downloading")) {
                    tasksToCancel.add(task);
                }
            }

            // Cancel each task
            for (DownloadTask task : tasksToCancel) {
                cancelTask(task);
            }

        } finally {
            queueLock.unlock();
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

                // Also remove any associated futures
                taskFutures.remove(task);
            }
        }

        // Now remove all tasks in the secondary list
        Platform.runLater(() -> downloadTasks.removeAll(tasksToRemove));
    }

    public boolean canClearTasks() {
        // Can clear if any tasks are in a final state (Complete, Cancelled, or Error)
        for (DownloadTask task : downloadTasks) {
            String status = task.getStatus();
            if ("Complete".equals(status) || "Cancelled".equals(status) ||
                    status.startsWith("Error")) {
                return true;
            }
        }
        return false;
    }

    public void shutdown() {
        if (downloadExecutor != null) {
            downloadExecutor.shutdownNow();
        }
        queueManagerExecutor.shutdownNow();
        scheduledExecutor.shutdownNow();
    }
}