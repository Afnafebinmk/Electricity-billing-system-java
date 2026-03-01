
package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import backend.Backend;

public class GenerateBill extends JFrame {

    private JTextPane billContentPane;
    private final String consumerNo;
    private final String month;

    public GenerateBill(String consumerNo, String month) {
        this.consumerNo = consumerNo;
        this.month = month;

        setTitle("Electricity Bill (KSEB Style) - " + consumerNo);
        setSize(800, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        JLabel heading = new JLabel("ELECTRICITY BILL - KERALA STATE ELECTRICITY BOARD (KSEB)", SwingConstants.CENTER);
        heading.setFont(new Font("Tahoma", Font.BOLD, 18));
        heading.setBorder(new EmptyBorder(10, 0, 10, 0));
        heading.setBackground(new Color(0, 102, 102));
        heading.setForeground(Color.WHITE);
        heading.setOpaque(true);
        add(heading, BorderLayout.NORTH);

        billContentPane = new JTextPane();
        billContentPane.setEditable(false);
        billContentPane.setContentType("text/html");
        JScrollPane scrollPane = new JScrollPane(billContentPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        add(scrollPane, BorderLayout.CENTER);

        loadBillDataAndGeneratePrintout();

        JButton closeButton = new JButton("Close");
        closeButton.setBackground(new Color(200, 50, 50));
        closeButton.setForeground(Color.WHITE);
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeButton.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBorder(new EmptyBorder(10, 10, 10, 10));
        footer.add(closeButton);
        add(footer, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void loadBillDataAndGeneratePrintout() {
        ResultSet rsCustomer = null;
        ResultSet rsBill = null;

        try {
            rsCustomer = Backend.getCustomerByConsumerNo(consumerNo);
            if (rsCustomer == null || !rsCustomer.next()) {
                billContentPane.setText("<html><body><h3>Customer not found for: " + consumerNo + "</h3></body></html>");
                return;
            }

            String lookupMonth = (month == null || month.isBlank())
                    ? java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM"))
                    : month;

            // Try specific month first
            try {
                rsBill = Backend.getSpecificBill(consumerNo, lookupMonth);
            } catch (SQLException ex) {
                rsBill = null;
            }

            // If no specific month found and month was not explicitly provided, try latest bill
            boolean haveBill = rsBill != null && rsBill.next();
            if (!haveBill && (month == null || month.isBlank())) {
                // try Backend.getLatestBillForConsumer if present
                try {
                    // attempt direct method
                    rsBill = Backend.getLatestBillForConsumer(consumerNo);
                    haveBill = rsBill != null && rsBill.next();
                } catch (NoSuchMethodError | SQLException | AbstractMethodError e) {
                    // method not present or failed — fallback to a message below
                    haveBill = false;
                } catch (Throwable t) {
                    haveBill = false;
                }
            }

            if (!haveBill) {
                String shownMonth = (month == null || month.isBlank()) ? "latest" : lookupMonth;
                String msg = "No bill found for consumer " + consumerNo + " for month: " + shownMonth + ".\n"
                        + "Make sure a bill exists for that month, or generate the bill first.";
                JOptionPane.showMessageDialog(this, msg, "Bill Not Found", JOptionPane.INFORMATION_MESSAGE);
                billContentPane.setText("<html><body><h3>No bill data available for " + shownMonth + ".</h3></body></html>");
                return;
            }

            // rsCustomer is already on first row, rsBill is already advanced to the row we want
            // generate HTML using current cursor positions
            String htmlContent = generateHtmlBillFromCurrentRows(rsCustomer, rsBill);
            billContentPane.setText(htmlContent);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database Error: Failed to fetch bill details.\n" + e.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
            billContentPane.setText("<html><body><h3>Database error while loading bill.</h3></body></html>");
        } finally {
            try {
                if (rsCustomer != null) rsCustomer.close();
            } catch (SQLException ignored) {}
            try {
                if (rsBill != null) rsBill.close();
            } catch (SQLException ignored) {}
        }
    }
// ...existing code...
private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
    ResultSetMetaData md = rs.getMetaData();
    int cols = md.getColumnCount();
    for (int i = 1; i <= cols; i++) {
        String col = md.getColumnLabel(i);
        if (col != null && col.equalsIgnoreCase(columnName)) return true;
    }
    return false;
}

private String generateHtmlBillFromCurrentRows(ResultSet rsCustomer, ResultSet rsBill) throws SQLException {
    String name = rsCustomer.getString("name");
    String address = (rsCustomer.getString("address") == null ? "" : rsCustomer.getString("address"))
            + ", " + (rsCustomer.getString("city") == null ? "" : rsCustomer.getString("city"));
    String meterNo = rsCustomer.getString("meter_no");
    String tariff = rsCustomer.getString("tariff_category");

    String billMonth = hasColumn(rsBill, "month") ? rsBill.getString("month") : "";
    double units = hasColumn(rsBill, "units_consumed") ? rsBill.getDouble("units_consumed") : 0.0;
    double energyCharge = hasColumn(rsBill, "energy_charge") ? rsBill.getDouble("energy_charge") : 0.0;
    double fixedCharge = hasColumn(rsBill, "fixed_charge") ? rsBill.getDouble("fixed_charge") : 0.0;
    double duty = hasColumn(rsBill, "duty") ? rsBill.getDouble("duty") : 0.0;
    double fuelSurcharge = hasColumn(rsBill, "fuel_surcharge") ? rsBill.getDouble("fuel_surcharge") : 0.0;
    double totalAmount = hasColumn(rsBill, "total_amount") ? rsBill.getDouble("total_amount") : 0.0;
    double previousReading = hasColumn(rsBill, "previous_reading") ? rsBill.getDouble("previous_reading") : 0.0;
    double currentReading = hasColumn(rsBill, "current_reading") ? rsBill.getDouble("current_reading") : 0.0;
    String readingDate = hasColumn(rsBill, "reading_date") ? rsBill.getString("reading_date") : "";
    Date dueDate = hasColumn(rsBill, "due_date") ? rsBill.getDate("due_date") : null;
    String status = hasColumn(rsBill, "status") ? rsBill.getString("status") : "";

    // Try multiple possible column names for who generated / calculated the bill
    String calculatedBy = "";
    if (hasColumn(rsBill, "calculated_by")) calculatedBy = rsBill.getString("calculated_by");
    else if (hasColumn(rsBill, "generated_by")) calculatedBy = rsBill.getString("generated_by");
    else if (hasColumn(rsBill, "created_by")) calculatedBy = rsBill.getString("created_by");
    else if (hasColumn(rsBill, "processed_by")) calculatedBy = rsBill.getString("processed_by");

    // If reading_date is empty, prefer showing who calculated the bill
    String operatorLine = "";
    if (calculatedBy != null && !calculatedBy.isBlank()) {
        operatorLine = "<div style='margin-top:8px; color:#555;'><small>Bill calculated by: <b>" + escape(calculatedBy) + "</b></small></div>";
    } else if (readingDate == null || readingDate.isBlank()) {
        operatorLine = "<div style='margin-top:8px; color:#555;'><small>Bill calculated by: <b>Admin </b></small></div>";
    } else {
        operatorLine = ""; // nothing extra if readingDate present and no operator info
    }

    StringBuilder html = new StringBuilder();
    html.append("<html><body style='font-family: Arial, sans-serif; padding: 20px; font-size: 13px;'>");

    html.append("<div style='border: 2px solid #006666; padding: 15px; background-color: #E6F3F3;'>");
    html.append("<table width='100%' cellpadding='5'>");
    html.append("<tr><td colspan='2'><h3 style='color: #006666; margin: 0;'>CONSUMER DETAILS</h3></td></tr>");
    html.append("<tr><td><b>Name:</b> ").append(escape(name)).append("</td><td><b>Consumer No:</b> ").append(escape(consumerNo)).append("</td></tr>");
    html.append("<tr><td><b>Address:</b> ").append(escape(address)).append("</td><td><b>Meter No:</b> ").append(escape(meterNo)).append("</td></tr>");
    html.append("<tr><td><b>Tariff Category:</b> ").append(escape(tariff)).append("</td><td><b>Bill Month:</b> ").append(escape(billMonth)).append("</td></tr>");
    html.append("</table>");
    // show operator immediately under consumer details
    html.append(operatorLine);
    html.append("</div><br>");

    html.append("<div style='border: 1px solid #CCCCCC; padding: 10px;'>");
    html.append("<table width='100%' cellpadding='5'>");
    html.append("<tr><td colspan='3'><h3 style='color: #333333; margin: 0;'>ENERGY CONSUMPTION</h3></td></tr>");
    html.append("<tr><td><b>Previous Reading:</b> ").append(String.format("%.0f", previousReading)).append(" kWh</td>");
    html.append("<td><b>Current Reading:</b> ").append(String.format("%.0f", currentReading)).append(" kWh</td>");
    html.append("<td><b>Reading Date:</b> ").append(escape(readingDate)).append("</td></tr>");
    html.append("<tr><td colspan='3'><b>Units Consumed:</b> <span style='font-size: 16px; font-weight: bold; color: #CC0000;'>")
            .append(String.format("%.0f", units)).append(" kWh</span></td></tr>");
    html.append("</table></div><br>");

    html.append("<div style='border: 1px solid #CCCCCC; padding: 10px;'>");
    html.append("<table width='100%' cellpadding='5'>");
    html.append("<tr><td colspan='2'><h3 style='color: #333333; margin: 0;'>CHARGE BREAKDOWN</h3></td></tr>");
    html.append("<tr><td>Energy Charge (for ").append(String.format("%.0f", units)).append(" units):</td><td align='right'>Rs. ").append(String.format("%.2f", energyCharge)).append("</td></tr>");
    html.append("<tr><td>Fixed Charge:</td><td align='right'>Rs. ").append(String.format("%.2f", fixedCharge)).append("</td></tr>");
    html.append("<tr><td>Electricity Duty:</td><td align='right'>Rs. ").append(String.format("%.2f", duty)).append("</td></tr>");
    html.append("<tr><td>Fuel Surcharge:</td><td align='right'>Rs. ").append(String.format("%.2f", fuelSurcharge)).append("</td></tr>");
    html.append("<tr><td><hr></td><td><hr></td></tr>");
    html.append("<tr><td><b>Total Current Bill Amount:</b></td><td align='right'><b style='font-size: 18px; color: #006666;'>Rs. ").append(String.format("%.2f", totalAmount)).append("</b></td></tr>");
    html.append("</table></div><br>");

    html.append("<div style='text-align: center; padding: 10px; border: 1px dashed #FF6600; background-color: #FFF0E0;'>");
    html.append("Please pay the total amount of <b>Rs. ").append(String.format("%.2f", totalAmount)).append("</b> by the due date.<br>");
    if (dueDate != null) html.append("<b>Due Date:</b> ").append(dueDate.toString()).append("<br>");
    html.append("<i>Thank you for using our service.</i></div>");

    html.append("</body></html>");
    return html.toString();
}
// ...existing code...

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}