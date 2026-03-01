package ui;

import backend.Backend;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.util.HashMap;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Admin dashboard that includes a MeterInformation page for adding/updating meter details.
 * Corrected to use static inner classes for UI frames.
 */
public class AdminDashboardWithMeterFlow extends JFrame {

    private String username;
    private String adminUsername;

    public AdminDashboardWithMeterFlow(String username) {
        this.username = username;
        this.adminUsername = username; // Store admin username for later use

        setTitle("Admin Dashboard - Welcome " + username);
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Set initial background same as CalculateBillPage (light blue)
        getContentPane().setBackground(new Color(240, 248, 255));
        setLayout(new BorderLayout());

        // ---------------- Menu Bar ----------------
        JMenuBar mb = new JMenuBar();

        // Master Menu
        JMenu master = new JMenu("Master");
        mb.add(master);

        JMenuItem newCustomer = new JMenuItem("New Customer");
        // 💡 Instantiates the static inner class
        newCustomer.addActionListener(e -> new NewCustomerPage(adminUsername).setVisible(true));
        master.add(newCustomer);

        JMenuItem customerDetails = new JMenuItem("Customer Details");
        // Assuming CustomerDetailsPage takes no arguments or handles admin role internally
        customerDetails.addActionListener(e -> new CustomerDetailsPage().setVisible(true)); 
        master.add(customerDetails);

        JMenuItem calculateBill = new JMenuItem("Calculate Bill");
        calculateBill.addActionListener(e -> {
            // Call the new parameterless constructor
            new CalculateBillPage().setVisible(true);
        });
master.add(calculateBill);

        JMenuItem billDetails = new JMenuItem("Bill Details");
        billDetails.addActionListener(e -> {
            String meter = JOptionPane.showInputDialog(this, "Enter Meter Number:");
            if (meter != null && !meter.isEmpty()) {
                new BillDetailsPage(meter).setVisible(true);
            }
        });
        master.add(billDetails);

        // ---------------- Utility Menu ----------------
        JMenu utility = new JMenu("Utility");
        mb.add(utility);

        JMenuItem notepad = new JMenuItem("Notepad");
        notepad.addActionListener(e -> openTextEditor());
        utility.add(notepad);

        JMenuItem calculator = new JMenuItem("Calculator");
        calculator.addActionListener(e -> openCalculator());
        utility.add(calculator);

        // ---------------- Theme Menu ----------------
        JMenu themeMenu = new JMenu("Theme");
        mb.add(themeMenu);

        JMenuItem lightTheme = new JMenuItem("Light Theme");
        lightTheme.addActionListener(e -> applyTheme(true));
        themeMenu.add(lightTheme);

        JMenuItem darkTheme = new JMenuItem("Dark Theme");
        darkTheme.addActionListener(e -> applyTheme(false));
        themeMenu.add(darkTheme);

        // ---------------- Logout Menu ----------------
        JMenu logout = new JMenu("Logout");
        mb.add(logout);
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> dispose());
        logout.add(exit);

        setJMenuBar(mb);

        JLabel lbl = new JLabel("Welcome Admin, manage your customers here.", JLabel.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        add(lbl, BorderLayout.CENTER);

        setVisible(true);
    }

    // ---------------- Theme Methods ----------------
    private void applyTheme(boolean light) {
        Color bgColor = light ? new Color(240, 248, 255) : new Color(45, 45, 45);
        Color fgColor = light ? Color.BLACK : Color.WHITE;

        getContentPane().setBackground(bgColor);
        updateComponentTreeUI(this.getContentPane(), bgColor, fgColor);
    }

