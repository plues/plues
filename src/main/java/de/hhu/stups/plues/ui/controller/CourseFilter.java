package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import de.hhu.stups.plues.data.entities.Course;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CourseFilter extends VBox implements Initializable {

    @FXML
    @SuppressWarnings("unused")
    private TableView<Course> courseListView;

    @FXML
    @SuppressWarnings("unused")
    private TableColumn<Course, String> nameColumn;

    @FXML
    @SuppressWarnings("unused")
    private TableColumn<Course, String> poColumn;

    @FXML
    @SuppressWarnings("unused")
    private TableColumn<Course, String> kzfaColumn;

    @Inject
    public CourseFilter(FXMLLoader loader) {
        loader.setLocation(getClass().getResource("/fxml/CourseFilter.fxml"));

        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("shortName"));
        kzfaColumn.setCellValueFactory(new PropertyValueFactory<>("kzfa"));
        poColumn.setCellValueFactory(new PropertyValueFactory<>("po"));
    }

    @SuppressWarnings("unused")
    public void setCourses(List<Course> courses) {
        courseListView.setItems(FXCollections.observableArrayList(courses));
    }

    @SuppressWarnings("unused")
    ReadOnlyObjectProperty<Course> selectedItemProperty() {
        return courseListView.getSelectionModel().selectedItemProperty();
    }

    @SuppressWarnings("unused")
    Course selectedItem() {
        return courseListView.getSelectionModel().getSelectedItem();
    }
}