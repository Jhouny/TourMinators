package frontend;

import org.junit.Assert;
import org.junit.Test;

import frontend.models.Edge;

public class EdgeTest {
   @Test
   public void testEdge() {
       Edge edge = new Edge(1L, 2L, 5.f, "Main Street");
       Assert.assertEquals(1L, edge.getOriginId());
       Assert.assertEquals(2L, edge.getDestinationId());
       Assert.assertEquals(5.f, edge.getLength(), 0.0001);
       Assert.assertEquals("Edge{originId=1, destinationId=2, length=5.0, streetName='Main Street'}", edge.toString());
   }
}
