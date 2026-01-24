package md.uzina.server;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class SalaryService {

    /**
     * Set or update hourly rate for a user
     * Creates new entry and closes previous one (keeping history)
     */
    public static boolean setHourlyRate(int userId, BigDecimal hourlyRate, int setByUserId) throws SQLException {
        String closePreviousSql = "UPDATE hourly_rates SET valid_to = CURRENT_TIMESTAMP " +
                                  "WHERE user_id = ? AND valid_to IS NULL";
        
        String insertSql = "INSERT INTO hourly_rates (user_id, hourly_rate, valid_from, created_by) " +
                          "VALUES (?, ?, CURRENT_TIMESTAMP, ?)";
        
        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Close previous rate
                try (PreparedStatement stmt = conn.prepareStatement(closePreviousSql)) {
                    stmt.setInt(1, userId);
                    stmt.executeUpdate();
                }
                
                // Insert new rate
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    stmt.setInt(1, userId);
                    stmt.setBigDecimal(2, hourlyRate);
                    stmt.setInt(3, setByUserId);
                    stmt.executeUpdate();
                }
                
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Get current hourly rate for a user
     */
    public static BigDecimal getCurrentHourlyRate(int userId) throws SQLException {
        String sql = "SELECT hourly_rate FROM hourly_rates " +
                    "WHERE user_id = ? AND valid_to IS NULL " +
                    "ORDER BY valid_from DESC LIMIT 1";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("hourly_rate");
                }
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get hourly rate history for a user
     */
    public static List<HourlyRateHistory> getHourlyRateHistory(int userId) throws SQLException {
        String sql = "SELECT hr.hourly_rate, hr.valid_from, hr.valid_to, u.name as set_by_name " +
                    "FROM hourly_rates hr " +
                    "LEFT JOIN users u ON hr.created_by = u.id " +
                    "WHERE hr.user_id = ? " +
                    "ORDER BY hr.valid_from DESC";
        
        List<HourlyRateHistory> history = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    HourlyRateHistory h = new HourlyRateHistory();
                    h.hourlyRate = rs.getBigDecimal("hourly_rate");
                    h.validFrom = rs.getTimestamp("valid_from");
                    h.validTo = rs.getTimestamp("valid_to");
                    h.setByName = rs.getString("set_by_name");
                    history.add(h);
                }
            }
        }
        return history;
    }

    /**
     * Calculate worked hours for a user in a given month
     */
    public static BigDecimal calculateWorkedHours(int userId, int month, int year) throws SQLException {
        String sql = "SELECT start_time, end_time FROM work_sessions " +
                    "WHERE user_id = ? " +
                    "AND EXTRACT(MONTH FROM work_date) = ? " +
                    "AND EXTRACT(YEAR FROM work_date) = ? " +
                    "AND status = 'COMPLETED' " +
                    "AND start_time IS NOT NULL " +
                    "AND end_time IS NOT NULL";
        
        BigDecimal totalHours = BigDecimal.ZERO;
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, month);
            stmt.setInt(3, year);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp start = rs.getTimestamp("start_time");
                    Timestamp end = rs.getTimestamp("end_time");
                    
                    // Calculate hours with fractions
                    long diffMillis = end.getTime() - start.getTime();
                    BigDecimal hours = BigDecimal.valueOf(diffMillis)
                        .divide(BigDecimal.valueOf(3600000), 2, RoundingMode.HALF_UP); // Convert to hours
                    totalHours = totalHours.add(hours);
                }
            }
        }
        return totalHours;
    }

    /**
     * Calculate salary for previous month for all workers and team leaders
     * Returns number of salaries calculated
     */
    public static int calculateSalariesForPreviousMonth(int calculatedByUserId) throws SQLException {
        LocalDate now = LocalDate.now();
        YearMonth previousMonth = YearMonth.from(now).minusMonths(1);
        int month = previousMonth.getMonthValue();
        int year = previousMonth.getYear();
        
        return calculateSalariesForMonth(month, year, calculatedByUserId);
    }

    /**
     * Calculate salary for specific month for all workers and team leaders
     */
    public static int calculateSalariesForMonth(int month, int year, int calculatedByUserId) throws SQLException {
        String getUsersSql = "SELECT id FROM users WHERE role IN ('WORKER', 'TEAMLEADER')";
        
        int count = 0;
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(getUsersSql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                int userId = rs.getInt("id");
                try {
                    calculateSalaryForUser(userId, month, year, calculatedByUserId);
                    count++;
                } catch (SQLException e) {
                    System.err.println("Error calculating salary for user " + userId + ": " + e.getMessage());
                    // Continue with other users
                }
            }
        }
        return count;
    }

    /**
     * Calculate salary for a specific user and month
     */
    public static void calculateSalaryForUser(int userId, int month, int year, int calculatedByUserId) throws SQLException {
        BigDecimal hoursWorked = calculateWorkedHours(userId, month, year);
        BigDecimal hourlyRate = getCurrentHourlyRate(userId);
        
        if (hourlyRate.compareTo(BigDecimal.ZERO) == 0) {
            throw new SQLException("No hourly rate set for user " + userId);
        }
        
        BigDecimal baseSalary = hoursWorked.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grossSalary = baseSalary; // Will be updated if bonuses exist
        BigDecimal taxAmount = grossSalary.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netSalary = grossSalary.subtract(taxAmount);
        
        String sql = "INSERT INTO salary_calculations " +
                    "(user_id, month, year, hours_worked, hourly_rate, base_salary, bonuses_total, " +
                    "gross_salary, tax_amount, net_salary, is_published) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE) " +
                    "ON CONFLICT (user_id, month, year) DO UPDATE SET " +
                    "hours_worked = EXCLUDED.hours_worked, " +
                    "hourly_rate = EXCLUDED.hourly_rate, " +
                    "base_salary = EXCLUDED.base_salary, " +
                    "gross_salary = EXCLUDED.gross_salary, " +
                    "tax_amount = EXCLUDED.tax_amount, " +
                    "net_salary = EXCLUDED.net_salary, " +
                    "calculated_at = CURRENT_TIMESTAMP";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, month);
            stmt.setInt(3, year);
            stmt.setBigDecimal(4, hoursWorked);
            stmt.setBigDecimal(5, hourlyRate);
            stmt.setBigDecimal(6, baseSalary);
            stmt.setBigDecimal(7, BigDecimal.ZERO);
            stmt.setBigDecimal(8, grossSalary);
            stmt.setBigDecimal(9, taxAmount);
            stmt.setBigDecimal(10, netSalary);
            stmt.executeUpdate();
        }
    }

    /**
     * Add bonus to salary calculation
     */
    public static void addBonus(int salaryCalculationId, BigDecimal amount, String description, int addedByUserId) throws SQLException {
        String insertBonusSql = "INSERT INTO salary_bonuses (salary_calculation_id, amount, description, added_by) " +
                               "VALUES (?, ?, ?, ?)";
        
        String updateSalarySql = "UPDATE salary_calculations SET " +
                                "bonuses_total = bonuses_total + ?, " +
                                "gross_salary = base_salary + bonuses_total + ?, " +
                                "tax_amount = (base_salary + bonuses_total + ?) * 0.15, " +
                                "net_salary = (base_salary + bonuses_total + ?) - ((base_salary + bonuses_total + ?) * 0.15) " +
                                "WHERE id = ?";
        
        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Insert bonus
                try (PreparedStatement stmt = conn.prepareStatement(insertBonusSql)) {
                    stmt.setInt(1, salaryCalculationId);
                    stmt.setBigDecimal(2, amount);
                    stmt.setString(3, description);
                    stmt.setInt(4, addedByUserId);
                    stmt.executeUpdate();
                }
                
                // Update salary totals
                try (PreparedStatement stmt = conn.prepareStatement(updateSalarySql)) {
                    stmt.setBigDecimal(1, amount);
                    stmt.setBigDecimal(2, amount);
                    stmt.setBigDecimal(3, amount);
                    stmt.setBigDecimal(4, amount);
                    stmt.setBigDecimal(5, amount);
                    stmt.setInt(6, salaryCalculationId);
                    stmt.executeUpdate();
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Publish salaries for a specific month (makes them visible to employees)
     */
    public static int publishSalaries(int month, int year, int publishedByUserId) throws SQLException {
        String sql = "UPDATE salary_calculations SET " +
                    "is_published = TRUE, " +
                    "published_at = CURRENT_TIMESTAMP, " +
                    "published_by = ? " +
                    "WHERE month = ? AND year = ? AND is_published = FALSE";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, publishedByUserId);
            stmt.setInt(2, month);
            stmt.setInt(3, year);
            return stmt.executeUpdate();
        }
    }

    /**
     * Get salary calculations for a specific month (HR view - all employees)
     */
    public static List<SalaryCalculation> getSalaryCalculationsForMonth(int month, int year) throws SQLException {
        String sql = "SELECT sc.*, u.name as user_name, u.role as user_role " +
                    "FROM salary_calculations sc " +
                    "JOIN users u ON sc.user_id = u.id " +
                    "WHERE sc.month = ? AND sc.year = ? " +
                    "ORDER BY u.name";
        
        List<SalaryCalculation> calculations = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, month);
            stmt.setInt(2, year);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SalaryCalculation calc = new SalaryCalculation();
                    calc.id = rs.getInt("id");
                    calc.userId = rs.getInt("user_id");
                    calc.userName = rs.getString("user_name");
                    calc.userRole = rs.getString("user_role");
                    calc.month = rs.getInt("month");
                    calc.year = rs.getInt("year");
                    calc.hoursWorked = rs.getBigDecimal("hours_worked");
                    calc.hourlyRate = rs.getBigDecimal("hourly_rate");
                    calc.baseSalary = rs.getBigDecimal("base_salary");
                    calc.bonusesTotal = rs.getBigDecimal("bonuses_total");
                    calc.grossSalary = rs.getBigDecimal("gross_salary");
                    calc.taxAmount = rs.getBigDecimal("tax_amount");
                    calc.netSalary = rs.getBigDecimal("net_salary");
                    calc.isPublished = rs.getBoolean("is_published");
                    calc.publishedAt = rs.getTimestamp("published_at");
                    calc.calculatedAt = rs.getTimestamp("calculated_at");
                    
                    // Load bonuses
                    calc.bonuses = getBonusesForCalculation(calc.id);
                    
                    calculations.add(calc);
                }
            }
        }
        return calculations;
    }

    /**
     * Get salary history for a specific user (Employee view)
     */
    public static List<SalaryCalculation> getSalaryHistoryForUser(int userId) throws SQLException {
        String sql = "SELECT sc.*, u.name as user_name, u.role as user_role " +
                    "FROM salary_calculations sc " +
                    "JOIN users u ON sc.user_id = u.id " +
                    "WHERE sc.user_id = ? AND sc.is_published = TRUE " +
                    "ORDER BY sc.year DESC, sc.month DESC";
        
        List<SalaryCalculation> calculations = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SalaryCalculation calc = new SalaryCalculation();
                    calc.id = rs.getInt("id");
                    calc.userId = rs.getInt("user_id");
                    calc.userName = rs.getString("user_name");
                    calc.userRole = rs.getString("user_role");
                    calc.month = rs.getInt("month");
                    calc.year = rs.getInt("year");
                    calc.hoursWorked = rs.getBigDecimal("hours_worked");
                    calc.hourlyRate = rs.getBigDecimal("hourly_rate");
                    calc.baseSalary = rs.getBigDecimal("base_salary");
                    calc.bonusesTotal = rs.getBigDecimal("bonuses_total");
                    calc.grossSalary = rs.getBigDecimal("gross_salary");
                    calc.taxAmount = rs.getBigDecimal("tax_amount");
                    calc.netSalary = rs.getBigDecimal("net_salary");
                    calc.isPublished = rs.getBoolean("is_published");
                    calc.publishedAt = rs.getTimestamp("published_at");
                    calc.calculatedAt = rs.getTimestamp("calculated_at");
                    
                    // Load bonuses
                    calc.bonuses = getBonusesForCalculation(calc.id);
                    
                    calculations.add(calc);
                }
            }
        }
        return calculations;
    }

    /**
     * Get bonuses for a salary calculation
     */
    private static List<SalaryBonus> getBonusesForCalculation(int salaryCalculationId) throws SQLException {
        String sql = "SELECT sb.*, u.name as added_by_name " +
                    "FROM salary_bonuses sb " +
                    "LEFT JOIN users u ON sb.added_by = u.id " +
                    "WHERE sb.salary_calculation_id = ? " +
                    "ORDER BY sb.added_at";
        
        List<SalaryBonus> bonuses = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, salaryCalculationId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SalaryBonus bonus = new SalaryBonus();
                    bonus.id = rs.getInt("id");
                    bonus.salaryCalculationId = rs.getInt("salary_calculation_id");
                    bonus.amount = rs.getBigDecimal("amount");
                    bonus.description = rs.getString("description");
                    bonus.addedByName = rs.getString("added_by_name");
                    bonus.addedAt = rs.getTimestamp("added_at");
                    bonuses.add(bonus);
                }
            }
        }
        return bonuses;
    }

    /**
     * Get all users with their current hourly rates
     */
    public static List<UserHourlyRate> getAllUsersWithHourlyRates() throws SQLException {
        String sql = "SELECT u.id, u.name, u.role, " +
                    "COALESCE(hr.hourly_rate, 0) as hourly_rate, " +
                    "hr.valid_from " +
                    "FROM users u " +
                    "LEFT JOIN hourly_rates hr ON u.id = hr.user_id AND hr.valid_to IS NULL " +
                    "WHERE u.role IN ('WORKER', 'TEAMLEADER') " +
                    "ORDER BY u.name";
        
        List<UserHourlyRate> users = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                UserHourlyRate uhr = new UserHourlyRate();
                uhr.userId = rs.getInt("id");
                uhr.userName = rs.getString("name");
                uhr.userRole = rs.getString("role");
                uhr.hourlyRate = rs.getBigDecimal("hourly_rate");
                uhr.validFrom = rs.getTimestamp("valid_from");
                users.add(uhr);
            }
        }
        return users;
    }

    // Data classes
    public static class HourlyRateHistory {
        public BigDecimal hourlyRate;
        public Timestamp validFrom;
        public Timestamp validTo;
        public String setByName;
    }

    public static class SalaryCalculation {
        public int id;
        public int userId;
        public String userName;
        public String userRole;
        public int month;
        public int year;
        public BigDecimal hoursWorked;
        public BigDecimal hourlyRate;
        public BigDecimal baseSalary;
        public BigDecimal bonusesTotal;
        public BigDecimal grossSalary;
        public BigDecimal taxAmount;
        public BigDecimal netSalary;
        public boolean isPublished;
        public Timestamp publishedAt;
        public Timestamp calculatedAt;
        public List<SalaryBonus> bonuses;
    }

    public static class SalaryBonus {
        public int id;
        public int salaryCalculationId;
        public BigDecimal amount;
        public String description;
        public String addedByName;
        public Timestamp addedAt;
    }

    public static class UserHourlyRate {
        public int userId;
        public String userName;
        public String userRole;
        public BigDecimal hourlyRate;
        public Timestamp validFrom;
    }
}
