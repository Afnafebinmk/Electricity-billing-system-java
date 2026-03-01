package backend;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import backend.Bill;    
import java.util.List;

public class Backend {

    // ---------- DATABASE CONNECTION ----------
    private static final String URL = "jdbc:mysql://localhost:3306/ebs";
    private static final String USER = "root";
    private static final String PASSWORD = "Ashna@2003"; 

    private static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    private static Connection con;

static {
    try {
        Class.forName("com.mysql.cj.jdbc.Driver");
        con = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/ebs",
            "root",
            "Ashna@2003"
        );
    } catch (Exception e) {
        e.printStackTrace();
    }
}
    // ========================================================================
    // --- CUSTOMER & METER RETRIEVAL METHODS ---
    // ========================================================================

    /**
     * Retrieves all unique consumer numbers from the customers table.
     */
    public static ResultSet getAllConsumerNumbers() throws SQLException {
        String query = "SELECT consumer_no FROM customers ORDER BY consumer_no";
        // NOTE: Caller is responsible for closing the ResultSet and Connection.
        Connection con = getConnection();
        PreparedStatement ps = con.prepareStatement(query);
        return ps.executeQuery();
    }

    /**
     * Retrieves customer and joined meter details, including meter readings.
     * **CRITICAL FIX:** Ensures necessary meter readings are selected for bill calculation.
     */
    public static ResultSet getCustomerByConsumerNo(String consumerNo) throws SQLException {
        Connection con = getConnection();
        PreparedStatement ps = null;

        String query = "SELECT " +
                       "c.name, c.address, c.city, c.state, c.email, c.phone, " +
                       "m.meter_no, m.tariff_type, m.tariff_category, m.load_connected, " +
                       "m.previous_reading, m.current_reading " +
                       "FROM customers c " +
                       "JOIN meter m ON c.consumer_no = m.consumer_no " +
                       "WHERE c.consumer_no = ?";

        try {
            ps = con.prepareStatement(query);
            ps.setString(1, consumerNo);
            return ps.executeQuery();
        } catch (SQLException e) {
            if (ps != null) try { ps.close(); } catch (SQLException ex) {}
            if (con != null) try { con.close(); } catch (SQLException ex) {}
            throw e;
        }
    }
    
    /**
     * Retrieves ONLY the meter number associated with a given consumer number.
     * Uses try-with-resources for clean closing.
     */

     // Add this method to backend/Backend.java
