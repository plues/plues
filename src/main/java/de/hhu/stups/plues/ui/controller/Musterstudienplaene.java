package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Musterstudienplaene extends GridPane implements Initializable {

    private final Delayed<Store> delayedStore;
    private final Delayed<SolverService> delayedSolverService;

    private final BooleanProperty solverProperty
            = new SimpleBooleanProperty(false);
    private SolverService solverService;

    private Task<FeasibilityResult> resultTask;

    @FXML
    @SuppressWarnings("unused")
    private ComboBox<Course> cbMajor;

    @FXML
    @SuppressWarnings("unused")
    private ComboBox<Course> cbMinor;

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
        final Course selectedMajorCourse = cbMajor.getSelectionModel()
                                                  .getSelectedItem();
        final Course selectedMinorCourse = cbMinor.getSelectionModel()
                                                  .getSelectedItem();

        resultTask
                = solverService.computeFeasibilityTask(selectedMajorCourse, selectedMinorCourse);

        progressGenerate.progressProperty().bind(resultTask.progressProperty());
        progressGenerate.visibleProperty().bind(resultTask.runningProperty());

        resultTask.setOnSucceeded(event -> {
            final FeasibilityResult result
                    = (FeasibilityResult) event.getSource().getValue();

            try(OutputStream file = new FileOutputStream("result.ser");
                final OutputStream buffer = new BufferedOutputStream(file);
                ObjectOutput output = new ObjectOutputStream(buffer)) {
                output.writeObject(result);
            } catch(FileNotFoundException e) {
                e.printStackTrace();
            } catch(IOException e) {
                e.printStackTrace();
            }
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
                        + "/musterstudienplan_" +
                        selectedMajorCourse.getName() + "_"
                        + selectedMinorCourse.getName() + ".pdf";
                Thread writeToPDF = new Thread(() -> {
                    try(OutputStream out = new FileOutputStream(path)) {
                        renderer.getResult().writeTo(out);
                    } catch(final IOException e) {
                        e.printStackTrace();
                    } catch(ParserConfigurationException e) {
                        e.printStackTrace();
                    } catch(SAXException e) {
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
    public final void initialize(final URL location,
                                 final ResourceBundle resources) {
        delayedStore.whenAvailable(this::initializeComboBoxes);

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
        });
    }

    private void initializeComboBoxes(final Store store) {
        final List<Course> courses = store.getCourses();

        final List<Course> majorCourseDisplayNames = courses.stream()
                                                            .filter(Course::isMajor)
                                                            .collect(Collectors.toList());

        final List<Course> minorCourseDisplayNames = courses.stream()
                                                            .filter(Course::isMinor)
                                                            .collect(Collectors.toList());

        cbMajor.setConverter(new CourseConverter());
        cbMinor.setConverter(new CourseConverter());

        cbMajor.setItems(
                FXCollections.observableArrayList(majorCourseDisplayNames));
        cbMinor.setItems(
                FXCollections.observableArrayList(minorCourseDisplayNames));

        cbMajor.getSelectionModel().select(0);
        cbMinor.getSelectionModel().select(0);
    }

    private static class CourseConverter extends StringConverter<Course> {
        @Override
        public String toString(final Course object) {
            return object.getFullName();
        }

        @Override
        public Course fromString(final String string) {
            throw new RuntimeException();
        }
    }
}
