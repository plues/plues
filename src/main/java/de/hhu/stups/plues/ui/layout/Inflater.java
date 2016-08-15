package de.hhu.stups.plues.ui.layout;

import com.google.inject.Inject;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

/**
 * Inflates a layout from a filename.
 */
public class Inflater {

  private final FXMLLoader loader;

  @Inject
  public Inflater(FXMLLoader loader) {
    this.loader = loader;
  }

  /**
   * Inflate a fxml resource as a layout from <tt>/fxml/</tt>.
   * @param name The name of the xml file without the <tt>.xml</tt> extension.
   * @return {@link Parent}
   */
  public Parent inflate(String name) {
    // set location explicitly to ensure using the injected fxml loader
    loader.setLocation(getClass().getResource("/fxml/" + name));

    try {
      return loader.load();
    } catch (IOException ignored) {
      ignored.printStackTrace();
    }

    // TODO: kill app!
    return null;
  }
}
