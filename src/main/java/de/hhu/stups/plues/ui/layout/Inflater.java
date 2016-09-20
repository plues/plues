package de.hhu.stups.plues.ui.layout;

import com.google.inject.Inject;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

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
   * @param bundleName The name of the used bundle
   * @return {@link Parent}
   */
  public Parent inflate(final String name, final String bundleName) {
    return inflate(name, null, null, bundleName);
  }

  /**
   * Inflate a fxml resource as a layout from <tt>/fxml/</tt>.
   * @param name The name of the xml file without the <tt>.xml</tt> extension.
   * @param root optional root node to inflate this layout into
   * @param bundleName Name of the i18n resource to bind
   */
  public Parent inflate(String name, Parent root, Object controller, String bundleName) {
    // set location explicitly to ensure using the injected fxml loader
    loader.setLocation(getClass().getResource("/fxml/" + name + ".fxml"));

    if (root != null) {
      loader.setRoot(root);
    }

    if (controller != null) {
      loader.setController(controller);
    }

    ResourceBundle bundle = ResourceBundle.getBundle("lang."+bundleName, new Locale("de"));
    loader.setResources(bundle);

    try {
      return loader.load();
    } catch (IOException ignored) {
      ignored.printStackTrace();
    }

    // TODO: kill app!
    return null;
  }
}
