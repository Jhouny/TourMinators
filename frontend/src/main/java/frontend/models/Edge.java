package frontend.models;

public class Edge {

    long originId;
    long destinationId;
    float length;
    String streetName;

    public Edge(long originId, long destinationId, float length, String streetName) {
        this.originId = originId;
        this.destinationId = destinationId;
        this.length = length;
        this.streetName = streetName;
        
    }

    public long getOriginId() {
        return originId;
    }

    public long getDestinationId() {
        return destinationId;
    }

    public float getLength() {
        return length;
    }

    public String getStreetName() {
        return streetName;
    }

    @Override
    public String toString() {
        return "Edge{" +
                "originId=" + originId +
                ", destinationId=" + destinationId +
                ", length=" + length +
                ", streetName='" + streetName + '\'' +
                '}';
    }
}
