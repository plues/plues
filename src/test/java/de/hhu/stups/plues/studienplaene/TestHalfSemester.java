package de.hhu.stups.plues.studienplaene;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;

public class TestHalfSemester extends TestBase {

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws URISyntaxException {
    super.halfSemester();
  }

  @Test
  public void testRhythm() {
    assertEquals(semesters[0].get("mon3"), "Abstract Unit: 3 (A);1");
    assertEquals(semesters[0].get("mon4"), "Abstract Unit: 4 (B);1");
    assertEquals(semesters[0].get("mon5"), "Abstract Unit: 5 (b);3");
  }

  @Test
  public void testHalfSemester() {
    assertEquals(semesters[0].get("mon2"), "Abstract Unit: 2 (s);1");
    assertEquals(semesters[0].get("mon1"), "Abstract Unit: 1 (f);1");
  }

}
