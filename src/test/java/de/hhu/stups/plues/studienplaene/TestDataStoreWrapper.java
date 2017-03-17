package de.hhu.stups.plues.studienplaene;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.Map;


public class TestDataStoreWrapper extends TestBase {

  private Map<String, String>[] semesters;

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws URISyntaxException {
    super.setUp();
    final DataPreparatory data = new DataPreparatory(store, result, course, null);
    final DataStoreWrapper wrap = new DataStoreWrapper(ColorChoice.COLOR, data);
    semesters = wrap.getSemesters();
  }

  @Test
  public void testGetSemesterSizes() {

    // Test sizes
    assertEquals(semesters[0].size(), 9);
    assertEquals(semesters[1].size(), 0);
    assertEquals(semesters[2].size(), 0);
    assertEquals(semesters[3].size(), 0);
    assertEquals(semesters[4].size(), 0);
    assertEquals(semesters[5].size(), 0);
  }

  @Test
  public void testConflicts() {
    assertEquals("Abstract Unit: 6;3", semesters[0].get("mon6"));
    assertEquals("Abstract Unit: 1 (f);1", semesters[0].get("mon1"));
    assertEquals("Abstract Unit: 3 (A);1", semesters[0].get("mon3"));
  }
}
