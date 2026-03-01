package ui;

import backend.Backend;
import backend.TariffCalculator;
import backend.TariffCalculator.BillComponents;
import backend.Bill;    
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.Vector;

public class CalculateBillPage extends JFrame implements ActionListener {

    private JComboBox<String> cbConsumerNo;
    private JComboBox<String> cbMonth;
    private JTextField tfYear, tfPrevReading, tfCurrReading;
    private JTextField tfName, tfTariffCategory;
    private JTextArea taAddress;
    private JButton btnCalculate;
    private JTable billTable;
    private DefaultTableModel tableModel;

    private String consumerNumber;
    private String meterNumber;

    public CalculateBillPage() {
        setTitle("Calculate Electricity Bill");
        setSize(1200, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // ---- Top Panels ----
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel selectionPanel = createSelectionPanel();
        JPanel infoPanel = createInfoPanel();
        JPanel readingPanel = createReadingPanel();
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        btnCalculate = new JButton("Calculate & Save Bill");
        btnCalculate.setEnabled(false);
        btnCalculate.addActionListener(this);
        buttonPanel.add(btnCalculate);

        topPanel.add(selectionPanel, BorderLayout.NORTH);
        topPanel.add(infoPanel, BorderLayout.WEST);
        topPanel.add(readingPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // ---- Table ----
        String[] columns = {
            "Bill ID", "Meter No", "Month", "Year", "Units", "Fixed", "Energy",
            "Surcharge", "Duty", "Total", "Due Date", "Status"
        };
        tableModel = new DefaultTableModel(columns, 0);
        billTable = new JTable(tableModel);
        add(new JScrollPane(billTable), BorderLayout.CENTER);

        // ---- Load consumers ----
        loadConsumerNumbers();

        setVisible(true);
    }

    // ---------------- UI Panels ----------------

    private JPanel createSelectionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.add(new JLabel("Select Consumer No:"));

        cbConsumerNo = new JComboBox<>();
        cbConsumerNo.setPreferredSize(new Dimension(200, 25));
        cbConsumerNo.addActionListener(e -> {
            if (cbConsumerNo.getSelectedIndex() > 0) {
                consumerNumber = (String) cbConsumerNo.getSelectedItem();
                loadCustomerAndMeterInfo();
                loadBills();
                btnCalculate.setEnabled(true);
            } else {
                clearDetails();
                btnCalculate.setEnabled(false);
            }
        });

        panel.add(cbConsumerNo);
        return panel;
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Customer Details"));

        tfName = new JTextField();
        tfName.setEditable(false);

        tfTariffCategory = new JTextField();
        tfTariffCategory.setEditable(false);

        taAddress = new JTextArea(3, 15);
        taAddress.setEditable(false);
        taAddress.setWrapStyleWord(true);
        taAddress.setLineWrap(true);

        panel.add(new JLabel("Name:"));
        panel.add(tfName);
        panel.add(new JLabel("Tariff Category:"));
        panel.add(tfTariffCategory);
        panel.add(new JLabel("Address:"));
        panel.add(new JScrollPane(taAddress));

        return panel;
    }

    private JPanel createReadingPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Bill Parameters"));

        cbMonth = new JComboBox<>(new String[]{
            "January","February","March","April","May","June",
            "July","August","September","October","November","December"
        });
        cbMonth.setSelectedIndex(LocalDate.now().getMonthValue() - 1);

        tfYear = new JTextField(String.valueOf(LocalDate.now().getYear()));
        tfPrevReading = new JTextField();
        tfCurrReading = new JTextField();

        panel.add(new JLabel("Month:"));
        panel.add(cbMonth);
        panel.add(new JLabel("Year:"));
        panel.add(tfYear);
        panel.add(new JLabel("Previous Reading:"));
        panel.add(tfPrevReading);
        panel.add(new JLabel("Current Reading:"));
        panel.add(tfCurrReading);

        return panel;
    }

    // ---------------- Data Loading ----------------

    private void clearDetails() {
        tfName.setText("");
        tfTariffCategory.setText("");
        taAddress.setText("");
        tfPrevReading.setText("");
        tfCurrReading.setText("");
        tableModel.setRowCount(0);
        meterNumber = null;
    }

