package de.hhu.stups.plues.ui.components.timetable;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.components.AbstractUnitFilter;
import de.hhu.stups.plues.ui.components.CheckCourseFeasibility;
import de.hhu.stups.plues.ui.components.SetOfCourseSelection;
import de.hhu.stups.plues.ui.controller.Timetable;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.fxmisc.easybind.EasyBind;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;


public class TimetableSideBar extends TabPane implements Initializable {

  private static final String SIDE_BAR_TAB_LAYOUT = "sideBarTabLayout";

  private Node selectedSubTab;
  private Timetable parent;
  private boolean fadingInProgress = false;
  private final BooleanProperty collapsed = new SimpleBooleanProperty(false);
  private final Map<Tab, Node> tabNodes = new HashMap<>(2);
  private final UiDataService uiDataService;

  @FXML
  @SuppressWarnings("unused")
  private Tab tabCourseFilters;
  @FXML
  @SuppressWarnings("unused")
  private Tab tabCheckFeasibility;
  @FXML
  @SuppressWarnings("unused")
  private SetOfCourseSelection setOfCourseSelection;
  @FXML
  @SuppressWarnings("unused")
  private AbstractUnitFilter abstractUnitFilter;
  @FXML
  @SuppressWarnings("unused")
  private CheckCourseFeasibility checkCourseFeasibility;

