public class VehicleDemo {
    public static void main(String[] args) {
        Vehicle vehicle = new Vehicle("Campus Vehicle");
        Bus bus = new Bus("FEU Tech Campus Bus", 40);

        System.out.println("=== Campus Vehicle Reporter ===");

        System.out.println("\nVehicle:");
        vehicle.describeMovement();

        System.out.println("\nBus:");
        bus.describeMovement();

        System.out.println("\nHierarchy Explanation:");
        System.out.println("Bus inherits the vehicleName property, getter, and general movement behavior from Vehicle.");
        System.out.println("Bus overrides describeMovement() to reuse the general movement message and add its specialized capacity detail.");
    }
}