package backend.TSP;

import java.lang.reflect.Array;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import backend.models.Pair;
import backend.models.PointOfInterest;

public class BruteForceTSP {
    private Graph g;
    private LocalTime startTime;
    private Set<Long> PoISet;  // Set of all Point of Interest IDs
    private ArrayList<Map<Long, Long>> solutionPaths;
    private ArrayList<Long> solutionOrder;

    public BruteForceTSP(Graph g) {
        this.g = g;
        this.startTime = LocalTime.of(8, 0); // Default start time 8:00 AM
        this.PoISet = new HashSet<>();
        this.solutionPaths = new ArrayList<>();
        this.solutionOrder = new ArrayList<>();
        PoISet = retrievePoIs();
    }

    public void solve() {
        // Retrieve all PoIs from the graph
        ArrayList<Map<Long, Long>> allPaths = new ArrayList<>();
        for( Long poiId : PoISet) {  // Calculates all paths between all PoIs (must be before permutations to update cost table)
            for( Long nextPoiId : PoISet) {
                if (poiId.equals(nextPoiId)) {
                    continue;
                }
                Map<Long, Long> path = g.AWAStar(poiId, nextPoiId);
                allPaths.add(path);
            }
        }

        // After computing all paths, iterate over all path orders to find the optimal (and valid) one
        ArrayList<Long> currentOrder = new ArrayList<>(PoISet);
        ArrayList<Long> bestOrder = null;
        float bestCost = Float.MAX_VALUE;
        ArrayList<ArrayList<Long>> permutations = generatePermutations(currentOrder); 
        // Add Warehouse at the end of each permutation to complete the tour
        for (ArrayList<Long> perm : permutations) {
            perm.add(perm.get(0)); // Return to warehouse
        }

        for (ArrayList<Long> order : permutations) {
            if (!isValidSolution(order)) {
                continue;  // Skip invalid solutions
            }

            // Calculate total cost for this order
            float totalCost = 0;
            for (int i = 0; i < order.size() - 1; i++) {
                totalCost += g.pathCost.get(new Pair<Long, Long>(order.get(i), order.get(i + 1)));
            }
            if (totalCost < bestCost) {
                bestCost = totalCost;
                bestOrder = order;
            }
        }

        if (bestOrder == null) {
            System.out.println("No valid solution found.");
            return;
        }

        this.solutionOrder = bestOrder;
        // Reconstruct solution paths based on best order
        for (int i = 0; i < bestOrder.size() - 1; i++) {
            Map<Long, Long> path = g.AWAStar(bestOrder.get(i), bestOrder.get(i + 1));
            this.solutionPaths.add(path);
        }
    }

    public ArrayList<Map<Long, Long>> getSolutionPaths() {
        return this.solutionPaths;
    }

    public ArrayList<Long> getSolutionOrder() {
        return this.solutionOrder;
    }

    public Set<Long> retrievePoIs() {
        // Retrieve all PoI IDs from the graph (warehouse, pickups and deliveries)
        Set<Long> poi = new HashSet<>();
        poi.add(g.getBeginId());  // Add warehouse ID
        poi.addAll(g.getPickupNodes());  // Add all pickup IDs
        poi.addAll(g.getDeliveryNodes());  // Add all delivery IDs
        return poi;
    }

    public boolean isValidSolution(ArrayList<Long> order) {
        // Check if the given order respects pickup-delivery constraints
        // 1. Warehouse must be first
        // 2. Each delivery must come after its corresponding pickup
        // 3. All PoIs must be visited
        // 4. Must return to warehouse at the end

        if (!order.get(0).equals(g.getBeginId())) {
            return false;  // Warehouse not first
        }
        Set<Long> pickedUp = new HashSet<>();
        for (Long poiId : order) {
            PointOfInterest poi = g.tour.get(poiId);

            // Verify that the path exists in the graph
            if (order.indexOf(poiId) > 0 && g.all_costs.get(new Pair<Long, Long>(order.get(order.indexOf(poiId)-1), poiId)) == null) {
                return false;  // No path between previous and current PoI
            }

            if (poi.getType() == PointOfInterest.PoIEnum.pickup) {
                pickedUp.add(poiId);
            } else if (poi.getType() == PointOfInterest.PoIEnum.delivery) {
                Long pickupId = poi.getAssociatedPoI();
                if (!pickedUp.contains(pickupId)) {
                    return false;  // Delivery before pickup
                }
            }
        }

        if (order.size()-1 != PoISet.size()) { // Exclude duplicate warehouse at end
            return false;  // Not all PoIs were visited
        }

        if (!order.get(order.size() - 1).equals(g.getBeginId())) {
            return false;  // Must return to warehouse at end
        }

        return true;
    }

    public ArrayList<ArrayList<Long>> generatePermutations(ArrayList<Long> items) {
        // Generate all permutations of the given list of items
        ArrayList<ArrayList<Long>> permutations = new ArrayList<>();
        permute(items, 1, permutations);  // Start from index 1 to keep warehouse fixed at start
        return permutations;
    }

    public void permute(ArrayList<Long> items, int start, ArrayList<ArrayList<Long>> result) {
        if (start >= items.size() - 1) {
            result.add(new ArrayList<>(items));
            return;
        }
        for (int i = start; i < items.size(); i++) {
            swap(items, start, i);
            permute(items, start + 1, result);
            swap(items, start, i);  // backtrack
        }
    }

    private void swap(ArrayList<Long> items, int i, int j) {
        Long temp = items.get(i);
        items.set(i, items.get(j));
        items.set(j, temp);
    }
}