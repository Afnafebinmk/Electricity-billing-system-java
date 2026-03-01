package backend;

public class TariffCalculator {

    public static class BillComponents {
        public final double unitsConsumed;
        public final double fixedCharge;
        public final double energyCharge;
        public final double fuelSurcharge;
        public final double duty;
        public final double totalAmount;

        public BillComponents(double unitsConsumed, double fixedCharge, double energyCharge,
                              double fuelSurcharge, double duty, double totalAmount) {
            this.unitsConsumed = unitsConsumed;
            this.fixedCharge = fixedCharge;
            this.energyCharge = energyCharge;
            this.fuelSurcharge = fuelSurcharge;
            this.duty = duty;
            this.totalAmount = totalAmount;
        }
    }

    public static BillComponents calculateBillComponents(String category, double unitsConsumed) {

        category = category.trim().toLowerCase();

        // --- Hardcoded rates ---
        double fixedCharge = 0;
        double energyRate = 0;
        double fuelSurchargePercent = 0;
        double dutyPercent = 0;

        switch(category) {
            case "domestic":
                fixedCharge = 50;
                energyRate = 5;
                fuelSurchargePercent = 10;
                dutyPercent = 5;
                break;
            case "commercial":
                fixedCharge = 100;
                energyRate = 10;
                fuelSurchargePercent = 15;
                dutyPercent = 10;
                break;
            case "industrial":
                fixedCharge = 200;
                energyRate = 8;
                fuelSurchargePercent = 12;
                dutyPercent = 8;
                break;
            case "agriculture":
                fixedCharge = 20;
                energyRate = 2;
                fuelSurchargePercent = 5;
                dutyPercent = 2;
                break;
            default:
                fixedCharge = 50;
                energyRate = 5;
                fuelSurchargePercent = 5;
                dutyPercent = 5;
        }

        // --- Energy charge with simple slab logic ---
        double energyCharge = 0;
        switch(category) {
            case "domestic":
                if (unitsConsumed <= 100) energyCharge = unitsConsumed * (energyRate * 0.8);
                else if (unitsConsumed <= 200) energyCharge = 100 * (energyRate * 0.8) + (unitsConsumed - 100) * energyRate;
                else energyCharge = 100 * (energyRate * 0.8) + 100 * energyRate + (unitsConsumed - 200) * (energyRate * 1.2);
                break;
            case "commercial":
                energyCharge = unitsConsumed * energyRate * 1.5;
                break;
            case "industrial":
                if (unitsConsumed <= 500) energyCharge = unitsConsumed * energyRate * 1.2;
                else energyCharge = 500 * energyRate * 1.2 + (unitsConsumed - 500) * energyRate * 1.05;
                break;
            case "agriculture":
                energyCharge = unitsConsumed * energyRate * 0.5;
                break;
            default:
                energyCharge = unitsConsumed * energyRate;
        }

        // --- Other components ---
        double fuelSurcharge = (energyCharge * fuelSurchargePercent) / 100.0;
        double duty = ((energyCharge + fixedCharge) * dutyPercent) / 100.0;
        double totalAmount = fixedCharge + energyCharge + fuelSurcharge + duty;

        return new BillComponents(unitsConsumed, fixedCharge, energyCharge, fuelSurcharge, duty, totalAmount);
    }
}