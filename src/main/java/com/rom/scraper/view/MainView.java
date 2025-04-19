package com.rom.scraper.view;

import com.rom.scraper.viewmodel.BatchViewModel;
import com.rom.scraper.viewmodel.ConfigViewModel;
import com.rom.scraper.viewmodel.DownloadViewModel;
import com.rom.scraper.viewmodel.SearchViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Main view for the application.
 * Contains the configuration panel and tab pane with different views.
 */
public class MainView {
    private final ConfigViewModel configViewModel;
    private final SearchViewModel searchViewModel;
    private final BatchViewModel batchViewModel;
    private final DownloadViewModel downloadViewModel;

    // UI Components
    private ConfigView configView;
    private ProgressIndicator loadingIndicator;
    private Label statusLabel;

    public MainView(
            ConfigViewModel configViewModel,
            SearchViewModel searchViewModel,
            BatchViewModel batchViewModel,
            DownloadViewModel downloadViewModel) {
        this.configViewModel = configViewModel;
        this.searchViewModel = searchViewModel;
        this.batchViewModel = batchViewModel;
        this.downloadViewModel = downloadViewModel;
    }

    public void initialize(Stage primaryStage) {
        // Create main layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Create configuration panel
        configView = new ConfigView(configViewModel);
        VBox configPanel = configView.createView();

        // Create main tab pane for different views
        TabPane mainTabPane = new TabPane();

        // Create tabs
        SearchView searchView = new SearchView(searchViewModel, configViewModel);
        BatchView batchView = new BatchView(batchViewModel, configViewModel);
        DownloadView downloadView = new DownloadView(downloadViewModel);

        Tab searchTab = new Tab("Search", searchView.createView());
        Tab batchTab = new Tab("Batch Processing", batchView.createView());
        Tab downloadsTab = new Tab("Downloads", downloadView.createView());

        mainTabPane.getTabs().addAll(searchTab, batchTab, downloadsTab);
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Create status bar
        HBox statusBar = createStatusBar();

        // Assemble layout
        root.setTop(configPanel);
        root.setCenter(mainTabPane);
        root.setBottom(statusBar);

        // Set the scene
        Scene scene = new Scene(root, 800, 750);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        statusBar.setAlignment(Pos.CENTER_LEFT);

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(20, 20);
        loadingIndicator.visibleProperty().bind(configViewModel.loadingProperty());

        statusLabel = new Label();
        statusLabel.textProperty().bind(configViewModel.statusMessageProperty());

        statusBar.getChildren().addAll(loadingIndicator, statusLabel);

        return statusBar;
    }
}