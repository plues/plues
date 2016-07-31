package de.hhu.stups.plues.ui.controller;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import de.hhu.stups.plues.data.AbstractStore;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.events.CourseSelectionChanged;
import javafx.beans.property.ObjectProperty;
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

import static tlc2.util.SimpUtil.store;

public class CourseFilter extends VBox implements Initializable {

    private final EventBus eventBus;
    private final ObjectProperty<AbstractStore> storeProperty;

    @FXML
    TableView<Course> courseListView;

    @FXML
    TableColumn<Course, String> nameColumn;

    @FXML
    TableColumn<Course, String> poColumn;

    @FXML
    TableColumn<Course, String> kzfaColumn;

    @Inject
    @SuppressWarnings("unchecked")
    public CourseFilter(FXMLLoader loader, ObjectProperty<AbstractStore> storeProperty, EventBus ev) {
        this.storeProperty = storeProperty;
        this.eventBus = ev;
        loader.setLocation(getClass().getResource("/fxml/CourseFilter.fxml"));

        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        System.out.println("Store" + store);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("short_name"));
        kzfaColumn.setCellValueFactory(new PropertyValueFactory<>("kzfa"));
        poColumn.setCellValueFactory(new PropertyValueFactory<>("po"));

        AbstractStore store = storeProperty.get();

        if (store == null) {
            storeProperty.addListener((observable, oldValue, newValue) -> {
                this.initializeCourseListView(newValue.getCourses());
            });
        } else {
            this.initializeCourseListView(store.getCourses());
        }
    }

    private void initializeCourseListView(List<Course> courses) {
        courseListView.setItems(FXCollections.observableArrayList(courses));
        courseListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            System.out.println(newSelection.getName());
            eventBus.post(new CourseSelectionChanged(newSelection));
        });
    }
}