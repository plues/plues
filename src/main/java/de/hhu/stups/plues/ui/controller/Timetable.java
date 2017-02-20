package de.hhu.stups.plues.ui.controller;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.components.timetable.SemesterChooser;
import de.hhu.stups.plues.ui.components.timetable.SessionFacade;
import de.hhu.stups.plues.ui.components.timetable.SessionListView;
import de.hhu.stups.plues.ui.components.timetable.SessionListViewFactory;
import de.hhu.stups.plues.ui.components.timetable.TimetableSideBar;
import de.hhu.stups.plues.ui.layout.Inflater;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.SetBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Timetable extends SplitPane implements Initializable, Activatable {

  private final Delayed<ObservableStore> delayedStore;
  private final SessionListViewFactory sessionListViewFactory;
  private final UiDataService uiDataService;
  private double userDefinedDividerPos = 0.15;
  private SplitPane.Divider splitPaneDivider;

  @FXML
  @SuppressWarnings("unused")
  private GridPane timeTablePane;
  @FXML
  @SuppressWarnings("unused")
  private SemesterChooser semesterToggle;
  @FXML
  @SuppressWarnings("unused")
  private Label multipleSelectionInfo;
  @FXML
  @SuppressWarnings("unused")
  private Tooltip multipleSelectionHint;
  @FXML
  @SuppressWarnings("unused")
  private TimetableSideBar timetableSideBar;

  private final ListProperty<SessionFacade> sessions = new SimpleListProperty<>();
  private final SetProperty<Integer> conflictedSemesters;

  /**
   * Timetable component.
   */
  @Inject
  public Timetable(final Inflater inflater, final Delayed<ObservableStore> delayedStore,
                   final UiDataService uiDataService,
                   final SessionListViewFactory sessionListViewFactory) {
    this.delayedStore = delayedStore;
    this.sessionListViewFactory = sessionListViewFactory;
    this.uiDataService = uiDataService;
    conflictedSemesters = new SimpleSetProperty<>(FXCollections.emptyObservableSet());

    // TODO: remove controller param if possible
    // TODO: currently not possible because of dependency circle
    inflater.inflate("components/Timetable", this, this, "timetable", "Days", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.delayedStore.whenAvailable(store -> {
      timetableSideBar.initializeComponents(store);
      setSessions(store.getSessions()
          .stream()
          .map(SessionFacade::new)
          .collect(Collectors.toList()));
    });

    timetableSideBar.setParent(this);

    multipleSelectionInfo.graphicProperty().bind(Bindings.createObjectBinding(() ->
        FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.INFO_CIRCLE, "12")));

    multipleSelectionInfo.setOnMouseEntered(event -> {
      final Point2D pos = multipleSelectionInfo.localToScreen(
          multipleSelectionInfo.getLayoutBounds().getMaxX(),
          multipleSelectionInfo.getLayoutBounds().getMaxY());
      multipleSelectionHint.show(multipleSelectionInfo, pos.getX(), pos.getY());
    });
    multipleSelectionInfo.setOnMouseExited(event -> multipleSelectionHint.hide());

    splitPaneDivider = getDividers().get(0);

    splitPaneDivider.positionProperty().addListener((observable, oldValue, newValue) -> {
      // don't store too small divider positions
      if (Math.abs(newValue.doubleValue() - timetableSideBar.getPaneMinWidth() / getWidth()) > 0.25
          && !timetableSideBar.isFadingInProgress()) {
        userDefinedDividerPos = newValue.doubleValue();
      }
    });

    widthProperty().addListener((observable, oldValue, newValue) -> {
      timetableSideBar.setTabPaneButtonHeight();
      if (timetableSideBar.isCollapsed()) {
        splitPaneDivider.setPosition(timetableSideBar.getMinWidth() / getWidth());
      }
    });

    semesterToggle.conflictedSemestersProperty().bind(conflictedSemesters);

    conflictedSemesters.bind(new ConflictedSemestersBinding());

    initSessionBoxes();
  }

  /**
   * Disable the split pane divider for the {@link #timetableSideBar}.
   */
  public void disableDivider(final boolean bool) {
    final Node divider = lookup(".split-pane-divider");
    if (divider != null) {
      divider.setDisable(bool);
    }
  }

  private void initSessionBoxes() {
    final int offX = 1;
    final int offY = 1;
    final int widthX = 5;

    IntStream.range(0, 35).forEach(i -> {
      final SessionFacade.Slot slot = getSlot(i, widthX);

      final ListView<SessionFacade> view = getSessionFacadeListView(slot);

      timeTablePane.add(view, i % widthX + offX, (i / widthX) + offY);
    });
  }

  private ListView<SessionFacade> getSessionFacadeListView(final SessionFacade.Slot slot) {
    final SessionListView view = sessionListViewFactory.create(slot);

    view.setSessions(sessions);

    final SortedList<SessionFacade> sortedSessions = sessions.sorted();
    sortedSessions.comparatorProperty().bind(Bindings.createObjectBinding(
        () -> SessionFacade.displayTextComparator(uiDataService.getSessionDisplayFormat()),
        uiDataService.sessionDisplayFormatProperty()));

    final FilteredList<SessionFacade> slotSessions
        = sortedSessions.filtered(facade -> facade.getSlot().equals(slot));
    final FilteredList<SessionFacade> filteredSessions = new FilteredList<>(slotSessions);
    filteredSessions.predicateProperty().bind(new FilteredSessionsPredicateBinding());

    view.itemsProperty().bind(new SimpleListProperty<>(filteredSessions));
    view.setFocusTraversable(false);

    return view;
  }

  private SessionFacade.Slot getSlot(final int index, final int widthX) {
    final DayOfWeek[] days = {MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY};
    final Integer[] times = {1, 2, 3, 4, 5, 6, 7};

    return new SessionFacade.Slot(days[index % widthX], times[index / widthX]);
  }

  @SuppressWarnings("unused")
  private void setSessions(final List<SessionFacade> sessions) {
    this.sessions.set(FXCollections.observableList(sessions,
        (SessionFacade session) -> new Observable[] {session.slotProperty()}));
  }

  /**
   * Highlight the given courses or session when the user navigates to the timetable via the {@link
   * de.hhu.stups.plues.routes.ControllerRoute}.
   */
  // XXX is it a good idea to use the router for selecting a session?
  // an alternative would be a selected session property in the timetable controller
  // or a global event bus that could unify the different event handling/notification and routing
  // mechanism.
  @Override
  public void activateController(final RouteNames routeName, final Object... args) {
    if (args.length == 0) {
      return;
    }
    switch (routeName) {
      case SESSION_IN_TIMETABLE:
        scrollToSession((Session) args[0]);
        break;
      case CONFLICT_IN_TIMETABLE:
        final List<Course> courses = (args.length > 1 && args[1] != null)
            ? Arrays.asList((Course) args[0], (Course) args[1])
            : Collections.singletonList((Course) args[0]);
        timetableSideBar.selectCourseFilter(courses);
        break;
      default:
        timetableSideBar.activateComponents(args);
        break;
    }
  }

  private void scrollToSession(final Session arg) {
    final Optional<SessionFacade> sessionFacade = sessions.stream()
        .filter(facade -> facade.getId() == arg.getId()).findFirst();

    sessionFacade.ifPresent(this::selectSemesterForSession);
    sessionFacade.ifPresent(facade -> timeTablePane.getChildren().forEach(node -> {
      if (node instanceof SessionListView) {
        final SessionListView sessionListView = (SessionListView) node;
        sessionListView.scrollTo(facade);
        sessionListView.getSelectionModel().select(facade);
      }
    }));
  }

  private void selectSemesterForSession(final SessionFacade facade) {
    final Set<Integer> selectedSemesters = semesterToggle.getSelectedSemesters();
    final Set<Integer> unitSemesters = facade.getUnitSemesters();

    // no semester or one of the unitSemesters is selected, hence all sessions are visible
    if (selectedSemesters.isEmpty() || !Collections.disjoint(selectedSemesters, unitSemesters)) {
      return;
    }

    final Integer first = Collections.min(unitSemesters);
    semesterToggle.setSelectedSemesters(FXCollections.observableSet(first));
  }

  public SplitPane.Divider getDivider() {
    return splitPaneDivider;
  }

  public double getUserDefinedDividerPos() {
    return userDefinedDividerPos;
  }

  private class ConflictedSemestersBinding extends SetBinding<Integer> {

    ConflictedSemestersBinding() {
      bind(uiDataService.conflictMarkedSessionsProperty());
    }

    @Override
    protected ObservableSet<Integer> computeValue() {
      final Set<Integer> sessionIds = new HashSet<>(uiDataService.conflictMarkedSessionsProperty());
      return sessions.filtered(facade -> sessionIds.contains(facade.getId())).stream()
          .map(SessionFacade::getUnitSemesters)
          .flatMap(Collection::stream)
          .collect(
              Collectors.collectingAndThen(Collectors.toSet(), FXCollections::observableSet));
    }
  }

  private class FilterPredicate implements Predicate<SessionFacade> {
    private final HashSet<Course> filteredCourses;
    private final HashSet<AbstractUnit> filteredAbstractUnits;
    private final Set<Integer> selectedSemesters;

    FilterPredicate(final HashSet<Course> filteredCourses,
                    final HashSet<AbstractUnit> filteredAbstractUnits,
                    final Set<Integer> selectedSemesters) {

      this.filteredCourses = filteredCourses;
      this.filteredAbstractUnits = filteredAbstractUnits;
      this.selectedSemesters = selectedSemesters;
    }

    @Override
    public boolean test(final SessionFacade facade) {
      return isIncludedBySemester(facade) && isNotExcluded(facade);
    }

    private boolean isIncludedBySemester(final SessionFacade session) {
      return selectedSemesters.isEmpty()
          || !Collections.disjoint(selectedSemesters, session.getUnitSemesters());
    }

    private boolean isNotExcluded(final SessionFacade session) {
      return sessionIsIncludedByConflict(session) || sessionIsNotExcluded(session);
    }

    private boolean sessionIsNotExcluded(final SessionFacade session) {
      return !(sessionIsExcludedByAbstractUnit(session) || sessionIsExcludedByCourse(session));
    }

    private boolean sessionIsIncludedByConflict(final SessionFacade session) {
      return uiDataService.conflictMarkedSessionsProperty().stream()
          .anyMatch(sessionId -> sessionId == session.getId());
    }

    private boolean sessionIsExcludedByCourse(final SessionFacade session) {
      if (filteredCourses.isEmpty()) {
        return false;
      }

      final Set<Course> sessionCourses = session.getIntendedCourses();
      return Collections.disjoint(filteredCourses, sessionCourses);
    }

    private boolean sessionIsExcludedByAbstractUnit(final SessionFacade session) {
      if (filteredAbstractUnits.isEmpty()) {
        return false;
      }

      final Set<AbstractUnit> sessionAbstractUnits = session.getIntendedAbstractUnits();
      return Collections.disjoint(filteredAbstractUnits, sessionAbstractUnits);
    }
  }

  private class FilteredSessionsPredicateBinding
      extends ObjectBinding<Predicate<? super SessionFacade>> {
    FilteredSessionsPredicateBinding() {
      bind(semesterToggle.selectedSemestersProperty(),
          timetableSideBar.getSetOfCourseSelection().selectedCoursesProperty(),
          timetableSideBar.getAbstractUnitFilter().selectedAbstractUnitsProperty(),
          uiDataService.conflictMarkedSessionsProperty());
    }

    @Override
    public void dispose() {
      super.dispose();
      unbind(semesterToggle.selectedSemestersProperty(),
          timetableSideBar.getSetOfCourseSelection().selectedCoursesProperty(),
          timetableSideBar.getAbstractUnitFilter().selectedAbstractUnitsProperty(),
          uiDataService.conflictMarkedSessionsProperty());
    }

    @Override
    protected Predicate<? super SessionFacade> computeValue() {
      final HashSet<AbstractUnit> filteredAbstractUnits
          = new HashSet<>(timetableSideBar.getAbstractUnitFilter().getSelectedAbstractUnits());
      final HashSet<Course> filteredCourses
          = new HashSet<>(timetableSideBar.getSetOfCourseSelection().getSelectedCourses());
      final Set<Integer> selectedSemesters = semesterToggle.getSelectedSemesters();

      return new FilterPredicate(filteredCourses, filteredAbstractUnits, selectedSemesters);
    }
  }
}
