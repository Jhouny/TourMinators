package backend.models;

class Edge {

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
}
