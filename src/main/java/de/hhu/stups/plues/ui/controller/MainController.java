package de.hhu.stups.plues.ui.controller;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.hhu.stups.plues.data.AbstractStore;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.Solver;
import de.hhu.stups.plues.tasks.SolverLoaderTask;
import de.hhu.stups.plues.tasks.SolverLoaderTaskFactory;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.StoreLoaderTask;
import de.hhu.stups.plues.ui.events.CourseSelectionChanged;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
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

    private final ObjectProperty<AbstractStore> storeProperty;

    private final Properties properties;

    private final ObjectProperty<Solver> solverProperty;
    private final Provider<SolverService> solverServiceProvider;
    private SolverLoaderTaskFactory solverLoaderTaskFactory;

    @FXML
    public GridPane foo;

    @FXML
    public TaskProgressView<Task<?>> taskProgress;
    @FXML
    public Label selection;
    public Button checkSelection;
    public Label result;
    private ObjectProperty<Course> courseProperty = new SimpleObjectProperty<>();

    private ExecutorService executor = Executors.newWorkStealingPool();
    private ExecutorService solverExecutor = Executors.newSingleThreadExecutor();


    @Inject
    public MainController(ObjectProperty<AbstractStore> storeProp, ObjectProperty<Solver> solverProp,
                          SolverLoaderTaskFactory solverLoaderTaskFactory,
                          Provider<SolverService> solverServiceProvider,
                          Properties properties, EventBus bus) {
        this.storeProperty = storeProp;
        this.properties = properties;

        this.solverProperty = solverProp;
        this.solverLoaderTaskFactory = solverLoaderTaskFactory;
        this.solverServiceProvider = solverServiceProvider;

        bus.register(this);
    }

    @Subscribe
    public void courseChanged(CourseSelectionChanged event) {
        System.out.println(event.getCourse().getName() + " selected.");
        this.courseProperty.set(event.getCourse());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (properties.get("dbpath") != null) {
            loadData();
        } else {
            throw new RuntimeException("No dbpath found. Please specify a dbpath property in the resources file.");
            // rely on user opening a database
        }


        selection.textProperty().bind(Bindings.selectString(courseProperty, "name"));

        checkSelection.setDefaultButton(true);
        checkSelection.disableProperty().bind(courseProperty.isNull().or(solverProperty.isNull()));

        IntStream.range(1, 20).forEach(x -> foo.add(new Label(String.valueOf(x)), x % foo.getColumnConstraints().size(), x % foo.getRowConstraints().size()));
    }

    private void loadData() {

        StoreLoaderTask storeLoader = new StoreLoaderTask((String) properties.get("dbpath"));

        storeLoader.setOnSucceeded(value -> System.out.println("STORE:loading Store succeeded"));
        storeLoader.progressProperty().addListener((observable, oldValue, newValue) -> System.out.println("STORE " + newValue));
        storeLoader.messageProperty().addListener((observable, oldValue, newValue) -> System.out.println("STORE " + newValue));
        storeLoader.setOnFailed(event -> {
            System.out.println(event);
            // TODO: proper error handling
            System.err.println("STORE: Loading failed");
            throw new RuntimeException("STORE: Loading failed");
        });
        storeLoader.setOnSucceeded(event -> Platform.runLater(() -> {
            Store s = (Store) event.getSource().getValue();
            this.storeProperty.set(s);
        }));


        this.storeProperty.addListener((observable, oldValue, newValue) -> Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                newValue.close();
            }
        }));

        SolverLoaderTask solverLoader = this.solverLoaderTaskFactory.create(storeLoader);
        solverLoader.setOnSucceeded(value -> System.out.println("loading Solver succeeded"));
        solverLoader.progressProperty().addListener((observable, oldValue, newValue) -> System.out.println(newValue));
        solverLoader.messageProperty().addListener((observable, oldValue, newValue) -> System.out.println(newValue));


        solverLoader.setOnFailed(event -> {
            System.out.println(event);
            System.out.println("Loading failed");
        });
        solverLoader.setOnSucceeded(event -> {
            System.out.println(event);
            Solver s = (Solver) event.getSource().getValue();
            Platform.runLater(() -> this.solverProperty.set(s));
        });
        this.submitTask(storeLoader);
        this.submitTask(solverLoader);
    }

    public void checkButtonPressed(ActionEvent actionEvent) {
        Course course = courseProperty.get();
        SolverService s = this.solverServiceProvider.get();
        Task<Boolean> t = s.checkFeasibilityTask(course);
        t.setOnSucceeded(event -> {
            Boolean i = (Boolean) event.getSource().getValue();
            result.setText(i.toString());
            System.out.println(course.getName() + ": " + i.toString());
        });
        this.submitTask(t, solverExecutor);
    }

    private void submitTask(Task<?> t, ExecutorService exec) {
        this.taskProgress.getTasks().add(t);
        exec.submit(t);
    }

    private void submitTask(Task<?> t) {
        submitTask(t, this.executor);
    }
}
