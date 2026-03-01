package ui;

import backend.Backend;
import backend.Bill;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;

public class PayBill extends JFrame {

    private final String meter;
    private JTable billTable;
    private DefaultTableModel tableModel;
    private JButton payBtn, backBtn;

    public PayBill(String meter) {
        this.meter = meter;

        setTitle("Pay Pending Bills");
        setSize(800, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        Color primaryColor = new Color(30, 60, 114);
        Color lightBackground = new Color(240, 240, 245);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(lightBackground);
        getContentPane().add(mainPanel);

        // Header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(primaryColor);
        JLabel heading = new JLabel("PENDING BILLS (Meter: " + meter + ")"); // Added meter for clarity
        heading.setFont(new Font("Segoe UI", Font.BOLD, 24));
        heading.setForeground(Color.WHITE);
        headerPanel.add(heading);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Table
        // The table columns match the data fields pulled from the List<Bill>
        String[] columns = {"Bill ID", "Month", "Year", "Amount (Rs.)", "Status"};
        tableModel = new DefaultTableModel(columns, 0);
        billTable = new JTable(tableModel);
        billTable.setRowHeight(25);
        billTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        billTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        billTable.getTableHeader().setBackground(primaryColor);
        billTable.getTableHeader().setForeground(Color.WHITE);
        billTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(billTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 15));
        buttonPanel.setBackground(lightBackground);

        payBtn = createButton("Pay Bill", primaryColor);
        payBtn.addActionListener(e -> paySelectedBill());
        buttonPanel.add(payBtn);

        backBtn = createButton("Back", Color.GRAY);
        backBtn.addActionListener(e -> dispose());
        buttonPanel.add(backBtn);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        loadBills();

        setVisible(true);
    }

    private JButton createButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(150, 35));
        button.setFocusPainted(false);
        return button;
    }

    /**
     * Loads pending bills by calling the backend method which returns a List<Bill>.
     */
    // ...existing code...
private void loadBills() {
    tableModel.setRowCount(0); // Clear table

    try {
        List<Bill> pendingBills = Backend.getPendingBills(meter);

        // Debug output
        System.out.println("DEBUG: getPendingBills for meter=" + meter + " returned: " + (pendingBills == null ? "null" : pendingBills.size() + " items"));

        if (pendingBills == null || pendingBills.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No pending bills found for meter: " + meter,
                    "No Data",
                    JOptionPane.INFORMATION_MESSAGE);
            payBtn.setEnabled(false);
            return;
        }

        for (Bill bill : pendingBills) {
            // Adjust field access if Bill uses getters or different names
            Object id = getFieldOrGetter(bill, "billId");
            Object month = getFieldOrGetter(bill, "month");
            Object year = getFieldOrGetter(bill, "year");
            Object total = getFieldOrGetter(bill, "totalAmount");
            Object status = getFieldOrGetter(bill, "status");

            // Print each bill for debugging
            System.out.println("DEBUG: bill -> id=" + id + " month=" + month + " year=" + year + " total=" + total + " status=" + status);

            tableModel.addRow(new Object[]{
                    id,
                    month,
                    year,
                    String.format("%.2f", total instanceof Number ? ((Number) total).doubleValue() : Double.parseDouble(String.valueOf(total))),
                    status
            });
        }

        payBtn.setEnabled(true);

    } catch (SQLException ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this,
                "Error loading bills: " + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        payBtn.setEnabled(false);
    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this,
                "Unexpected error: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        payBtn.setEnabled(false);
    }
}

// helper method (place below loadBills in same file)
private Object getFieldOrGetter(Bill b, String fieldName) {
    try {
        // try public field
        java.lang.reflect.Field f = b.getClass().getField(fieldName);
        return f.get(b);
    } catch (NoSuchFieldException | IllegalAccessException e) {
        // try getter
        try {
            String getter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            java.lang.reflect.Method m = b.getClass().getMethod(getter);
            return m.invoke(b);
        } catch (Exception ex) {
            return null;
        }
    }
}
// ...existing code...

    private void paySelectedBill() {
        int selectedRow = billTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a bill to pay!",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // CRITICAL CHANGE: Retrieving the unique Bill ID from the first column
        int billId = (int) tableModel.getValueAt(selectedRow, 0);
        String month = (String) tableModel.getValueAt(selectedRow, 1);
        String year = tableModel.getValueAt(selectedRow, 2).toString();
        String amount = tableModel.getValueAt(selectedRow, 3).toString();

        int confirm = JOptionPane.showConfirmDialog(this,
                "Confirm payment of Rs." + amount + " for the bill (" + month + " " + year + ")?",
                "Confirm Payment",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Call the preferred backend method using the unique Bill ID
                boolean success = Backend.payBillById(billId); 
                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "Bill ID " + billId + " paid successfully! Status updated.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadBills(); // Refresh table
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Payment failed. Bill may already be paid or Bill ID is invalid.",
                            "Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Database error during payment: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PayBill("123456")); // Example meter number
    }
}
