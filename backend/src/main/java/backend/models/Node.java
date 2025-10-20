package backend.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Node {

    private long id;
    private double latitude;
    private double longitude;
    private String type;

    @JsonCreator
    public Node(
            @JsonProperty("id") long id,
            @JsonProperty("latitude") double latitude,
            @JsonProperty("longitude") double longitude) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = null;
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
    
    public long getId() {
        return id;
    }

    public String getType() {
        return type; 
    }

    public void setType(String type) {
        this.type = type;
    }
}
