package de.hhu.stups.plues.routes;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class IndexRoute implements Route {

  private final Stage stage;
  private final Inflater inflater;

  @Inject
  public IndexRoute(final Inflater inflater, final Stage stage) {
    this.inflater = inflater;
    this.stage = stage;
  }

  @Override
  public void transition(Course... courses) {
    final Parent root = inflater.inflate("main", "MainController", "Days");
    final Scene scene = new Scene(root, 800, 600);

    scene.getStylesheets().add("/styles/index.css");
    stage.setScene(scene);
    stage.setMaximized(true);
  }
}