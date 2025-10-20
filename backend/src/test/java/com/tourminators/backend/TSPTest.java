package com.tourminators.backend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import backend.TSP.TSP;
import backend.TSP.TSP2;
import backend.models.Edge;
import backend.models.Node;
import backend.models.PointOfInterest;
import backend.TSP.Graph;


public class TSPTest {
   @Test
   public void testTSP() {
       TSP tsp = new TSP2();

        Map<Long, Node> nodes = new HashMap<>();
        nodes.put(Long.valueOf(0), new Node(0, 45.751904, 4.857877));
        nodes.put(Long.valueOf(1), new Node(1, 45.752000, 4.860000));
        nodes.put(Long.valueOf(2), new Node(2, 45.753000, 4.861000));

        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge(Long.valueOf(0), Long.valueOf(1), 250.0f, "edge_0_1"));
        edges.add(new Edge(Long.valueOf(1), Long.valueOf(2), 300.0f, "edge_1_2"));
        edges.add(new Edge(Long.valueOf(2), Long.valueOf(0), 350.0f, "edge_2_0"));


        Map<Long, PointOfInterest> tour = new HashMap<>();
        tour.put(Long.valueOf(0), new PointOfInterest(nodes.get(Long.valueOf(0)), PointOfInterest.PoIEnum.WAREHOUSE, null, 0));
        tour.put(Long.valueOf(1), new PointOfInterest(nodes.get(Long.valueOf(1)), PointOfInterest.PoIEnum.DELIVERY, Long.valueOf(2), 200));
        tour.put(Long.valueOf(2), new PointOfInterest(nodes.get(Long.valueOf(2)), PointOfInterest.PoIEnum.PICKUP, Long.valueOf(1), 300));



		Graph g = new Graph(nodes, edges, tour);
		long tempsDebut = System.currentTimeMillis();
		tsp.chercheSolution(60000, g);

        Assert.assertEquals(0L, tsp.getSolution(0));
        Assert.assertEquals(2L, tsp.getSolution(1));
        Assert.assertEquals(1L, tsp.getSolution(2));

    }
}
