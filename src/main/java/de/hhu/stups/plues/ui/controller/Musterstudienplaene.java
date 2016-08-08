package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.studienplaene.Renderer;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;

public class Musterstudienplaene extends GridPane implements Initializable {

    private final Delayed<Store> delayedStore;
    private final Delayed<SolverService> delayedSolverService;

    private final BooleanProperty solverProperty;
    private SolverService solverService;

    private Task<FeasibilityResult> resultTask;

    @FXML
    @SuppressWarnings("unused")
    private MajorMinorCourseSelection courseSelection;

    @FXML
    @SuppressWarnings("unused")
    private Button btGenerate;

    @FXML
    @SuppressWarnings("unused")
    private Button btCancel;

    @FXML
    @SuppressWarnings("unused")
    private ProgressBar progressGenerate;

    @Inject
    public Musterstudienplaene(final FXMLLoader loader, final Delayed<Store> delayedStore,
                               final Delayed<SolverService> delayedSolverService) {
        this.delayedStore = delayedStore;
        this.delayedSolverService = delayedSolverService;

        this.solverProperty = new SimpleBooleanProperty(false);

        this.setVgap(10.0);

        loader.setLocation(getClass().getResource("/fxml/musterstudienplaene.fxml"));

        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch(final IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    @SuppressWarnings("unused")
    public void btGeneratePressed() {
        final Course selectedMajorCourse = courseSelection.getSelectedMajorCourse();
        final Course selectedMinorCourse = courseSelection.getSelectedMinorCourse();

        resultTask = solverService.computeFeasibilityTask(selectedMajorCourse, selectedMinorCourse);

        progressGenerate.progressProperty().bind(resultTask.progressProperty());
        progressGenerate.visibleProperty().bind(resultTask.runningProperty());

        resultTask.setOnSucceeded(event -> {
            final FeasibilityResult result = (FeasibilityResult) event.getSource().getValue();

            final Store store = delayedStore.get();
            final Renderer renderer
                    = new Renderer(store, result.getGroupChoice(), result.getSemesterChoice(),
                                   result.getModuleChoice(), result.getUnitChoice(), selectedMajorCourse, "true");

            final DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose the pdf file's location");
            final File selectedDirectory = directoryChooser.showDialog(null);

            if(selectedDirectory == null) {
                resultTask.cancel();
            } else {
                final String path = selectedDirectory.getAbsolutePath()
                        + "/musterstudienplan_"
                        + selectedMajorCourse.getName() + "_"
                        + selectedMinorCourse.getName() + ".pdf";

                Thread writeToPDF = new Thread(() -> {

                    try(OutputStream out = new FileOutputStream(path)) {
                        renderer.getResult().writeTo(out);
                    } catch(final IOException | ParserConfigurationException | SAXException e) {
                        e.printStackTrace();
                    }

                });
                writeToPDF.start();
            }
        });

        resultTask.setOnFailed(event -> {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Generation failed");
            alert.setHeaderText("Invalid course combination");
            alert.setContentText("The chosen combination of major and minor course is not possible.");
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
        });

        solverService.submit(resultTask);
    }

    @FXML
    @SuppressWarnings("unused")
    public final void btCancelPressed() {
        if(resultTask != null && resultTask.isRunning()) {
            resultTask.cancel();
        }
    }

    @Override
    public final void initialize(final URL location, final ResourceBundle resources) {
        delayedStore.whenAvailable(courseSelection::initializeItemsFromStore);

        btGenerate.setDefaultButton(true);
        btGenerate.disableProperty().bind(
                solverProperty.not()
                              .or(progressGenerate.visibleProperty()));
        btCancel.disableProperty().bind(
                solverProperty.not()
                              .or(progressGenerate.visibleProperty().not()));

        delayedSolverService.whenAvailable(s -> {
            this.solverService = s;
            this.solverProperty.set(true);

            Task<Set<String>> impossibleCoursesTask = solverService.impossibleCoursesTask();
            impossibleCoursesTask.setOnSucceeded(event -> {
                courseSelection.highlightImpossibleCourses((Set<String>) event.getSource().getValue());
            });
            solverService.submit(impossibleCoursesTask);

        });
    }
}