public static ResultSet getSpecificBill(String consumerNo, String month) throws SQLException {
    Connection con = null;
    PreparedStatement ps = null;
    try {
        con = getConnection();
        // Query joins customers/meter/bills tables to ensure bill belongs to the consumer
        String sql = "SELECT b.* FROM bills b JOIN meter m ON b.meter_number = m.meter_no WHERE m.consumer_no = ? AND b.month = ? ORDER BY b.year DESC LIMIT 1";
        ps = con.prepareStatement(sql);
        ps.setString(1, consumerNo);
        ps.setString(2, month);
        return ps.executeQuery();
    } catch (SQLException e) {
        if (ps != null) try { ps.close(); } catch (SQLException ex) {}
        if (con != null) try { con.close(); } catch (SQLException ex) {}
        throw e;
    }
}
    public static String getMeterByConsumerNo(String consumerNo) throws SQLException {
        String meterNo = null;
        String query = "SELECT meter_no FROM meter WHERE consumer_no = ?";

        try (Connection con = getConnection(); 
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, consumerNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    meterNo = rs.getString("meter_no");
                }
            }
        }
        return meterNo;
    }
    
    /**
     * Retrieves the tariff category from the meter table.
     */
    public static String getTariffTypeByMeter(String meterNo) throws SQLException {
        String sql = "SELECT tariff_category FROM meter WHERE meter_no=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, meterNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("tariff_category") : null;
            }
        }
    }
    
    // ========================================================================
    // --- TARIFF & TAX METHODS ---
    // ========================================================================

    private static double getTaxValue(String category, String fieldName) throws SQLException {
        // Gets the latest active tax rate
        String sql = "SELECT " + fieldName + " FROM taxes WHERE tariff_category=? ORDER BY effective_from DESC LIMIT 1";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(fieldName) : 0.0;
            }
        }
    }

    public static double getFixedCharge(String category) throws SQLException {
        return getTaxValue(category, "fixed_charge");
    }

    public static double getEnergyChargeRate(String category) throws SQLException {
        return getTaxValue(category, "energy_charge_rate");
    }

    public static double getFuelSurcharge(String category) throws SQLException {
        return getTaxValue(category, "fuel_surcharge_percent");
    }

    public static double getDutyPercent(String category) throws SQLException {
        return getTaxValue(category, "duty_percent");
    }

    /**
     * Retrieves the latest tax rates for a given tariff category.
     */
    public static ResultSet getTaxesByCategory(String tariffCategory) throws SQLException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            // Get the latest tax rate by ordering
            ps = con.prepareStatement("SELECT * FROM taxes WHERE tariff_category=? ORDER BY effective_from DESC LIMIT 1");
            ps.setString(1, tariffCategory);
            return ps.executeQuery();
        } finally {
            // NOTE: Connection is left open for the caller to process the ResultSet.
        }
    }

    // ========================================================================
    // --- BILL CALCULATION & MANAGEMENT METHODS ---
    // ========================================================================
    
    /**
     * Retrieves the single latest bill data for a meter, including the meter readings used.
     * This is crucial for setting the 'Previous Reading' for the new bill.
     */
   public static List<Bill> getBillsByMeter(String meterNo) throws SQLException {
    List<Bill> bills = new ArrayList<>();
    String sql = "SELECT * FROM bills WHERE meter_number=? " +
                 "ORDER BY year DESC, FIELD(month, 'January','February','March','April','May','June','July','August','September','October','November','December') DESC";

    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, meterNo);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Bill bill = new Bill(
                    rs.getInt("bill_id"),
                    rs.getString("meter_number"),
                    rs.getString("month"),
                    rs.getInt("year"),
                    rs.getDouble("units_consumed"),
                    rs.getDouble("fixed_charge"),
                    rs.getDouble("energy_charge"),
                    rs.getDouble("fuel_surcharge"),
                    rs.getDouble("duty"),
                    rs.getDouble("total_amount"),
                    rs.getDate("due_date"),
                    rs.getString("status"),
                    rs.getInt("previous_reading"),
                    rs.getInt("current_reading")
                );
                bills.add(bill);
            }
        }
    }
    return bills;
}
    
    /**
     * Inserts or updates a bill record.
     * **CRITICAL FIX:** Added previousReading and currentReading to the signature and queries.
     */
    public static boolean addOrUpdateBill(String meterNumber, String month, int year,
                                          double unitsConsumed, double fixedCharge,
                                          double energyCharge, double fuelSurcharge,
                                          double duty, double totalAmount, Date dueDate,
                                          int previousReading, int currentReading) throws SQLException {
        
        String checkSql = "SELECT bill_id FROM bills WHERE meter_number=? AND month=? AND year=?";
        
        try (Connection con = getConnection(); PreparedStatement check = con.prepareStatement(checkSql)) {
            check.setString(1, meterNumber);
            check.setString(2, month);
            check.setInt(3, year);
            ResultSet rs = check.executeQuery();
            
            if (rs.next()) {
                // UPDATE BILL
                int billId = rs.getInt("bill_id");
                try (PreparedStatement update = con.prepareStatement(
                        "UPDATE bills SET units_consumed=?, fixed_charge=?, energy_charge=?, fuel_surcharge=?, duty=?, total_amount=?, due_date=?, previous_reading=?, current_reading=? WHERE bill_id=?")) {
                    update.setDouble(1, unitsConsumed);
                    update.setDouble(2, fixedCharge);
                    update.setDouble(3, energyCharge);
                    update.setDouble(4, fuelSurcharge);
                    update.setDouble(5, duty);
                    update.setDouble(6, totalAmount);
                    update.setDate(7, dueDate);
                    update.setInt(8, previousReading);   // Corrected Parameter
                    update.setInt(9, currentReading);    // Corrected Parameter
                    update.setInt(10, billId);
                    return update.executeUpdate() > 0;
                }
            } else {
                // INSERT NEW BILL
                try (PreparedStatement insert = con.prepareStatement(
                        "INSERT INTO bills(meter_number, month, year, units_consumed, fixed_charge, energy_charge, fuel_surcharge, duty, total_amount, due_date, status, previous_reading, current_reading) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                    insert.setString(1, meterNumber);
                    insert.setString(2, month);
                    insert.setInt(3, year);
                    insert.setDouble(4, unitsConsumed);
                    insert.setDouble(5, fixedCharge);
                    insert.setDouble(6, energyCharge);
                    insert.setDouble(7, fuelSurcharge);
                    insert.setDouble(8, duty);
                    insert.setDouble(9, totalAmount);
                    insert.setDate(10, dueDate);
                    insert.setString(11, "Unpaid");
                    insert.setInt(12, previousReading);  // Corrected Parameter
                    insert.setInt(13, currentReading);   // Corrected Parameter
                    return insert.executeUpdate() > 0;
                }
            }
        }
    }
    
    /**
     * Updates the meter table with the new reading.
     */
    public static boolean updateMeterReadings(String meterNumber, int prevReading, int currReading) throws SQLException {
        String query = "UPDATE meter SET previous_reading = ?, current_reading = ?, reading_date = CURDATE() WHERE meter_no = ?";
        
        try (Connection con = getConnection(); 
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, prevReading);
            ps.setInt(2, currReading);
            ps.setString(3, meterNumber);
            return ps.executeUpdate() > 0;
        }
    }
    
    /**
     * Retrieves all bills for a given consumer number, ordered by most recent.
     */
    public static ResultSet getBillsByConsumerNo(String consumerNo) throws SQLException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = con.prepareStatement(
                    "SELECT b.* FROM bills b JOIN meter m ON b.meter_number=m.meter_no WHERE m.consumer_no=? ORDER BY b.year DESC, b.month DESC");
            ps.setString(1, consumerNo);
            return ps.executeQuery();
        } finally {
            // NOTE: Connection is left open for the caller to process the ResultSet.
        }
    }
    
    /**
     * Checks if a bill for a specific meter, month, and year already exists.
     */
    public static boolean isBillExists(String meterNo, String month, int year) throws SQLException {
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM bills WHERE meter_number=? AND month=? AND year=?")) {
            ps.setString(1, meterNo);
            ps.setString(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
   // ...existing code...
public static List<Bill> getPendingBills(String meterNo) throws SQLException {
    List<Bill> list = new ArrayList<>();
    String sql = "SELECT bill_id, meter_number, month, year, units_consumed, fixed_charge, energy_charge, " +
                 "fuel_surcharge, duty, total_amount, due_date, status, previous_reading, current_reading " +
                 "FROM bills WHERE meter_number = ? AND status = 'unpaid' " +
                 "ORDER BY year DESC, FIELD(month, 'January','February','March','April','May','June','July','August','September','October','November','December') DESC";

    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, meterNo);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Bill b = new Bill(
                    rs.getInt("bill_id"),
                    rs.getString("meter_number"),
                    rs.getString("month"),
                    rs.getInt("year"),
                    rs.getDouble("units_consumed"),
                    rs.getDouble("fixed_charge"),
                    rs.getDouble("energy_charge"),
                    rs.getDouble("fuel_surcharge"),
                    rs.getDouble("duty"),
                    rs.getDouble("total_amount"),
                    rs.getDate("due_date"),
                    rs.getString("status"),
                    rs.getInt("previous_reading"),
                    rs.getInt("current_reading")
                );
                list.add(b);
            }
        }
    }
    return list;
}
// ...existing code...
// ...existing code...
    public static boolean payBill(String meterNumber, String month) throws SQLException {
        String query = "UPDATE billS SET status = 'Paid' WHERE meter_number = ? AND month = ? AND status = 'unpaid'";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, meterNumber);
            ps.setString(2, month);
            int updated = ps.executeUpdate();
            return updated > 0;
        }
    }
        // ...existing code...
