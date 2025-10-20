package backend.models;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TSPRequest {

    private Map<Long, Node> allNodes;
    private List<Edge> allEdges;
    private List<PointOfInterest> tour;


    @JsonCreator
    public TSPRequest(
        @JsonProperty("allNodes") Map<Long, Node> allNodes, 
        @JsonProperty("allEdges") List<Edge> allEdges, 
        @JsonProperty("tour") List<PointOfInterest> tour) {
        this.allNodes = allNodes;
        this.allEdges = allEdges;
        this.tour = tour;
    }

    @Override
    public String toString() {
        return "TSPRequest{" +
                "allNodes=" + allNodes +
                ", allEdges=" + allEdges +
                ", tour=" + tour +
                '}';
    }

    public Map<Long, Node> getAllNodes() {
        return allNodes;
    }

    public List<Edge> getAllEdges() {
        return allEdges;
    }

    public List<PointOfInterest> getTour() {
        return tour;
    }
}
