package com.rom.scraper.service;

import com.rom.scraper.model.RomFile;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service class that handles the ROM scraping logic.
 */
public class RomScraperService {
    private final ExecutorService executorService;
    private List<RomFile> romFiles;
    private final Pattern revisionPattern = Pattern.compile("\\(Rev (\\d+)\\)");
    private final String[] filterTerms = {"(demo", "(beta", "(pirate", "(sample", "virtual console"};
    private String currentExtension = "";

    // Status properties
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final BooleanProperty loading = new SimpleBooleanProperty(false);

    public RomScraperService(ExecutorService executorService) {
        this.executorService = executorService;
        this.romFiles = new ArrayList<>();
    }

    public boolean hasConnectionWithExtension(String extension) {
        return currentExtension.equals(extension);
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public void setStatusMessage(String message) {
        Platform.runLater(() -> statusMessage.set(message));
    }

    public void connectToUrl(String url, String fileExtension, Consumer<Boolean> callback) {
        setLoading(true);
        setStatusMessage("Connecting to " + url + "...");

        executorService.submit(() -> {
            boolean success = connectToUrlInternal(url, fileExtension);
            Platform.runLater(() -> {
                setLoading(false);
                if (success) {
                    setStatusMessage("Connected. Found " + romFiles.size() + " files.");
                } else {
                    setStatusMessage("Connection failed. Check the URL and try again.");
                }
                callback.accept(success);
            });
        });
    }

    private boolean connectToUrlInternal(String url, String fileExtension) {
        if (fileExtension == null) {
            fileExtension = "";
        }

        if (!fileExtension.isEmpty() && !fileExtension.startsWith(".")) {
            fileExtension = "." + fileExtension;
        }

        // Store the current extension
        this.currentExtension = fileExtension;

        try {
            Document doc = Jsoup.connect(url).get();
            Elements links = doc.select("a[href]");

            romFiles.clear();

            for (Element link : links) {
                String href = link.attr("href");
                if (fileExtension.isEmpty() || href.endsWith(fileExtension)) {
                    String romName = URLDecoder.decode(href, StandardCharsets.UTF_8);

                    // Filter out unwanted ROMs
                    if (!containsFilteredTerms(romName.toLowerCase())) {
                        RomFile romFile = new RomFile(
                                romName,
                                resolveUrl(url, href)
                        );
                        romFiles.add(romFile);
                    }
                }
            }

            return !romFiles.isEmpty();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void searchRoms(String searchTerm, String region, Consumer<List<RomFile>> callback) {
        setLoading(true);
        setStatusMessage("Searching for: " + searchTerm);

        executorService.submit(() -> {
            List<RomFile> results = searchRomsInternal(searchTerm, region);
            Platform.runLater(() -> {
                setLoading(false);
                setStatusMessage("Found " + results.size() + " results for: " + searchTerm);
                callback.accept(results);
            });
        });
    }

    private List<RomFile> searchRomsInternal(String searchTerm, String region) {
        if (romFiles.isEmpty()) {
            return Collections.emptyList();
        }

        searchTerm = searchTerm.toLowerCase();

        // First try exact matches
        String finalSearchTerm = searchTerm;
        List<RomFile> matches = romFiles.stream()
                .filter(rom -> rom.getName().toLowerCase().contains(finalSearchTerm))
                .collect(Collectors.toList());

        // If no exact matches, try fuzzy search
        if (matches.isEmpty()) {
            String finalSearchTerm1 = searchTerm;
            matches = romFiles.stream()
                    .filter(rom -> calculateSimilarity(finalSearchTerm1, rom.getName().toLowerCase()) >= 70)
                    .collect(Collectors.toList());
        }

        // Filter revisions to only keep the latest one
        matches = filterLatestRevisions(matches);

        // Apply region filtering if requested
        if (region != null && !matches.isEmpty()) {
            List<RomFile> regionMatches = matches.stream()
                    .filter(rom -> rom.getName().contains("(" + region + ")") ||
                            rom.getName().toLowerCase().contains("(world)"))
                    .collect(Collectors.toList());

            if (!regionMatches.isEmpty()) {
                return regionMatches;
            }
        }

        return matches;
    }

    public String detectMostCommonExtension(String url) {
        try {
            // Create a connection to the URL
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            // Check if connection was successful
            if (conn.getResponseCode() != 200) {
                return "";
            }

            // Read the first part of the page content
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {

                StringBuilder content = new StringBuilder();
                String line;
                int lineCount = 0;

                // Only read the first 100 lines to avoid large pages
                while ((line = reader.readLine()) != null && lineCount < 100) {
                    content.append(line);
                    lineCount++;
                }

                // Use regex to find file extensions in href attributes
                Pattern pattern = Pattern.compile("href=\"[^\"]*?(\\.[a-zA-Z0-9]{1,4})\"");
                Matcher matcher = pattern.matcher(content);

                // Count occurrences of each extension
                Map<String, Integer> extensionCounts = new HashMap<>();

                while (matcher.find()) {
                    String ext = matcher.group(1).toLowerCase();
                    extensionCounts.put(ext, extensionCounts.getOrDefault(ext, 0) + 1);
                }

                // Find the most common extension
                String mostCommonExt = "";
                int maxCount = 0;

                for (Map.Entry<String, Integer> entry : extensionCounts.entrySet()) {
                    if (entry.getValue() > maxCount) {
                        maxCount = entry.getValue();
                        mostCommonExt = entry.getKey();
                    }
                }

                return mostCommonExt;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public int getRomFilesCount() {
        return romFiles.size();
    }

    private void setLoading(boolean loading) {
        Platform.runLater(() -> this.loading.set(loading));
    }

    private boolean containsFilteredTerms(String name) {
        for (String term : filterTerms) {
            if (name.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private List<RomFile> filterLatestRevisions(List<RomFile> roms) {
        Map<String, RomFile> latestRevisions = new HashMap<>();

        for (RomFile rom : roms) {
            // Extract the base name (without revision) and remove all spaces
            String baseName = rom.getName().replaceAll("\\(Rev \\d+\\)", "").replace(" ", "");

            // Extract revision number if present
            Matcher revMatcher = revisionPattern.matcher(rom.getName());
            int revision = 0;
            if (revMatcher.find()) {
                revision = Integer.parseInt(revMatcher.group(1));
            }

            // Update if no entry exists or the current revision is higher
            if (!latestRevisions.containsKey(baseName) ||
                    getRevisionNumber(latestRevisions.get(baseName).getName()) < revision) {
                latestRevisions.put(baseName, rom);
            }
        }

        // Return only the latest ROMs
        return new ArrayList<>(latestRevisions.values());
    }

    private int getRevisionNumber(String romName) {
        Matcher matcher = revisionPattern.matcher(romName);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    private String resolveUrl(String base, String href) {
        if (href.startsWith("http")) {
            return href;
        }

        if (!base.endsWith("/")) {
            base = base + "/";
        }

        if (href.startsWith("/")) {
            href = href.substring(1);
        }

        return base + href;
    }

    // Simple fuzzy matching implementation (similar to FuzzyWuzzy's partial_ratio)
    private int calculateSimilarity(String s1, String s2) {
        // A basic implementation of partial string similarity
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0;
        }

        String shorter = s1.length() < s2.length() ? s1 : s2;
        String longer = s1.length() >= s2.length() ? s1 : s2;

        int maxScore = 0;

        // Slide the shorter string across the longer one
        for (int i = 0; i <= longer.length() - shorter.length(); i++) {
            String substring = longer.substring(i, i + shorter.length());
            int commonChars = countCommonChars(shorter, substring);
            int score = (commonChars * 100) / shorter.length();
            maxScore = Math.max(maxScore, score);
        }

        return maxScore;
    }

    private int countCommonChars(String s1, String s2) {
        int count = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (i < s2.length() && s1.charAt(i) == s2.charAt(i)) {
                count++;
            }
        }
        return count;
    }
}