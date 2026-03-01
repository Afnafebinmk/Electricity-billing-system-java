package backend;
import java.sql.Date;

public class Bill {
    public int billId;
    public String meterNumber;
    public String month;
    public int year;
    public double unitsConsumed;
    public double fixedCharge;
    public double energyCharge;
    public double fuelSurcharge;
    public double duty;
    public double totalAmount;
    public Date dueDate;
    public String status;
    public int previousReading;
    public int currentReading;

    public Bill(int billId, String meterNumber, String month, int year,
                double unitsConsumed, double fixedCharge, double energyCharge,
                double fuelSurcharge, double duty, double totalAmount,
                Date dueDate, String status, int previousReading, int currentReading) {
        this.billId = billId;
        this.meterNumber = meterNumber;
        this.month = month;
        this.year = year;
        this.unitsConsumed = unitsConsumed;
        this.fixedCharge = fixedCharge;
        this.energyCharge = energyCharge;
        this.fuelSurcharge = fuelSurcharge;
        this.duty = duty;
        this.totalAmount = totalAmount;
        this.dueDate = dueDate;
        this.status = status;
        this.previousReading = previousReading;
        this.currentReading = currentReading;
    }
}