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
import javafx.scene.layout.GridPane;
import org.controlsfx.control.TaskProgressView;

import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class MainController implements Initializable {

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

        if(properties.get("dbpath") != null) {
            loadData();
        } else {
            throw new RuntimeException("No dbpath found. Please specify a "
                                               + "dbpath property in the resources file.");
            // rely on user opening a database
        }


        this.delayedStore.whenAvailable(s
                                                -> Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                s.close();
            }
        }));
        this.delayedStore.whenAvailable(s
                                                -> System.out.println("Store Loaded " + s));

        courseProperty.bind(courseFilter.selectedItemProperty());
        //
        selection.textProperty().bind(
                Bindings.selectString(courseProperty, "name"));
        //
        checkSelection.setDefaultButton(true);
        checkSelection.disableProperty().bind(
                courseProperty.isNull().or(solverProperty.not()));
        this.delayedSolverService.whenAvailable(s -> {
            this.solverService = s;
            this.solverProperty.set(true);
            System.out.println("SolverService loaded");
        });

        //
        IntStream.range(1, 20).forEach(x -> foo.add(new Label(String.valueOf(x)), x % foo.getColumnConstraints().size(), x % foo.getRowConstraints().size()));
    }

    private void loadData() {

        final StoreLoaderTask storeLoader = getStoreLoaderTask();
        final SolverLoaderTask solverLoader = getSolverLoaderTask(storeLoader);

        this.submitTask(storeLoader);
        this.submitTask(solverLoader);
    }

    private StoreLoaderTask getStoreLoaderTask() {

        final StoreLoaderTask storeLoader
                = new StoreLoaderTask((String) properties.get("dbpath"));
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
            System.out.println(event);
            // TODO: proper error handling
            System.err.println("STORE: Loading failed");
            throw new RuntimeException("STORE: Loading failed");
        });
        //
        storeLoader.setOnSucceeded(value
                                           -> System.out.println("STORE:loading Store succeeded"));

        storeLoader.setOnSucceeded(event -> Platform.runLater(() -> {
            final Store s = (Store) event.getSource().getValue();
            this.delayedStore.set(s);
        }));
        return storeLoader;
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

        return solverLoader;
    }

    @FXML
    @SuppressWarnings({"UnusedParameters", "unused"})
    private void checkButtonPressed(final ActionEvent actionEvent) {
        final Course course = courseProperty.get();

        final SolverService s = this.solverService;
        assert s != null;

        final Task<Boolean> t = s.checkFeasibilityTask(course);
        t.setOnSucceeded(event -> {
            final Boolean i = (Boolean) event.getSource().getValue();
            result.setText(i.toString());
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
        submitTask(t, this.executor);
    }
}
