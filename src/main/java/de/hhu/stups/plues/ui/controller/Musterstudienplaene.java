package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.AbstractStore;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.studienplaene.Renderer;
import de.hhu.stups.plues.tasks.SolverService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import org.controlsfx.control.TaskProgressView;

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Musterstudienplaene extends GridPane implements Initializable {

    private final Delayed<AbstractStore> delayedStore;
    private final Delayed<SolverService> delayedSolverService;

    private List<Course> majorCourses;
    private List<Course> minorCourses;

    private BooleanProperty solverProperty = new SimpleBooleanProperty(false);
    private SolverService solverService;

    private Task<FeasibilityResult> resultTask;

    @FXML
    private ComboBox<String> cbMajor;
    @FXML
    private ComboBox<String> cbMinor;
    @FXML
    private Button btGenerate;
    @FXML
    private Button btCancel;
    @FXML
    private ProgressBar progressGenerate;

    @Inject
    public Musterstudienplaene(FXMLLoader loader, Delayed<AbstractStore> delayedStore,
                               Delayed<SolverService> delayedSolverService) {
        this.delayedStore = delayedStore;
        this.delayedSolverService = delayedSolverService;

        this.setVgap(10.0);

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
    public void btGeneratePressed() {
        Course selectedMajorCourse = majorCourses.get(cbMajor.getSelectionModel().getSelectedIndex());
        Course selectedMinorCourse = minorCourses.get(cbMinor.getSelectionModel().getSelectedIndex());

        resultTask = solverService.computeFeasibilityTask(selectedMajorCourse,selectedMinorCourse);

        progressGenerate.progressProperty().bind(resultTask.progressProperty());
        progressGenerate.visibleProperty().bind(resultTask.runningProperty());

        resultTask.setOnSucceeded(event -> {
            FeasibilityResult result = (FeasibilityResult) event.getSource().getValue();

            AbstractStore store = delayedStore.get();
            // TODO: get colorChoice (?), empty string by now
            Renderer renderer = new Renderer(store, result.getGroupChoice(), result.getSemesterChoice(),
                    result.getModuleChoice(), result.getUnitChoice(), selectedMajorCourse, "");

            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose the pdf file's location");
            File selectedDirectory = directoryChooser.showDialog(null);
            if (selectedDirectory == null){
                resultTask.cancel();
            } else {
                try(OutputStream out = new FileOutputStream(selectedDirectory.getAbsolutePath()+
                        "/musterstudienplan_"+selectedMajorCourse.getName()+"_"+selectedMinorCourse.getName()+".pdf")) {
                    renderer.getResult().writeTo(out);
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        });

        resultTask.setOnFailed(event -> {
            // TODO: show proper message
            System.out.println("Failed due to invalid combination!");
        });

        solverService.submit(resultTask);
    }

    @FXML
    public void btCancelPressed(){
        if (resultTask != null && resultTask.isRunning()){
            resultTask.cancel();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        delayedStore.whenAvailable(this::initializeComboBoxes);

        btGenerate.setDefaultButton(true);
        btGenerate.disableProperty().bind(solverProperty.not().or(progressGenerate.visibleProperty()));
        btCancel.disableProperty().bind(solverProperty.not().or(progressGenerate.visibleProperty().not()));

        delayedSolverService.whenAvailable(s -> {
            this.solverService = s;
            this.solverProperty.set(true);
        });
    }

    private void initializeComboBoxes(AbstractStore store) {
        List<Course> courses = store.getCourses();

        majorCourses = courses.stream().filter(c -> c.isMajor()).collect(Collectors.toList());
        minorCourses = courses.stream().filter(c -> c.isMinor()).collect(Collectors.toList());

        List<String> majorCourseDisplayNames = majorCourses.stream().map(c -> c.getFullName()).collect(Collectors.toList());
        List<String> minorCourseDisplayNames = minorCourses.stream().map(c -> c.getFullName()).collect(Collectors.toList());

        cbMajor.setItems(FXCollections.observableArrayList(majorCourseDisplayNames));
        cbMinor.setItems(FXCollections.observableArrayList(minorCourseDisplayNames));
        cbMajor.getSelectionModel().select(0);
        cbMinor.getSelectionModel().select(0);
    }

}
