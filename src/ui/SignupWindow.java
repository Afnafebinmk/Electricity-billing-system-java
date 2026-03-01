package ui;

import backend.Backend;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class SignupWindow extends JFrame {
    private JTextField usernameField, meterField, nameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleBox;
    private JLabel lblMeter, lblName;

    public SignupWindow() {
        setBounds(450, 150, 500, 400);
        getContentPane().setBackground(Color.WHITE);
        setLayout(null);

        JPanel panel = new JPanel();
        panel.setBounds(30, 30, 420, 300);
        panel.setBorder(new TitledBorder(new LineBorder(new Color(173, 216, 230), 2),
                                         "Create-Account",
                                         TitledBorder.LEADING, TitledBorder.TOP,
                                         null, new Color(72, 118, 255)));
        panel.setBackground(Color.WHITE);
        panel.setLayout(null);
        add(panel);

        JLabel lblRole = new JLabel("Create Account As");
        lblRole.setBounds(60, 40, 140, 20);
        lblRole.setForeground(Color.GRAY);
        lblRole.setFont(new Font("Tahoma", Font.BOLD, 14));
        panel.add(lblRole);

        roleBox = new JComboBox<>(new String[]{"Customer", "Admin"});
        roleBox.setBounds(220, 40, 130, 22);
        panel.add(roleBox);

        lblMeter = new JLabel("consumer Number");
        lblMeter.setBounds(60, 80, 140, 20);
        lblMeter.setForeground(Color.GRAY);
        lblMeter.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblMeter.setVisible(false);
        panel.add(lblMeter);

        meterField = new JTextField();
        meterField.setBounds(220, 80, 130, 22);
        meterField.setVisible(false);
        panel.add(meterField);

        JLabel lblUsername = new JLabel("Username");
        lblUsername.setBounds(60, 120, 140, 20);
        lblUsername.setForeground(Color.GRAY);
        lblUsername.setFont(new Font("Tahoma", Font.BOLD, 14));
        panel.add(lblUsername);

        usernameField = new JTextField();
        usernameField.setBounds(220, 120, 130, 22);
        panel.add(usernameField);

        lblName = new JLabel("Name");
        lblName.setBounds(60, 160, 140, 20);
        lblName.setForeground(Color.GRAY);
        lblName.setFont(new Font("Tahoma", Font.BOLD, 14));
        panel.add(lblName);

        nameField = new JTextField();
        nameField.setBounds(220, 160, 130, 22);
        nameField.setEditable(true);
        panel.add(nameField);

        JLabel lblPassword = new JLabel("Password");
        lblPassword.setBounds(60, 200, 140, 20);
        lblPassword.setForeground(Color.GRAY);
        lblPassword.setFont(new Font("Tahoma", Font.BOLD, 14));
        panel.add(lblPassword);

        passwordField = new JPasswordField();
        passwordField.setBounds(220, 200, 130, 22);
        panel.add(passwordField);

        roleBox.addActionListener(e -> {
        String role = (String) roleBox.getSelectedItem();
        if ("Customer".equals(role)) {
        lblMeter.setVisible(true);
        meterField.setVisible(true);
        // Keep name editable so user can type manually
        nameField.setEditable(true);
        // Clear any previous text
        nameField.setText("");
        } else {
        lblMeter.setVisible(false);
        meterField.setVisible(false);
        nameField.setEditable(true);
        nameField.setText("");
    }
});


        // Fetch name while typing meter number
        meterField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                fetchCustomerName();
            }
        });

        JButton create = new JButton("Create");
        create.setBackground(Color.BLACK);
        create.setForeground(Color.WHITE);
        create.setBounds(90, 250, 100, 25);
        create.addActionListener(e -> signup());
        panel.add(create);

        JButton back = new JButton("Back");
        back.setBackground(Color.BLACK);
        back.setForeground(Color.WHITE);
        back.setBounds(220, 250, 100, 25);
        back.addActionListener(e -> {
            dispose();
            new LoginWindow();
        });
        panel.add(back);

        setVisible(true);
    }

    private void fetchCustomerName() {
        String meter = meterField.getText().trim();
        if (meter.isEmpty()) {
            nameField.setText("");
            return;
        }

        try {
            String name = Backend.getCustomerName(meter);
            if (name != null) {
                nameField.setText(name);
            } else {
                nameField.setText("");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

  private void signup() {
    String username = usernameField.getText().trim();
    String password = new String(passwordField.getPassword());
    String role = (String) roleBox.getSelectedItem();
    String name = nameField.getText().trim();
    String meterNo = (role.equals("Customer")) ? meterField.getText().trim() : null;

    boolean success = false;
    try {
        if ("Admin".equals(role)) {
            success = Backend.createUser(username, password, role, name,null);
        } else {
            success = Backend.createUser(username, password, role, name, meterNo);
        }
    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        return;
    }

   

    if (success) {
        JOptionPane.showMessageDialog(this, "Account created successfully!");
        dispose();
        new LoginWindow();
    } else {
        JOptionPane.showMessageDialog(this, "Error creating account!");
    }
}
}
    