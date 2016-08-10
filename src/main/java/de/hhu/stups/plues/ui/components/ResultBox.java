package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import de.hhu.stups.plues.data.entities.Course;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ResultBox extends GridPane implements Initializable {

    private BooleanProperty feasible;
    private ObjectProperty<Course> majorCourse, minorCourse;

    @FXML
    @SuppressWarnings("unused")
    private Pane icon;

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

    @FXML
    @SuppressWarnings("unused")
    private Button cancel;

    @Inject
    public ResultBox(final FXMLLoader loader) {
        majorCourse = new SimpleObjectProperty<>();
        minorCourse = new SimpleObjectProperty<>();
        feasible = new SimpleBooleanProperty();

        loader.setLocation(getClass().getResource("/fxml/components/resultbox.fxml"));

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
        major.textProperty().bind(Bindings.selectString(this.majorCourse, "fullName"));
        minor.textProperty().bind(Bindings.selectString(this.minorCourse, "fullName"));

        ProgressIndicator progressIndicator = new ProgressIndicator();

        progressIndicator.setPrefSize(100, 100);
        progressIndicator.setStyle(" -fx-progress-color: #BDE5F8;");

        icon.getChildren().add(progressIndicator);

        cancel.setDisable(false);

        feasible.addListener(((observable, oldValue, newValue) -> {
            Label label = new Label();
            label.setPrefSize(100, 100);
            label.setAlignment(Pos.CENTER);
            if (newValue) {
                FontAwesomeIconFactory.get().setIcon(label, FontAwesomeIcon.CHECK, "100");
                label.setStyle("-fx-background-color: #DFF2BF");
                show.setDisable(false);
                download.setDisable(false);
                cancel.setDisable(true);
            } else {
                FontAwesomeIconFactory.get().setIcon(label, FontAwesomeIcon.REMOVE, "100");
                label.setStyle("-fx-background-color: #FFBABA");
                show.setDisable(true);
                download.setDisable(true);
                cancel.setDisable(true);
            }
            icon.getChildren().clear();
            icon.getChildren().add(label);
        }));
    }

    @FXML
    @SuppressWarnings("unused")
    public void showPdf() {

    }

    @FXML
    @SuppressWarnings("unused")
    public void downloadPdf() {

    }

    @FXML
    @SuppressWarnings("unused")
    public void interrupt() {
        Label label = new Label();
        label.setPrefSize(100, 100);
        label.setAlignment(Pos.CENTER);
        FontAwesomeIconFactory.get().setIcon(label, FontAwesomeIcon.QUESTION, "100");
        label.setStyle("-fx-background-color: #FEEFB3");
        icon.getChildren().clear();
        icon.getChildren().add(label);
        cancel.setDisable(true);
    }

    public void setMajorCourse(Course majorCourse) {
        this.majorCourse.set(majorCourse);
    }

    public void setMinorCourse(Course minorCourse) {
        this.minorCourse.set(minorCourse);
    }

    public void setFeasible(boolean feasible) {
        this.feasible.set(feasible);
    }
}
