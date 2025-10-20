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

    @SuppressWarnings("unlikely-arg-type")
    public void solve() {
        // Retrieve all PoIs from the graph
        ArrayList<Map<Long, Long>> allPaths = new ArrayList<>();
        for( Long poiId : PoISet) {  // Calculates all paths between all PoIs (must be before permutations to update cost table)
            for( Long nextPoiId : PoISet) {
                if (poiId.equals(nextPoiId)) {
                    continue;
                }
                Map<Long, Long> path = g.AWAStar(poiId, nextPoiId);
                //System.out.println("Computed path from " + poiId + " to " + nextPoiId + ": " + path);
                allPaths.add(path);
            }
        }

        // After computing all paths, iterate over all path orders to find the optimal (and valid) one
        ArrayList<Long> currentOrder = new ArrayList<>();
        ArrayList<Long> bestOrder = null;
        float bestCost = Float.MAX_VALUE;

        ArrayList<Long> PoIelements = new ArrayList<>(PoISet);
        PoIelements.remove(g.getBeginId()); // Remove warehouse from elements to permute

        ArrayList<ArrayList<Long>> permutations = backtrackPermutations(currentOrder, PoIelements);
        // Add Warehouse at the start and end of each permutation to complete the tour
        for (ArrayList<Long> perm : permutations) {
            if (!perm.get(0).equals(g.getBeginId())) {
                perm.add(0, g.getBeginId());
            }
            if (!perm.get(perm.size() - 1).equals(g.getBeginId())) {
                perm.add(g.getBeginId());
            }
        }

        ArrayList<Integer> toRemove = new ArrayList<>();
        for (ArrayList<Long> perm : permutations) {
            if (!isValidSolution(perm)) {
                //System.out.println("Skipping invalid order: " + perm);
                toRemove.add(permutations.indexOf(perm));
            }
        }
        permutations.removeAll(toRemove);

        for (ArrayList<Long> order : permutations) {
            // Log order
            //System.out.println("Valid order found: " + order);

            // Calculate total cost for this order
            float totalCost = 0;
            for (int i = 0; i < order.size() - 1; i++) {
                if (g.pathCost.get(new Pair<Long, Long>(order.get(i), order.get(i + 1))) == null) {
                    totalCost = Float.MAX_VALUE; // No path exists
                    break;
                }

                totalCost += g.pathCost.get(new Pair<Long, Long>(order.get(i), order.get(i + 1)));
            }
            if (totalCost < bestCost) {
                bestCost = totalCost;
                bestOrder = order;
            }

            // Log cost
            //System.out.println("Total cost for this order: " + totalCost);
        }

        if (bestOrder == null) {
            //System.out.println("No valid solution found.");
            return;
        }

        this.solutionOrder = bestOrder;
        // Reconstruct solution paths based on best order
        for (int i = 0; i < bestOrder.size() - 1; i++) {
            Map<Long, Long> path = g.AWAStar(bestOrder.get(i), bestOrder.get(i + 1));

            // Filter out null and non-optimal edges from the path
            Map<Long, Long> filteredPath = new java.util.LinkedHashMap<>();
            Long current = bestOrder.get(i);
            
            while (current != null && !current.equals(bestOrder.get(i + 1))) {
                Long next = path.get(current);
                if (next == null) {
                    break; // No further path
                }
                filteredPath.put(current, next);
                current = next;
            }

            this.solutionPaths.add(filteredPath);
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
            //System.out.println("Invalid solution: Warehouse not first");
            return false;  // Warehouse not first
        }
        Set<Long> pickedUp = new HashSet<>();
        for (Long poiId : order) {
            //PointOfInterest poi = g.tour.get(poiId);
            PointOfInterest poi = null;
            for (PointOfInterest p : g.tour) {
                if (p.getNode().getId() == poiId) {
                    poi = p;
                    break;
                }
            }

            if (poi == null) {
                //System.out.println("Invalid solution: PoI " + poiId + " not found in tour");
                return false;  // PoI not found in tour
            }

            // Verify that the path exists in the graph
            if (order.indexOf(poiId) > 0 && g.pathCost.get(new Pair<Long, Long>(order.get(order.indexOf(poiId)-1), poiId)) == null) {
                //System.out.println("Invalid solution: No path between " + order.get(order.indexOf(poiId)-1) + " and " + poiId);
                return false;  // No path between previous and current PoI
            }

            if (poi.getType() == PointOfInterest.PoIEnum.PICKUP) {
                pickedUp.add(poiId);
            } else if (poi.getType() == PointOfInterest.PoIEnum.DELIVERY) {
                Long pickupId = poi.getAssociatedPoI();
                if (!pickedUp.contains(pickupId)) {
                    //System.out.println("Invalid solution: Delivery " + poiId + " before its pickup " + pickupId);
                    return false;  // Delivery before pickup
                }
            }
        }

        if (order.size()-1 != PoISet.size()) { // Exclude duplicate warehouse at end
            //System.out.println("Invalid solution: Not all PoIs visited");
            return false;  // Not all PoIs were visited
        }

        if (!order.get(order.size() - 1).equals(g.getBeginId())) {
            //System.out.println("Invalid solution: Did not return to warehouse at end");
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

    private ArrayList<ArrayList<Long>> backtrackPermutations(ArrayList<Long> current, ArrayList<Long> nonUsed) {
        // This method will use two lists to generate permutations via backtracking, one for remaining items and one for the current permutation
        ArrayList<ArrayList<Long>> permutations = new ArrayList<>();
        if (nonUsed.isEmpty()) {
            permutations.add(new ArrayList<>(current));
            return permutations;
        }

        for (int i = 0; i < nonUsed.size(); i++) {
            Long poiId = nonUsed.get(i);
            PointOfInterest poi = null;
            for (PointOfInterest p : g.tour) {
                if (p.getNode().getId() == poiId) {
                    poi = p;
                    break;
                }
            }
            if (poi == null) {
                continue; // Skip if PoI not found
            }
            
            if (poi.getType() == PointOfInterest.PoIEnum.DELIVERY) {
                Long pickupId = poi.getAssociatedPoI();
                if (!current.contains(pickupId)) {
                    continue; // Skip this delivery as its pickup hasn't been included yet
                }
            }
            current.add(poiId);
            ArrayList<Long> newNonUsed = new ArrayList<>(nonUsed);
            newNonUsed.remove(i);
            permutations.addAll(backtrackPermutations(current, newNonUsed));
            current.remove(current.size() - 1); // backtrack
        }
        
        return permutations;
    }
}