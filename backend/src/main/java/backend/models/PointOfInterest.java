package backend.models;

public class PointOfInterest {

    private Node node;
    private String type; // evolution : enum
    private Long associatedPoI; // id of associated point of interest (null if type = warehouse)
    private int duration; // in seconds, time needed to pickup/deliver, 0 if warehouse

    public PointOfInterest(Node node, String type, Long associatedPoI, int duration) {
        this.node = node;
        this.type = type;
        this.associatedPoI = associatedPoI;
        this.duration = duration;
    }

    public String getType() {
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
