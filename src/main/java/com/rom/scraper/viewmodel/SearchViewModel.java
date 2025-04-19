package com.rom.scraper.viewmodel;

import com.rom.scraper.model.RomFile;
import com.rom.scraper.service.DownloadService;
import com.rom.scraper.service.RomScraperService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * ViewModel that manages the search functionality.
 */
public class SearchViewModel {
    private final RomScraperService romScraperService;
    private final DownloadService downloadService;
    private final ConfigViewModel configViewModel;

    // Properties
    private final StringProperty searchTermProperty = new SimpleStringProperty("");
    private final ListProperty<RomFile> searchResultsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<RomFile> selectedRomProperty = new SimpleObjectProperty<>();
    private final BooleanProperty downloadButtonEnabledProperty = new SimpleBooleanProperty(false);

    public SearchViewModel(RomScraperService romScraperService, DownloadService downloadService, ConfigViewModel configViewModel) {
        this.romScraperService = romScraperService;
        this.downloadService = downloadService;
        this.configViewModel = configViewModel;

        // Bind the download button enabled state to whether a ROM is selected
        selectedRomProperty.addListener((obs, oldValue, newValue) ->
                downloadButtonEnabledProperty.set(newValue != null));
    }

    public void performSearch() {
        String searchTerm = searchTermProperty.get().trim();
        if (searchTerm.isEmpty()) {
            return;
        }

        String region = configViewModel.getSelectedRegion();

        romScraperService.searchRoms(searchTerm, region, results -> {
            searchResultsProperty.set(FXCollections.observableArrayList(results));
        });
    }

    public void downloadSelectedRom(String destinationFolder) {
        RomFile selectedRom = selectedRomProperty.get();
        if (selectedRom != null) {
            downloadService.addToQueue(selectedRom, destinationFolder);
        }
    }

    // Getters for properties
    public StringProperty searchTermProperty() {
        return searchTermProperty;
    }

    public ListProperty<RomFile> searchResultsProperty() {
        return searchResultsProperty;
    }

    public ObjectProperty<RomFile> selectedRomProperty() {
        return selectedRomProperty;
    }

    public BooleanProperty downloadButtonEnabledProperty() {
        return downloadButtonEnabledProperty;
    }
}