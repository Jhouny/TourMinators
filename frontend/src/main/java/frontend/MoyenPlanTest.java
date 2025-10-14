package frontend;
import java.util.*;
import frontend.models.Pair;

public class MoyenPlanTest {

    public static void main(String[] args) {
        // Simulated bestSolution
        List<Long> bestSolution = Arrays.asList(
            4150019167L, 21992645L, 55444215L, 26155372L, 1036842078L,
            25610684L, 21717915L, 1400900990L, 208769083L, 26317393L, 60755991L
        );

        // Simulated tour
        Map<Pair<Long, Long>, Map<Long, Long>> tour = new HashMap<>();

        // Path for Pickup 1 -> Delivery 1
        Pair<Long, Long> pickupDeliveryPair1 = new Pair<>(21992645L, 55444215L);
        Map<Long, Long> path1 = new HashMap<>();
        path1.put(55444215L, 21992645L);
        path1.put(21992645L, 4150019167L);
        path1.put(4150019167L, null);

        // Path for Pickup 2 -> Delivery 2
        Pair<Long, Long> pickupDeliveryPair2 = new Pair<>(26155372L, 1036842078L);
        Map<Long, Long> path2 = new HashMap<>();
        path2.put(1036842078L, 26155372L);
        path2.put(26155372L, 55444215L);

        // Add paths to the tour
        tour.put(pickupDeliveryPair1, path1);
        tour.put(pickupDeliveryPair2, path2);

        // Generate JSON manually
        StringBuilder json = new StringBuilder();
        json.append("{");

        // Add bestSolution
        json.append("\"bestSolution\": [");
        for (int i = 0; i < bestSolution.size(); i++) {
            json.append(bestSolution.get(i));
            if (i < bestSolution.size() - 1) {
                json.append(", ");
            }
        }
        json.append("],");

        // Add tour
        json.append("\"tour\": {");
        int pairCount = 0;
        for (Map.Entry<Pair<Long, Long>, Map<Long, Long>> entry : tour.entrySet()) {
            Pair<Long, Long> pair = entry.getKey();
            Map<Long, Long> path = entry.getValue();

            json.append("\"(").append(pair.getLeft()).append(",").append(pair.getRight()).append(")\": {");
            int pathCount = 0;
            for (Map.Entry<Long, Long> pathEntry : path.entrySet()) {
                json.append("\"").append(pathEntry.getKey()).append("\": ");
                if (pathEntry.getValue() == null) {
                    json.append("null");
                } else {
                    json.append("\"").append(pathEntry.getValue()).append("\"");
                }
                if (pathCount < path.size() - 1) {
                    json.append(", ");
                }
                pathCount++;
            }
            json.append("}");
            if (pairCount < tour.size() - 1) {
                json.append(", ");
            }
            pairCount++;
        }
        json.append("}");

        json.append("}");

        // Print the JSON
        System.out.println(json.toString());
    }
}