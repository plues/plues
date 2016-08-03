package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.AbstractStore;
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

    private final Delayed<AbstractStore> delayedStore;

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
    public CourseFilter(FXMLLoader loader, Delayed<AbstractStore> delayedStore) {
        this.delayedStore = delayedStore;
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
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("short_name"));
        kzfaColumn.setCellValueFactory(new PropertyValueFactory<>("kzfa"));
        poColumn.setCellValueFactory(new PropertyValueFactory<>("po"));

        delayedStore.whenAvailable(s -> this.initializeCourseListView(s.getCourses()));
    }

    private void initializeCourseListView(List<Course> courses) {
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