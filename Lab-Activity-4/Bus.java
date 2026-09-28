public class Bus extends Vehicle {
    private int capacity;

    public Bus(String vehicleName, int capacity) {
        super(vehicleName);
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    public void describeMovement() {
        super.describeMovement();
        System.out.println("This campus bus has a capacity of " + capacity + " passengers.");
    }
}