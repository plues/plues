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

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Presents a combo box with selectable {@link ColorScheme color schemes}. Mainly used to determine
 * the color for PDFs to be generated.
 */
public class ColorSchemeSelection extends GridPane implements Initializable {

  private ResourceBundle resources;

  @FXML
  @SuppressWarnings("unused")
  private ComboBox<ColorScheme> cbColorSchemeSelection;
  @FXML
  @SuppressWarnings("unused")
  private ColumnConstraints columnConstraints;

  @Inject
  public ColorSchemeSelection(final Inflater inflater) {
    inflater.inflate("components/ColorSchemeSelection", this, this, "colorSchemeSelection");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;

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
    colorScheme.addColorPreviews(imageBox,
        Math.round(cbColorSchemeSelection.widthProperty().get() / previewRectangleSize) / 2,
        previewRectangleSize);
    cbColorSchemeSelection.widthProperty().addListener((observable, oldValue, newValue) -> {
      imageBox.getChildren().clear();
      colorScheme.addColorPreviews(imageBox,
          Math.round(newValue.doubleValue() / previewRectangleSize) / 2,
          previewRectangleSize);
    });
    return imageBox;
  }

  /**
   * Default initialization for our intentioned behavior since we use the same color schemes for all
   * used pdf color scheme selections.
   */
  public void defaultInitialization() {
    addColorScheme(resources.getString("bluePastel"), ColorChoice.COLOR,
        new LinkedHashSet<>(Arrays.asList("#b2ccc6", "#afcdee", "#e0cd9f",
            "#ecd7a0", "#d2dcee", "#72bef2", "#8fd3ff", "#b4dffc", "#d7f2e7", "#caecbc",
            "#edf5ca", "#dee7bc", "#94c2e9", "#aec5da", "#f5cdaf", "#d7d5e9", "#e6ab9e",
            "#d8b194", "#8dd2d8", "#eab2ae", "#e6bfa2", "#d0e5d7", "#a8cee5", "#ccd4aa",
            "#b9e7f2", "#c9d2a8", "#afb9cb", "#f2e6ce", "#bfd1d4", "#fddbd0", "#a7d5be",
            "#c5ebe3", "#c3d3bb", "#c9d8f5", "#a1d3c9", "#e8c6bb", "#b4dbe7", "#ffdab3",
            "#cde8e2", "#c4acb2", "#d7e7cf", "#eeae99", "#fdfad5", "#d5cdbb", "#adc7a1")));
    addColorScheme(resources.getString("yellowPastel"), ColorChoice.COLOR,
        new LinkedHashSet<>(Arrays.asList("#ffe9db", "#f1c887", "#e7cfc6",
            "#e3cc55", "#f1c650", "#e1bcb3", "#f3d465", "#ffe29a", "#f2a449", "#f1e27e",
            "#f2e3bb", "#fcc358", "#ffde71", "#e8ada2", "#fee77e", "#d5dcbd", "#ffc1b1",
            "#f2dd82", "#ff9c6e", "#ffc7ae", "#d6efc2", "#ffe0c2", "#ffd2bb", "#ffdd7c",
            "#ffc671", "#e7ccab", "#ffbe6c", "#f0e2b3", "#f8c291", "#e7d798", "#dfa78b",
            "#dfcc7f", "#ffb591", "#ffc9ad", "#ffe2ab", "#ffb278", "#e4ecc9", "#cec194",
            "#ffb384", "#cfd39d", "#e1a782", "#d6c689", "#d8ac77", "#ffd8a5", "#ffd190")));
    addColorScheme(resources.getString("grayscale"), ColorChoice.GRAYSCALE,
        new LinkedHashSet<>(Arrays.asList("#000000", "#FFFFFF")));
  }
}
