package de.hhu.stups.plues.ui.components.timetable;

import com.google.inject.Inject;

import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.components.AbstractUnitFilter;
import de.hhu.stups.plues.ui.components.CheckCourseFeasibility;
import de.hhu.stups.plues.ui.components.SetOfCourseSelection;
import de.hhu.stups.plues.ui.controller.Timetable;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;


public class TimetableSideBar extends TabPane implements Initializable {

  private static final String SIDE_BAR_TAB_LAYOUT = "sideBarTabLayout";

  private final UiDataService uiDataService;
  private Node selectedSubTab;
  private Timetable parent;

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

  private BooleanProperty collapsed = new SimpleBooleanProperty(false);

  @Inject
  public TimetableSideBar(final Inflater inflater, final UiDataService uiDataService) {
    this.uiDataService = uiDataService;
    inflater.inflate("components/timetable/TimetableSideBar", this, this, "timetable");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    VBox.setVgrow(this, Priority.ALWAYS);

    getTabs().forEach(sideTab -> {
      sideTab.getStyleClass().addAll(SIDE_BAR_TAB_LAYOUT, "tab", "sideBarTabVisible");
      sideTab.setOnCloseRequest(Event::consume);
    });

    this.collapsed.addListener((observable, oldValue, shouldHide) -> {
      if (shouldHide) {
        hideSideBar();
      } else {
        showSideBar();
      }
    });

    addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
      final Node clickedTab = getClickedTab(mouseEvent);
      if (clickedTab != null) {
        if (selectedSubTab == null) {
          selectedSubTab = this.lookup(".tab:first-child");
        }
        if (selectedSubTab.equals(clickedTab)) { // switch state
          mouseEvent.consume();
          this.collapsed.set(!this.collapsed.get());
        } else { // switch tab and make visible
          selectedSubTab = clickedTab;
          this.collapsed.set(false);
        }
      }
    });
  }

  /**
   * Find the tab that was clicked in the mouse event.
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
  public void initializeComponents(final ObservableStore store) {
    abstractUnitFilter.setAbstractUnits(store.getAbstractUnits());
    abstractUnitFilter.courseFilterProperty().bind(
        setOfCourseSelection.selectedCoursesProperty());

    setOfCourseSelection.setCourses(store.getCourses());

    checkCourseFeasibility.setCourses(store.getCourses());
    checkCourseFeasibility.impossibleCoursesProperty().bind(
        uiDataService.impossibleCoursesProperty());
    setTabPaneButtonHeight();

    parent.widthProperty().addListener((observable, oldValue, newValue) -> {
      if (selectedSubTab == null) {
        parent.setDividerPosition(0, getMinWidth() / parent.getWidth());
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

  private void hideSideBar() {
    getSelectionModel().select(null);

    parent.setDividerPosition(0, getMinWidth() / parent.getWidth());
    parent.disableDivider(true);
    getTabs().forEach(sideTab -> {
      sideTab.getStyleClass().clear();
      sideTab.getStyleClass().addAll(SIDE_BAR_TAB_LAYOUT, "tab", "sideBarTabHidden");
    });
    this.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
    this.getSelectionModel().select(null);
  }

  private void showSideBar() {
    parent.restoreUserDefinedDividerPos();
    parent.disableDivider(false);
    getTabs().forEach(sideTab -> {
      sideTab.getStyleClass().clear();
      sideTab.getStyleClass().addAll(SIDE_BAR_TAB_LAYOUT, "tab", "sideBarTabVisible");
    });
    this.setTabClosingPolicy(TabClosingPolicy.SELECTED_TAB);
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

  public void setParent(final Timetable parent) {
    this.parent = parent;
  }

  private void selectSideBarTab(final Tab tab) {
    getSelectionModel().select(getTabs().indexOf(tab));
    showSideBar();
  }

  public SetOfCourseSelection getSetOfCourseSelection() {
    return setOfCourseSelection;
  }

  public AbstractUnitFilter getAbstractUnitFilter() {
    return abstractUnitFilter;
  }
}
