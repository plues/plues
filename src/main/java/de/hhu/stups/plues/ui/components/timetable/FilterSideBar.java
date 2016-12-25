package de.hhu.stups.plues.ui.components.timetable;

import com.google.inject.Inject;

import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.components.AbstractUnitFilter;
import de.hhu.stups.plues.ui.components.CheckCourseFeasibility;
import de.hhu.stups.plues.ui.components.SetOfCourseSelection;
import de.hhu.stups.plues.ui.components.Timetable;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;


public class FilterSideBar extends TabPane implements Initializable {

  private static final String SIDE_BAR_TAB_LAYOUT = "sideBarTabLayout";

  private final BooleanProperty solverProperty;
  private UiDataService uiDataService;
  private Tab selectedSubTab;
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

  @Inject
  public FilterSideBar(final Inflater inflater) {
    solverProperty = new SimpleBooleanProperty();
    inflater.inflate("components/timetable/FilterSideBar", this, this, "timetable");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    VBox.setVgrow(this, Priority.ALWAYS);

    getTabs().forEach(sideTab ->
        sideTab.getStyleClass().addAll(SIDE_BAR_TAB_LAYOUT, "tab", "sideBarTabVisible"));
    selectedSubTab = getSelectionModel().getSelectedItem();

    addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
      final EventTarget eventTarget = mouseEvent.getTarget();
      if (eventTarget instanceof Text) {
        handleSideBarTabs(((Text) eventTarget).getText());
      } else if (eventTarget instanceof StackPane) {
        final ObservableList<String> styleClasses = ((StackPane) eventTarget).getStyleClass();
        if (styleClasses.contains("tab-container")) {
          final Optional<Node> optionalLabel = ((StackPane) eventTarget).getChildren().stream()
              .filter(node -> node instanceof Label).findFirst();
          optionalLabel.ifPresent(node -> handleSideBarTabs(((Label) node).getText()));
        }
      }
    });
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
    checkCourseFeasibility.getSolverProperty().bind(solverProperty);
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

  /**
   * Identify each tab by its text and handle the visibility of the side bar according to the
   * selected tab's state.
   */
  @SuppressWarnings("unused")
  private void handleSideBarTabs(final String tabText) {
    final Optional<Tab> optionalTab = getTabs().stream()
        .filter(tab -> tab.getText().equals(tabText)).findFirst();
    optionalTab.ifPresent(this::showOrHideSideBar);
  }

  /**
   * Either show or hide the tab pane's content by moving the {@link #parent split pane's} slider.
   * The content is hidden if a selected tab is clicked again.
   */
  private void showOrHideSideBar(final Tab tab) {
    if (tab.equals(selectedSubTab)) {
      parent.setDividerPosition(0, getMinWidth() / parent.getWidth());
      parent.disableDivider(true);
      selectedSubTab = null;
      tab.getStyleClass().clear();
      getTabs().forEach(sideTab -> {
        sideTab.getStyleClass().clear();
        sideTab.getStyleClass().addAll(SIDE_BAR_TAB_LAYOUT, "tab", "sideBarTabHidden");
      });
    } else {
      showSideBar(tab);
    }
  }

  private void showSideBar(final Tab tab) {
    getSelectionModel().select(getTabs().indexOf(tab));
    if (selectedSubTab == null) {
      parent.restoreUserDefinedDividerPos();
    }
    parent.disableDivider(false);
    selectedSubTab = tab;
    getTabs().forEach(sideTab -> {
      sideTab.getStyleClass().clear();
      sideTab.getStyleClass().addAll(SIDE_BAR_TAB_LAYOUT, "tab", "sideBarTabVisible");
    });
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

  public BooleanProperty getSolverProperty() {
    return solverProperty;
  }

  public void setParent(final Timetable parent) {
    this.parent = parent;
  }

  private void selectSideBarTab(final Tab tab) {
    getSelectionModel().select(getTabs().indexOf(tab));
    showSideBar(tab);
  }

  public void setUiDataService(final UiDataService uiDataService) {
    this.uiDataService = uiDataService;
  }

  public SetOfCourseSelection getSetOfCourseSelection() {
    return setOfCourseSelection;
  }

  public AbstractUnitFilter getAbstractUnitFilter() {
    return abstractUnitFilter;
  }
}
