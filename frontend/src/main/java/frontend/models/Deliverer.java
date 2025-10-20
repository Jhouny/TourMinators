package frontend.models;

public class Deliverer {

    private static long nextId;

    private long id;
    private String name;
    private Request[] requests;

    public Deliverer() {
        this.id = nextId++;
        this.name = null;
        this.requests = new Request[0];
    }

    public Deliverer(String name) {
        this.id = nextId++;
        this.name = name;
        this.requests = new Request[0];
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Request[] getRequests() {
        return requests;
    }

    public void setRequests(Request[] requests) {
        this.requests = requests != null ? requests : new Request[0];
    }
}
