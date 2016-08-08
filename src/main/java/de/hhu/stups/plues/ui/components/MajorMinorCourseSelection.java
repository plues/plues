package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import de.hhu.stups.plues.data.entities.Course;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class MajorMinorCourseSelection extends VBox implements Initializable{

    private ComboBox<Course> cbMajor;
    private ComboBox<Course> cbMinor;


    @Inject
    public MajorMinorCourseSelection(final FXMLLoader loader){
        this.cbMajor = new ComboBox<>();
        this.cbMinor = new ComboBox<>();

        cbMajor.prefHeight(30);
        cbMajor.prefWidth(400);

        cbMinor.prefHeight(cbMajor.getHeight());
        cbMinor.prefWidth(cbMajor.getWidth());


    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
