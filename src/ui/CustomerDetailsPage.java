package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import backend.Backend;

public class CustomerDetailsPage extends JFrame implements ActionListener {

    private JTable table;
    private JButton printBtn;
    private JPanel detailPanel, mainPanel;
    private Timer slideTimer;
    private int targetHeight = 0;
    private boolean isExpanded = false;

    public CustomerDetailsPage() {
        setTitle("Customer & Meter Details");
        setSize(1100, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        mainPanel = new JPanel(new BorderLayout());
        add(mainPanel);

        // Table
        table = new JTable();
        loadCustomerMeterData();
        JScrollPane scroll = new JScrollPane(table);
        mainPanel.add(scroll, BorderLayout.CENTER);

        // Detail Panel (starts hidden)
        detailPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        detailPanel.setBorder(BorderFactory.createTitledBorder("Tariff Details"));
        detailPanel.setPreferredSize(new Dimension(0, 0));
        detailPanel.setOpaque(true);
        detailPanel.setBackground(new Color(245, 245, 245));
        mainPanel.add(detailPanel, BorderLayout.SOUTH);

        // Print Button
        printBtn = new JButton("Print");
        printBtn.addActionListener(this);
        JPanel btnPanel = new JPanel();
        btnPanel.add(printBtn);
        mainPanel.add(btnPanel, BorderLayout.NORTH);

        // Row click listener
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                    showTariffDetailsForSelectedCustomer();
                }
            }
        });

        setVisible(true);
    }

    private void loadCustomerMeterData() {
        try {
            ResultSet rs = Backend.getCustomerMeterTariffInfo();
            if (rs == null) return;

            String[] columnNames = {
                "Consumer No", "Name", "Address", "City", "State", "Email", "Phone",
                "Meter No", "Tariff Type", "Tariff Category", "Meter Type", "Load Connected",
                "Installation Date", "Status", "Location", "Previous Reading", "Current Reading",
                "Multiplier Factor", "Reading Date", "Remarks"
            };

            DefaultTableModel model = new DefaultTableModel(columnNames, 0);

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("consumer_no"),
                        rs.getString("name"),
                        rs.getString("address"),
                        rs.getString("city"),
                        rs.getString("state"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("meter_no"),
                        rs.getString("tariff_type"),
                        rs.getString("tariff_category"),
                        rs.getString("meter_type"),
                        rs.getString("load_connected"),
                        rs.getString("installation_date"),
                        rs.getString("status"),
                        rs.getString("location"),
                        rs.getString("previous_reading"),
                        rs.getString("current_reading"),
                        rs.getString("multiplier_factor"),
                        rs.getString("reading_date"),
                        rs.getString("remarks")
                });
            }

            table.setModel(model);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading customer data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showTariffDetailsForSelectedCustomer() {
        int row = table.getSelectedRow();
        if (row == -1) return;

        String tariffCategory = table.getValueAt(row, 9).toString(); // Tariff Category column

        try {
            ResultSet rs = Backend.getTaxesByCategory(tariffCategory);
            if (rs != null && rs.next()) {
                detailPanel.removeAll();
                detailPanel.add(new JLabel("Tariff Category:"));
                detailPanel.add(new JLabel(rs.getString("tariff_category")));
                detailPanel.add(new JLabel("Effective From:"));
                detailPanel.add(new JLabel(rs.getString("effective_date")));
                detailPanel.add(new JLabel("Remarks:"));
                detailPanel.add(new JLabel(rs.getString("remarks")));

                animatePanelExpand(120);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching tariff details.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void animatePanelExpand(int finalHeight) {
        if (slideTimer != null && slideTimer.isRunning()) slideTimer.stop();

        int startHeight = isExpanded ? detailPanel.getHeight() : 0;
        targetHeight = isExpanded ? 0 : finalHeight;
        isExpanded = !isExpanded;

        slideTimer = new Timer(10, new ActionListener() {
            int step = (targetHeight - startHeight) / 20;
            int current = startHeight;

            public void actionPerformed(ActionEvent e) {
                current += step;
                if ((step > 0 && current >= targetHeight) || (step < 0 && current <= targetHeight)) {
                    current = targetHeight;
                    slideTimer.stop();
                }
                detailPanel.setPreferredSize(new Dimension(detailPanel.getWidth(), current));
                detailPanel.revalidate();
            }
        });
        slideTimer.start();
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == printBtn) {
            try {
                table.print();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Printing failed.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        new CustomerDetailsPage();
    }
}