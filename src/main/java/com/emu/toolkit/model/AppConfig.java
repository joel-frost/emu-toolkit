package com.emu.toolkit.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Configuration manager that handles all platform configs and application settings
 */
@Getter
public class AppConfig {
    private final ObservableList<PlatformConfig> platforms = FXCollections.observableArrayList();
    @Setter private boolean advancedMode = false;
    @Setter private boolean wizardShown = false;
    @Setter private String lastDownloadFolder = "";
    @Setter private PlatformConfig selectedPlatform = null;

    public AppConfig() {
        initDefaultPlatforms();
    }

    /**
     * Initialize the default list of supported platforms
     */
    private void initDefaultPlatforms() {
        platforms.addAll(
                new PlatformConfig("nes", "Nintendo Entertainment System"),
                new PlatformConfig("snes", "Super Nintendo Entertainment System"),
                new PlatformConfig("gb", "Game Boy"),
                new PlatformConfig("gbc", "Game Boy Color"),
                new PlatformConfig("gba", "Game Boy Advance"),
                new PlatformConfig("n64", "Nintendo 64"),
                new PlatformConfig("nds", "Nintendo DS"),
                new PlatformConfig("3ds", "Nintendo 3DS"),
                new PlatformConfig("gc", "Nintendo GameCube"),
                new PlatformConfig("wii", "Nintendo Wii"),
                new PlatformConfig("wiiu", "Nintendo Wii U"),
                new PlatformConfig("ps1", "Sony PlayStation"),
                new PlatformConfig("ps2", "Sony PlayStation 2"),
                new PlatformConfig("psp", "Sony PlayStation Portable"),
                new PlatformConfig("psvita", "Sony PlayStation Vita"),
                new PlatformConfig("xbox", "Microsoft Xbox"),
                new PlatformConfig("32x", "Sega 32X"),
                new PlatformConfig("gg", "Sega Game Gear"),
                new PlatformConfig("sms", "Sega Master System"),
                new PlatformConfig("genesis", "Sega Megadrive/Genesis"),
                new PlatformConfig("dreamcast", "Sega Dreamcast"),
                new PlatformConfig("saturn", "Sega Saturn"),
                new PlatformConfig("segacd", "Sega CD")
        );
    }

    /**
     * Find a platform by its ID
     */
    public PlatformConfig getPlatformById(String id) {
        return platforms.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if any platforms are configured
     */
    public boolean hasNotConfiguredPlatforms() {
        return platforms.stream().noneMatch(PlatformConfig::isConfigured);
    }

    /**
     * Get a list of all configured platforms
     */
    public List<PlatformConfig> getConfiguredPlatforms() {
        List<PlatformConfig> configured = new ArrayList<>();
        for (PlatformConfig platform : platforms) {
            if (platform.isConfigured()) {
                configured.add(platform);
            }
        }
        return configured;
    }

    /**
     * Convert the configuration to a Map for JSON serialization
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("advancedMode", advancedMode);
        map.put("wizardShown", wizardShown);
        map.put("lastDownloadFolder", lastDownloadFolder);

        List<Map<String, String>> platformList = new ArrayList<>();
        for (PlatformConfig platform : platforms) {
            if (platform.isConfigured()) {
                Map<String, String> platformMap = new HashMap<>();
                platformMap.put("id", platform.getId());
                platformMap.put("url", platform.getUrl());
                platformMap.put("extension", platform.getFileExtension());
                platformList.add(platformMap);
            }
        }
        map.put("platforms", platformList);

        if (selectedPlatform != null) {
            map.put("selectedPlatform", selectedPlatform.getId());
        }

        return map;
    }
}