public static boolean payBillById(int billId) throws SQLException {
    String sql = "UPDATE bills SET status = 'Paid', payment_date = CURDATE() WHERE bill_id = ? AND status <> 'Paid'";
    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, billId);
        return ps.executeUpdate() > 0;
    }
}

// ...existing code...
public static ResultSet getLatestBillForConsumer(String consumerNo) throws SQLException {
    String sql = "SELECT b.* " +
                 "FROM bills b " +
                 "JOIN meter m ON b.meter_number = m.meter_no " +
                 "WHERE m.consumer_no = ? " +
                 "ORDER BY b.year DESC, FIELD(b.month, 'January','February','March','April','May','June','July','August','September','October','November','December') DESC " +
                 "LIMIT 1";
    Connection con = getConnection();
    PreparedStatement ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    ps.setString(1, consumerNo);
    return ps.executeQuery(); // caller must close the ResultSet (and underlying connection stays open until closed)
}
// ...existing code...
// ...existing code...
    // ========================================================================
    // --- USER/LOGIN & CUSTOMER CRUD METHODS ---
    // ========================================================================

    /**
     * Checks if a customer exists by consumer number.
     */
    public static boolean customerExists(String consumerNo) {
        String sql = "SELECT COUNT(*) FROM customers WHERE consumer_no=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, consumerNo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Adds a new customer record.
     */
    public static boolean addCustomer(String name, String consumerNo, String address, String city,
                                      String state, String email, String phone) {
        String sql = "INSERT INTO customers(name, consumer_no, address, city, state, email, phone) VALUES(?,?,?,?,?,?,?)";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, consumerNo);
            ps.setString(3, address);
            ps.setString(4, city);
            ps.setString(5, state);
            ps.setString(6, email);
            ps.setString(7, phone);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Updates customer details.
     */
    public static boolean updateCustomer(String consumerNo, String address, String city, String state, String email, String phone) throws SQLException {
        String query = "UPDATE customers SET address = ?, city = ?, state = ?, email = ?, phone = ? WHERE consumer_no = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, address);
            ps.setString(2, city);
            ps.setString(3, state);
            ps.setString(4, email);
            ps.setString(5, phone);
            ps.setString(6, consumerNo);
            return ps.executeUpdate() > 0;
        }
    }
    
    /**
     * Retrieves customer name by consumer number.
     */
    public static String getCustomerName(String consumerNo) throws SQLException {
        String sql = "SELECT name FROM customers WHERE consumer_no=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, consumerNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("name") : null;
            }
        }
    }

    /**
     * Validates user credentials.
     */
    public static boolean validateUser(String username, String password, String role) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username=? AND password=? AND role=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }
    
    /**
     * Creates a new user (for customer or admin login).
     */
    public static boolean createUser(String username, String password, String role, String name, String consumerNo) throws SQLException {
        String sql = "INSERT INTO users(username, password, role, name, consumer_no) VALUES(?,?,?,?,?)";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setString(4, name);
            ps.setString(5, consumerNo);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves the consumer number associated with a username.
     */
    public static String getConsumerByUsername(String username) throws SQLException {
        String consumerNo = null;
        String query = "SELECT consumer_no FROM users WHERE username = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    consumerNo = rs.getString("consumer_no");
                }
            }
        }
        return consumerNo;
    }
    
    /**
     * Retrieves the meter number associated with a username.
     */
    public static String getMeterByUsername(String username) throws SQLException {
        String sql = "SELECT consumer_no FROM users WHERE username=?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                String consumerNo = rs.next() ? rs.getString("consumer_no") : null;
                if (consumerNo == null) return null;
                
                return getMeterByConsumerNo(consumerNo);
            }
        }
    }
    
    // ========================================================================
    // --- REPORTING & MISCELLANEOUS METHODS ---
    // ========================================================================

    /**
     * Retrieves comprehensive information about customers, meters, and tariffs.
     */
    public static ResultSet getCustomerMeterTariffInfo() throws SQLException {
        Connection con = null;
        Statement st = null;
        try {
            String sql = "SELECT c.*, m.*, t.* FROM customers c " +
                         "JOIN meter m ON c.consumer_no = m.consumer_no " +
                         "LEFT JOIN taxes t ON m.tariff_category = t.tariff_category";
            
            con = getConnection();
            st = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            return st.executeQuery(sql);
        } finally {
             // NOTE: Connection is left open for the caller to process the ResultSet.
        }
    }
     public static String getConsumerNoByMeter(String meterNo) throws SQLException {
    String consumerNo = null;
    String query = "SELECT consumer_no FROM meter WHERE meter_no = ?";
    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(query)) {
        ps.setString(1, meterNo);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                consumerNo = rs.getString("consumer_no");
            }
        }
    }
    return consumerNo;
}

       

