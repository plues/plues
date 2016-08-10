package de.hhu.stups.plues.ui.components;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.injector.PluesModule;
import de.hhu.stups.plues.prob.FeasibilityResult;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.concurrent.Future;

public class ResultBoxMain extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Injector injector = Guice.createInjector(
                com.google.inject.Stage.DEVELOPMENT, new PluesModule(primaryStage));

        ResultBoxFactory rbf = injector.getInstance(ResultBoxFactory.class);
        Task<FeasibilityResult> t = new Task<FeasibilityResult>() {
            @Override
            protected FeasibilityResult call() throws Exception {
                return null;
            }
        };

        ResultBox running = rbf.create(t);
        ResultBox success = rbf.create(t);
        ResultBox failure = rbf.create(t);
        ResultBox interrupted = rbf.create(t);

        Course major = new Course();
        Course minor = new Course();

        major.setLongName("Informatik Bachelor"); major.setPo(2013); major.setDegree("ba"); major.setKzfa("H");
        minor.setLongName("Nebenfach Physik"); minor.setPo(2013); minor.setDegree("ba"); minor.setKzfa("N");

        success.setFeasible(true);
        failure.setFeasible(true); // TODO: HACK --> Bessere Lösung
        failure.setFeasible(false);

        running.setMajorCourse(major);
        success.setMajorCourse(major);
        failure.setMajorCourse(major);
        interrupted.setMajorCourse(major);

        running.setMinorCourse(minor);
        success.setMinorCourse(minor);
        failure.setMinorCourse(minor);
        interrupted.setMinorCourse(minor);

        interrupted.interrupt();

        VBox root = new VBox(running, success, failure, interrupted);
        primaryStage.setScene(new Scene(root, 800, 600));

        primaryStage.setTitle("PlÜS");

        // TODO: properly close the application on close request
        Platform.setImplicitExit(true);
        primaryStage.setOnCloseRequest(e -> Platform.exit());

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