    private void loadConsumerNumbers() {
        cbConsumerNo.removeAllItems();
        cbConsumerNo.addItem("--- Select Consumer ---");
        try (ResultSet rs = Backend.getAllConsumerNumbers()) {
            while (rs.next()) {
                cbConsumerNo.addItem(rs.getString("consumer_no"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading consumers: " + e.getMessage());
        }
    }

    private void loadCustomerAndMeterInfo() {
        try {
            meterNumber = Backend.getMeterByConsumerNo(consumerNumber);
            if (meterNumber == null) {
                JOptionPane.showMessageDialog(this, "Meter not found for this consumer.");
                return;
            }

            ResultSet rsCustomer = Backend.getCustomerByConsumerNo(consumerNumber);
            if (rsCustomer.next()) {
                tfName.setText(rsCustomer.getString("name"));
                taAddress.setText(rsCustomer.getString("address") + ", " +
                                  rsCustomer.getString("city") + ", " +
                                  rsCustomer.getString("state"));
            }

            String tariffCategory = Backend.getTariffTypeByMeter(meterNumber);
            tfTariffCategory.setText(tariffCategory);

           List<Bill> bills = Backend.getBillsByMeter(meterNumber);
int latestReading = 0;
if (!bills.isEmpty()) {
    latestReading = bills.get(0).currentReading; // Most recent bill
}
tfPrevReading.setText(String.valueOf(latestReading));

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading customer info: " + e.getMessage());
        }
    }

    private void loadBills() {
        try (ResultSet rs = Backend.getBillsByConsumerNo(consumerNumber)) {
            tableModel.setRowCount(0);
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("bill_id"));
                row.add(rs.getString("meter_number"));
                row.add(rs.getString("month"));
                row.add(rs.getInt("year"));
                row.add(rs.getDouble("units_consumed"));
                row.add(rs.getDouble("fixed_charge"));
                row.add(rs.getDouble("energy_charge"));
                row.add(rs.getDouble("fuel_surcharge"));
                row.add(rs.getDouble("duty"));
                row.add(rs.getDouble("total_amount"));
                row.add(rs.getDate("due_date"));
                row.add(rs.getString("status"));
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading bills: " + e.getMessage());
        }
    }

    // ---------------- Bill Calculation ----------------

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnCalculate) {
            calculateAndSaveBill();
        }
    }

   
    // ...existing code...
private void calculateAndSaveBill() {
    try {
        String month = cbMonth.getSelectedItem().toString();
        int year = Integer.parseInt(tfYear.getText());
        int prev = Integer.parseInt(tfPrevReading.getText());
        int curr = Integer.parseInt(tfCurrReading.getText());

        if (curr < prev) {
            JOptionPane.showMessageDialog(this, "Current reading cannot be less than previous.");
            return;
        }

        double units = curr - prev;

        if (Backend.isBillExists(meterNumber, month, year)) {
            int opt = JOptionPane.showConfirmDialog(this,
                    "Bill already exists for " + month + " " + year + ". Overwrite?",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.NO_OPTION) return;
        }

        // Debug logs to help diagnose zero result
        System.out.println("DEBUG: meterNumber=" + meterNumber + " prev=" + prev + " curr=" + curr + " units=" + units);
        String category = tfTariffCategory.getText();
        System.out.println("DEBUG: tariffCategory=" + category);

        // CORRECT ARG ORDER: pass prev then curr (not curr, prev)
        BillComponents comp = TariffCalculator.calculateBillComponents(category,units);

        System.out.println("DEBUG: energy=" + comp.energyCharge + " fixed=" + comp.fixedCharge + " duty=" + comp.duty + " surcharge=" + comp.fuelSurcharge);
        System.out.println("BILL CALCULATED:" + comp.totalAmount);

        Date dueDate = Date.valueOf(LocalDate.now().plusDays(15));
// ...existing code...
        // call backend with expected parameters (now including prev and curr)
        boolean saved = Backend.addOrUpdateBill(
            meterNumber,
            month,
            year,
            comp.unitsConsumed,
            comp.fixedCharge,
            comp.energyCharge,
            comp.fuelSurcharge,
            comp.duty,
            comp.totalAmount,
            dueDate,
            prev,   // previous reading (int)
            curr    // current reading (int)
        );
// ...existing code...
        if (saved) {
            // update meter readings separately
            try {
                Backend.updateMeterReadings(meterNumber, prev, curr);
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Bill saved but failed to update meter readings: " + e.getMessage());
            }

            JOptionPane.showMessageDialog(this,
                    "Bill saved successfully. Total = Rs. " + comp.totalAmount);

            loadBills();
            tfPrevReading.setText(String.valueOf(curr));
        } else {
            JOptionPane.showMessageDialog(this, "Error saving bill.");
        }

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
    }
}
// ...existing code...

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CalculateBillPage::new);
    }
}