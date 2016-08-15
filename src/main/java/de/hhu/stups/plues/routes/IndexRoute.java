package de.hhu.stups.plues.routes;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class IndexRoute implements Route {

  private final Parent root;
  private final Stage stage;

  @Inject
  public IndexRoute(Inflater inflater, Stage stage) {
    this.root = inflater.inflate("main.fxml");
    this.stage = stage;
  }

  @Override
  public void transition() {
    stage.setScene(new Scene(root, 800, 600));
  }
}
