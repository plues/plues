package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ResultBox extends HBox implements Initializable {

    private boolean feasible;
    private Course majorCourse, minorCourse;

    @FXML
    @SuppressWarnings("unused")
    private HBox box;

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
    public ResultBox(final FXMLLoader loader, boolean feasible, Course majorCourse, Course minorCourse) {
        this.feasible = feasible;
        this.majorCourse = majorCourse;
        this.minorCourse = minorCourse;

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
        major.setText(majorCourse.getFullName());
        minor.setText(minorCourse.getFullName());

        if (feasible) {
            image.setImage(new Image(String.valueOf(getClass().getResource("/Haken.png"))));
            show.setVisible(true);
            download.setVisible(true);
        } else {
            image.setImage(new Image(String.valueOf(getClass().getResource("/Kreuz.png"))));
            show.setVisible(false);
            download.setVisible(false);
        }
    }

    public final void initComponents(Store store) {

    }

    public void showPdf() {

    }

    public void downloadPdf() {

    }
}
