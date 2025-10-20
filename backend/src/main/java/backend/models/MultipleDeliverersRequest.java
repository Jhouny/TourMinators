package backend.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.List;

public class MultipleDeliverersRequest {
    private Map<Long, Node> allNodes;
    private List<Edge> allEdges;
    private Map<String, Map<Long, PointOfInterest>> delivererAssignments;

    @JsonCreator
    public MultipleDeliverersRequest(
            @JsonProperty("allNodes") Map<Long, Node> allNodes,
            @JsonProperty("allEdges") List<Edge> allEdges,
            @JsonProperty("delivererAssignments") Map<String, Map<Long, PointOfInterest>> delivererAssignments) {
        this.allNodes = allNodes;
        this.allEdges = allEdges;
        this.delivererAssignments = delivererAssignments;
    }

    public Map<Long, Node> getAllNodes() {
        return allNodes;
    }

    public List<Edge> getAllEdges() {
        return allEdges;
    }

    public Map<String, Map<Long, PointOfInterest>> getDelivererAssignments() {
        return delivererAssignments;
    }
}