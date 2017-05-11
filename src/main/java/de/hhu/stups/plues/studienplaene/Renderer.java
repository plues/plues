package de.hhu.stups.plues.studienplaene;

import de.hhu.stups.plues.Helpers;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.ui.controller.PdfRenderingHelper;
import de.hhu.stups.plues.ui.exceptions.RenderingException;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.annotation.Nullable;

public class Renderer {

  private static final EnvironmentConfiguration config
      = EnvironmentConfigurationBuilder.configuration()
      .render()
      .withOutputCharset(Charset.forName("utf8"))
      .and().build();

  private String major;
  private Map<String, String>[] semesters;
  private Map<String, String> colorMap;
  private Map<String, String> fonts;
  private String minor;

  public Renderer(final Store store,
                  final FeasibilityResult feasibilityResult,
                  final Course major,
                  final Course minor,
                  final ColorScheme colorScheme) {
    setup(store, feasibilityResult, major, minor, colorScheme);
  }

  public Renderer(final Store store,
                  final FeasibilityResult feasibilityResult,
                  final Course major,
                  final ColorScheme colorScheme) {
    setup(store, feasibilityResult, major, null, colorScheme);
  }

  private void setup(final Store store,
                     final FeasibilityResult feasibilityResult,
                     final Course major,
                     @Nullable final Course minor,
                     final ColorScheme colorScheme) {
    final DataPreparatory prep = new DataPreparatory(store, feasibilityResult);
    final DataStoreWrapper wrap = new DataStoreWrapper(colorScheme, prep);

    this.major = major.getLongName();
    if (minor != null) {
      this.minor = minor.getLongName();
    } else {
      this.minor = "";
    }
    this.semesters = wrap.getSemesters();
    this.colorMap = wrap.getColorMap();
    this.fonts = wrap.getFonts();
  }

  private ByteArrayOutputStream render() throws RenderingException {
    final URL logo = this.getClass().getResource("/images/HHU_Logo.jpeg");

    final LocalDate date = LocalDate.now();
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    final String formattedDate = date.format(formatter);

    final JtwigModel model = JtwigModel.newModel()
        .with("major", major)
        .with("minor", minor)
        .with("semesters", this.semesters)
        .with("modules", colorMap)
        .with("times", Helpers.timeIntervalMap)
        .with("date", formattedDate)
        .with("logo", logo)
        .with("fonts", fonts);

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final JtwigTemplate template = JtwigTemplate
        .classpathTemplate("/studienplaene/templates/timetableTemplate.twig", config);
    template.render(model, out);

    return PdfRenderingHelper.toPdf(out);
  }

  /**
   * Render the current document and return the result as a ByteArrayOutputStream.
   *
   * @return ByteArrayOutputStream byte stream representing the rendered pdf.
   * @throws RenderingException if an error occurred.
   */
  public final ByteArrayOutputStream getResult() throws RenderingException {
    return this.render();
  }
}
