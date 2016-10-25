package de.hhu.stups.plues.studienplaene;

import static org.junit.Assert.assertNotNull;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import javax.xml.parsers.ParserConfigurationException;

public class TestRenderer {

  private MockStore store;
  private Course course;
  private FeasibilityResult result;

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws URISyntaxException {

    store = new MockStore();
    course = store.getCourseByKey("foo");


    final Map<Integer, Integer> groupChoice = new HashMap<>();
    groupChoice.put(1, 1);
    groupChoice.put(2, 12);
    groupChoice.put(3, 3);
    groupChoice.put(4, 4);
    groupChoice.put(5, 11);
    groupChoice.put(6, 6);
    groupChoice.put(7, 7);
    groupChoice.put(8, 10);
    groupChoice.put(9, 9);

    final Map<Integer, Integer> semesterChoice = new HashMap<>();
    IntStream.rangeClosed(1, 9).forEach(i -> semesterChoice.put(i, 1));

    final Map<String, Set<Integer>> moduleChoice = new HashMap<>();
    final Set<Integer> integerSet = new HashSet<Integer>();
    integerSet.add(1);
    integerSet.add(3);
    moduleChoice.put("foo", integerSet);

    this.result = new FeasibilityResult(moduleChoice, semesterChoice, groupChoice);
  }

  @Test
  public void testItWorksForColor() throws IOException, ParserConfigurationException, SAXException {
    final Renderer renderer = new Renderer(store, result, course, ColorChoice.COLOR);
    final ByteArrayOutputStream result = renderer.getResult();

    File pdf = File.createTempFile("color", ".pdf");
    try (FileOutputStream outputStream = new FileOutputStream(pdf.getAbsoluteFile())) {
      result.writeTo(outputStream);
    }
    assertNotNull(result);
  }

  @Test
  public void testItWorksForGrayscale()
      throws IOException, ParserConfigurationException, SAXException {
    final Renderer renderer = new Renderer(store, result, course, ColorChoice.GRAYSCALE);
    final ByteArrayOutputStream result = renderer.getResult();

    File pdf = File.createTempFile("gray", ".pdf");
    try (FileOutputStream stream = new FileOutputStream(pdf.getAbsoluteFile())) {
      result.writeTo(stream);
    }
    assertNotNull(result);
  }
}
