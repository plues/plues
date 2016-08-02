package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import de.hhu.stups.plues.data.AbstractStore;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.Solver;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Musterstudienplaene extends GridPane implements Initializable {

    private final ObjectProperty<AbstractStore> storeProperty;
    private final ObjectProperty<Solver> solverProperty;

    @FXML
    ComboBox<Course> cbMajor;
    @FXML
    ComboBox<Course> cbMinor;

    @Inject
    public Musterstudienplaene(FXMLLoader loader, ObjectProperty<AbstractStore> storeProperty, ObjectProperty<Solver> solverProperty) {
        this.storeProperty = storeProperty;
        this.solverProperty = solverProperty;

        loader.setLocation(getClass().getResource("/fxml/musterstudienplaene.fxml"));

        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    public void btGeneratePressed(){
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        AbstractStore store = storeProperty.get();

        if (store == null){
            storeProperty.addListener((observable, oldValue, newValue) -> {
                    this.initializeComboBoxes(newValue);});
        } else {
            this.initializeComboBoxes(store);
        }
    }

    private void initializeComboBoxes(AbstractStore store){
        List<Course> courses = store.getCourses();

        List<Course> majorCourses = courses.stream().filter(c -> c.isMajor()).collect(Collectors.toList());
        List<Course> minorCourses = courses.stream().filter(c -> c.isMinor()).collect(Collectors.toList());

        cbMajor.setItems(FXCollections.observableArrayList(majorCourses));
        cbMinor.setItems(FXCollections.observableArrayList(minorCourses));
    }

}
