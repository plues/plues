package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

@SuppressWarnings("WeakerAccess")
public class AboutWindow extends GridPane implements Initializable {

  public static final String URL = System.getProperty("user.dir") + "/src/main/resources/Logo.svg";
  @FXML
  public Label logo;

  @Inject
  public AboutWindow(final Inflater inflater) {
    inflater.inflate("components/AboutWindow", this, this, "aboutWindow");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    logo.setGraphic(new ImageView(new Image(URL)));
  }
}
