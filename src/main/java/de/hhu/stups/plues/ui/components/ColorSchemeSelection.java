package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.studienplaene.ColorChoice;
import de.hhu.stups.plues.studienplaene.ColorScheme;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Presents a combo box with selectable {@link ColorScheme color schemes}. Mainly used to determine
 * the color for PDFs to be generated.
 */
public class ColorSchemeSelection extends GridPane implements Initializable {

  @FXML
  @SuppressWarnings("unused")
  private ComboBox<ColorScheme> cbColorSchemeSelection;
  @FXML
  @SuppressWarnings("unused")
  private ColumnConstraints columnConstraints;

  @Inject
  public ColorSchemeSelection(final Inflater inflater) {
    inflater.inflate("components/ColorSchemeSelection", this, this);
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    cbColorSchemeSelection.setCellFactory(param -> getColorSchemeListCell());
    cbColorSchemeSelection.setButtonCell(getColorSchemeListCell());
    cbColorSchemeSelection.getItems().addListener((ListChangeListener<ColorScheme>) change ->
        cbColorSchemeSelection.getSelectionModel().selectFirst());
  }

  /**
   * Set the percent width of this component according to the node it is placed in.
   */
  public void setPercentWidth(final double percentWidth) {
    columnConstraints.setPercentWidth(percentWidth);
  }

  /**
   * Add a color scheme for a given color choice, i.e. either {@link ColorChoice#COLOR} or {@link
   * ColorChoice#GRAYSCALE}, and a set of hexadecimal color codes.
   */
  @SuppressWarnings("WeakerAccess")
  public void addColorScheme(final ColorChoice colorChoice, final Set<String> colors) {
    cbColorSchemeSelection.getItems().add(new ColorScheme(colorChoice, colors));
  }

  public ReadOnlyObjectProperty<ColorScheme> selectedColorScheme() {
    return cbColorSchemeSelection.getSelectionModel().selectedItemProperty();
  }

  /**
   * Create a list cell for {@link #cbColorSchemeSelection}.
   */
  private ListCell<ColorScheme> getColorSchemeListCell() {
    return new ListCell<ColorScheme>() {
      @Override
      protected void updateItem(final ColorScheme colorScheme, final boolean empty) {
        super.updateItem(colorScheme, empty);
        if (colorScheme == null || empty) {
          setGraphic(null);
        } else {
          setGraphic(getColorVisualization(colorScheme.getColors()));
        }
      }
    };
  }

  /**
   * Create a visualization of the {@link ColorScheme}.
   */
  @SuppressWarnings("unused")
  private HBox getColorVisualization(final Set<String> colors) {
    final Set<Rectangle> colorRectangles = new HashSet<>(colors.size());
    colors.forEach(color -> {
      final Rectangle rectangle = new Rectangle(15.0, 15.0);
      rectangle.setFill(Color.web(color));
      rectangle.setStroke(Color.BLACK);
      rectangle.setStrokeWidth(1.0);
      colorRectangles.add(rectangle);
    });
    final HBox imageBox = new HBox();
    imageBox.setSpacing(2.0);
    imageBox.getChildren().addAll(colorRectangles);
    return imageBox;
  }

  /**
   * Default initialization for our intentioned behavior since we use the same color schemes for all
   * used pdf color scheme selections.
   */
  public void defaultInitialization() {
    addColorScheme(ColorChoice.COLOR,
        new HashSet<>(Arrays.asList("#DCBFBE", "#DCD6BE", "#C1DCBE", "#F1EAB4", "#C5CBF1",
            "#EFF1CB", "#E5CBF1", "#DCF1E9", "#EFB9B9", "#FFA6A6", "#FCFE80", "#C7FF72",
            "#9AFFA4", "#9AFFD6", "#9AFFF9", "#94E5FF", "#A4C1FF", "#CFA4FF", "#F2A4FF",
            "#F6CCFF", "#FFB5F0", "#F7D9E4", "#E78FFB", "#DAFFB4", "#B4FFFD", "#69BCFF",
            "#FFA361")));
    addColorScheme(ColorChoice.COLOR,
        new HashSet<>(Arrays.asList("#889EB7", "#536D89", "#95B7BF", "#556745", "#A3BC8F",
            "#C19CB5", "#4F483D", "#CCCACB", "#978B95", "#9E8E99", "#6C5B6E", "#B0B398",
            "#787E64", "#A09C90", "#AAADBE", "#9FA080", "#D9CDD7", "#8C7678", "#CCEBB7", "#C5B8B0",
            "#A3C46C", "#95DA6A", "#C4C26C", "#F5ECBB", "#FBC796", "#F0A259", "#D7975A")));
    addColorScheme(ColorChoice.GRAYSCALE, new HashSet<>(Arrays.asList("#000000", "#FFFFFF")));
  }
}
