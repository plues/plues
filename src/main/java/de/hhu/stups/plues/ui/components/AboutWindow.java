package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.layout.Inflater;
import de.prob.scripting.Api;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;

@SuppressWarnings("WeakerAccess")
public class AboutWindow extends GridPane implements Initializable {

  public final InputStream url = getClass().getResourceAsStream("/images/logo-min.png");
  private final Api api;
  private final Properties properties;

  @FXML
  public Label logo;
  @FXML
  public Label cliVersion;
  @FXML
  public Label solverVersion;
  @FXML
  public Label version;
  @FXML
  public Label revision;
  @FXML
  public Label buildDate;

  /**
   * Create a new AboutWindow.
   * @param inflater Inflater to load fxml
   * @param api Api ProB API interface
   * @param properties Properties loaded application properties
   */
  @Inject
  public AboutWindow(final Inflater inflater, final Api api, final Properties properties) {
    this.api = api;
    this.properties = properties;
    inflater.inflate("components/AboutWindow", this, this, "aboutWindow");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    if (url != null) {
      logo.setGraphic(new ImageView(new Image(url)));
    }

    cliVersion.setText(this.api.getVersion().toString());

    this.version.setText(properties.getProperty("version", "..."));
    this.solverVersion.setText(properties.getProperty("model_version", "..."));
    this.revision.setText(properties.getProperty("revision", "..."));
    this.buildDate.setText(properties.getProperty("build_date", "..."));
  }
}
