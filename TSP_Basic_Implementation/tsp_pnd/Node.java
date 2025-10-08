package tsp_pnd;

public class Node {

    private long id;
    private double latitude;
    private double longitude;

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
    
    public long getId() {
        return id;
    }
}
