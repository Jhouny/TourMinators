package backend.models;

public class Node {

    private long id;
    private double latitude;
    private double longitude;
    private String type;

    public Node(long id, double latitude, double longitude) {
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
