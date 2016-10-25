package de.hhu.stups.plues.studienplaene;

import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.xmlgraphics.util.MimeConstants;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class Renderer {

  private static final EnvironmentConfiguration config
      = EnvironmentConfigurationBuilder.configuration()
        .render()
        .withOutputCharset(Charset.forName("utf8"))
        .and().build();
  private static final LinkedHashMap<String, String> timeMap = new LinkedHashMap<>(6);

  static {
    timeMap.put("1", "08:30-10:00");
    timeMap.put("2", "10:30-12:00");
    timeMap.put("3", "12:30-14:00");
    timeMap.put("4", "14:30-16:00");
    timeMap.put("5", "16:30-18:00");
    timeMap.put("6", "18:30-20:00");
  }

  private String major;
  private Map<String, String>[] semesters;
  private Map<String, String> colorMap;
  private Map<String, String> fonts;
  private String minor;

  public Renderer(final Store store,
                  final FeasibilityResult feasibilityResult,
                  final Course major,
                  final Course minor,
                  final ColorChoice colorChoice) {
    setup(store, feasibilityResult, major, minor, colorChoice);
  }

  public Renderer(final Store store,
                  final FeasibilityResult feasibilityResult,
                  final Course major,
                  final ColorChoice colorChoice) {
    setup(store, feasibilityResult, major, null, colorChoice);
  }

  public Renderer(final Store store,
                  final FeasibilityResult feasibilityResult,
                  final Course major,
                  final Course minor) {
    setup(store, feasibilityResult, major, minor, ColorChoice.COLOR);
  }

  public Renderer(final Store store,
                  final FeasibilityResult feasibilityResult,
                  final Course major) {
    setup(store, feasibilityResult, major, null, ColorChoice.COLOR);
  }

  private void setup(final Store store,
                     final FeasibilityResult feasibilityResult,
                     final Course major,
                     @Nullable final Course minor,
                     final ColorChoice colorChoice) {
    final DataPreparatory prep = new DataPreparatory(store, feasibilityResult, major, minor);
    final DataStoreWrapper wrap = new DataStoreWrapper(colorChoice, prep);

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

  private ByteArrayOutputStream render()
      throws SAXException, ParserConfigurationException, IOException {
    final URL logo = this.getClass().getResource("/studienplaene/HHU_Logo.jpeg");

    final JtwigModel model = JtwigModel.newModel()
        .with("major", major)
        .with("minor", minor)
        .with("semesters", this.semesters)
        .with("modules", colorMap)
        .with("times", timeMap)
        .with("date", new SimpleDateFormat("dd.MM.yyyy").format(new Date()))
        .with("logo", logo)
        .with("fonts", fonts);

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final JtwigTemplate template = JtwigTemplate
        .classpathTemplate("/studienplaene/templates/XslTemplate.twig", config);
    template.render(model, out);

    return toPdf(out);
  }

  private ByteArrayOutputStream toPdf(final ByteArrayOutputStream out)
      throws ParserConfigurationException, SAXException, IOException {

    final FopFactory fopFactory
        = FopFactory.newInstance(new File(".").toURI());
    final ByteArrayOutputStream pdf = new ByteArrayOutputStream();
    final Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, pdf);
    //
    final SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setNamespaceAware(true);
    final SAXParser saxParser = spf.newSAXParser();

    final XMLReader xmlReader = saxParser.getXMLReader();
    xmlReader.setContentHandler(fop.getDefaultHandler());
    xmlReader.parse(new InputSource(new ByteArrayInputStream(out.toByteArray())));
    //
    return pdf;
  }

  public final ByteArrayOutputStream getResult()
      throws ParserConfigurationException, SAXException, IOException {
    return this.render();
  }

}
