package frontend;
import java.util.*;
import frontend.models.Pair;
import java.time.LocalTime;

public class MoyenPlanTest {

    public static void main(String[] args) {
        // Simulated bestSolution: List of Pair<Long, LocalTime>
        List<Pair<Long, LocalTime>> bestSolution = new ArrayList<>();
        bestSolution.add(new Pair<>(4150019167L, LocalTime.of(8, 0, 0))); // Entrepôt
        bestSolution.add(new Pair<>(21992645L, LocalTime.of(8, 10, 0))); // Pickup 1
        bestSolution.add(new Pair<>(55444215L, LocalTime.of(8, 20, 0))); // Delivery 1
        bestSolution.add(new Pair<>(26155372L, LocalTime.of(8, 30, 0))); // Pickup 2
        bestSolution.add(new Pair<>(1036842078L, LocalTime.of(8, 40, 0))); // Delivery 2
        bestSolution.add(new Pair<>(25610684L, LocalTime.of(8, 50, 0))); // Pickup 3
        bestSolution.add(new Pair<>(21717915L, LocalTime.of(9, 0, 0))); // Delivery 3
        bestSolution.add(new Pair<>(1400900990L, LocalTime.of(9, 10, 0))); // Pickup 4
        bestSolution.add(new Pair<>(208769083L, LocalTime.of(9, 20, 0))); // Delivery 4
        bestSolution.add(new Pair<>(26317393L, LocalTime.of(9, 30, 0))); // Pickup 5
        bestSolution.add(new Pair<>(60755991L, LocalTime.of(9, 40, 0))); // Delivery 

        // Simulated tour: Map of Pair<pickupID, deliveryID> to Map<arrival, predecessor>
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

        // Path for Pickup 3 -> Delivery 3
        Pair<Long, Long> pickupDeliveryPair3 = new Pair<>(25610684L, 21717915L);
        Map<Long, Long> path3 = new HashMap<>();
        path3.put(21717915L, 25610684L);
        path3.put(25610684L, 1036842078L);

        // Path for Pickup 4 -> Delivery 4
        Pair<Long, Long> pickupDeliveryPair4 = new Pair<>(1400900990L, 208769083L);
        Map<Long, Long> path4 = new HashMap<>();
        path4.put(208769083L, 1400900990L);
        path4.put(1400900990L, 21717915L);

        // Path for Pickup 5 -> Delivery 5
        Pair<Long, Long> pickupDeliveryPair5 = new Pair<>(26317393L, 60755991L);
        Map<Long, Long> path5 = new HashMap<>();
        path5.put(60755991L, 26317393L);
        path5.put(26317393L, 208769083L);

        // Add paths to the tour
        tour.put(pickupDeliveryPair1, path1);
        tour.put(pickupDeliveryPair2, path2);
        tour.put(pickupDeliveryPair3, path3);
        tour.put(pickupDeliveryPair4, path4);
        tour.put(pickupDeliveryPair5, path5);

        // Generate JSON manually
        StringBuilder json = new StringBuilder();
        json.append("{");

        // Add bestSolution
        json.append("\"bestSolution\": [");
        for (int i = 0; i < bestSolution.size(); i++) {
            Pair<Long, LocalTime> pair = bestSolution.get(i);
            json.append("{\"id\": ").append(pair.getLeft())
                .append(", \"time\": \"").append(pair.getRight()).append("\"}");
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