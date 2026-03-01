package ui;

import javax.swing.*;
import java.awt.*;
import backend.Backend;
import java.sql.SQLException;

public class LoginWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleBox;

    public LoginWindow() {
        setTitle("Electricity Billing System - Login");
        setSize(450, 340);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Electricity Billing System", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        panel.add(title, g);

        g.gridwidth = 1;
        g.gridy++;
        panel.add(new JLabel("Username:"), g);
        usernameField = new JTextField(15);
        g.gridx = 1; panel.add(usernameField, g);

        g.gridx = 0; g.gridy++;
        panel.add(new JLabel("Password:"), g);
        passwordField = new JPasswordField(15);
        g.gridx = 1; panel.add(passwordField, g);

        g.gridx = 0; g.gridy++;
        panel.add(new JLabel("Login as:"), g);
        roleBox = new JComboBox<>(new String[]{"Customer", "Admin"});
        g.gridx = 1; panel.add(roleBox, g);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton login = new JButton("Login");
        JButton signup = new JButton("Signup");
        JButton cancel = new JButton("Cancel");
        btns.add(login);
        btns.add(signup);
        btns.add(cancel);
        g.gridx = 0; g.gridy++; g.gridwidth = 2;
        panel.add(btns, g);

        login.addActionListener(e -> login());
        signup.addActionListener(e -> {
            dispose();
            new SignupWindow();
        });
        cancel.addActionListener(e -> System.exit(0));

        add(panel);
        setVisible(true);
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String role = (String) roleBox.getSelectedItem();

       
        boolean success = false;
        try {
            success = Backend.validateUser(username, password, role);
        } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
        if (success) {
            JOptionPane.showMessageDialog(this, "Login Successful as " + role + "!");
            dispose();

            if (role.equalsIgnoreCase("Admin")) {
                new AdminDashboardWithMeterFlow(username); // admin doesn’t have meter
            } else {
                // Get the customer's meter number using username
                
                new CustomerDashboard(username);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Invalid Credentials!");
        }
    }

    public static void main(String[] args) {
        new LoginWindow();
    }
}
