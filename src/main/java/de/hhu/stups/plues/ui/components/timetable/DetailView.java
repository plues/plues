package de.hhu.stups.plues.ui.components.timetable;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class DetailView extends VBox implements Initializable {

  private final Delayed<Store> delayedStore;

  @FXML
  @SuppressWarnings("unused")
  private HBox session;

  @FXML
  @SuppressWarnings("unused")
  private HBox title;

  @FXML
  @SuppressWarnings("unused")
  private HBox group;

  @FXML
  @SuppressWarnings("unused")
  private HBox semesters;

  @FXML
  @SuppressWarnings("unused")
  private TableView<CourseTableEntry> courseTable;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, String> courseKey;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, String> module;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, Integer> abstractUnit;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, String> courseSemesters;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, Character> type;

  @Inject
  public DetailView(final Inflater inflater,
                    final Delayed<Store> delayedStore) {
    inflater.inflate("components/DetailView", this, this, "detailView");
    this.delayedStore = delayedStore;
  }

  /**
   * Set content for detail view.
   *
   * @param session Session to build content for
   */
  public void setContent(final Session session, final SessionFacade.Slot slot) {
    this.session.getChildren().add(new Label(slot.toString()));
    title.getChildren().add(new Label(session.getGroup().getUnit().getTitle()));
    group.getChildren().add(new Label(String.valueOf(session.getGroup().getId())));

    final StringBuilder builder = new StringBuilder();
    session.getGroup().getUnit().getSemesters().forEach(integer -> {
      builder.append(integer);
      builder.append(",");
    });
    if (builder.length() > 0) {
      builder.setLength(builder.length() - 1);
    }
    semesters.getChildren().add(new Label(builder.toString()));

    Map<Course, Map<Module, Set<AbstractUnit>>> courseModuleAbstractUnit = new HashMap<>();
    Map<AbstractUnit, Set<Integer>> abstractUnitSemesters = new HashMap<>();
    Map<AbstractUnit, Character> abstractUnitType = new HashMap<>();

    delayedStore.whenAvailable(store -> {
      store.getModuleAbstractUnitSemester().forEach(entry -> {
        final Module module = entry.getModule();
        final Integer semester = entry.getSemester();
        final AbstractUnit abstractUnit = entry.getAbstractUnit();

        Set<Integer> semesters = new HashSet<>();
        if (abstractUnitSemesters.containsKey(abstractUnit)) {
          semesters = abstractUnitSemesters.get(abstractUnit);
        }
        semesters.add(semester);
        abstractUnitSemesters.put(abstractUnit, semesters);

        module.getCourses().forEach(course -> {
          Map<Module, Set<AbstractUnit>> innerMap = new HashMap<>();
          if (courseModuleAbstractUnit.containsKey(course)) {
            innerMap = courseModuleAbstractUnit.get(course);
            Set<AbstractUnit> unitIds = new HashSet<>();
            if (innerMap.containsKey(module)) {
              unitIds = innerMap.get(module);
            }
            unitIds.add(abstractUnit);
            innerMap.put(module, unitIds);
          }

          courseModuleAbstractUnit.put(course, innerMap);
        });
      });

      store.getModuleAbstractUnitType().forEach(entry ->
          abstractUnitType.put(entry.getAbstractUnit(), entry.getType()));
    });

    courseModuleAbstractUnit.forEach((course, moduleSetMap) ->
        moduleSetMap.forEach((module, units) ->
            units.forEach(unit ->
                courseTable.getItems().add(
                  new CourseTableEntry(course, module, unit,
                    abstractUnitSemesters.get(unit), abstractUnitType.get(unit))))));
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    courseKey.setCellValueFactory(new PropertyValueFactory<>("courseKey"));
    courseKey.setText(resources.getString("courseCell"));
    module.setCellValueFactory(new PropertyValueFactory<>("module"));
    module.setText(resources.getString("moduleCell"));
    abstractUnit.setCellValueFactory(new PropertyValueFactory<>("abstractUnit"));
    abstractUnit.setText(resources.getString("abstractUnitCell"));
    courseSemesters.setCellValueFactory(new PropertyValueFactory<>("semesters"));
    courseSemesters.setText(resources.getString("semesterCell"));
    type.setCellValueFactory(new PropertyValueFactory<>("type"));
    type.setText(resources.getString("typeCell"));
  }

  public static final class CourseTableEntry {
    private final String courseKey;
    private final String module;
    private final Integer abstractUnit;
    private final Set<Integer> semesters;
    private final Character type;

    /**
     * Constructor for course table.
     * @param course       Course key
     * @param module       Module title
     * @param abstractUnit Abstract Unit title
     * @param semesters     Semester
     * @param type Type
     */
    CourseTableEntry(final Course course,
                     final Module module,
                     final AbstractUnit abstractUnit,
                     final Set<Integer> semesters,
                     final Character type) {
      this.courseKey = course.getKey();
      this.module = module.getTitle();
      this.abstractUnit = abstractUnit.getId();
      this.semesters = semesters;
      this.type = type;
    }

    public String getCourseKey() {
      return courseKey;
    }

    public String getModule() {
      return module;
    }

    public Integer getAbstractUnit() {
      return abstractUnit;
    }

    /**
     * Create string based on semesters.
     * @return String with comma seperated semesters.
     */
    public String getSemesters() {
      final StringBuilder builder = new StringBuilder();
      semesters.forEach(integer -> {
        builder.append(integer);
        builder.append(",");
      });
      if (builder.length() > 0) {
        builder.setLength(builder.length() - 1);
      }
      return builder.toString();
    }

    public Character getType() {
      return type;
    }
  }
}
