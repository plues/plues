package de.hhu.stups.plues.ui.components;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.injector.PluesModule;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ResultBoxMain extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Injector injector = Guice.createInjector(
                com.google.inject.Stage.DEVELOPMENT, new PluesModule(primaryStage));

        ResultBox running = injector.getInstance(ResultBox.class);
        ResultBox success = injector.getInstance(ResultBox.class);
        ResultBox failure = injector.getInstance(ResultBox.class);
        ResultBox interrupted = injector.getInstance(ResultBox.class);
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
        primaryStage.setOnCloseRequest(t -> Platform.exit());

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
