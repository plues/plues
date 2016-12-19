package de.hhu.stups.plues.keys;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import de.hhu.stups.plues.data.entities.Course;
import net.sf.ehcache.util.FindBugsSuppressWarnings;
import org.junit.Before;
import org.junit.Test;

public class CourseSelectionTest {

  private CourseSelection csSingle;
  private CourseSelection csStandalone;
  private CourseSelection csCombination;

  /**
   * Test Setup.
   */
  @Before
  @FindBugsSuppressWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void setup() {
    final Course course1 = mock(Course.class);
    final Course course2 = mock(Course.class);
    final Course course3 = mock(Course.class);
    doReturn(true).when(course1).isCombinable();
    doReturn(true).when(course1).isMajor();
    doReturn(false).when(course1).isMinor();

    doReturn(false).when(course2).isCombinable();

    doReturn(false).when(course3).isMajor();
    doReturn(true).when(course3).isMinor();



    csSingle = new CourseSelection(course1);
    csStandalone = new CourseSelection(course2);
    csCombination = new CourseSelection(course1, course3);
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
