package de.hhu.stups.plues.ui.components.detailView;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;

public class SessionDetailView extends VBox implements Initializable {

  private final ObjectProperty<Session> sessionProperty;
  private final ObjectProperty<SessionFacade> sessionFacadeProperty;

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
  private Label tentative;
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

  /**
   * Constructor.
   * @param inflater Inflater instance to load FXMl
   */
  @Inject
  public SessionDetailView(final Inflater inflater) {
    sessionProperty = new SimpleObjectProperty<>();
    sessionFacadeProperty = new SimpleObjectProperty<>();
    inflater.inflate("components/detailView/SessionDetailView", this, this, "detailView");
  }

  /**
   * Set content for detail view.
   *
   * @param sessionFacade SessionFacade to build content for
   */
  @SuppressWarnings("WeakerAccess")
  public void setSession(final SessionFacade sessionFacade) {
    this.sessionProperty.set(sessionFacade.getSession());
    this.sessionFacadeProperty.set(sessionFacade);
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.title.textProperty().bind(Bindings.when(sessionProperty.isNotNull()).then(
        Bindings.selectString(sessionProperty, "group", "unit", "title")).otherwise(""));
    this.session.textProperty().bind(Bindings.when(sessionProperty.isNotNull()).then(
        Bindings.selectString(sessionFacadeProperty, "slot")).otherwise(""));
    this.group.textProperty().bind(Bindings.when(sessionProperty.isNotNull()).then(
        Bindings.selectString(sessionProperty, "group", "id")).otherwise(""));
    this.semesters.textProperty().bind(new StringBinding() {
      {
        bind(sessionProperty);
      }

      @Override
      protected String computeValue() {
        final Session sessionFacade = sessionProperty.get();
        if (sessionFacade == null) {
          return "";
        }
        return Joiner.on(", ").join(sessionFacade.getGroup().getUnit().getSemesters());
      }
    });
    this.tentative.textProperty().bind(Bindings.createStringBinding(() -> {
      Session sessionFacade = sessionProperty.get();
      if (sessionFacade == null) {
        return "?";
      }

      return sessionFacade.isTentative() ? "✔︎" : "✗";
    }, sessionProperty));

    courseTable.itemsProperty().bind(new ListBinding<CourseTableEntry>() {
      {
        bind(sessionProperty);
      }

      @Override
      protected ObservableList<CourseTableEntry> computeValue() {
        final Session sessionFacade = sessionProperty.get();
        if (sessionFacade == null) {
          return FXCollections.observableArrayList();
        }
        final Set<AbstractUnit> abstractUnits
            = sessionFacade.getGroup().getUnit().getAbstractUnits();
        final ObservableList<CourseTableEntry> result = FXCollections.observableArrayList();
        abstractUnits.forEach(au ->
            au.getModuleAbstractUnitTypes().forEach(entry ->
              entry.getModule().getCourses().forEach(course -> {
                final Module entryModule = entry.getModule();
                final CourseTableEntry tableEntry = new CourseTableEntry(course, entryModule, au,
                    entryModule.getSemestersForAbstractUnit(au), entry.getType());
                result.add(tableEntry);
              })));
        return result;
      }
    });

    courseKey.setCellValueFactory(new PropertyValueFactory<>("courseKey"));
    module.setCellValueFactory(new PropertyValueFactory<>("module"));
    abstractUnit.setCellValueFactory(new PropertyValueFactory<>("abstractUnit"));
    courseSemesters.setCellValueFactory(new PropertyValueFactory<>("semesters"));
    type.setCellValueFactory(new PropertyValueFactory<>("type"));
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
