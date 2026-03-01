package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import backend.Backend;
import java.sql.SQLException;

public class CustomerDashboard extends JFrame implements ActionListener {

    private final String username;
    private final String consumerNo;
    private final JPanel panel;

    private String displayMeterNo;

    public CustomerDashboard(String username) {
        this.username = username;

        String tempConsumer = null;
        try {
            tempConsumer = Backend.getConsumerByUsername(username);
            if (tempConsumer == null || tempConsumer.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Consumer number not found for user: " + username,
                        "Initialization Error", JOptionPane.ERROR_MESSAGE);
                tempConsumer = "N/A";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error during initialization: " + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
            tempConsumer = "ERROR";
        }
        this.consumerNo = tempConsumer;

        // attempt to fetch meter number for this consumer
        this.displayMeterNo = "N/A";
        try {
            String fetchedMeterNo = Backend.getMeterByConsumerNo(this.consumerNo);
            if (fetchedMeterNo != null && !fetchedMeterNo.isEmpty()) {
                this.displayMeterNo = fetchedMeterNo;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            this.displayMeterNo = "DB_ERROR";
        }

        setTitle("Customer Dashboard");
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

     panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(null);
        add(panel);

        JLabel welcomeLabel = new JLabel("Welcome Customer! Consumer No: " + this.consumerNo + " | Meter No: " + this.displayMeterNo);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        welcomeLabel.setForeground(Color.BLACK);
        welcomeLabel.setBounds(150, 40, 800, 40);
        panel.add(welcomeLabel);

        JMenuBar mb = new JMenuBar();
        mb.setBackground(new Color(25, 25, 25));
        mb.setBorderPainted(false);

        JMenu info = themedMenu("Information", Color.WHITE);
        JMenuItem updateInformation = themedItem("Update Information");
        JMenuItem viewInformation = themedItem("View Information");
        info.add(updateInformation);
        info.add(viewInformation);

        JMenu user = themedMenu("User", Color.WHITE);
        JMenuItem payBill = themedItem("Pay Bill");
        JMenuItem billDetails = themedItem("Bill Details");
        JMenuItem generateBill = themedItem("Generate Bill");
        user.add(payBill);
        user.add(billDetails);
        user.add(generateBill);

        JMenu utility = themedMenu("Utility", Color.WHITE);
        JMenuItem notepad = themedItem("Notepad");
        JMenuItem calculator = themedItem("Calculator");
        utility.add(notepad);
        utility.add(calculator);

        JMenu exitMenu = themedMenu("Exit", Color.WHITE);
        JMenuItem exit = themedItem("Exit");
        exitMenu.add(exit);

        mb.add(info);
        mb.add(user);
        mb.add(utility);
        mb.add(exitMenu);
        setJMenuBar(mb);

        // listeners: pass meter number to pages that need meter, use consumerNo for info pages
        updateInformation.addActionListener(e -> new UpdateInformation(consumerNo));
        viewInformation.addActionListener(e -> new ViewInformation(consumerNo));

        payBill.addActionListener(e -> {
            if (displayMeterNo == null || displayMeterNo.equals("N/A") || displayMeterNo.equals("DB_ERROR")) {
                JOptionPane.showMessageDialog(this, "Meter number not available for this account.", "No Meter", JOptionPane.WARNING_MESSAGE);
            } else {
                new PayBill(displayMeterNo);
            }
        });

        billDetails.addActionListener(e -> {
            if (displayMeterNo == null || displayMeterNo.equals("N/A") || displayMeterNo.equals("DB_ERROR")) {
                JOptionPane.showMessageDialog(this, "Meter number not available for this account.", "No Meter", JOptionPane.WARNING_MESSAGE);
            } else {
                new BillDetailsPage(displayMeterNo);
            }
        });

        generateBill.addActionListener(e -> new GenerateBill(consumerNo,null));

        notepad.addActionListener(e -> openTextEditor());
        calculator.addActionListener(e -> openCalculator());
        exit.addActionListener(e -> dispose());

        setVisible(true);
    }

    private JMenu themedMenu(String title, Color c) {
        JMenu menu = new JMenu(title);
        menu.setFont(new Font("Segoe UI", Font.BOLD, 15));
        menu.setForeground(c);
        return menu;
    }

    private JMenuItem themedItem(String title) {
        JMenuItem item = new JMenuItem(title);
        item.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        item.setBackground(new Color(230, 230, 230));
        return item;
    }

    private void openTextEditor() {
        JFrame editor = new JFrame("Notepad");
        editor.setSize(500, 400);
        editor.setLocationRelativeTo(this);
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(textArea);
        editor.add(scroll, BorderLayout.CENTER);
        editor.setVisible(true);
    }

    private void openCalculator() {
        JFrame calc = new JFrame("Calculator");
        calc.setSize(300, 400);
        calc.setLocationRelativeTo(this);

        JTextField display = new JTextField();
        display.setEditable(false);
        display.setFont(new Font("Segoe UI", Font.BOLD, 20));
        calc.add(display, BorderLayout.NORTH);

        JPanel panelCalc = new JPanel(new GridLayout(4, 4, 5, 5));
        String[] buttons = {"7","8","9","/","4","5","6","*","1","2","3","-","0",".","=","+"};

        final StringBuilder current = new StringBuilder();
        final double[] lastValue = new double[1];
        final String[] lastOp = new String[1];
        lastOp[0] = "";

        for (String text : buttons) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
            btn.setBackground(new Color(200, 200, 200));
            btn.addActionListener(e -> {
                String cmd = e.getActionCommand();
                if ("0123456789.".contains(cmd)) {
                    if (current.indexOf(".") != -1 && cmd.equals(".")) return;
                    current.append(cmd);
                    display.setText(current.toString());
                } else if ("+-*/".contains(cmd)) {
                    if (current.length() > 0) {
                        lastValue[0] = Double.parseDouble(current.toString());
                    }
                    lastOp[0] = cmd;
                    current.setLength(0);
                } else if ("=".equals(cmd)) {
                    if (lastOp[0].isEmpty() || current.length() == 0) return;
                    double curr = Double.parseDouble(current.toString());
                    double res = 0;
                    try {
                        switch (lastOp[0]) {
                            case "+": res = lastValue[0] + curr; break;
                            case "-": res = lastValue[0] - curr; break;
                            case "*": res = lastValue[0] * curr; break;
                            case "/":
                                if (curr == 0) {
                                    display.setText("Error: Div by 0");
                                    return;
                                }
                                res = lastValue[0] / curr;
                                break;
                        }
                        display.setText(String.valueOf(res));
                        lastValue[0] = res;
                        current.setLength(0);
                        lastOp[0] = "";
                    } catch (NumberFormatException ex) {
                        display.setText("Error");
                        current.setLength(0);
                    }
                }
            });
            panelCalc.add(btn);
        }

        calc.add(panelCalc, BorderLayout.CENTER);
        calc.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        String msg = ae.getActionCommand();
        switch (msg) {
            case "Update Information" -> new UpdateInformation(consumerNo);
            case "View Information" -> new ViewInformation(consumerNo);
            case "Pay Bill" -> {
                if (displayMeterNo == null || displayMeterNo.equals("N/A") || displayMeterNo.equals("DB_ERROR")) {
                    JOptionPane.showMessageDialog(this, "Meter number not available for this account.", "No Meter", JOptionPane.WARNING_MESSAGE);
                } else {
                    new PayBill(displayMeterNo);
                }
            }
            case "Bill Details" -> {
                if (displayMeterNo == null || displayMeterNo.equals("N/A") || displayMeterNo.equals("DB_ERROR")) {
                    JOptionPane.showMessageDialog(this, "Meter number not available for this account.", "No Meter", JOptionPane.WARNING_MESSAGE);
                } else {
                    new BillDetailsPage(displayMeterNo);
                }
            }
            case "Generate Bill" -> new GenerateBill(consumerNo, null);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CustomerDashboard("afna23"));
    }
}