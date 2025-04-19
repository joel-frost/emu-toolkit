package com.rom.scraper.util;

import com.rom.scraper.model.RomFile;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Utility class for showing dialogs.
 */
public class DialogHelper {

    /**
     * Shows an error dialog.
     */
    public static void showError(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);
        alert.showAndWait();
    }

    /**
     * Shows an information dialog.
     */
    public static void showInfo(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);
        alert.showAndWait();
    }

    /**
     * Shows a dialog for selecting one ROM from a list of matches.
     */
    public static void showMultipleMatchesDialog(Window owner, String game, List<RomFile> matches, Consumer<RomFile> callback) {
        Dialog<RomFile> dialog = new Dialog<>();
        dialog.setTitle("Multiple Matches Found");
        dialog.setHeaderText("Select a ROM file for: " + game);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        // Buttons
        ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        ButtonType skipButtonType = new ButtonType("Skip", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, skipButtonType);

        // Create content
        ListView<RomFile> listView = new ListView<>();
        listView.getItems().addAll(matches);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(RomFile item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getName());
            }
        });

        // Select the first item by default
        if (!matches.isEmpty()) {
            listView.getSelectionModel().select(0);
        }

        // Make the dialog a bit bigger
        listView.setPrefWidth(600);
        listView.setPrefHeight(300);

        VBox content = new VBox(10);
        content.getChildren().add(new Label("Please select one ROM from the list below:"));
        content.getChildren().add(listView);
        dialog.getDialogPane().setContent(content);

        // Make sure the select button is only enabled when an item is selected
        Button selectButton = (Button) dialog.getDialogPane().lookupButton(selectButtonType);
        selectButton.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        // Show dialog and process result
        Optional<RomFile> result = dialog.showAndWait();
        callback.accept(result.orElse(null));
    }

    /**
     * Shows a confirmation dialog.
     */
    public static boolean showConfirmation(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}