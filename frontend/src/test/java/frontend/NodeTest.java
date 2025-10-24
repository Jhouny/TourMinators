package frontend;

import org.junit.Test;
import org.junit.Assert;

import frontend.models.Node;

public class NodeTest {
   @Test
   public void testNode() {
        Node node = new Node(1L, Double.valueOf(45.0), Double.valueOf(90.0));
        Assert.assertEquals(1L, node.getId());
        Assert.assertEquals(45.0, node.getLatitude(), 0.0001);
        Assert.assertEquals(90.0, node.getLongitude(), 0.0001);
        Assert.assertEquals("Node{id=1, latitude=45.0, longitude=90.0}", node.toString());
   }
}
