package de.hhu.stups.plues.ui.components.timetable;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

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
import java.util.ResourceBundle;
import java.util.Set;

public class DetailView extends VBox implements Initializable {

  @FXML
  @SuppressWarnings("unused")
  private Label session;

  @FXML
  @SuppressWarnings("unused")
  private Label title;

  @FXML
  @SuppressWarnings("unused")
  private Label group;

  @FXML
  @SuppressWarnings("unused")
  private Label semesters;

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
  public DetailView(final Inflater inflater) {
    inflater.inflate("components/DetailView", this, this, "detailView");
  }

  /**
   * Set content for detail view.
   *
   * @param session Session to build content for
   * @param slot Get slot information
   */
  @SuppressWarnings("WeakerAccess")
  public void setContent(final Session session, final SessionFacade.Slot slot) {
    this.session.setText(slot.toString());
    title.setText(session.getGroup().getUnit().getTitle());
    group.setText(String.valueOf(session.getGroup().getId()));
    semesters.setText(Joiner.on(",").join(session.getGroup().getUnit().getSemesters()));

    session.getGroup().getUnit().getAbstractUnits().forEach(au ->
        au.getModuleAbstractUnitTypes().forEach(entry ->
          entry.getModule().getCourses().forEach(course -> {
            final Module entryModule = entry.getModule();
            final CourseTableEntry tableEntry = new CourseTableEntry(course, entryModule, au,
                entryModule.getSemestersForAbstractUnit(au), entry.getType());
            courseTable.getItems().add(tableEntry);
          })));

  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
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

  public String getTitle() {
    return title.getText();
  }

  @SuppressWarnings("WeakerAccess")
  public static final class CourseTableEntry {
    private final String courseKey;
    private final String module;
    private final String abstractUnit;
    private final Set<Integer> semesters;
    private final Character type;


    /**
     * Constructor for course table.
     *
     * @param course       Course key
     * @param module       Module title
     * @param abstractUnit Abstract Unit title
     * @param semesters    Semester
     * @param type         Type
     */
    CourseTableEntry(final Course course,
                     final Module module,
                     final AbstractUnit abstractUnit,
                     final Set<Integer> semesters,
                     final Character type) {
      this.courseKey = course.getKey();
      this.module = module.getTitle();
      this.abstractUnit = abstractUnit.getKey();
      this.semesters = semesters;
      this.type = type;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(final Object other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }

      final CourseTableEntry that = (CourseTableEntry) other;

      if (!courseKey.equals(that.courseKey)) {
        return false;
      }
      if (!module.equals(that.module)) {
        return false;
      }
      if (!abstractUnit.equals(that.abstractUnit)) {
        return false;
      }
      if (!semesters.equals(that.semesters)) {
        return false;
      }
      return type.equals(that.type);

    }

    @Override
    public int hashCode() {
      int result = courseKey.hashCode();
      result = 31 * result + module.hashCode();
      result = 31 * result + abstractUnit.hashCode();
      result = 31 * result + semesters.hashCode();
      result = 31 * result + type.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "CourseTableEntry{"
        + "courseKey='" + courseKey + '\''
        + ", module='" + module + '\''
        + ", abstractUnit='" + abstractUnit + '\''
        + ", semesters=" + semesters
        + ", type=" + type
        + '}';
    }

    @SuppressWarnings("unused")
    public String getCourseKey() {
      return courseKey;
    }

    @SuppressWarnings("unused")
    public String getModule() {
      return module;
    }

    @SuppressWarnings("unused")
    public String getAbstractUnit() {
      return abstractUnit;
    }

    /**
     * Create string based on semesters.
     *
     * @return String with comma separated semesters.
     */
    public String getSemesters() {
      return Joiner.on(',').join(semesters);
    }

    public Character getType() {
      return type;
    }
  }
}
