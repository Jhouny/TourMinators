package backend.models;

public class NodeWithCost implements Comparable<NodeWithCost> {
        long id;
        float cost; // f = g + h

        public NodeWithCost(long id, float cost) {
            this.id = id;
            this.cost = cost;
        }

        @Override
        public int compareTo(NodeWithCost other) {
            return Double.compare(this.cost, other.cost);
        }

        public Long getId() {
            return id;
        }
        public Float getCost() {
            return cost;
        }
    
}
