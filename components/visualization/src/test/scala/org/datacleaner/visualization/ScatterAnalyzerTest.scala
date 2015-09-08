package org.datacleaner.visualization

import org.scalatest.junit.AssertionsForJUnit
import org.junit.Test
import org.junit.Assert
import org.junit.Test
import org.datacleaner.data.MockInputColumn
import org.datacleaner.data.MockInputRow
import scala.collection.SortedMap
import scala.collection.immutable.TreeMap
import org.datacleaner.storage.RowAnnotations

class ScatterAnalyzerTest extends AssertionsForJUnit {

  @Test
  def testMapDefaultValue = {
    val analyzer = new ScatterAnalyzer()
    analyzer.rowAnnotationFactory = RowAnnotations.getInMemoryFactory()
    analyzer.variable1 = new MockInputColumn("foo");
    analyzer.variable2 = new MockInputColumn("bar");

    analyzer.run(new MockInputRow().put(analyzer.variable1, 1).put(analyzer.variable2, 1), 1);
    analyzer.run(new MockInputRow().put(analyzer.variable1, 1).put(analyzer.variable2, 2), 1);
    analyzer.run(new MockInputRow().put(analyzer.variable1, 1).put(analyzer.variable2, 1), 1);
    
    val group = analyzer.getResult.groups.head
    val str = group.annotations.map(entry => (entry._1, entry._2.getRowCount())).mkString(", ");
    Assert.assertEquals("(1,1) -> 2, (1,2) -> 1", str);
  }
}
