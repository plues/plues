package de.hhu.stups.plues.ui.components.conflictmatrix;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.routes.Router;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import java.util.ResourceBundle;

class ResultContextMenu extends ContextMenu {

  private final MenuItem itemShowInTimetable;
  private final MenuItem itemGeneratePartialTimetable;
  private final MenuItem itemComputeConflict;
  private final MenuItem itemGeneratePdf;

  /**
   * The contex menu for a {@link ResultGridCell} in the {@link de.hhu.stups.plues.ui.controller
   * .ConflictMatrix}.
   */

  ResultContextMenu(final Router router, final ObjectProperty<ResultState> resultState,
                    final Course... courses) {
    final ResourceBundle resources = ResourceBundle.getBundle("lang.conflictMatrixContextMenu");

    itemShowInTimetable = new MenuItem(resources.getString("showInTimetable"));
    itemGeneratePartialTimetable = new MenuItem(resources.getString("generatePartialTimetable"));
    itemComputeConflict = new MenuItem(resources.getString("computeConflict"));
    itemGeneratePdf = new MenuItem(resources.getString("generatePdf"));

    resultState.addListener((observable, oldValue, newValue) -> updateMenu(newValue));

    itemShowInTimetable.setOnAction(event -> router.transitionTo("timetableView", courses));
    itemGeneratePdf.setOnAction(event -> router.transitionTo("pdfTimetables", courses));
    itemComputeConflict.setOnAction(event -> router.transitionTo("unsatCore", courses));
    itemGeneratePartialTimetable.setOnAction(
        event -> router.transitionTo("partialTimetables", courses));
  }

  private void updateMenu(final ResultState resultState) {
    getItems().clear();
    switch (resultState) {
      case SUCCEEDED:
        getItems().addAll(itemShowInTimetable, itemGeneratePartialTimetable, itemGeneratePdf);
        break;
      case IMPOSSIBLE:
        getItems().addAll(itemShowInTimetable);
        break;
      case FAILED:
        getItems().addAll(itemShowInTimetable, itemComputeConflict);
        break;
      default:
        break;
    }
  }
}
