package de.hhu.stups.studienplaene;

import static org.junit.Assert.assertNotNull;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
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
  private FeasibilityResult feasibilityResult;
  private Course course;

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws URISyntaxException {

    store = new MockStore();
    course = store.getCourseByKey("foo");

    HashMap<Integer, Integer> groupChoice;
    HashMap<Integer, Integer> semesterChoice;
    HashMap<Integer, Integer> unitChoice;
    Map<String, Set<Integer>> moduleChoice;

    groupChoice = new HashMap<>();
    groupChoice.put(1, 1);
    groupChoice.put(2, 12);
    groupChoice.put(3, 3);
    groupChoice.put(4, 4);
    groupChoice.put(5, 11);
    groupChoice.put(6, 6);
    groupChoice.put(7, 7);
    groupChoice.put(8, 10);
    groupChoice.put(9, 9);

    semesterChoice = new HashMap<>();
    IntStream.rangeClosed(1, 9).forEach(i -> semesterChoice.put(i, 1));

    unitChoice = new HashMap<>();
    IntStream.rangeClosed(1, 9).forEach(i -> unitChoice.put(i, i));

    moduleChoice = new HashMap<>();
    final Set<Integer> integerSet = new HashSet<Integer>();
    integerSet.add(1);
    integerSet.add(3);
    moduleChoice.put("foo", integerSet);

    feasibilityResult =
      new FeasibilityResult(moduleChoice, unitChoice, semesterChoice, groupChoice);
  }

  @Test
  public void testItWorksForColor() throws IOException, ParserConfigurationException, SAXException {
    final Renderer pdf = new Renderer(store, feasibilityResult, course, "true");
    final ByteArrayOutputStream result = pdf.getResult();
    try (FileOutputStream outputStream = new FileOutputStream("/tmp/foo.pdf")) {
      result.writeTo(outputStream);
    }
    assertNotNull(result);
  }

  @Test
  public void testItWorksForGrayscale()
      throws IOException, ParserConfigurationException, SAXException {
    final Renderer pdf = new Renderer(store, feasibilityResult, course, "false");
    final ByteArrayOutputStream result = pdf.getResult();
    try (FileOutputStream stream = new FileOutputStream("/tmp/gray.pdf")) { //TODO
      result.writeTo(stream);
    }
    assertNotNull(result);
  }
}