    private void updateComponentTreeUI(Component comp, Color bg, Color fg) {
        // Only update JComponents to avoid issues with native components
        if (comp instanceof JComponent) {
             comp.setBackground(bg);
             comp.setForeground(fg);
        }

        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                updateComponentTreeUI(child, bg, fg);
            }
        }
    }

    // ---------------- Utility Methods ----------------
    private void openTextEditor() {
        JFrame editor = new JFrame("Text Editor");
        editor.setSize(500, 400);
        editor.setLocationRelativeTo(null);

        JTextArea textArea = new JTextArea();
        JScrollPane scroll = new JScrollPane(textArea);
        editor.add(scroll);

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files (*.txt)", "txt");
            fc.setFileFilter(filter);
            
            int option = fc.showSaveDialog(editor);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".txt")) {
                    file = new File(file.getAbsolutePath() + ".txt");
                }
                
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(textArea.getText());
                    JOptionPane.showMessageDialog(editor, "File saved successfully!");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(editor, "Error saving file: " + ex.getMessage());
                }
            }
        });

        editor.add(saveBtn, BorderLayout.SOUTH);
        editor.setVisible(true);
    }

    private void openCalculator() {
        JFrame calc = new JFrame("Calculator");
        calc.setSize(300, 400);
        calc.setLocationRelativeTo(null);

        JTextField display = new JTextField();
        display.setEditable(false);
        calc.add(display, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridLayout(4, 4, 5, 5));
        String[] buttons = {"7","8","9","/","4","5","6","*","1","2","3","-","0",".","=","+"};

        final StringBuilder current = new StringBuilder();
        final double[] lastValue = {0.0};
        final String[] lastOp = {""};

        for (String text : buttons) {
            JButton btn = new JButton(text);
            btn.addActionListener(e -> {
                String cmd = e.getActionCommand();
                if ("0123456789.".contains(cmd)) {
                    // Prevent multiple dots
                    if (cmd.equals(".") && current.toString().contains(".")) return;
                    current.append(cmd);
                    display.setText(current.toString());
                } else if ("+-*/".contains(cmd)) {
                    if (current.length() > 0) {
                        // If an operation button is pressed, store the current number
                        lastValue[0] = Double.parseDouble(current.toString());
                    }
                    lastOp[0] = cmd;
                    current.setLength(0); // Clear for the next number
                } else if ("=".equals(cmd)) {
                    if (lastOp[0].isEmpty() || current.length() == 0) return;

                    double curr = Double.parseDouble(current.toString());
                    double res = lastValue[0]; // Start with the stored value

                    switch (lastOp[0]) {
                        case "+": res = lastValue[0] + curr; break;
                        case "-": res = lastValue[0] - curr; break;
                        case "*": res = lastValue[0] * curr; break;
                        case "/": res = curr != 0 ? lastValue[0] / curr : Double.NaN; break; // Handle division by zero
                    }
                    
                    String resultText = String.valueOf(res);
                    if (resultText.endsWith(".0")) {
                        resultText = resultText.substring(0, resultText.length() - 2);
                    }
                    display.setText(resultText);
                    
                    // Set the result as the new starting value for chaining calculations
                    lastValue[0] = res;
                    current.setLength(0);
                    lastOp[0] = ""; // Clear operation
                }
            });
            panel.add(btn);
        }

        calc.add(panel, BorderLayout.CENTER);
        calc.setVisible(true);
    }
} // End of AdminDashboardWithMeterFlow

