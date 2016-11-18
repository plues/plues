package de.hhu.stups.plues.routes;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class IndexRoute implements Route {

  private final Parent root;
  private final Stage stage;
  private Scene scene;

  @Inject
  public IndexRoute(Inflater inflater, Stage stage) {
    this.root = inflater.inflate("main", "main");
    this.stage = stage;
  }

  @Override
  public void transition() {
    scene = new Scene(root, 800, 600);

    scene.getStylesheets().add("/styles/index.css");
    stage.setScene(scene);
    stage.setMaximized(true);
  }
}
