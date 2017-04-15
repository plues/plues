package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Presents a combo box with selectable {@link ColorScheme color schemes}. Mainly used to determine
 * the color for PDFs to be generated.
 */
public class ColorSchemeSelection extends GridPane implements Initializable {

  private final Logger logger = LoggerFactory.getLogger(getClass());

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
  void setPercentWidth(final double percentWidth) {
    columnConstraints.setPercentWidth(percentWidth);
  }

  public void addColorScheme(final String name, final Set<String> colors) {
    cbColorSchemeSelection.getItems().add(new ColorScheme(name, colors));
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
   * A color scheme definition yielding a set of hexadecimal colors and a visualization of the
   * colors.
   */
  private final class ColorScheme {

    private final String name;
    private final Set<String> colors;
    private final Pattern hexColorPattern = Pattern.compile("^#(?:[0-9a-fA-F]{3}){1,2}$");

    ColorScheme(final String name, final Set<String> colors) {
      this.name = name;
      this.colors = colors;
      colors.forEach(color -> {
        if (!hexColorPattern.matcher(color).matches()) {
          logger.error("Wrong color code definition for " + color + " in class ColorScheme.");
          throw new InstantiationError();
        }
      });
    }

    Set<String> getColors() {
      return colors;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
