package frontend;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;

import frontend.models.Node;
import frontend.models.Triple;
import frontend.models.PointOfInterest;

public class DeliveryRequestParserTest {

    private String filename;
    Map<Long, Node> nodes = new HashMap<>();
    Map<Long, Node> expectedNodes = new HashMap<>();

    Map<Long, Triple<Node, Long, Integer>> expectedDeliveries = new HashMap<>();

    @Before
    public void setUp() {
        filename = "src/test/resources/demandePetit1.xml";

        Node n1 = new Node(342873658, 0, 0);
        Node n2 = new Node(208769039, 2, 2);
        Node n3 = new Node(25173820, 0, 1);

        Node n1exp = new Node(342873658, 0, 0);
        Node n2exp = new Node(208769039, 2, 2);
        Node n3exp = new Node(25173820, 0, 1);

        n1exp.setType("warehouse");
        n2exp.setType("pickup");
        n3exp.setType("delivery");

        nodes.put(342873658L, n1);
        nodes.put(208769039L, n2);
        nodes.put(25173820L, n3);

        expectedNodes.put(342873658L, n1exp);
        expectedNodes.put(208769039L, n2exp);
        expectedNodes.put(25173820L, n3exp);

        expectedDeliveries.put(342873658L, new Triple<>(expectedNodes.get(342873658L), -1L, 0)); // Warehouse
        expectedDeliveries.put(208769039L, new Triple<>(expectedNodes.get(208769039L), 1L, 180)); // Pickup
        expectedDeliveries.put(25173820L, new Triple<>(expectedNodes.get(25173820L), 1L, 240)); // Delivery
    }

    @Test
    public void testParseDeliveries_InvalidFile() {
        filename = "src/test/resources/invalid.xml";

        Assert.assertThrows(
                Exception.class,
                () -> {
                    DeliveryRequestParser.parseDeliveries(filename, nodes);
                });

    }

    // ############### Failing #####################

    // @Test
    // public void testParseDeliveries() throws Exception {
    // Map<Long, Triple<Node, Long, Integer>> deliveries =
    // DeliveryRequestParser.parseDeliveries(filename, nodes);

    // Assert.assertEquals(expectedDeliveries, deliveries);
    // }

    // @Test
    // public void mapDeliveries() throws Exception {
    // Map<Long, PointOfInterest> poiMap =
    // DeliveryRequestParser.mapDeliveries(expectedDeliveries);

    // Map<Long, PointOfInterest> expectedPoiMap = new HashMap<>();
    // expectedPoiMap.put(342873658L,
    // new PointOfInterest(expectedNodes.get(342873658L),
    // PointOfInterest.PoIEnum.WAREHOUSE, null, 0));
    // expectedPoiMap.put(208769039L,
    // new PointOfInterest(expectedNodes.get(208769039L),
    // PointOfInterest.PoIEnum.PICKUP, 25173820L, 180));
    // expectedPoiMap.put(25173820L,
    // new PointOfInterest(expectedNodes.get(25173820L),
    // PointOfInterest.PoIEnum.DELIVERY, 208769039L, 240));

    // Assert.assertEquals(expectedPoiMap, poiMap);
    // }
}
