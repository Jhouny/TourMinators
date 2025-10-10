package backend.models;

public class Node {

    long id;
    double latitude;
    double longitude;

    public Node(long id, double latitude, double longitude) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public double getLat() {
        return latitude;
    }

    public double getLong() {
        return longitude;
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    public double getLat() {
        return latitude;
    }

    public double getLong() {
        return longitude;
    }
}
