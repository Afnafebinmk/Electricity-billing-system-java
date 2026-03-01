package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import backend.Backend;

public class ViewInformation extends JFrame implements ActionListener {

    private final String consumerId; // Renamed for clarity (Consumer No)
    private JButton cancel;
    
    // Labels that will display the fetched customer data (public access modifier removed)
    private JLabel lblNameVal, lblAddressVal, lblCityVal, lblStateVal, lblEmailVal, lblPhoneVal;
    
    // Meter info will be embedded or omitted entirely for simplicity, following your request.

    /**
     * Constructor accepts the Consumer Number, which is the key for the customers table.
     */
    public ViewInformation(String consumerNo) {
        this.consumerId = consumerNo;

        // --- Frame Setup (Modern) ---
        setTitle("Customer Profile - " + consumerNo);
        setSize(800, 500);
        setLocationRelativeTo(null); // Center the window
        getContentPane().setBackground(new Color(245, 245, 245)); // Light gray background
        setLayout(new BorderLayout(20, 20)); // Use BorderLayout for structure
        
        // --- Header Panel ---
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(new Color(30, 60, 114)); // Deep blue header
        headerPanel.setBorder(new EmptyBorder(15, 0, 15, 0));

        JLabel heading = new JLabel("CUSTOMER PROFILE DETAILS");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 24));
        heading.setForeground(Color.WHITE);
        headerPanel.add(heading);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // --- Main Content Panel (Grid Layout) ---
        JPanel mainPanel = new JPanel(new GridLayout(4, 4, 15, 15)); // 4 rows, 4 columns (Label + Value)
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));
        mainPanel.setBackground(new Color(245, 245, 245));
        
        // --- 1. Top Row: Name and Consumer No ---
        // Name
        addDetailPair(mainPanel, "Name:", lblNameVal = new JLabel());
        
        // Consumer Number (Highlighted)
        addDetailPair(mainPanel, "Consumer No:", new JLabel(consumerId), true); 
        
        // --- 2. Second Row: City and State ---
        addDetailPair(mainPanel, "City:", lblCityVal = new JLabel());
        addDetailPair(mainPanel, "State:", lblStateVal = new JLabel());
        
        // --- 3. Third Row: Email and Phone ---
        addDetailPair(mainPanel, "Email:", lblEmailVal = new JLabel());
        addDetailPair(mainPanel, "Phone:", lblPhoneVal = new JLabel());
        
        // --- 4. Bottom Row: Address ---
        // Address (Spanning two columns for space)
        addLabel(mainPanel, "Address:", new Font("Segoe UI", Font.BOLD, 14), new Color(30, 60, 114));
        lblAddressVal = new JLabel();
        lblAddressVal.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        // Use a wrapping JPanel for the address to handle long text
        JPanel addressPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        addressPanel.setBackground(new Color(245, 245, 245));
        addressPanel.add(lblAddressVal);
        
        // Span the Address value across the remaining three columns
        mainPanel.add(addressPanel);
        mainPanel.add(new JPanel()); // Empty placeholder
        mainPanel.add(new JPanel()); // Empty placeholder


        add(mainPanel, BorderLayout.CENTER);

        // --- Footer/Button Panel ---
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        footerPanel.setBackground(Color.WHITE);
        
        cancel = new JButton("Close");
        cancel.setBackground(new Color(200, 50, 50)); // Reddish color for action
        cancel.setForeground(Color.WHITE);
        cancel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancel.setPreferredSize(new Dimension(120, 35));
        cancel.setFocusPainted(false);
        cancel.addActionListener(this);
        footerPanel.add(cancel);
        
        add(footerPanel, BorderLayout.SOUTH);

        // --- Data Fetching ---
        fetchCustomerInfo(consumerNo);

        setVisible(true);
    }
    
    /**
     * Helper method to add a Label/Value pair to the main panel.
     */
    private void addDetailPair(JPanel parent, String labelText, JLabel valueLabel) {
        addDetailPair(parent, labelText, valueLabel, false);
    }
    
    private void addDetailPair(JPanel parent, String labelText, JLabel valueLabel, boolean highlight) {
        // Label (Key)
        JLabel keyLabel = new JLabel(labelText);
        keyLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        keyLabel.setForeground(new Color(30, 60, 114)); // Deep blue color
        parent.add(keyLabel);

        // Value
        valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        if (highlight) {
            valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 16f));
            valueLabel.setForeground(new Color(25, 135, 84)); // Green highlight
        } else {
            valueLabel.setForeground(Color.BLACK);
        }
        parent.add(valueLabel);
    }
    
    // Overload for simple label without associated value field (used for the Address Key)
    private void addLabel(JPanel parent, String labelText, Font font, Color color) {
        JLabel keyLabel = new JLabel(labelText);
        keyLabel.setFont(font);
        keyLabel.setForeground(color);
        parent.add(keyLabel);
    }


    /**
     * Fetches all customer data and populates the labels.
     */
    private void fetchCustomerInfo(String consumerNo) {
        if (consumerNo == null || consumerNo.trim().isEmpty()) return;

        ResultSet rs = null;
        try {
            // Assumes Backend.getCustomerByConsumerNo returns customer details + meter_no
            rs = Backend.getCustomerByConsumerNo(consumerNo);
            
            if (rs != null && rs.next()) {
                // Populate UI fields
                lblNameVal.setText(rs.getString("name"));
                lblAddressVal.setText("<html>" + rs.getString("address") + "</html>"); // Use HTML for wrapping long address
                lblCityVal.setText(rs.getString("city"));
                lblStateVal.setText(rs.getString("state"));
                lblEmailVal.setText(rs.getString("email"));
                lblPhoneVal.setText(rs.getString("phone"));
                
                // IMPORTANT: Since we removed the meterNumber JLabel, 
                // we don't try to read rs.getString("meter_number") here, which solves the previous SQL error 
                // IF the only fix was to remove the code reading the column.
                // However, if the Backend method still uses a JOIN, it should be fine.
                
            } else {
                JOptionPane.showMessageDialog(this, "No customer found for Consumer: " + consumerNo);
                dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching customer info: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            dispose();
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException ex) { /* Log or ignore */ }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == cancel) {
            dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ViewInformation("CUST001")); 
    }
}