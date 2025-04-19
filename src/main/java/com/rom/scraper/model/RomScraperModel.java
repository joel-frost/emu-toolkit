package com.rom.scraper.model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Model class that handles the ROM scraping logic.
 */
public class RomScraperModel {

    private List<RomFile> romFiles;
    private Pattern revisionPattern = Pattern.compile("\\(Rev (\\d+)\\)");
    private String[] filterTerms = {"(demo", "(beta", "(pirate", "(sample", "virtual console"};

    public RomScraperModel() {
        this.romFiles = new ArrayList<>();
    }

    public boolean connectToUrl(String url, String fileExtension) {
        if (!fileExtension.startsWith(".")) {
            fileExtension = "." + fileExtension;
        }

        try {
            Document doc = Jsoup.connect(url).get();
            Elements links = doc.select("a[href]");

            romFiles.clear();

            for (Element link : links) {
                String href = link.attr("href");
                if (href.endsWith(fileExtension)) {
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

    public List<RomFile> searchRoms(String searchTerm, String region) {
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

    public int getRomFilesCount() {
        return romFiles.size();
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