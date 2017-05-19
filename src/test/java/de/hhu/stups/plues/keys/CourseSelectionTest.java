package de.hhu.stups.plues.keys;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import de.hhu.stups.plues.data.entities.Course;
import net.sf.ehcache.util.FindBugsSuppressWarnings;
import org.junit.Before;
import org.junit.Test;

public class CourseSelectionTest {

  private Course major;
  private Course minor;

  @Test
  public void getMajor() throws Exception {
    assertEquals(major, csSingle.getMajor());
    assertEquals(major, csCombination.getMajor());
  }

  @Test
  public void getMinor() throws Exception {
    assertEquals(minor, csCombination.getMinor());
  }

  @Test(expected = IllegalArgumentException.class)
  public void getMinorError() throws Exception {
    csStandalone.getMinor();
  }


  private CourseSelection csSingle;
  private CourseSelection csStandalone;
  private CourseSelection csCombination;

  /**
   * Test Setup.
   */
  @Before
  @FindBugsSuppressWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void setup() {
    major = mock(Course.class);
    final Course course2 = mock(Course.class);
    minor = mock(Course.class);
    doReturn(true).when(major).isCombinable();
    doReturn(true).when(major).isMajor();
    doReturn(false).when(major).isMinor();

    doReturn(false).when(course2).isCombinable();

    doReturn(false).when(minor).isMajor();
    doReturn(true).when(minor).isMinor();



    csSingle = new CourseSelection(major);
    csStandalone = new CourseSelection(course2);
    csCombination = new CourseSelection(major, minor);
  }

  @Test
  public void isStandalone() throws Exception {
    assertTrue(csStandalone.isStandalone());
    assertFalse(csSingle.isStandalone());
    assertFalse(csCombination.isStandalone());
  }

  @Test
  public void isSingle() throws Exception {
    assertTrue(csSingle.isSingle());
    assertTrue(csStandalone.isSingle());
    assertFalse(csCombination.isSingle());
  }

  @Test
  public void isCombination() throws Exception {
    assertTrue(csCombination.isCombination());
    assertFalse(csSingle.isCombination());
    assertFalse(csStandalone.isCombination());
  }

  @Test
  public void isCurriculum() throws Exception {
    assertTrue(csCombination.isCurriculum());
    assertTrue(csStandalone.isCurriculum());
    assertFalse(csSingle.isCurriculum());

  }

}
