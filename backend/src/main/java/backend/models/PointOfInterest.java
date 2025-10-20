package backend.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PointOfInterest {

    private Node node;
    private PoIEnum type; // evolution : enum
    private Long associatedPoI; // id of associated point of interest (null if type = warehouse)
    private int duration; // in seconds, time needed to pickup/deliver, 0 if warehouse

    public enum PoIEnum {
        WAREHOUSE,
        PICKUP,
        DELIVERY;
    }

    @JsonCreator
    public PointOfInterest(
            @JsonProperty("node") Node node,
            @JsonProperty("type") PoIEnum type,
            @JsonProperty("associatedPoI") Long associatedPoI,
            @JsonProperty("duration") int duration) {
        this.node = node;
        this.type = type;
        this.associatedPoI = associatedPoI;
        this.duration = duration;
    }

    public PoIEnum getType() {
        return type;
    }

    public int getDuration() {
        return duration;
    }
    public Long getAssociatedPoI() {
        return associatedPoI;
    }
    public Node getNode() {
        return node;
    }
}
