package frontend.models;

public class Request {
    private PointOfInterest pickup;
    private PointOfInterest delivery;

    public Request(PointOfInterest pickup, PointOfInterest delivery) {
        this.pickup = pickup;
        this.delivery = delivery;
    }

    public PointOfInterest getPickup() {
        return pickup;
    }

    public void setPickup(PointOfInterest pickup) {
        this.pickup = pickup;
    }

    public PointOfInterest getDelivery() {
        return delivery;
    }

    public void setDelivery(PointOfInterest delivery) {
        this.delivery = delivery;
    }
}
