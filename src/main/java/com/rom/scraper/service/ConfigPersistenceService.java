package com.rom.scraper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rom.scraper.model.AppConfig;
import com.rom.scraper.model.PlatformConfig;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for saving and loading application configuration.
 */
public class ConfigPersistenceService {
    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".rom-scraper";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.json";
    private final ObjectMapper objectMapper;

    public ConfigPersistenceService() {
        this.objectMapper = new ObjectMapper();
        ensureConfigDirectory();
    }

    /**
     * Ensure the configuration directory exists
     */
    private void ensureConfigDirectory() {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
    }

    /**
     * Save the application configuration to JSON
     */
    public void saveConfig(AppConfig config) throws IOException {
        File configFile = new File(CONFIG_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, config.toMap());
    }

    /**
     * Load the application configuration from JSON
     */
    public AppConfig loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            return new AppConfig(); // Return default config if file doesn't exist
        }

        try {
            Map<String, Object> configMap = objectMapper.readValue(configFile, Map.class);
            AppConfig config = new AppConfig();

            // Load basic settings
            if (configMap.containsKey("advancedMode")) {
                config.setAdvancedMode((Boolean) configMap.get("advancedMode"));
            }

            if (configMap.containsKey("wizardShown")) {
                config.setWizardShown((Boolean) configMap.get("wizardShown"));
            }

            if (configMap.containsKey("lastDownloadFolder")) {
                config.setLastDownloadFolder((String) configMap.get("lastDownloadFolder"));
            }

            // Load platform configurations
            if (configMap.containsKey("platforms")) {
                List<Map<String, String>> platforms = (List<Map<String, String>>) configMap.get("platforms");

                for (Map<String, String> platformMap : platforms) {
                    String id = platformMap.get("id");
                    PlatformConfig platform = config.getPlatformById(id);

                    if (platform != null) {
                        platform.setUrl(platformMap.get("url"));
                        platform.setFileExtension(platformMap.getOrDefault("extension", ""));
                    }
                }
            }

            // Set selected platform
            if (configMap.containsKey("selectedPlatform")) {
                String selectedId = (String) configMap.get("selectedPlatform");
                PlatformConfig selectedPlatform = config.getPlatformById(selectedId);
                config.setSelectedPlatform(selectedPlatform);
            }

            return config;

        } catch (IOException e) {
            e.printStackTrace();
            return new AppConfig(); // Return default config on error
        }
    }

    /**
     * Check if the configuration file exists
     */
    public boolean configExists() {
        return new File(CONFIG_FILE).exists();
    }
}