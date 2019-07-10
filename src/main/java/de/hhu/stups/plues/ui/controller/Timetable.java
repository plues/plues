package de.hhu.stups.plues.ui.controller;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.StoreChange;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Log;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.services.HistoryManager;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.TooltipAllocator;
import de.hhu.stups.plues.ui.components.timetable.MoveSessionDialog;
import de.hhu.stups.plues.ui.components.timetable.SemesterChooser;
import de.hhu.stups.plues.ui.components.timetable.SessionFacade;
import de.hhu.stups.plues.ui.components.timetable.SessionListView;
import de.hhu.stups.plues.ui.components.timetable.SessionListViewFactory;
import de.hhu.stups.plues.ui.components.timetable.TimetableSideBar;
import de.hhu.stups.plues.ui.layout.Inflater;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.SetBinding;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import org.fxmisc.easybind.EasyBind;
import org.reactfx.util.FxTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Timetable extends StackPane implements Activatable {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Delayed<ObservableStore> delayedStore;
  private final SessionListViewFactory sessionListViewFactory;
  private final UiDataService uiDataService;
  private final ListeningExecutorService executorService;
  private final SetProperty<Integer> conflictedSemesters;
  private final HistoryManager historyManager;
  private final Map<Integer, SessionFacade> sessionFacadeMap;
  private final Map<SessionFacade.Slot, ObservableList<SessionFacade>> slotSessionFacadeMap;

  private double userDefinedDividerPos = 0.15;
  private SplitPane.Divider splitPaneDivider;

  @FXML
  @SuppressWarnings("unused")
  private SplitPane timeTableSplitPane;
  @FXML
  @SuppressWarnings("unused")
  private MoveSessionDialog moveSessionDialog;
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
  @FXML
  @SuppressWarnings("unused")
  private HBox semesterToggleBox;


  /**
   * Timetable component.
   */
  @Inject
  public Timetable(final Inflater inflater,
                   final Delayed<ObservableStore> delayedStore,
                   final UiDataService uiDataService,
                   final SessionListViewFactory sessionListViewFactory,
                   final HistoryManager historyManager,
                   final ListeningExecutorService executorService) {
    this.delayedStore = delayedStore;
    this.sessionListViewFactory = sessionListViewFactory;
    this.uiDataService = uiDataService;
    this.executorService = executorService;
    this.historyManager = historyManager;
    conflictedSemesters = new SimpleSetProperty<>(FXCollections.emptyObservableSet());
    sessionFacadeMap = new HashMap<>();
    slotSessionFacadeMap = new HashMap<>();

    inflater.inflate("components/Timetable", this, this, "timetable", "Days", "Column");
  }

  @FXML
  public void initialize() {
    delayedStore.whenAvailable(store -> {
      store.getChanges().subscribe(this::handleStoreChanges);

      timetableSideBar.initializeComponents(store);
      setSessionFacades(store);

      final List<Integer> range = getSemesterRange(store);
      semesterToggle.setSemesters(range);
      semesterToggleBox.setVisible(true);
    });

    timetableSideBar.setParent(this);

    semesterToggle.conflictedSemestersProperty().bind(conflictedSemesters);
    conflictedSemesters.bind(new ConflictedSemestersBinding());

    initSessionBoxes();
    initializeSplitPaneDivider();
    initializeInfoTooltip();

    getChildren().remove(moveSessionDialog);
    moveSessionDialog.setTranslateZ(1);

    setUiDataServiceListener();
  }

  /**
   * {@link #updateSessionFacade(Session, DayOfWeek, Integer) Update}  the changed {@link
   * SessionFacade} according to the {@link de.hhu.stups.plues.HistoryChangeType}, i.e., either
   * going back (undo) or forward (redo, move).
   */
  private void handleStoreChanges(final StoreChange change) {
    final Log log = change.getLog();
    final DayOfWeek dayOfWeek;
    final Integer time;
    switch (change.historyChangeType()) {
      case BACK:
        dayOfWeek = change.getSession().getDayOfWeekMap().get(log.getSrcDay());
        time = log.getSrcTime();
        break;
      case FORWARD:
        dayOfWeek = change.getSession().getDayOfWeekMap().get(log.getTargetDay());
        time = log.getTargetTime();
        break;
      default:
        return;
    }
    updateSessionFacade(change.getSession(), dayOfWeek, time);
  }

  private void updateSessionFacade(final Session session,
                                   final DayOfWeek targetDayOfWeek,
                                   final Integer targetTime) {
    final SessionFacade sessionFacade = sessionFacadeMap.get(session.getId());
    slotSessionFacadeMap.get(sessionFacade.getSlot()).remove(sessionFacade);
    final SessionFacade.Slot slot = new SessionFacade.Slot(targetDayOfWeek, targetTime);
    sessionFacade.setSlot(slot);
    slotSessionFacadeMap.get(slot).add(sessionFacade);
    highlightSession(sessionFacade, slot);
  }

  private void initializeInfoTooltip() {
    multipleSelectionInfo.graphicProperty().bind(Bindings.createObjectBinding(() ->
        FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.INFO_CIRCLE, "14")));
    TooltipAllocator.showTooltipOnEnter(multipleSelectionInfo, multipleSelectionHint,
        new SimpleBooleanProperty(false));
  }

  private void initializeSplitPaneDivider() {
    splitPaneDivider = timeTableSplitPane.getDividers().get(0);

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
  }

  private void setUiDataServiceListener() {
    // move session and hide warning automatically when all running tasks finished
    EasyBind.subscribe(uiDataService.runningTasksProperty(), newValue
        -> moveSessionIfNoTasksRunning(newValue.intValue()));

    EasyBind.subscribe(uiDataService.moveSessionTaskProperty(), newValue -> {
      if (newValue == null) {
        getChildren().remove(moveSessionDialog);
        return;
      }
      showMoveSessionWarning();
    });
  }

  /**
   * Move the session stored in {@link UiDataService#moveSessionTaskProperty()} if there are
   * currently no running tasks described by the given parameter.
   */
  private void moveSessionIfNoTasksRunning(final int runningTasks) {
    final SolverTask<Void> moveSessionTask = uiDataService.moveSessionTaskProperty().get();
    if (runningTasks == 0 && moveSessionTask != null) {
      //noinspection ResultOfMethodCallIgnored
      executorService.submit(moveSessionTask);
      uiDataService.moveSessionTaskProperty().set(null);
    }
  }

  /**
   * Show the {@link MoveSessionDialog} if more than one task is running.
   */
  private void showMoveSessionWarning() {
    if (uiDataService.runningTasksProperty().greaterThan(1).get()) {
      moveSessionDialog.setLayoutX(getWidth() / 2);
      moveSessionDialog.setLayoutY(getHeight() / 2);
      getChildren().add(moveSessionDialog);
    }
  }

  private void setSessionFacades(final ObservableStore store) {
    logger.debug("Loading and setting SessionFacades");
    final Task<Void> setSessionsTask = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        setSessions(store.getSessions().stream().map(SessionFacade::new)
            .collect(Collectors.toList()));
        return null;
      }
    };
    final Thread setSessionsThread = new Thread(setSessionsTask);
    setSessionsThread.start();
  }

  private List<Integer> getSemesterRange(final Store store) {
    final List<Integer> semesters = store.getUnits().stream()
        .flatMap(unit -> unit.getSemesters().stream())
        .distinct()
        .collect(Collectors.toList());
    final Integer min = Collections.min(semesters);
    final Integer max = Collections.max(semesters);
    return IntStream.rangeClosed(min, max).boxed().collect(Collectors.toList());
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
      final ObservableList<SessionFacade> sessionFacadeList =
          FXCollections.observableList(FXCollections.observableArrayList(),
              (SessionFacade session) -> {
                sessionFacadeMap.put(session.getId(), session);
                return new Observable[] {session.slotProperty()};
              });
      final SortedList<SessionFacade> sortedList = sessionFacadeList.sorted();
      sortedList.comparatorProperty().bind(getSessionComparator());
      slotSessionFacadeMap.put(slot, sessionFacadeList);
      final ListView<SessionFacade> view = getSessionFacadeListView(sortedList, slot);
      timeTablePane.add(view, i % widthX + offX, (i / widthX) + offY);
    });
  }

  /**
   * Sort the session according to the current {@link UiDataService#sessionDisplayFormatProperty
   * session display format}.
   */
  private ObservableValue<Comparator<SessionFacade>> getSessionComparator() {
    return Bindings.createObjectBinding(
        () -> SessionFacade.displayTextComparator(uiDataService.getSessionDisplayFormat()),
        uiDataService.sessionDisplayFormatProperty());
  }

  private ListView<SessionFacade> getSessionFacadeListView(
      final SortedList<SessionFacade> sortedSessions, final SessionFacade.Slot slot) {
    final SessionListView view = sessionListViewFactory.create(slot);

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
    sessions.forEach(sessionFacade -> Platform.runLater(() -> {
      final ObservableList<SessionFacade> observableList =
          slotSessionFacadeMap.get(sessionFacade.getSlot());
      if (observableList != null) { // some sessions are on the weekend like "blockseminare"
        observableList.add(sessionFacade);
        sessionFacadeMap.put(sessionFacade.getId(), sessionFacade);
      }
    }));
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

  /**
   * Highlight a given session, i.e., scroll to the session in its listview, select the list cell
   * and {@link #highlightListViewForSlot(SessionFacade.Slot) highlight the listview} for a few
   * seconds.
   */
  private void highlightSession(final SessionFacade sessionFacade,
                                final SessionFacade.Slot slot) {
    selectSemesterForSession(sessionFacade);
    highlightListViewForSlot(slot);
    scrollToSessionFacade(sessionFacade);
  }

  /**
   * Highlight a session list view for a few seconds by setting a border color.
   */
  private void highlightListViewForSlot(final SessionFacade.Slot slot) {
    for (final Node node : timeTablePane.getChildren()) {
      if (!(node instanceof SessionListView)
          || !slot.equals(((SessionListView) node).getSlot())) {
        continue;
      }
      Platform.runLater(() -> {
        node.setStyle("-fx-border-width: 2px; -fx-border-color: #FF8000;");
        FxTimer.runLater(Duration.ofMillis(500), () -> {
          node.setStyle("-fx-border-insets: 0;");
          historyManager.historyEnabledProperty().set(true);
        });
      });
      return;
    }
  }

  private void scrollToSession(final Session session) {
    final SessionFacade sessionFacade = sessionFacadeMap.get(session.getId());
    scrollToSessionFacade(sessionFacade);
  }

  private void scrollToSessionFacade(final SessionFacade sessionFacade) {
    selectSemesterForSession(sessionFacade);
    timeTablePane.getChildren().forEach(node -> {
      if (node instanceof SessionListView) {
        final SessionListView sessionListView = (SessionListView) node;
        Platform.runLater(() -> sessionListView.scrollTo(sessionFacade));
        sessionListView.getSelectionModel().select(sessionFacade);
      }
    });
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

  public void setDividerPosition(final double pos) {
    timeTableSplitPane.setDividerPosition(0, pos);
  }

  private class ConflictedSemestersBinding extends SetBinding<Integer> {

    ConflictedSemestersBinding() {
      bind(uiDataService.conflictMarkedSessionsProperty());
    }

    @Override
    protected ObservableSet<Integer> computeValue() {
      final Set<Integer> sessionIds = new HashSet<>(uiDataService.conflictMarkedSessionsProperty());
      return sessionFacadeMap.entrySet().stream()
          .filter(entry -> sessionIds.contains(entry.getKey()))
          .map(entry -> entry.getValue().getUnitSemesters())
          .flatMap(Collection::stream)
          .collect(Collectors.collectingAndThen(Collectors.toSet(), FXCollections::observableSet));
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