  @Inject
  public TimetableSideBar(final Inflater inflater, final UiDataService uiDataService) {
    this.uiDataService = uiDataService;
    inflater.inflate("components/timetable/TimetableSideBar", this, this, "timetable");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    VBox.setVgrow(this, Priority.ALWAYS);

    addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
      final Node clickedTab = getClickedTab(mouseEvent);
      if (clickedTab == null) {
        return;
      }
      handleTabClicked(mouseEvent, clickedTab);
    });

    getTabs().forEach(this::setupTab);

    getTabs().addListener((ListChangeListener<? super Tab>) change ->
        change.getAddedSubList().forEach(this::setupTab));

    initializeCollapsedBindings();
  }

  private void initializeCollapsedBindings() {
    EasyBind.subscribe(collapsed, this::hideSideBar);

    tabClosingPolicyProperty().bind(Bindings.createObjectBinding(() -> {
      if (collapsed.get()) {
        return TabClosingPolicy.UNAVAILABLE;
      } else {
        return TabClosingPolicy.SELECTED_TAB;
      }
    }, collapsed));

  }

  private void setupTab(final Tab tab) {
    tab.setOnCloseRequest(Event::consume);
    tab.getStyleClass().setAll(SIDE_BAR_TAB_LAYOUT, "tab");
    EasyBind.includeWhen(tab.getStyleClass(), "sideBarTabHidden", collapsed);
    EasyBind.includeWhen(tab.getStyleClass(), "sideBarTabVisible", collapsed.not());
  }

  private void handleTabClicked(final MouseEvent mouseEvent, final Node clickedTab) {
    if (fadingInProgress) {
      mouseEvent.consume();
      return;
    }
    //
    if (selectedSubTab == null) {
      selectedSubTab = this.lookup(".tab:first-child");
    }
    //
    if (selectedSubTab.equals(clickedTab)) { // switch state
      mouseEvent.consume();
      this.collapsed.set(!this.collapsed.get());
    } else { // switch tab and make visible
      this.collapsed.set(false);
      selectedSubTab = clickedTab;
    }
  }

  /**
   * Find the tab that was clicked in the mouse event.
   *
   * @param mouseEvent MouseEvent for the click
   * @return Node representing the clicked tab, null if the click was not on a tab header.
   */
  private Node getClickedTab(final MouseEvent mouseEvent) {
    Node node = (Node) mouseEvent.getTarget();

    if (!inTabHeader(node)) {
      return null;
    }

    do {
      if (!node.getStyleClass().contains("tab")) {
        continue;
      }
      return node;
    }
    while ((node = node.getParent()) != null);

    return null;
  }

  /**
   * Check if the node is in a tab header.
   *
   * @param root Node to check
   * @return boolean
   */
  private boolean inTabHeader(final Node root) {
    Node node = root;
    do {
      if (!node.getStyleClass().contains("tab-container")) {
        continue;
      }
      return true;
    }
    while ((node = node.getParent()) != null);

    return false;
  }

  /**
   * Initialize the components {@link #abstractUnitFilter}, {@link #setOfCourseSelection} and {@link
   * #checkCourseFeasibility}.
   */
  public void initializeComponents(final Store store) {
    getTabs().forEach(tab -> tabNodes.put(tab, lookup("#" + tab.getId())));
    selectedSubTab = lookup(".tab:first-child");

    abstractUnitFilter.setAbstractUnits(store.getAbstractUnits());
    abstractUnitFilter.courseFilterProperty().bind(setOfCourseSelection.selectedCoursesProperty());

    setOfCourseSelection.setCourses(store.getCourses());

    checkCourseFeasibility.setCourses(store.getCourses());
    checkCourseFeasibility.impossibleCoursesProperty().bind(
        uiDataService.impossibleCoursesProperty());
    setTabPaneButtonHeight();

    parent.widthProperty().addListener((observable, oldValue, newValue) -> {
      if (selectedSubTab == null) {
        parent.setDividerPosition(getMinWidth() / parent.getWidth());
      }
    });
  }

  /**
   * Activate the component by showing selecting the specific tab according to the given parameter
   * and pre selecting the courses.
   */
  public void activateComponents(final Object[] args) {
    final Course[] courses = (Course[]) args[0];
    final ResultState resultState = (ResultState) args[1];
    setOfCourseSelection.setSelectedCourses(Arrays.asList(courses));

    if (resultState == null) {
      return;
    }

    final boolean bringToFront = args.length != 3 || (boolean) args[2];
    switch (resultState) {
      case FAILED:
        selectCheckFeasibility(courses);
        break;
      case UNKNOWN:
        selectCheckFeasibility(courses);
        if (!bringToFront) {
          checkCourseFeasibility.checkFeasibility();
        }
        break;
      case TIMEOUT:
        selectCheckFeasibility(courses);
        checkCourseFeasibility.checkFeasibility();
        break;
      default:
        selectSideBarTab(tabCourseFilters);
    }
  }

  public void selectCourseFilter(final List<Course> courses) {
    selectSideBarTab(tabCourseFilters);
    setOfCourseSelection.setSelectedCourses(courses);
  }

  private void selectCheckFeasibility(final Course[] courses) {
    selectSideBarTab(tabCheckFeasibility);
    checkCourseFeasibility.selectCourses(courses);
  }

  /**
   * Fade-in or fade-out the {@link this TimetableSideBar} by moving the {@link #parent
   * Timetables's} split pane divider to the destination.
   */
  @SuppressWarnings("unused")
  private void hideSideBar(final boolean hide) {
    if (fadingInProgress) {
      return;
    }
    if (parent == null) {
      return;
    }
    if (hide) {
      selectedSubTab = null;
      getSelectionModel().select(null);
    }
    parent.disableDivider(hide);
    fadingInProgress = true;

    runAnimation(hide);
  }

  private void runAnimation(final boolean hide) {
    final Timeline timeline = new Timeline();
    final double destination;

    if (hide) {
      destination = getMinWidth() / parent.getWidth();
    } else {
      destination = parent.getUserDefinedDividerPos();
    }

    final KeyValue dividerPosition =
          new KeyValue(parent.getDivider().positionProperty(), destination);

    timeline.getKeyFrames().add(new KeyFrame(Duration.millis(250), dividerPosition));
    timeline.setOnFinished(event -> fadingInProgress = false);
    Platform.runLater(timeline::play);
  }

  /**
   * Set the minimum width of the tab pane according to its header, so that the header, i.e. the
   * tabs, is always visible even if the side bar is hidden.
   */
  public void setTabPaneButtonHeight() {
    final StackPane tabPaneHeader = (StackPane) lookup(".tab-header-area");
    setMinWidth(tabPaneHeader.getHeight());
  }

  public double getPaneMinWidth() {
    return getMinWidth();
  }

  public boolean isFadingInProgress() {
    return fadingInProgress;
  }

  public void setParent(final Timetable parent) {
    this.parent = parent;
  }

  private void selectSideBarTab(final Tab tab) {
    getSelectionModel().select(tab);
    selectedSubTab = tabNodes.get(tab);
    collapsed.set(false);
  }

  public SetOfCourseSelection getSetOfCourseSelection() {
    return setOfCourseSelection;
  }

  public AbstractUnitFilter getAbstractUnitFilter() {
    return abstractUnitFilter;
  }

  public boolean isCollapsed() {
    return collapsed.get();
  }
}
