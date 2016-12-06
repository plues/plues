package de.hhu.stups.plues.ui.components.detailView;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class ModuleDetailView extends VBox implements Initializable {

  @Inject
  public ModuleDetailView(final Inflater inflater) {
    inflater.inflate("/components/detailView/SessionDetailView", this, this, "detailView");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {

  }
}
