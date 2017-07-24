package de.hhu.stups.plues.ui.controller;

import static de.hhu.stups.plues.ui.components.OpenFileHandler.tryOpenFile;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.exceptions.RenderingException;

import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.xmlgraphics.util.MimeConstants;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class PdfRenderingHelper {

  private static final String PDF_SAVE_DIR = "LAST_PDF_SAVE_DIR";
  private static final String MSG = "Error! Copying of temporary file into target file failed.";

  private static final Logger logger = LoggerFactory.getLogger(PdfRenderingHelper.class);

  private PdfRenderingHelper() {
  }

  /**
   * Unified function to show a pdf. On error callback will be invoked
   *
   * @param file     Temporary file to show pdf
   * @param callback Consumer (callback) to be invoked if opening the document fails.
   */
  public static void showPdf(final Path file, final Consumer<Exception> callback) {
    SwingUtilities.invokeLater(() -> {
      try {
        Desktop.getDesktop().open(file.toFile());
      } catch (final IOException exc) {
        logger.error(MSG, exc);
        if (callback != null) {
          callback.accept(exc);
        }
      }
    });
  }

  /**
   * Function to show a pdf. Errors will be ignored
   *
   * @param file Temporary file to show pdf
   */
  public static void showPdf(final Path file) {
    showPdf(file, null);
  }

  /**
   * Unified function to save a pdf for a given major and minor course. Error messages will be
   * printed on label if present or on stack trace.
   *
   * @param pdf        Path to pdf to save
   * @param major      Major course for pdf
   * @param minor      Minor course for pdf (could be null)
   * @param lbErrorMsg Label to print error messages on. Can be null
   */
  public static void savePdf(final Path pdf, final Course major, final Course minor,
                             final Label lbErrorMsg) {
    final File file = getTargetFile(major, minor);

    if (file != null) {
      try {
        Files.copy(pdf, Paths.get(file.getAbsolutePath()));
      } catch (final IOException exc) {
        logger.error(MSG, exc);

        if (lbErrorMsg != null) {
          lbErrorMsg.setText(MSG);
        }
      }
    }
  }

  /**
   * Find the target file choosen by the user.
   *
   * @param majorCourse Major course
   * @param minorCourse Minor course
   * @return File object representing the choosen file by user
   */
  private static File getTargetFile(final Course majorCourse, final Course minorCourse) {

    final String documentName;
    documentName = getDocumentName(majorCourse, minorCourse);

    final FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Choose the pdf file's location");
    //
    final Preferences preferences = Preferences.userNodeForPackage(PdfRenderingHelper.class);
    final File initialDirectory = new File(
        preferences.get(PDF_SAVE_DIR, System.getProperty("user.home")));

    if (initialDirectory.isDirectory()) {
      fileChooser.setInitialDirectory(initialDirectory);
    }
    //
    fileChooser.setInitialFileName(documentName);
    fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

    final File file = fileChooser.showSaveDialog(null);
    if (file != null) {
      preferences.put(PDF_SAVE_DIR, file.getAbsoluteFile().getParent());
    }

    return file;
  }

  /**
   * Helper function to find the file name containing major and minor name.
   *
   * @param major Course object representing the chosen major course
   * @param minor Course object representing the chosen minor course
   * @return String representing the file name
   */
  static String getDocumentName(final Course major, final Course minor) {
    if (minor == null) {
      return getDocumentName(major);
    }
    return "musterstudienplan_" + major.getName() + "_" + minor.getName() + ".pdf";
  }

  /**
   * Helper function to find file name containing major name and no minor existing.
   *
   * @param course Course object representing the choosen major course
   * @return String representing the file name
   */
  private static String getDocumentName(final Course course) {
    return "musterstudienplan_" + course.getName() + ".pdf";
  }

  /**
   * Convert OutputStream to pdf using sax.
   *
   * @param out The output stream to be converted.
   * @return Finished pdf
   * @throws RenderingException encapsulating error cause
   */
  public static ByteArrayOutputStream toPdf(final ByteArrayOutputStream out)
      throws RenderingException {

    final ByteArrayOutputStream pdf = new ByteArrayOutputStream();

    final DefaultHandler handler = getFopHandler(pdf);
    final SAXParser saxParser = getSaxParser();
    final InputSource input = new InputSource(new ByteArrayInputStream(out.toByteArray()));

    try {
      final XMLReader xmlReader = saxParser.getXMLReader();
      xmlReader.setContentHandler(handler);
      xmlReader.parse(input);
    } catch (final SAXException | IOException exc) {
      logger.error("Error with SAX parser", exc);
      throw new RenderingException(exc);
    }
    //
    return pdf;
  }

  private static DefaultHandler getFopHandler(final ByteArrayOutputStream pdf)
      throws RenderingException {

    final DefaultHandler handler;
    final FopFactory fopFactory = FopFactory.newInstance(new File(".").toURI());

    try {
      synchronized (FopFactory.class) {
        final Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, pdf);
        handler = fop.getDefaultHandler();
      }
    } catch (final FOPException exc) {
      logger.error("Error creating Fop object", exc);
      throw new RenderingException(exc);
    }

    return handler;
  }

  private static SAXParser getSaxParser() throws RenderingException {
    final SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setNamespaceAware(true);
    final SAXParser saxParser;

    try {
      saxParser = spf.newSAXParser();
    } catch (ParserConfigurationException | SAXException exc) {
      logger.error("Error creating SAX parser", exc);
      throw new RenderingException(exc);
    }

    return saxParser;
  }

  /**
   * Given a {@link JtwigModel}, the path to the template file rooted in src/main/resources/
   * and the file output name.
   */
  public static void writeJtwigTemplateToPdfFile(final JtwigModel model,
                                                 final String templateResourcePath,
                                                 final String fileOutputName) {
    try {
      final EnvironmentConfiguration config = EnvironmentConfigurationBuilder.configuration()
          .render().withOutputCharset(Charset.forName("utf8")).and().build();

      // load template
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final JtwigTemplate template =
          JtwigTemplate.classpathTemplate(templateResourcePath, config);
      template.render(model, out);

      // write to file
      final File file = File.createTempFile(fileOutputName, ".pdf");
      try (final OutputStream stream = new FileOutputStream(file)) {
        final ByteArrayOutputStream pdf = PdfRenderingHelper.toPdf(out);
        pdf.writeTo(stream);
        tryOpenFile(file);
      }
    } catch (final RenderingException | IOException exc) {
      logger.error("Exception while rendering changelogs", exc);
    }
  }
}
