package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import de.hhu.stups.plues.data.AbstractStore;
import de.hhu.stups.plues.prob.Solver;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static tlc2.util.SimpUtil.store;

public class Musterstudienplaene extends VBox implements Initializable {

    private final ObjectProperty<AbstractStore> storeProperty;
    private final ObjectProperty<Solver> solverProperty;

    @FXML
    ComboBox major;

    @FXML
    ComboBox minor;

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
        System.out.println("Store" + store);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
