package backend;

public class BillComponents {
    public double unitsConsumed;
    public double fixedCharge;
    public double energyCharge;
    public double fuelSurcharge;
    public double duty;
    public double totalAmount;

    public BillComponents(double unitsConsumed,double fixedCharge, double energyCharge, double fuelSurcharge, double duty, double totalAmount) {
        this.fixedCharge = fixedCharge;
        this.energyCharge = energyCharge;
        this.fuelSurcharge = fuelSurcharge;
        this.duty = duty;
        this.totalAmount = totalAmount;
    }
}