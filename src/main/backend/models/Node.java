package backend.models;

class Node {

    long id;
    double latitude;
    double longitude;

    public Node(long id, double latitude, double longitude) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
