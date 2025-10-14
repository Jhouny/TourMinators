package frontend;

import org.junit.Test;
import org.junit.Assert;

import frontend.models.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for the XMLUploadController.
 */
public class XMLUploadControllerTest {

    @Test
    public void testGraphStorage() {
        GraphStorage storage = new GraphStorage();
        Map<Long, Node> nodes = new HashMap<>();
        nodes.put(1L, new Node(1L, 45.0, 3.0));
        nodes.put(2L, new Node(2L, 46.0, 4.0));
        storage.setGraphNodes(nodes);

        Map<Long, Node> retrievedNodes = storage.getGraphNodes();
        Assert.assertEquals(2, retrievedNodes.size());  // Check size
        Assert.assertTrue(retrievedNodes.containsKey(1L));  // Check if node 1 is present
        Assert.assertTrue(retrievedNodes.containsKey(2L));  // Check if node 2 is present
        Assert.assertEquals(45.0, retrievedNodes.get(1L).getLatitude(), 0.0001);  // Check node 1 latitude
        Assert.assertEquals(4.0, retrievedNodes.get(2L).getLongitude(), 0.0001);
    }
}