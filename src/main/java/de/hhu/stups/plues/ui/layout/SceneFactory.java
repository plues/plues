package de.hhu.stups.plues.ui.layout;

import javafx.scene.Parent;
import javafx.scene.Scene;

public class SceneFactory {

  private SceneFactory() {
    throw new IllegalAccessError("Utility class");
  }

  /**
   * Create a scene around a given component and set the default stylesheet.
   * @param component Parent the root of the scene
   * @return Scene new Scene with default style
   */
  public static Scene create(final Parent component) {
    final  Scene scene = new Scene(component);
    scene.getStylesheets().add("/styles/index.css");

    return scene;
  }
}