// ---------------- New Customer Page ----------------
// 💡 CORRECTION: Defined as static inner class
 class NewCustomerPage extends JFrame {

    private String adminUsername;

    public NewCustomerPage(String adminUsername) { 
        setTitle("New Customer");
        setSize(500, 350);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        this.adminUsername = adminUsername; // Initialize the field

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel form = new JPanel(new GridLayout(7,2,8,8));
        JTextField nameField = new JTextField();
        JTextField consumerNoField = new JTextField();
        JTextField addressField = new JTextField();
        JTextField cityField = new JTextField();
        JTextField stateField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField phoneField = new JTextField();

        form.add(new JLabel("Customer Name:")); form.add(nameField);
        form.add(new JLabel("Consumer Number:")); form.add(consumerNoField);
        form.add(new JLabel("Address:")); form.add(addressField);
        form.add(new JLabel("City:")); form.add(cityField);
        form.add(new JLabel("State:")); form.add(stateField);
        form.add(new JLabel("Email:")); form.add(emailField);
        form.add(new JLabel("Phone:")); form.add(phoneField);

        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(form, gbc);

        JPanel btns = new JPanel();
        JButton next = new JButton("Next");
        JButton cancel = new JButton("Cancel");
        btns.add(next); btns.add(cancel);
        gbc.gridy = 1;
        mainPanel.add(btns, gbc);

        next.addActionListener(e -> {
            // Extract values from fields
            String name = nameField.getText().trim();
            String consumerNo = consumerNoField.getText().trim();
            String address = addressField.getText().trim();
            String city = cityField.getText().trim();
            String state = stateField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            
            if (consumerNo.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Consumer Number and Name are mandatory.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if customer already exists
            if (Backend.customerExists(consumerNo)) {
                JOptionPane.showMessageDialog(this, "Consumer number already exists!", "Duplicate Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Save customer in DB
            try {
                boolean success = Backend.addCustomer(name, consumerNo, address, city, state, email, phone);

                if(success) {
                    JOptionPane.showMessageDialog(this, "Customer Added Successfully! Proceeding to Meter setup.");

                    // Open meter info page
                    new MeterInformationPage(consumerNo, adminUsername).setVisible(true);

                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Error adding customer", "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch(Exception ex) { // Catch generic Exception to handle both SQL and other runtime errors
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }); // ⬅️ CRITICAL FIX: The missing closing brace was here.
        
        cancel.addActionListener(e -> dispose());

        add(mainPanel);
    } 
} 

// ---------------- Meter Information Page ----------------
// 💡 CORRECTION: Defined as static inner class
 class MeterInformationPage extends JFrame {
    private String adminUsername;

    public MeterInformationPage(String consumerNo ,String adminUsername) {

        this.adminUsername = adminUsername;
        setTitle("Meter Information");
        setSize(620, 600);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(14, 2, 8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10)); // Add padding

        // 🔹 Fields
        JTextField consumerField = new JTextField(consumerNo);
        consumerField.setEditable(false);

        JTextField meterNoField = new JTextField();

        JComboBox<String> tariffTypeBox = new JComboBox<>(new String[]{
                "Domestic", "Commercial", "Industrial", "Agricultural"
        });

        JComboBox<String> tariffCategoryBox = new JComboBox<>();

        JComboBox<String> meterTypeBox = new JComboBox<>(new String[]{
                "Electric Meter", "Solar Meter", "Smart Meter"
        });

        JTextField loadConnectedField = new JTextField("1.0");
        JTextField installationDateField = new JTextField(LocalDate.now().toString());
        installationDateField.setEditable(false);

        JComboBox<String> statusBox = new JComboBox<>(new String[]{
                "Active", "Inactive", "Faulty"
        });

        JComboBox<String> locationBox = new JComboBox<>(new String[]{
                "Inside", "Outside", "Pole Mounted", "Underground"
        });

        JTextField previousReadingField = new JTextField("0");
        JTextField currentReadingField = new JTextField("0");
        JTextField multiplierFactorField = new JTextField("1.0");
        JTextField readingDateField = new JTextField(LocalDate.now().toString());
        JTextField remarksField = new JTextField();

        // 🔹 Tariff Categories (based on type)
        HashMap<String, String[]> tariffCategories = new HashMap<>();
        tariffCategories.put("Domestic", new String[]{
                "Domestic A1 (LT-1A)",
                "Domestic A2 (LT-1B)"
        });
        tariffCategories.put("Commercial", new String[]{
                "Commercial (LT-7A)",
                "Commercial Small Shop (LT-7B)"
        });
        tariffCategories.put("Industrial", new String[]{
                "Industrial (LT-4A)",
                "Industrial (LT-4B)"
        });
        tariffCategories.put("Agricultural", new String[]{
                "Agricultural (LT-5A)",
                "Agricultural Pump (LT-5B)"
        });

        // 🧠 Auto-update categories based on type
        tariffTypeBox.addActionListener(e -> {
            String selected = (String) tariffTypeBox.getSelectedItem();
            tariffCategoryBox.removeAllItems();
            String[] cats = tariffCategories.get(selected);
            if (cats != null) {
                for (String cat : cats) tariffCategoryBox.addItem(cat);
            }
        });

        // Default load: manually trigger the action for initial load
        tariffTypeBox.setSelectedIndex(0);
        tariffTypeBox.getActionListeners()[0].actionPerformed(null);

        // 🔹 Add Components
        panel.add(new JLabel("Consumer No:")); panel.add(consumerField);
        panel.add(new JLabel("Meter No:")); panel.add(meterNoField);
        panel.add(new JLabel("Tariff Type:")); panel.add(tariffTypeBox);
        panel.add(new JLabel("Tariff Category:")); panel.add(tariffCategoryBox);
        panel.add(new JLabel("Meter Type:")); panel.add(meterTypeBox);
        panel.add(new JLabel("Load Connected (kW):")); panel.add(loadConnectedField);
        panel.add(new JLabel("Installation Date:")); panel.add(installationDateField);
        panel.add(new JLabel("Status:")); panel.add(statusBox);
        panel.add(new JLabel("Location:")); panel.add(locationBox);
        panel.add(new JLabel("Previous Reading:")); panel.add(previousReadingField);
        panel.add(new JLabel("Current Reading:")); panel.add(currentReadingField);
        panel.add(new JLabel("Multiplier Factor:")); panel.add(multiplierFactorField);
        panel.add(new JLabel("Reading Date:")); panel.add(readingDateField);
        panel.add(new JLabel("Remarks:")); panel.add(remarksField);

        // 🔹 Buttons
        JPanel btnPanel = new JPanel();
        JButton submit = new JButton("Save Meter Info");
        JButton cancel = new JButton("Cancel");
        btnPanel.add(submit);
        btnPanel.add(cancel);

        add(panel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        // 🔹 Submit Action
        submit.addActionListener(e -> {
            try {
                String meterNo = meterNoField.getText().trim();
                String tariffType = (String) tariffTypeBox.getSelectedItem();
                String tariffCategory = (String) tariffCategoryBox.getSelectedItem();
                String meterType = (String) meterTypeBox.getSelectedItem();
                
                // Input validation
                if (meterNo.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Meter Number cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                double loadConnected = Double.parseDouble(loadConnectedField.getText().trim());

                // Use current date for installation date if field is disabled
                Date installDate = Date.valueOf(installationDateField.getText().trim()); 
                String status = (String) statusBox.getSelectedItem();
                String location = (String) locationBox.getSelectedItem();
                int prevReading = Integer.parseInt(previousReadingField.getText().trim());
                int currReading = Integer.parseInt(currentReadingField.getText().trim());
                double multiplier = Double.parseDouble(multiplierFactorField.getText().trim());

                Date readingDateSQL = Date.valueOf(readingDateField.getText().trim());
                String remarks = remarksField.getText().trim();
                String consumer = consumerField.getText().trim();

                boolean success = Backend.addOrUpdateMeter(
                        meterNo, consumer, tariffType, tariffCategory, meterType, 
                        loadConnected, installDate, status, location, 
                        prevReading, currReading, multiplier, readingDateSQL, 
                        remarks, adminUsername
                );

                if (success) {
                    JOptionPane.showMessageDialog(this, "✅ Meter Information Saved Successfully!");
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Error saving meter information.", "Database Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "⚠️ Invalid number format in reading or load fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "⚠️ Invalid date format. Use YYYY-MM-DD.", "Input Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } catch (SQLException ex) {
                 JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "An unexpected error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        cancel.addActionListener(e2 -> dispose());
    }
}