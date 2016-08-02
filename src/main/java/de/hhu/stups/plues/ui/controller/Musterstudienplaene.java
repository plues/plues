package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import de.hhu.stups.plues.data.AbstractStore;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.Solver;
import de.hhu.stups.plues.Delayed;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Musterstudienplaene extends GridPane implements Initializable {

    private final ObjectProperty<Solver> solverProperty;
    private final Delayed<AbstractStore> delayedStore;

    @FXML
    ComboBox<Course> cbMajor;
    @FXML
    ComboBox<Course> cbMinor;

    @Inject
    public Musterstudienplaene(FXMLLoader loader, Delayed<AbstractStore> delayedStore, ObjectProperty<Solver> solverProperty) {
        this.delayedStore = delayedStore;
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
        delayedStore.whenAvailable(this::initializeComboBoxes);
    }

    private void initializeComboBoxes(AbstractStore store){
        List<Course> courses = store.getCourses();

        List<Course> majorCourses = courses.stream().filter(c -> c.isMajor()).collect(Collectors.toList());
        List<Course> minorCourses = courses.stream().filter(c -> c.isMinor()).collect(Collectors.toList());

        cbMajor.setItems(FXCollections.observableArrayList(majorCourses));
        cbMinor.setItems(FXCollections.observableArrayList(minorCourses));
    }

}
