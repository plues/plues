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
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.components.timetable.SessionListView;
import de.hhu.stups.plues.ui.components.timetable.SessionListViewFactory;
import de.hhu.stups.plues.ui.components.timetable.TimetableSideBar;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.Observable;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.SetBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import org.controlsfx.control.SegmentedButton;

import java.net.URL;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Timetable extends SplitPane implements Initializable, Activatable {

  private final Delayed<ObservableStore> delayedStore;
  private final SessionListViewFactory sessionListViewFactory;
  private final UiDataService uiDataService;
  private double userDefinedDividerPos = 0.65;

  @FXML
  @SuppressWarnings("unused")
  private GridPane timeTable;
  @FXML
  @SuppressWarnings("unused")
  private SegmentedButton semesterToggle;
  @FXML
  @SuppressWarnings("unused")
  private TimetableSideBar timetableSideBar;

  private final ListProperty<SessionFacade> sessions = new SimpleListProperty<>();
  private final SetProperty<String> conflictedSemesters;

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

    getDividers().get(0).positionProperty().addListener((observable, oldValue, newValue) -> {
      timetableSideBar.setTabPaneButtonHeight();
      // don't store too small divider positions
      if (Math.abs(newValue.doubleValue()
          - timetableSideBar.getPaneMinWidth() / getWidth()) > 0.25) {
        userDefinedDividerPos = newValue.doubleValue();
      }
    });


    conflictedSemesters.addListener((observable, oldValue, newValue)
        -> this.highlightConflictedSemesters(newValue));

    this.delayedStore.whenAvailable(store
        -> conflictedSemesters.bind(new ConflictedSemestersBinding(store)));

    initSessionBoxes();
  }

  private void highlightConflictedSemesters(final ObservableSet<String> semesters) {
    semesterToggle.getButtons().forEach(toggle -> {
      final String value = (String) toggle.getUserData();

      if (semesters.contains(value)) {
        toggle.getStyleClass().add("conflicted-semester");
      } else {
        toggle.getStyleClass().remove("conflicted-semester");
      }
    });
  }

  /**
   * Disable the split pane divider for the {@link #timetableSideBar}.
   */
  public void disableDivider(final boolean bool) {
    final Node divider = lookup("#timetable > .split-pane-divider");
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

      timeTable.add(view, i % widthX + offX, (i / widthX) + offY);
    });
  }

  private ListView<SessionFacade> getSessionFacadeListView(final SessionFacade.Slot slot) {
    final SessionListView view = sessionListViewFactory.create(slot);

    view.setSessions(sessions);

    final ListBinding<SessionFacade> slotBinding = new SlotBinding(slot);
    final ListBinding<SessionFacade> semesterBinding = new SemesterBinding(slotBinding);
    final ListBinding<SessionFacade> filterBinding = new FilterBinding(semesterBinding);

    view.itemsProperty().bind(filterBinding);
    return view;
  }

  private SessionFacade.Slot getSlot(final int index, final int widthX) {
    final DayOfWeek[] days = {MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY};
    final Integer[] times = {1, 2, 3, 4, 5, 6, 7};

    return new SessionFacade.Slot(days[index % widthX], times[index / widthX]);
  }

  private void setSessions(final List<SessionFacade> sessions) {
    sessions.forEach(SessionFacade::initSlotProperty);
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
        final Optional<SessionFacade> sessionFacade = sessions.stream()
            .filter(facade -> facade.getId() == ((Session) args[0]).getId()).findFirst();

        sessionFacade.ifPresent(this::selectSemesterForSession);
        sessionFacade.ifPresent(facade -> timeTable.getChildren().forEach(node -> {
          if (node instanceof SessionListView) {
            final SessionListView sessionListView = (SessionListView) node;
            sessionListView.scrollTo(facade);
            sessionListView.getSelectionModel().select(facade);
          }
        }));
        break;
      default:
        timetableSideBar.activateComponents(args);
        break;
    }
  }

  private void selectSemesterForSession(final SessionFacade facade) {
    final Toggle selectedSemester = semesterToggle.getToggleGroup().getSelectedToggle();

    // no semester is selected, hence all sessions are visible
    if (selectedSemester == null) {
      return;
    }

    final Set<Integer> unitSemesters = facade.getUnitSemesters();
    final int semester = Integer.parseInt(String.valueOf(selectedSemester.getUserData()));

    // a semester for the session is already selected
    if (unitSemesters.contains(semester)) {
      return;
    }

    final Integer first = Collections.min(unitSemesters);
    final Optional<ToggleButton> toggleButton = semesterToggle.getButtons().stream()
        .filter(button -> button.getUserData().equals(String.valueOf(first)))
        .findFirst();
    toggleButton.ifPresent(button -> button.setSelected(true));
  }

  public void restoreUserDefinedDividerPos() {
    getDividers().get(0).setPosition(userDefinedDividerPos);
  }

  private class ConflictedSemestersBinding extends SetBinding<String> {

    private final ObservableStore store;

    ConflictedSemestersBinding(final ObservableStore store) {
      this.store = store;
      bind(uiDataService.conflictMarkedSessionsProperty());
    }

    @Override
    protected ObservableSet<String> computeValue() {
      final Set<Integer> sessionIds = new HashSet<>(uiDataService.conflictMarkedSessionsProperty());
      return sessions.filtered(facade -> sessionIds.contains(facade.getId())).stream()
          .map(SessionFacade::getUnitSemesters)
          .flatMap(Collection::stream)
          .collect(
              Collectors.collectingAndThen(
                  Collectors.mapping(String::valueOf, Collectors.toSet()),
                  FXCollections::observableSet));
    }
  }

  private class SlotBinding extends ListBinding<SessionFacade> {

    private final SessionFacade.Slot slot;

    SlotBinding(final SessionFacade.Slot slot) {
      this.slot = slot;

      bind(sessions);
    }

    @Override
    protected ObservableList<SessionFacade> computeValue() {
      return sessions.filtered(facade -> facade.getSlot().equals(slot));
    }
  }

  private class SemesterBinding extends ListBinding<SessionFacade> {

    private final ListBinding<SessionFacade> slotBinding;

    SemesterBinding(final ListBinding<SessionFacade> slotBinding) {
      this.slotBinding = slotBinding;
      bind(slotBinding);
      bind(semesterToggle.getToggleGroup().selectedToggleProperty());
    }

    @Override
    protected ObservableList<SessionFacade> computeValue() {
      final Integer semester = getSelectedSemester();
      return slotBinding.filtered(facade -> {
        final Set<Integer> semesters = facade.getUnitSemesters();
        return semester == null || semesters.contains(semester);
      });
    }

    private Integer getSelectedSemester() {
      final ToggleButton semesterButton =
          (ToggleButton) semesterToggle.getToggleGroup().getSelectedToggle();

      if (null != semesterButton) {
        return Integer.valueOf((String) semesterButton.getUserData());
      }

      return null;
    }
  }

  private class FilterBinding extends ListBinding<SessionFacade> {
    private final ListBinding<SessionFacade> semesterBinding;
    private Set<Course> filteredCourses;
    private Set<AbstractUnit> filteredAbstractUnits;

    FilterBinding(final ListBinding<SessionFacade> semesterBinding) {

      this.semesterBinding = semesterBinding;
      bind(semesterBinding);
      bind(timetableSideBar.getSetOfCourseSelection().selectedCoursesProperty(),
          timetableSideBar.getAbstractUnitFilter().selectedAbstractUnitsProperty(),
          uiDataService.conflictMarkedSessionsProperty());
    }

    @Override
    protected ObservableList<SessionFacade> computeValue() {
      return semesterBinding.filtered(this::isNotExcluded);
    }

    private boolean isNotExcluded(final SessionFacade session) {
      return sessionIsIncludedByConflict(session) || sessionIsNotExcluded(session);
    }

    private boolean sessionIsNotExcluded(final SessionFacade session) {
      return !(sessionIsExcludedByAbstractUnit(session) || sessionIsExcludedByCourse(session));
    }

    private boolean sessionIsIncludedByConflict(final SessionFacade session) {
      this.filteredAbstractUnits =
        new HashSet<>(timetableSideBar.getAbstractUnitFilter().getSelectedAbstractUnits());
      this.filteredCourses =
        new HashSet<>(timetableSideBar.getSetOfCourseSelection().getSelectedCourses());

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
}
