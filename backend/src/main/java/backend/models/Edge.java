package backend.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Edge {

    long originId;
    long destinationId;
    float length;
    String streetName;

    @JsonCreator
    public Edge(
            @JsonProperty("originId") long originId,
            @JsonProperty("destinationId") long destinationId,
            @JsonProperty("length") float length,
            @JsonProperty("streetName") String streetName) {
        this.originId = originId;
        this.destinationId = destinationId;
        this.length = length;
        this.streetName = streetName;
        
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

    public long getOrigin() {
        return originId;
    }
    public long getDestination() {
        return destinationId;
    }
    public float getLength() {
        return length;
    }
    
}
