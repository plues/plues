package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ResultBox extends HBox implements Initializable {

    @FXML
    @SuppressWarnings("usused")
    private ImageView image;

    @FXML
    @SuppressWarnings("unused")
    private Label major;

    @FXML
    @SuppressWarnings("unused")
    private Label minor;

    @FXML
    @SuppressWarnings("unused")
    private Button show;

    @FXML
    @SuppressWarnings("unused")
    private Button download;

    @Inject
    public ResultBox(final FXMLLoader loader) {
        loader.setLocation(getClass().getResource("/fxml/resultbox.fxml"));

        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch(final IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        major.setText("MAJOR");
        minor.setText("MINOR");
    }
}
