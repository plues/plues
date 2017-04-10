package de.hhu.stups.plues.studienplaene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;

import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Collectors;

public class TestDataPreperatory extends TestBase {

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws URISyntaxException {
    super.setUp();
  }


  @Test
  public void testGetUnitGroup() {
    final DataPreparatory data = new DataPreparatory(store, result);

    final Map<AbstractUnit, Group> groups = data.getUnitGroup();

    // Test not null and size
    assertNotNull(groups);
    assertEquals(groups.size(), result.getGroupChoice().size());

    final Map<Integer, Integer> ids = groups.entrySet().stream().collect(Collectors.toMap(
        e -> e.getKey().getId(),
        e -> e.getValue().getId()));

    for (final Map.Entry<Integer, Integer> gc : result.getGroupChoice().entrySet()) {
      final Integer key = gc.getKey();
      final Integer value = gc.getValue();
      assertTrue(ids.containsKey(key));
      assertEquals(ids.get(key), value);
    }
  }

  @Test
  public void testUnitModuleMapping() {
    final DataPreparatory dp = new DataPreparatory(store, result);

    final Map<AbstractUnit, Module> um = dp.getUnitModule();
    final Map<Integer, Integer> ids = um.entrySet().stream().collect(Collectors.toMap(
        e -> e.getKey().getId(),
        e -> e.getValue().getId()));
    assertEquals(ids.get(2).intValue(), 1);
    assertEquals(ids.get(3).intValue(), 1);
    assertEquals(ids.get(6).intValue(), 3);
    assertEquals(ids.get(7).intValue(), 3);
  }


}
