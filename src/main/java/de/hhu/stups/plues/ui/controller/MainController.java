package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.Solver;
import de.hhu.stups.plues.tasks.SolverLoaderTask;
import de.hhu.stups.plues.tasks.SolverLoaderTaskFactory;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.StoreLoaderTask;
import de.hhu.stups.plues.ui.components.ExceptionDialog;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.controlsfx.control.TaskProgressView;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;
import java.util.stream.IntStream;

public class MainController implements Initializable {

    private static final String LAST_DIR = "LAST_DIR";
    private final Properties properties;

    private final Delayed<Store> delayedStore;
    private final Delayed<SolverService> delayedSolverService;
    private final SolverLoaderTaskFactory solverLoaderTaskFactory;
    private final ObjectProperty<Course>
            courseProperty = new SimpleObjectProperty<>();
    private final BooleanProperty
            solverProperty = new SimpleBooleanProperty(false);
    private final ExecutorService
            executor = Executors.newWorkStealingPool();

    @FXML
    private MenuItem openFileMenuItem;
    @FXML
    private GridPane foo;
    @FXML
    private TaskProgressView<Task<?>> taskProgress;
    @FXML
    private Label selection;
    @FXML
    private Button checkSelection;
    @FXML
    private Label result;
    @FXML
    private CourseFilter courseFilter;
    private SolverService solverService;


    @Inject
    public MainController(final Delayed<Store> ds,
                          final Delayed<SolverService> dss,
                          final SolverLoaderTaskFactory sltf,
                          final Properties pp) {

        this.delayedStore = ds;
        this.properties = pp;

        this.delayedSolverService = dss;
        this.solverLoaderTaskFactory = sltf;
    }

    @Override
    public final void initialize(final URL location,
                                 final ResourceBundle resources) {

        if(this.properties.get("dbpath") != null) {
            this.loadData((String) this.properties.get("dbpath"));
        }


        this.delayedStore.whenAvailable(s -> {
            Runtime.getRuntime().addShutdownHook(
                    new Thread() {
                        @Override
                        public void run() {
                            s.close();
                        }
                    });
            System.out.println("Store Loaded " + s);
            this.courseFilter.setCourses(s.getCourses());
        });
        //
        this.courseProperty.bind(this.courseFilter.selectedItemProperty());
        this.selection.textProperty().bind(
                Bindings.selectString(this.courseProperty, "name"));
        //
        this.checkSelection.setDefaultButton(true);
        this.checkSelection.disableProperty().bind(
                this.courseProperty.isNull().or(this.solverProperty.not()));
        
        this.delayedSolverService.whenAvailable(s -> {
            this.solverService = s;
            this.solverProperty.set(true);
            System.out.println("SolverService loaded");
        });

        //
        IntStream.range(1, 20).forEach(x -> this.foo.add(
                new Label(String.valueOf(x)),
                x % this.foo.getColumnConstraints().size(),
                x % this.foo.getRowConstraints().size()));
    }

    private void loadData(final String path) {

        final StoreLoaderTask storeLoader = this.getStoreLoaderTask(path);
        final SolverLoaderTask solverLoader
                = this.getSolverLoaderTask(storeLoader);

        this.openFileMenuItem.setDisable(true);
        this.submitTask(storeLoader);

        this.submitTask(solverLoader);
    }

    private StoreLoaderTask getStoreLoaderTask(final String path) {

        final StoreLoaderTask storeLoader
                = new StoreLoaderTask(path);
        //
        storeLoader.progressProperty().addListener(
                (observable, oldValue, newValue) ->
                        System.out.println("STORE " + newValue));
        //
        storeLoader.messageProperty().addListener(
                (observable, oldValue, newValue) ->
                        System.out.println("STORE " + newValue));
        //
        storeLoader.setOnFailed(event -> {
            final Throwable ex = event.getSource().getException();
            showCriticalExceptionDialog(ex, "Database could not be loaded");
            Platform.exit();
        });
        //
        storeLoader.setOnSucceeded(
                value -> System.out.println("STORE:loading Store succeeded"));

        storeLoader.setOnSucceeded(event -> Platform.runLater(() -> {
            final Store s = (Store) event.getSource().getValue();
            this.delayedStore.set(s);
        }));
        return storeLoader;
    }

    private void showCriticalExceptionDialog(final Throwable ex, final String message) {
        final ExceptionDialog ed = new ExceptionDialog();

        ed.setTitle("Critical Exception");
        ed.setHeaderText(message);
        ed.setException(ex);

        ed.showAndWait();
    }

    private SolverLoaderTask getSolverLoaderTask(
            final StoreLoaderTask storeLoader) {

        final SolverLoaderTask solverLoader
                = this.solverLoaderTaskFactory.create(storeLoader);

        solverLoader.progressProperty()
                    .addListener((observable, oldValue, newValue)
                                         -> System.out.println(newValue));
        //
        solverLoader.messageProperty()
                    .addListener((observable, oldValue, newValue)
                                         -> System.out.println(newValue));
        //
        solverLoader.setOnSucceeded(
                value -> System.out.println("loading Solver succeeded"));
        solverLoader.setOnSucceeded(event -> {
            final Solver s = (Solver) event.getSource().getValue();
            // TODO: check if this needs to run on UI thread
            this.delayedSolverService.set(new SolverService(s));
        });
        //
        solverLoader.setOnFailed(event -> {
            final Throwable ex = event.getSource().getException();
            showCriticalExceptionDialog(ex, "Solver could not be loaded");
            Platform.exit();
        });

        return solverLoader;
    }

    @FXML
    @SuppressWarnings({"UnusedParameters", "unused"})
    private void checkButtonPressed(final ActionEvent actionEvent) {
        final Course course = this.courseProperty.get();

        final SolverService s = this.solverService;
        assert s != null;

        final Task<Boolean> t = s.checkFeasibilityTask(course);
        t.setOnSucceeded(event -> {
            final Boolean i = (Boolean) event.getSource().getValue();
            this.result.setText(i.toString());
            System.out.println(course.getName() + ": " + i.toString());
        });
        this.taskProgress.getTasks().add(t);
        s.submit(t);
    }

    private void submitTask(final Task<?> t, final ExecutorService exec) {
        this.taskProgress.getTasks().add(t);
        exec.submit(t);
    }

    private void submitTask(final Task<?> t) {
        this.submitTask(t, this.executor);
    }

    @SuppressWarnings("UnusedParameters")
    public final void openFile(final ActionEvent actionEvent) {
        final Preferences prefs
                = Preferences.userNodeForPackage(MainController.class);
        final String initialDir
                = prefs.get(LAST_DIR, System.getProperty("user.home"));
        //
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open a Database"); // TODO: i18n

        fileChooser.setInitialDirectory(new File(initialDir));
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter(
                "SQLite3 Database", "*.sqlite", "*.sqlite3"));
        //
        final File file = fileChooser.showOpenDialog(this.result.getScene()
                                                                .getWindow());
        //
        if(file != null) {
            final String newInitialDir = file.getAbsoluteFile().getParent();
            if(!newInitialDir.equals(initialDir)) {
                prefs.put(LAST_DIR, newInitialDir);
            }
            //
            this.loadData(file.getAbsolutePath());
        }
    }
}
