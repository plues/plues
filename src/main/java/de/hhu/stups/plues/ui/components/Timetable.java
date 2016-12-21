package de.hhu.stups.plues.ui.components;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.components.timetable.SessionListView;
import de.hhu.stups.plues.ui.components.timetable.SessionListViewFactory;
import de.hhu.stups.plues.ui.controller.Activatable;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.ListBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Timetable extends SplitPane implements Initializable, Activatable {

  private final Delayed<ObservableStore> delayedStore;
  private final SessionListViewFactory sessionListViewFactory;
  private final Delayed<SolverService> delayedSolverService;
  private final UiDataService uiDataService;
  private Divider splitPaneDivider;
  private double userDefinedDividerPos = 0.65;
  private Tab selectedSubTab;

  @FXML
  @SuppressWarnings("unused")
  private TabPane tabPaneSide;
  @FXML
  @SuppressWarnings("unused")
  private Tab tabCourseFilters;
  @FXML
  @SuppressWarnings("unused")
  private Tab tabCheckFeasibility;
  @FXML
  @SuppressWarnings("unused")
  private GridPane timeTable;
  @FXML
  @SuppressWarnings("unused")
  private SetOfCourseSelection setOfCourseSelection;
  @FXML
  @SuppressWarnings("unused")
  private AbstractUnitFilter abstractUnitFilter;

  @FXML
  @SuppressWarnings("unused")
  private CheckCourseFeasibility checkCourseFeasibility;

  @FXML
  @SuppressWarnings("unused")
  private ToggleGroup semesterToggle;

  private final ListProperty<SessionFacade> sessions = new SimpleListProperty<>();

  /**
   * Timetable component.
   */
  @Inject
  public Timetable(final Inflater inflater, final Delayed<ObservableStore> delayedStore,
                   final Delayed<SolverService> delayedSolverService,
                   final UiDataService uiDataService,
                   final SessionListViewFactory sessionListViewFactory) {
    this.delayedStore = delayedStore;
    this.sessionListViewFactory = sessionListViewFactory;
    this.delayedSolverService = delayedSolverService;
    this.uiDataService = uiDataService;

    // TODO: remove controller param if possible
    // TODO: currently not possible because of dependency circle
    inflater.inflate("components/Timetable", this, this, "timetable", "Days", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.delayedStore.whenAvailable(store -> {
      this.abstractUnitFilter.setAbstractUnits(store.getAbstractUnits());
      setOfCourseSelection.setCourses(store.getCourses());
      checkCourseFeasibility.setCourses(store.getCourses());
      abstractUnitFilter.courseFilterProperty().bind(
          setOfCourseSelection.selectedCoursesProperty());

      setSessions(store.getSessions()
          .parallelStream()
          .map(SessionFacade::new)
          .collect(Collectors.toList()));
    });

    VBox.setVgrow(tabPaneSide, Priority.ALWAYS);

    tabPaneSide.getTabs().forEach(sideTab -> {
      sideTab.getStyleClass().clear();
      sideTab.getStyleClass().add("sideBarTab");
    });

    splitPaneDivider = getDividers().get(0);
    selectedSubTab = tabPaneSide.getSelectionModel().getSelectedItem();

    tabPaneSide.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
      final EventTarget eventTarget = mouseEvent.getTarget();
      if (eventTarget instanceof Text) {
        handleSideBarTabs(((Text) eventTarget).getText());
      } else if (eventTarget instanceof StackPane) {
        final ObservableList<String> styleClasses = ((StackPane) eventTarget).getStyleClass();
        styleClasses.stream().filter("tab-container"::equals).forEach(styleClass -> {
          final Optional<Node> optionalLabel = ((StackPane) eventTarget).getChildren().stream()
              .filter(node -> node instanceof Label).findFirst();
          if (optionalLabel.isPresent()) {
            handleSideBarTabs(((Label) optionalLabel.get()).getText());
          }
        });
      }
    });

    splitPaneDivider.positionProperty().addListener((observable, oldValue, newValue) -> {
      setTabPaneButtonHeight();
      // don't store too small divider positions
      if (Math.abs(newValue.doubleValue() - tabPaneSide.getMinWidth() / getWidth()) > 0.3) {
        userDefinedDividerPos = newValue.doubleValue();
      }
      if ((newValue.doubleValue() > tabPaneSide.getMinWidth() / getWidth())
          && selectedSubTab != null) {
        disableDivider(false);
      }
    });

    widthProperty().addListener((observable, oldValue, newValue) -> {
      if (selectedSubTab == null) {
        splitPaneDivider.setPosition(tabPaneSide.getMinWidth() / getWidth());
      }
    });

    // initialize checkCourseFeasibility component
    checkCourseFeasibility.impossibleCoursesProperty().bind(
        uiDataService.impossibleCoursesProperty());
    delayedSolverService.whenAvailable(
        solverService -> checkCourseFeasibility.setSolverProperty(true));

    initSessionBoxes();
  }

  /**
   * Identify each tab by its text and handle the visibility of the side bar according to the
   * selected tab's state.
   */
  @SuppressWarnings("unused")
  private void handleSideBarTabs(final String tabText) {
    final Optional<Tab> optionalTab = tabPaneSide.getTabs().stream()
        .filter(tab -> tab.getText().equals(tabText)).findFirst();
    if (optionalTab.isPresent()) {
      showOrHideSideBar(optionalTab.get());
    }
  }

  /**
   * Either show or hide the {@link #tabPaneSide tab pane's} content by moving the split pane's
   * slider. The content is hidden if a selected tab is clicked again.
   */
  private void showOrHideSideBar(final Tab tab) {
    if (tab.equals(selectedSubTab)) {
      splitPaneDivider.setPosition(tabPaneSide.getMinWidth() / getWidth());
      selectedSubTab = null;
      disableDivider(true);
      tab.getStyleClass().clear();
      tabPaneSide.getTabs().forEach(sideTab -> {
        sideTab.getStyleClass().clear();
        sideTab.getStyleClass().add("sideBarHiddenTab");
      });
    } else {
      showSideBar(tab);
    }
  }

  private void showSideBar(final Tab tab) {
    tabPaneSide.getSelectionModel().select(tabPaneSide.getTabs().indexOf(tab));
    if (selectedSubTab == null) {
      splitPaneDivider.setPosition(userDefinedDividerPos);
    }
    disableDivider(false);
    selectedSubTab = tab;
    tabPaneSide.getTabs().forEach(sideTab -> {
      sideTab.getStyleClass().clear();
      sideTab.getStyleClass().add("sideBarTab");
    });
  }

  private void disableDivider(final boolean bool) {
    lookup(".split-pane-divider").setDisable(bool);
  }

  /**
   * Set the minimum width of the {@link #tabPaneSide tab pane} according to its header, so that the
   * header, i.e. the tabs, is always visible even if the side bar is hidden.
   */
  private void setTabPaneButtonHeight() {
    final StackPane tabPaneHeader = (StackPane) tabPaneSide.lookup(".tab-header-area");
    tabPaneSide.setMinWidth(tabPaneHeader.getHeight());
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
    this.sessions.set(FXCollections.observableArrayList(sessions));
  }

  /**
   * Highlight the given courses when the user navigates to the timetable via the {@link
   * de.hhu.stups.plues.routes.ControllerRoute}.
   */
  @Override
  public void activateController(final Object... args) {
    final Course[] courses = (Course[]) args[0];
    final ResultState resultState = (ResultState) args[1];
    setOfCourseSelection.setSelectedCourses(Arrays.asList(courses));
    switch (resultState) {
      case FAILED:
        selectSideBarTab(tabCheckFeasibility);
        checkCourseFeasibility.selectCourses(courses);
        break;
      case TIMEOUT:
        selectSideBarTab(tabCheckFeasibility);
        checkCourseFeasibility.selectCourses(courses);
        checkCourseFeasibility.checkFeasibility();
        break;
      default:
        selectSideBarTab(tabCourseFilters);
    }
  }

  private void selectSideBarTab(final Tab tab) {
    tabPaneSide.getSelectionModel().select(tabPaneSide.getTabs().indexOf(tab));
    showSideBar(tab);
  }

  private class SessionFacadeListBinding extends ListBinding<SessionFacade> {

    private final SessionFacade.Slot slot;

    SessionFacadeListBinding(final SessionFacade.Slot slot) {
      this.slot = slot;
      bind(sessions, semesterToggle.selectedToggleProperty());

      // http://stackoverflow.com/questions/32536096/javafx-bindings-not-working-as-expected
      sessions.addListener((Change<? extends SessionFacade> change) -> {
        while (change.next()) {
          if (change.wasAdded()) {
            change.getAddedSubList()
                .forEach(sessionFacade -> bind(sessionFacade.slotProperty()));
          }

          if (change.wasRemoved()) {
            change.getRemoved().forEach(sessionFacade -> unbind(sessionFacade.slotProperty()));
          }
        }
      });
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
            && (semester == null || semesters.contains(semester));
      });
    }
  }
}
