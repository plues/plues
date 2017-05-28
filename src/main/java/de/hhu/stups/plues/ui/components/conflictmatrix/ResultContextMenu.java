package de.hhu.stups.plues.ui.components.conflictmatrix;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.fxmisc.easybind.EasyBind;

import java.util.ResourceBundle;

public class ResultContextMenu extends ContextMenu {

  private final Router router;
  private final Course[] courses;
  private final MenuItem itemShowInTimetable;
  private final MenuItem itemGeneratePartialTimetable;
  private final MenuItem itemComputeConflict;
  private final MenuItem itemGeneratePdf;
  private final MenuItem itemRecomputeFeasibility;
  private final MenuItem itemCheckFeasibility;

  private final ObjectProperty<ResultState> resultState
      = new SimpleObjectProperty<>(ResultState.UNKNOWN);

  /**
   * The context menu for a {@link ResultGridCell} in the {@link de.hhu.stups.plues.ui.controller
   * .ConflictMatrix}.
   */
  @Inject
  ResultContextMenu(final Router router, @Assisted final Course... courses) {
    this.router = router;
    this.courses = courses;
    final ResourceBundle resources = ResourceBundle.getBundle("lang.conflictMatrixContextMenu");

    itemShowInTimetable = new MenuItem(resources.getString("showInTimetable"));
    itemGeneratePartialTimetable = new MenuItem(resources.getString("generatePartialTimetable"));
    itemComputeConflict = new MenuItem(resources.getString("computeConflict"));
    itemGeneratePdf = new MenuItem(resources.getString("generatePdf"));
    itemRecomputeFeasibility = new MenuItem(resources.getString("recomputeFeasibility"));
    itemCheckFeasibility = new MenuItem(resources.getString("checkFeasibility"));

    EasyBind.subscribe(resultState, newValue -> Platform.runLater(() -> updateMenu(newValue)));

    itemGeneratePdf.setOnAction(event ->
        router.transitionTo(RouteNames.PDF_TIMETABLES, (Object[]) courses));
    itemComputeConflict.setOnAction(event ->
        router.transitionTo(RouteNames.UNSAT_CORE, (Object[]) courses));
    itemGeneratePartialTimetable.setOnAction(event ->
        router.transitionTo(RouteNames.PARTIAL_TIMETABLES, (Object[]) courses));
    itemRecomputeFeasibility.setOnAction(event ->
        router.transitionTo(RouteNames.TIMETABLE, courses, ResultState.TIMEOUT));
    itemCheckFeasibility.setOnAction(event ->
        // do not open timetable view but run check feasibility task in background
        router.transitionTo(RouteNames.CHECK_FEASIBILITY_TIMETABLE, courses,
            ResultState.UNKNOWN, false));
  }

  private void updateMenu(final ResultState resultState) {
    getItems().clear();
    if (resultState == null) {
      return;
    }
    switch (resultState) {
      case SUCCEEDED:
        updateToSucceedingState();
        break;
      case IMPOSSIBLE:
        getItems().addAll(itemShowInTimetable);
        itemShowInTimetable.setOnAction(event ->
            router.transitionTo(RouteNames.TIMETABLE, courses, ResultState.IMPOSSIBLE));
        break;
      case FAILED:
        getItems().addAll(itemShowInTimetable, itemComputeConflict);
        itemShowInTimetable.setOnAction(event ->
            router.transitionTo(RouteNames.TIMETABLE, courses, ResultState.FAILED));
        break;
      case TIMEOUT:
        getItems().add(itemRecomputeFeasibility);
        break;
      case UNKNOWN:
        getItems().addAll(itemCheckFeasibility, itemShowInTimetable);
        itemShowInTimetable.setOnAction(event ->
            router.transitionTo(RouteNames.TIMETABLE, courses, ResultState.UNKNOWN));
        break;
      default:
        break;
    }
  }

  private void updateToSucceedingState() {
    if (courses.length == 1 && courses[0].isCombinable()) {
      getItems().add(itemShowInTimetable);
    } else {
      getItems().addAll(itemShowInTimetable, itemGeneratePartialTimetable, itemGeneratePdf);
    }
    itemShowInTimetable.setOnAction(event ->
        router.transitionTo(RouteNames.TIMETABLE, courses, ResultState.SUCCEEDED));
  }

  public ResultState getResultState() {
    return resultState.get();
  }

  public ObjectProperty<ResultState> resultStateProperty() {
    return resultState;
  }

  public void setResultState(final ResultState resultState) {
    this.resultState.set(resultState);
  }
}
