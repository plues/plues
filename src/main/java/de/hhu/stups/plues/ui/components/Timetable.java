package de.hhu.stups.plues.ui.components;

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
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.components.timetable.FilterSideBar;
import de.hhu.stups.plues.ui.components.timetable.SessionListView;
import de.hhu.stups.plues.ui.components.timetable.SessionListViewFactory;
import de.hhu.stups.plues.ui.controller.Activatable;
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
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
  private ToggleGroup semesterToggle;
  @FXML
  @SuppressWarnings("unused")
  private FilterSideBar filterSideBar;

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
      filterSideBar.initializeComponents(store);
      setSessions(store.getSessions()
          .parallelStream()
          .map(SessionFacade::new)
          .collect(Collectors.toList()));
    });

    filterSideBar.setParent(this);
    filterSideBar.setUiDataService(uiDataService);

    getDividers().get(0).positionProperty().addListener((observable, oldValue, newValue) -> {
      filterSideBar.setTabPaneButtonHeight();
      // don't store too small divider positions
      if (Math.abs(newValue.doubleValue() - filterSideBar.getPaneMinWidth() / getWidth()) > 0.25) {
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
    semesterToggle.getToggles().forEach(toggle -> {
      final ToggleButton button = (ToggleButton) toggle;
      final String value = (String) button.getUserData();

      if (semesters.contains(value)) {
        button.getStyleClass().add("conflicted-semester");
      } else {
        button.getStyleClass().remove("conflicted-semester");
      }
    });
  }

  public void disableDivider(final boolean bool) {
    lookup(".split-pane-divider").setDisable(bool);
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
    final ListView<SessionFacade> view = sessionListViewFactory.create(slot);

    ((SessionListView) view).setSessions(sessions);
    view.itemsProperty().bind(new SessionFacadeListBinding(slot));
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
   * Highlight the given courses when the user navigates to the timetable via the {@link
   * de.hhu.stups.plues.routes.ControllerRoute}.
   */
  @Override
  public void activateController(final Object... args) {
    filterSideBar.activateComponents(args);
  }

  public void restoreUserDefinedDividerPos() {
    getDividers().get(0).setPosition(userDefinedDividerPos);
  }

  private class SessionFacadeListBinding extends ListBinding<SessionFacade> {

    private final SessionFacade.Slot slot;

    SessionFacadeListBinding(final SessionFacade.Slot slot) {
      this.slot = slot;

      bind(sessions, semesterToggle.selectedToggleProperty(),
          filterSideBar.getSetOfCourseSelection().selectedCoursesProperty(),
          filterSideBar.getAbstractUnitFilter().selectedAbstractUnitsProperty());
    }

    @Override
    protected ObservableList<SessionFacade> computeValue() {
      final ToggleButton semesterButton = (ToggleButton) semesterToggle.getSelectedToggle();

      return sessions.filtered(session -> {
        final Set<Integer> semesters = session.getUnitSemesters();

        Integer semester = null;
        if (null != semesterButton) {
          semester = Integer.valueOf(semesterButton.getText());
        }

        return session.getSlot().equals(slot)
            && !sessionIsExcludedByCourse(session)
            && !sessionIsExcludedByAbstractUnit(session)
            && (semester == null || semesters.contains(semester));
      });
    }

    private boolean sessionIsExcludedByCourse(SessionFacade session) {
      final Set<Course> filteredCourses =
          new HashSet<>(filterSideBar.getSetOfCourseSelection().getSelectedCourses());

      final Set<Course> sessionCourses = session.getIntendedCourses();

      sessionCourses.retainAll(filteredCourses);

      return !filteredCourses.isEmpty() && sessionCourses.isEmpty();
    }

    private boolean sessionIsExcludedByAbstractUnit(final SessionFacade session) {
      final Set<AbstractUnit> filteredAbstractUnits =
          new HashSet<>(filterSideBar.getAbstractUnitFilter().getSelectedAbstractUnits());

      final Set<AbstractUnit> sessionAbstractUnits = session.getIntendedAbstractUnits();

      sessionAbstractUnits.retainAll(filteredAbstractUnits);

      return !filteredAbstractUnits.isEmpty() && sessionAbstractUnits.isEmpty();
    }
  }

  private class ConflictedSemestersBinding extends SetBinding<String> {

    private final ObservableStore store;

    ConflictedSemestersBinding(final ObservableStore store) {
      this.store = store;
      bind(uiDataService.conflictMarkedSessionsProperty());
    }

    @Override
    protected ObservableSet<String> computeValue() {
      return uiDataService.getConflictMarkedSessions().parallelStream()
        .map(store::getSessionById)
        .map(SessionFacade::new)
        .map(SessionFacade::getUnitSemesters)
        .flatMap(Collection::stream)
        .collect(
          Collectors.collectingAndThen(
            Collectors.mapping(String::valueOf, Collectors.toSet()),
            FXCollections::observableSet));
    }
  }
}
