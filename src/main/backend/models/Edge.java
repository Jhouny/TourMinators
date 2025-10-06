package backend.models;

class Edge {

    long originId;
    long destinationId;
    String streetName;

    public Edge(long originId, long destinationId, String streetName) {
        this.originId = originId;
        this.destinationId = destinationId;
        this.streetName = streetName;
    }
}
