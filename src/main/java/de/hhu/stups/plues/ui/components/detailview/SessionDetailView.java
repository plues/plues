package de.hhu.stups.plues.ui.components.detailview;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import de.hhu.stups.plues.Helpers;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.components.timetable.SessionFacade;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class SessionDetailView extends VBox implements Initializable {

  private final ObjectProperty<SessionFacade> sessionProperty = new SimpleObjectProperty<>();
  private final Router router;

  @FXML
  @SuppressWarnings("unused")
  private Label lbSession;
  @FXML
  @SuppressWarnings("unused")
  private Label lbTitle;
  @FXML
  @SuppressWarnings("unused")
  private Label lbGroup;
  @FXML
  @SuppressWarnings("unused")
  private Label lbSemesters;
  @FXML
  @SuppressWarnings("unused")
  private Label lbTentative;
  @FXML
  @SuppressWarnings("unused")
  private TableView<CourseTableEntry> courseTable;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, String> tableColumnCourseKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, String> tableColumnModule;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, Integer> tableColumnAbstractUnit;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, Integer> tableColumnSemester;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, Integer> tableColumnType;

  /**
   * Constructor.
   *
   * @param inflater Inflater instance to load FXMl
   */
  @Inject
  public SessionDetailView(final Inflater inflater, final Router router) {
    this.router = router;

    inflater.inflate("components/detailview/SessionDetailView", this, this, "detailView", "Days");
  }

  /**
   * Set content for detail view.
   *
   * @param sessionFacade SessionFacade to build content for
   */
  @SuppressWarnings("WeakerAccess")
  public void setSession(final SessionFacade sessionFacade) {
    this.sessionProperty.set(sessionFacade);
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    sessionProperty.addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        final SessionFacade.Slot slot = newValue.getSlot();
        lbSession.setText(resources.getString(slot.getDayString()) + ", "
            + Helpers.timeMap.get(slot.getTime()));
      }
    });

    lbTitle.textProperty().bind(Bindings.when(sessionProperty.isNotNull()).then(
        Bindings.selectString(sessionProperty, "title")).otherwise(""));

    lbGroup.textProperty().bind(Bindings.when(sessionProperty.isNotNull()).then(
        Bindings.selectString(sessionProperty, "group", "id")).otherwise(""));

    lbSemesters.textProperty().bind(Bindings.createStringBinding(() -> {
      final SessionFacade session = sessionProperty.get();
      if (session == null) {
        return "";
      }
      return session.getUnitSemesters().stream()
          .sorted()
          .map(String::valueOf)
          .collect(Collectors.joining(", "));
    }, sessionProperty));

    lbTentative.textProperty().bind(Bindings.createStringBinding(() -> {
      final SessionFacade session = sessionProperty.get();
      if (session == null) {
        return "?";
      }

      return session.isTentative() ? "✔︎" : "✗";
    }, sessionProperty));

    courseTable.itemsProperty().bind(new CourseTableItemsBinding(sessionProperty));

    courseTable.setOnMouseClicked(this::handleMouseClicked);
  }

  @SuppressWarnings("unused")
  private void handleMouseClicked(final MouseEvent mouseEvent) {
    if (mouseEvent.getClickCount() < 2) {
      return;
    }

    final CourseTableEntry tableEntry = courseTable.getSelectionModel().getSelectedItem();
    if (tableEntry == null) {
      return;
    }

    final TableColumn column
        = courseTable.getSelectionModel().getSelectedCells().get(0).getTableColumn();

    if (column.equals(tableColumnModule)) {
      router.transitionTo(RouteNames.MODULE_DETAIL_VIEW, tableEntry.getModule());
    } else if (column.equals(tableColumnAbstractUnit)) {
      router.transitionTo(RouteNames.ABSTRACT_UNIT_DETAIL_VIEW, tableEntry.getAbstractUnit());
    } else if (column.equals(tableColumnCourseKey)) {
      router.transitionTo(RouteNames.COURSE_DETAIL_VIEW, tableEntry.getCourse());
    }
  }

  public String getTitle() {
    return lbTitle.getText();
  }

  @SuppressWarnings("WeakerAccess")
  public static final class CourseTableEntry {
    private final String courseKey;
    private final Course course;
    private final Module module;
    private final AbstractUnit abstractUnit;
    private final Set<Integer> semesters;
    private final Character type;


    /**
     * Constructor for course table.
     */
    CourseTableEntry(final Course course,
                     final Module module,
                     final AbstractUnit abstractUnit,
                     final Set<Integer> semesters,
                     final Character type) {
      this.course = course;
      this.courseKey = course.getKey();
      this.module = module;
      this.abstractUnit = abstractUnit;
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
      if (!module.getTitle().equals(that.module.getTitle())) {
        return false;
      }
      if (!abstractUnit.getTitle().equals(that.abstractUnit.getTitle())) {
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
    public String getModuleTitle() {
      return module.getTitle();
    }

    @SuppressWarnings("unused")
    public String getAbstractUnitTitle() {
      return abstractUnit.getTitle();
    }

    @SuppressWarnings("unused")
    public Module getModule() {
      return module;
    }

    @SuppressWarnings("unused")
    public AbstractUnit getAbstractUnit() {
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

    public Course getCourse() {
      return course;
    }
  }

  private static class CourseTableItemsBinding extends ListBinding<CourseTableEntry> {
    private final ObjectProperty<SessionFacade> sessionProperty;

    private CourseTableItemsBinding(final ObjectProperty<SessionFacade> sessionProperty) {
      this.sessionProperty = sessionProperty;
      bind(sessionProperty);
    }

    @Override
    protected ObservableList<CourseTableEntry> computeValue() {
      final SessionFacade session = sessionProperty.get();
      if (session == null) {
        return FXCollections.emptyObservableList();
      }
      final Set<AbstractUnit> abstractUnits = session.getIntendedAbstractUnits();
      return abstractUnits.stream().flatMap(au ->
          au.getModuleAbstractUnitTypes().stream().flatMap(entry ->
              entry.getModule().getCourses().stream().map(course -> {
                final Module entryModule = entry.getModule();
                return new CourseTableEntry(course, entryModule, au,
                    entryModule.getSemestersForAbstractUnit(au), entry.getType());
              }))).collect(
          Collectors.collectingAndThen(Collectors.toList(), FXCollections::observableList));
    }
  }
}
