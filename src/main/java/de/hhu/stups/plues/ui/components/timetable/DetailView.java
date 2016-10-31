package de.hhu.stups.plues.ui.components.timetable;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class DetailView extends VBox implements Initializable {

  @FXML
  @SuppressWarnings("unused")
  private TableView<SessionTableEnry> sessionTable;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<SessionTableEnry, String> keyColumn;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<SessionTableEnry, String> valueColumn;

  @FXML
  @SuppressWarnings("unused")
  private TableView<CourseTableEntry> courseTable;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, String> courseName;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, Integer> po;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, String> moduleName;

  @Inject
  public DetailView(final Inflater inflater) {
    inflater.inflate("components/DetailView", this, this);
  }

  /**
   * Set content for detail view.
   * @param abstractUnits Abstract unit to display
   * @param unit Unit to display
   * @param courseModuleMap Module to display
   */
  public void setContent(final Set<AbstractUnit> abstractUnits,
                         final Unit unit,
                         final Map<Course, Set<Module>> courseModuleMap) {
    sessionTable.getItems().add(new SessionTableEnry("Unit", unit.getTitle()));

    StringBuilder abstractUnitBuilder = new StringBuilder();
    abstractUnits.forEach(abstractUnit -> {
      abstractUnitBuilder.append(abstractUnit.getId());
      abstractUnitBuilder.append(",");
    });
    if (abstractUnitBuilder.length() > 0) {
      abstractUnitBuilder.setLength(abstractUnitBuilder.length() - 1);
    }
    sessionTable.getItems().add(
        new SessionTableEnry("Abstract Units", abstractUnitBuilder.toString()));

    courseModuleMap.forEach((course, modules) ->
        courseTable.getItems().add(new CourseTableEntry(course, modules)));
  }

  /**
   * Remove header line of table.
   * @throws NullPointerException TODO: Exception sollte nicht geworfen werden
   */
  private void removeHeader() throws NullPointerException {
    widthProperty().addListener((observable, oldValue, newValue) -> {
      Pane header = (Pane) lookup("TableHeaderRow");
      header.setVisible(false);
      header.setMaxHeight(0);
      header.setMinHeight(0);
      header.setPrefHeight(0);
    });
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    keyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
    valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));

    courseName.setCellValueFactory(new PropertyValueFactory<>("courseKey"));
    courseName.setText("Course Name");
    po.setCellValueFactory(new PropertyValueFactory<>("po"));
    po.setText("PO");
    moduleName.setCellValueFactory(new PropertyValueFactory<>("modules"));
    moduleName.setText("Modules");
  }

  public static final class SessionTableEnry {
    private final String key;
    private final String value;

    SessionTableEnry(final String key, final String value) {
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public String getValue() {
      return value;
    }
  }

  public static final class CourseTableEntry {
    private final String courseKey;
    private final Integer po;
    private final Set<Module> modules;

    /**
     * Constructor for course table entry.
     * @param course Course object; display key and po
     * @param modules Set of modules belonging to the course; display ids
     */
    CourseTableEntry(final Course course, final Set<Module> modules) {
      this.courseKey = course.getKey();
      this.po = course.getPo();
      this.modules = modules;
    }

    public String getCourseKey() {
      return courseKey;
    }

    public Integer getPo() {
      return po;
    }

    /**
     * Build String containing comma separated ids of modules.
     * @return String with module ids
     */
    public String getModules() {
      StringBuilder builder = new StringBuilder();
      modules.forEach(module -> {
        builder.append(module.getId());
        builder.append(",");
      });
      if (builder.length() > 0) {
        builder.setLength(builder.length() - 1);
      }
      return builder.toString();
    }
  }
}