/**
 * DEPRECATED: Retained for compatibility with older UI components (like MeterInformation) 
 * that incorrectly call this method to finalize meter creation.
 * It inserts a dummy bill record (usually all zeros) which is not ideal 
 * but ensures the UI flow doesn't break.
 */
public static boolean saveBill(String meterNo, String month, int year, double units,
                               double fixedCharge, double energyCharge, double fuelSurcharge,
                               double duty, double totalAmount) throws SQLException {
    
    // NOTE: This implementation assumes the method is called to finalize a meter record 
    // and inserts a dummy bill.
    
    // 1. Define dummy/default values for the missing parameters required by addOrUpdateBill
    java.sql.Date dummyDueDate = java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(15));
    
    // We pass 0 for readings since this might be the very first dummy record before meter reading starts.
    int previousReading = 0;
    int currentReading = 0;

    // 2. Call the main bill-saving method
    return addOrUpdateBill(
        meterNo, month, year, units, fixedCharge, energyCharge, fuelSurcharge,
        duty, totalAmount, dummyDueDate, 
        previousReading, // Assuming 0
        currentReading   // Assuming 0
    );
}


        // ...existing code...
public static boolean meterExists(String meterNo) throws SQLException {
    String sql = "SELECT 1 FROM meter WHERE meter_no = ?";
    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, meterNo);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next();
        }
    }
}
// ...existing code...

    /**
     * DEPRECATED: This method is replaced by the more robust addOrUpdateBill.
     */
   

    /**
     * Complex method for managing meter details (kept for completeness).
     */
    public static boolean addOrUpdateMeter(
            String meterNo, String consumerNo, String tariffType, String tariffCategory, 
            String meterType, double loadConnected, Date installationDate, String status, 
            String location, int previousReading, int currentReading, double multiplierFactor, 
            Date readingDate, String remarks, String createdBy) throws SQLException {

        if (meterExists(meterNo)) {
            // Case 1: UPDATE existing meter
            String updateSql = "UPDATE meter SET consumer_no=?, tariff_type=?, tariff_category=?, meter_type=?, load_connected=?, installation_date=?, status=?, location=?, previous_reading=?, current_reading=?, multiplier_factor=?, reading_date=?, remarks=?, created_by=? WHERE meter_no=?";
            try (Connection con = getConnection();
                 PreparedStatement pstmt = con.prepareStatement(updateSql)) {
                
                pstmt.setString(1, consumerNo);
                pstmt.setString(2, tariffType);
                pstmt.setString(3, tariffCategory);
                pstmt.setString(4, meterType);
                pstmt.setDouble(5, loadConnected);
                pstmt.setDate(6, installationDate);
                pstmt.setString(7, status);
                pstmt.setString(8, location);
                pstmt.setInt(9, previousReading);
                pstmt.setInt(10, currentReading);
                pstmt.setDouble(11, multiplierFactor);
                pstmt.setDate(12, readingDate);
                pstmt.setString(13, remarks);
                pstmt.setString(14, createdBy);
                pstmt.setString(15, meterNo); // WHERE clause
                
                return pstmt.executeUpdate() > 0;
            }
        } else {
            // Case 2: INSERT new meter
            String insertSql = "INSERT INTO meter (meter_no, consumer_no, tariff_type, tariff_category, meter_type, load_connected, installation_date, status, location, previous_reading, current_reading, multiplier_factor, reading_date, remarks, created_by) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection con = getConnection();
                 PreparedStatement pstmt = con.prepareStatement(insertSql)) {
                
                pstmt.setString(1, meterNo);
                pstmt.setString(2, consumerNo);
                pstmt.setString(3, tariffType);
                pstmt.setString(4, tariffCategory);
                pstmt.setString(5, meterType);
                pstmt.setDouble(6, loadConnected);
                pstmt.setDate(7, installationDate);
                pstmt.setString(8, status);
                pstmt.setString(9, location);
                pstmt.setInt(10, previousReading);
                pstmt.setInt(11, currentReading);
                pstmt.setDouble(12, multiplierFactor);
                pstmt.setDate(13, readingDate);
                pstmt.setString(14, remarks);
                pstmt.setString(15, createdBy);
                
                return pstmt.executeUpdate() > 0;
            }
        }
    }
}