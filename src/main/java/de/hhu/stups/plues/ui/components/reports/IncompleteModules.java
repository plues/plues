package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.layout.Inflater;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

public class IncompleteModules extends ListView implements Initializable {

  @Inject
  public IncompleteModules(final Inflater inflater) {
    inflater.inflate("/components/reports/IncompleteModules", this, this, "reports");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {

  }

  public void setData(final Set<Integer> incompleteModules) {
    getItems().setAll(incompleteModules);
  }
}
