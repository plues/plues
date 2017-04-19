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
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
    cbColorSchemeSelection.disableProperty().bind(disableProperty());
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
  public void addColorScheme(final String name, final ColorChoice colorChoice,
                             final Set<String> colors) {
    cbColorSchemeSelection.getItems().add(new ColorScheme(name, colorChoice, colors));
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
          setGraphic(getColorVisualization(colorScheme));
        }
      }
    };
  }

  /**
   * Create a visualization of the {@link ColorScheme} whereat the amount of previewing boxes is
   * limited.
   */
  @SuppressWarnings("unused")
  private HBox getColorVisualization(final ColorScheme colorScheme) {
    final Iterator<String> colors = colorScheme.getColors().iterator();
    final Set<Rectangle> colorRectangles = new LinkedHashSet<>();

    final int previewAmount = 20;
    int previewCounter = 0;
    while (colors.hasNext()) {
      final Rectangle rectangle = new Rectangle(15.0, 15.0);
      rectangle.setFill(Color.web(colors.next()));
      rectangle.setStroke(Color.BLACK);
      rectangle.setStrokeWidth(1.0);
      colorRectangles.add(rectangle);
      previewCounter++;
      if (previewCounter == previewAmount) {
        break;
      }
    }

    final HBox imageBox = new HBox();
    imageBox.setSpacing(2.0);
    imageBox.getChildren().addAll(colorRectangles);
    final Label label = new Label(colorScheme.getName());
    label.setStyle("-fx-text-fill: #000000;");
    imageBox.getChildren().add(imageBox.getChildren().size(), label);
    return imageBox;
  }

  /**
   * Default initialization for our intentioned behavior since we use the same color schemes for all
   * used pdf color scheme selections.
   */
  public void defaultInitialization() {
    addColorScheme("Intense", ColorChoice.COLOR,
        new LinkedHashSet<>(Arrays.asList("#aa7959",
            "#56c33d", "#4691eb", "#a4c230", "#3568b0", "#e0b53b", "#3ba7e5", "#f8bb4a",
            "#46beda", "#e57f2d", "#6e97d7", "#529d21", "#82bcec", "#d59324", "#3e94bf",
            "#b3aa2a", "#537ca1", "#45c66a", "#996a21", "#4cc8b7", "#e2835c", "#4ac08c",
            "#ac6c27", "#448ba0", "#dba04f", "#84a9c8", "#779021", "#5aacaa", "#958220",
            "#67a194", "#c5b653", "#3d856a", "#dca371", "#419c48", "#d7a48b", "#559340",
            "#af7d5b", "#81c475", "#83825d", "#8dbe56", "#80793a", "#85b78f", "#9d8042",
            "#4a8250", "#c1b57f", "#789a5a", "#aab765", "#8d9059", "#73944c", "#5e835e")));
    addColorScheme("Pastel", ColorChoice.COLOR,
        new LinkedHashSet<>(Arrays.asList("#acd8ba",
            "#dbbbec", "#a1c293", "#b7a9d4", "#b5d7a7", "#e8a7ba", "#ccf4cc", "#e4bad9",
            "#87cdbf", "#e8aa95", "#8ae1f9", "#e3aba7", "#74cce4", "#eddaac", "#9cb8e2",
            "#dee7bc", "#c9cdf5", "#bbc49a", "#8bc0dd", "#e6c6a6", "#a0d5f2", "#c9ae8e",
            "#a2e7ed", "#f2c0ce", "#8bb598", "#eed8e4", "#98c3a6", "#bcadc4", "#e9f5d3",
            "#a8b3c4", "#d0d9ae", "#7fc4ca", "#efcdc2", "#8dd2d8", "#cbbaab", "#b6eee2",
            "#d5c1d0", "#86bcb1", "#ebe2cf", "#97b1ab", "#d4efe9", "#d7e0b5", "#bacfde",
            "#c7b794", "#bddfe5", "#a0b099", "#d0e0c8", "#abc5bf", "#adbea6", "#bbcbb3")));
    addColorScheme("Grayscale", ColorChoice.GRAYSCALE,
        new LinkedHashSet<>(Arrays.asList("#000000", "#FFFFFF")));
  }
}
