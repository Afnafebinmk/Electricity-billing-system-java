package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import backend.Bill;    
import java.util.List;
import backend.Backend; // Ensure this import is present
import java.util.Vector;

public class BillDetailsPage extends JFrame {

    private JTable table;
    private DefaultTableModel tableModel;
    private String meterNumber; // Renamed for clarity

    public BillDetailsPage(String meterNumber) {
        this.meterNumber = meterNumber;

        setTitle("Bill Details - Meter: " + meterNumber);
        setSize(1000, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Define table columns
        String[] columns = {
            "Bill ID", "Meter No", "Month", "Year", "Previous Reading", "Current Reading",
            "Units Consumed", "Fixed Charge", "Energy Charge", "Fuel Surcharge", 
            "Duty", "Total Amount", "Due Date", "Status"
        };
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);

        // Display nicely
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        loadBillData();

        JScrollPane scroll = new JScrollPane(table);
        add(scroll, BorderLayout.CENTER);

        setVisible(true);
    }

    private void loadBillData() {
        try {
            List<Bill> bills = Backend.getBillsByMeter(meterNumber);
            tableModel.setRowCount(0); // Clear previous rows

            if (bills.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No bill records found for meter: " + meterNumber,
                        "No Data", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            for (Bill b : bills) {
                Vector<Object> row = new Vector<>();
                row.add(b.billId);
                row.add(b.meterNumber);
                row.add(b.month);
                row.add(b.year);
                row.add(b.previousReading);
                row.add(b.currentReading);
                row.add(b.unitsConsumed);
                row.add(b.fixedCharge);
                row.add(b.energyCharge);
                row.add(b.fuelSurcharge);
                row.add(b.duty);
                row.add(b.totalAmount);
                row.add(b.dueDate);
                row.add(b.status);

                tableModel.addRow(row);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading bills: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BillDetailsPage("MTR001"));
    }
}