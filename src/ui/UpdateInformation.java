package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import backend.Backend;

public class UpdateInformation extends JFrame implements ActionListener {

    // Input fields
    JTextField tfAddress, tfCity, tfState, tfEmail, tfPhone;
    JButton update, cancel;
    
    private String consumerNumber; 
    
    // Labels for read-only display
    JLabel lblNameVal, lblConsumerNoVal; 

    /**
     * Constructor accepts the Consumer Number.
     */
    public UpdateInformation(String consumerNo) {
        this.consumerNumber = consumerNo;

        // --- Frame Setup (Modern Dark Accent) ---
        setTitle("Update Customer Information");
        setSize(700, 500); // Increased height slightly for better spacing
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(240, 240, 245)); // Light background
        setLayout(new BorderLayout());

        // --- Header Panel (Deep Blue Accent) ---
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(new Color(30, 60, 114)); // Deep Blue
        headerPanel.setBorder(new EmptyBorder(15, 0, 15, 0));

        JLabel heading = new JLabel("UPDATE CUSTOMER DETAILS");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        heading.setForeground(Color.WHITE);
        headerPanel.add(heading);
        
        add(headerPanel, BorderLayout.NORTH);

        // --- Main Content Panel (Grid for fields) ---
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(240, 240, 245));
        mainPanel.setBorder(new EmptyBorder(30, 50, 20, 50));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        int row = 0;

        // Helper function for adding read-only pair
        row = addReadOnlyPair(mainPanel, gbc, row, "Name:", lblNameVal = new JLabel());
        row = addReadOnlyPair(mainPanel, gbc, row, "Consumer No:", lblConsumerNoVal = new JLabel(this.consumerNumber), new Color(25, 135, 84));

        // Separator
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        mainPanel.add(new JSeparator(), gbc);
        
        // Helper function for adding editable fields
        row = addEditableField(mainPanel, gbc, row, "Address:", tfAddress = new JTextField(20));
        row = addEditableField(mainPanel, gbc, row, "City:", tfCity = new JTextField(20));
        row = addEditableField(mainPanel, gbc, row, "State:", tfState = new JTextField(20));
        row = addEditableField(mainPanel, gbc, row, "Email:", tfEmail = new JTextField(20));
        row = addEditableField(mainPanel, gbc, row, "Phone:", tfPhone = new JTextField(20));

        add(mainPanel, BorderLayout.CENTER);

        // --- Footer/Button Panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        buttonPanel.setBackground(Color.WHITE);
        
        update = createStyledButton("Update", new Color(30, 60, 114)); // Deep Blue for Update
        update.addActionListener(this);
        buttonPanel.add(update);

        cancel = createStyledButton("Cancel", new Color(200, 50, 50)); // Red for Cancel
        cancel.addActionListener(this);
        buttonPanel.add(cancel);
        
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Data Fetching ---
        fetchInitialData();
        
        // Finalizing frame properties
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }
    
    // --- UI Helper Methods ---
    
    private JButton createStyledButton(String text, Color background) {
        JButton button = new JButton(text);
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(120, 35));
        button.setFocusPainted(false);
        return button;
    }

    private int addReadOnlyPair(JPanel panel, GridBagConstraints gbc, int row, String labelText, JLabel valueLabel) {
        return addReadOnlyPair(panel, gbc, row, labelText, valueLabel, Color.BLACK);
    }
    
    private int addReadOnlyPair(JPanel panel, GridBagConstraints gbc, int row, String labelText, JLabel valueLabel, Color valueColor) {
        JLabel keyLabel = new JLabel(labelText);
        keyLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        keyLabel.setForeground(new Color(30, 60, 114)); // Deep Blue text
        
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.1;
        panel.add(keyLabel, gbc);

        valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        valueLabel.setForeground(valueColor);
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 0.9;
        panel.add(valueLabel, gbc);
        
        return row + 1;
    }
    
    private int addEditableField(JPanel panel, GridBagConstraints gbc, int row, String labelText, JTextField textField) {
        JLabel keyLabel = new JLabel(labelText);
        keyLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        keyLabel.setForeground(new Color(50, 50, 50));
        
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.1;
        panel.add(keyLabel, gbc);

        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
        textField.setPreferredSize(new Dimension(250, 30));
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 0.9;
        panel.add(textField, gbc);
        
        return row + 1;
    }

    // --- Data Logic ---
    
    private void fetchInitialData() {
        if (consumerNumber == null || consumerNumber.trim().isEmpty()) return;

        ResultSet rs = null;
        try {
            // Assumes Backend.getCustomerByConsumerNo selects customer columns
            rs = Backend.getCustomerByConsumerNo(consumerNumber); 
            
            if (rs != null && rs.next()) {
                lblNameVal.setText(rs.getString("name"));
                lblConsumerNoVal.setText(consumerNumber); // Set the consumer number value label
                
                // Set editable fields
                tfAddress.setText(rs.getString("address"));
                tfCity.setText(rs.getString("city"));
                tfState.setText(rs.getString("state"));
                tfEmail.setText(rs.getString("email"));
                tfPhone.setText(rs.getString("phone"));
                
            } else {
                JOptionPane.showMessageDialog(this, "No customer found for Consumer No: " + consumerNumber, "Data Error", JOptionPane.ERROR_MESSAGE);
                update.setEnabled(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching initial customer data!", "Database Error", JOptionPane.ERROR_MESSAGE);
            update.setEnabled(false);
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException ex) { /* Log or ignore */ }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == update) {
            // Collect updated data
            String address = tfAddress.getText().trim();
            String city = tfCity.getText().trim();
            String state = tfState.getText().trim();
            String email = tfEmail.getText().trim();
            String phone = tfPhone.getText().trim();
            
            try{
                boolean updated = Backend.updateCustomer(consumerNumber, address, city, state, email, phone);
                
                if (updated) {
                    JOptionPane.showMessageDialog(null, "Customer information updated successfully! ✅");
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(null, "Failed to update information. Check database connection or consumer number.", "Update Failed", JOptionPane.ERROR_MESSAGE);
                }
            }catch(java.sql.SQLException ex){
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database error during update: " + ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
            }
            
        } else if (ae.getSource() == cancel) {
            dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UpdateInformation("CUST001"));
    }
}