package de.hhu.stups.plues.ui.components.detailview;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;


public class CourseDetailView extends VBox implements Initializable {

  private final ObjectProperty<Course> courseProperty;

  @FXML
  @SuppressWarnings("unused")
  private Label key;
  @FXML
  @SuppressWarnings("unused")
  private Label name;
  @FXML
  @SuppressWarnings("unused")
  private Label po;
  @FXML
  @SuppressWarnings("unused")
  private Label kzfa;
  @FXML
  @SuppressWarnings("unused")
  private Label degree;

  /**
   * Default constructor.
   */
  @Inject
  public CourseDetailView(final Inflater inflater) {
    courseProperty = new SimpleObjectProperty<>();

    inflater.inflate("components/detailview/CourseDetailView", this, this, "detailView", "Column");
  }

  public void setCourse(final Course course) {
    courseProperty.set(course);
  }

  public String getTitle() {
    return name.getText();
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    this.key.textProperty().bind(Bindings.when(courseProperty.isNotNull()).then(
        Bindings.selectString(courseProperty, "key")).otherwise(""));
    this.name.textProperty().bind(Bindings.when(courseProperty.isNotNull()).then(
        Bindings.selectString(courseProperty, "fullName")).otherwise(""));
    this.po.textProperty().bind(Bindings.when(courseProperty.isNotNull()).then(
        Bindings.selectString(courseProperty, "po")).otherwise(""));
    this.kzfa.textProperty().bind(Bindings.when(courseProperty.isNotNull()).then(
        Bindings.selectString(courseProperty, "kzfa")).otherwise(""));
    this.degree.textProperty().bind(Bindings.when(courseProperty.isNotNull()).then(
        Bindings.selectString(courseProperty, "degree")).otherwise(""));
  }
}
