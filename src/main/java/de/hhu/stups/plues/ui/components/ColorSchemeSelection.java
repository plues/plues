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
   * limited to fit the {@link #cbColorSchemeSelection}.
   */
  @SuppressWarnings("unused")
  private HBox getColorVisualization(final ColorScheme colorScheme) {
    final double previewRectangleSize = 15.0;
    final HBox imageBox = new HBox();
    imageBox.setSpacing(2.0);
    addColorPreviews(imageBox,
        Math.round(cbColorSchemeSelection.widthProperty().get() / previewRectangleSize) / 2,
        previewRectangleSize, colorScheme);
    cbColorSchemeSelection.widthProperty().addListener((observable, oldValue, newValue) -> {
      imageBox.getChildren().clear();
      addColorPreviews(imageBox,
          Math.round(newValue.doubleValue() / previewRectangleSize) / 2,
          previewRectangleSize, colorScheme);
    });
    return imageBox;
  }

  private void addColorPreviews(final HBox imageBox,
                                final long previewAmount,
                                final double previewRectangleSize,
                                final ColorScheme colorScheme) {
    final Iterator<String> colors = colorScheme.getColors().iterator();
    int previewCounter = 0;
    while (colors.hasNext()) {
      if (colors.hasNext()) {
        imageBox.getChildren().add(getColorPreview(previewRectangleSize, colors.next()));
      }
      previewCounter++;
      if (previewCounter == previewAmount) {
        break;
      }
    }
    final Label label = new Label(" - " + colorScheme.getName());
    label.setStyle("-fx-text-fill: #000000;");
    imageBox.getChildren().add(imageBox.getChildren().size(), label);
  }

  private Rectangle getColorPreview(final double rectangleSize, final String hexColorCode) {
    final Rectangle rectangle = new Rectangle(rectangleSize, rectangleSize);
    rectangle.setFill(Color.web(hexColorCode));
    rectangle.setStroke(Color.BLACK);
    rectangle.setStrokeWidth(1.0);
    return rectangle;
  }

  /**
   * Default initialization for our intentioned behavior since we use the same color schemes for all
   * used pdf color scheme selections.
   */
  public void defaultInitialization() {
    addColorScheme("Blue Pastel", ColorChoice.COLOR,
        new LinkedHashSet<>(Arrays.asList("#caecbc", "#e0cd9f", "#ecd7a0",
            "#eeae99", "#d7f2e7", "#e2c2f3", "#edf5ca", "#dee7bc", "#9db3d6", "#f5cdaf",
            "#d7d5e9", "#e6ab9e", "#acd2f2", "#d8b194", "#8dd2d8", "#eab2ae", "#e6bfa2",
            "#f2def6", "#ddacb9", "#d0e5d7", "#ebc1e0", "#ccc1e2", "#ccd4aa", "#e6d6ee",
            "#c9d2a8", "#afb9cb", "#f2e6ce", "#bfd1d4", "#fddbd0", "#a7d5be", "#eac4ce",
            "#c3d3bb", "#efd5dc", "#a1d3c9", "#e8c6bb", "#b4dbe7", "#ffdab3", "#cde8e2",
            "#c4acb2", "#d7e7cf", "#d2dcee", "#fdfad5", "#b2ccc6", "#d5cdbb", "#adc7a1")));
    addColorScheme("Yellow Pastel", ColorChoice.COLOR,
        new LinkedHashSet<>(Arrays.asList("#ffe9db", "#f1c887", "#e7cfc6",
            "#e3cc55", "#ffd462", "#e1bcb3", "#eece59", "#ffe29a", "#f2a449", "#efdc60",
            "#f2e3bb", "#fcc358", "#ffde71", "#e8ada2", "#fee77e", "#d5dcbd", "#ffc1b1",
            "#f2dd82", "#ff9c6e", "#ffc7ae", "#d6efc2", "#ffe0c2", "#ffd2bb", "#ffdd7c",
            "#ffc671", "#e7ccab", "#ffbe6c", "#f0e2b3", "#f8c291", "#e7d798", "#dfa78b",
            "#dfcc7f", "#ffb591", "#ffc9ad", "#ffe2ab", "#ffb278", "#e4ecc9", "#cec194",
            "#ffb384", "#cfd39d", "#e1a782", "#d6c689", "#d8ac77", "#ffd8a5", "#ffd190")));
    addColorScheme("Grayscale", ColorChoice.GRAYSCALE,
        new LinkedHashSet<>(Arrays.asList("#000000", "#FFFFFF")));
  }
}
