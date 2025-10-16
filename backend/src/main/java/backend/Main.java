package backend;

import backend.models.*;
import backend.TSP.*;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) throws Exception {
        String xmlPath = "../frontend/src/main/resources/Plan/petitPlan.xml";
        String deliveriesPath = "../frontend/src/main/resources/Demande/demandePetit1.xml";
        String xmlPath = "../frontend/src/main/resources/Plan/petitPlan.xml";
        String deliveriesPath = "../frontend/src/main/resources/Demande/demandePetit1.xml";
        Map<Long, Node> nodes = XMLParser.parseNodes(xmlPath);
        List<Edge> edges = XMLParser.parseEdges(xmlPath);
        System.out.println("Nodes: " + nodes.size());
        System.out.println("Edges: " + edges.size());

        if (!nodes.isEmpty()) {
            // Map n'a pas d'ordre défini, on prend simplement le premier via iterator()
            Node firstNode = nodes.values().iterator().next();
            System.out.println("\n--- Premier Node ---");
            System.out.println(firstNode);
        } else {
            System.out.println("Aucun Node trouvé !");
        }

        if (!edges.isEmpty()) {
            Edge firstEdge = edges.get(0);
            System.out.println("\n--- Premier Edge ---");
            System.out.println(firstEdge);
        } else {
            System.out.println("Aucun Edge trouvé !");
        }

        System.out.println("\n--- Demande de delivery---");

        Map<Long, Triple<Node, Long, Integer>> sommets =
                DeliveryRequestParser.parseDeliveries(deliveriesPath, nodes);

        for (Map.Entry<Long, Triple<Node, Long, Integer>> entry : sommets.entrySet()) {
            System.out.println("NodeId=" + entry.getKey() + " -> " + entry.getValue());
        }

        HashMap tour = new HashMap<Long, PointOfInterest>();

        //Graph g = new Graph(sommets, edges, tour);


    }
}
