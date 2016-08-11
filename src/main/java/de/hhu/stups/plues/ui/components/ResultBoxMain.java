package de.hhu.stups.plues.ui.components;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.injector.PluesModule;
import de.hhu.stups.plues.prob.FeasibilityResult;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ResultBoxMain extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Injector injector = Guice.createInjector(
                com.google.inject.Stage.DEVELOPMENT, new PluesModule(primaryStage));
        final ExecutorService e = Executors.newWorkStealingPool();

        ResultBoxFactory rbf = injector.getInstance(ResultBoxFactory.class);
        Task<FeasibilityResult>[] t = getTasks();

        for(Task<FeasibilityResult> i : t) {
           e.submit(i) ;
        }

        ResultBox running = rbf.create(t[0]);
        ResultBox success = rbf.create(t[1]);
        ResultBox failure = rbf.create(t[2]);
        ResultBox interrupted = rbf.create(t[3]);

        Course major = new Course();
        Course minor = new Course();

        major.setLongName("Informatik Bachelor");
        major.setPo(2013);
        major.setDegree("ba");
        major.setKzfa("H");
        minor.setLongName("Nebenfach Physik");
        minor.setPo(2013);
        minor.setDegree("ba");
        minor.setKzfa("N");

        running.setMajorCourse(major);
        success.setMajorCourse(major);
        failure.setMajorCourse(major);
        interrupted.setMajorCourse(major);

        running.setMinorCourse(minor);
        success.setMinorCourse(minor);
        failure.setMinorCourse(minor);
        interrupted.setMinorCourse(minor);

        VBox root = new VBox(running, success, failure, interrupted);
        primaryStage.setScene(new Scene(root, 800, 600));

        primaryStage.setTitle("PlÜS");

        // TODO: properly close the application on close request
        Platform.setImplicitExit(true);
        primaryStage.setOnCloseRequest(x -> Platform.exit());

        primaryStage.show();
    }

    private Task<FeasibilityResult>[] getTasks() {
        Task<FeasibilityResult>[] t = new Task[4];
        t[0] = new Task<FeasibilityResult>() {
            @Override
            protected FeasibilityResult call() throws Exception {
                TimeUnit.DAYS.sleep(10);
                return null;
            }
        };
        t[1] = new Task<FeasibilityResult>() {
            @Override
            protected FeasibilityResult call() throws Exception {
                return null;
            }
        };
        t[2] = new Task<FeasibilityResult>() {
            @Override
            protected FeasibilityResult call() throws Exception {
                throw new RuntimeException();
            }
        };
        t[3] = new Task<FeasibilityResult>() {
            @Override
            protected FeasibilityResult call() throws Exception {
                cancel();
                return null;
            }
        };
        return t;
    }

    public static void main(String[] args) {
        launch(args);
    }

}
