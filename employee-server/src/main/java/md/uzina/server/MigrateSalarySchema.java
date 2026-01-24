package md.uzina.server;

import java.sql.Connection;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Script pentru aplicarea migratiei salary_calculations
 */
public class MigrateSalarySchema {
    public static void main(String[] args) {
        try {
            System.out.println("=== Migrare Schema Salary Calculations ===");
            System.out.println();
            
            // Inițializează conexiunea la baza de date
            DBManager.init();
            System.out.println("Conexiune la baza de date: OK");
            
            // Citește scriptul SQL
            String scriptPath = "sql/migrate_salary_schema.sql";
            String sql = new String(Files.readAllBytes(Paths.get(scriptPath)));
            System.out.println("Script SQL citit: " + scriptPath);
            System.out.println();
            
            // Execută scriptul
            System.out.println("Executare migrare...");
            try (Connection conn = DBManager.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // Execută tot scriptul odată - PostgreSQL suportă multiple statements
                stmt.execute(sql);
                System.out.println("✓ Script executat cu succes");
                
                System.out.println();
                System.out.println("===========================================");
                System.out.println("✓ SUCCES! Schema a fost actualizată!");
                System.out.println("===========================================");
                System.out.println();
                System.out.println("Verifică că noile coloane au fost create:");
                
                // Verifică că coloanele există
                try (var rs = stmt.executeQuery(
                    "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_name = 'salary_calculations' " +
                    "AND column_name IN ('month', 'year', 'bonuses_total', 'gross_salary', 'tax_amount', 'net_salary', 'is_published', 'published_at', 'calculated_at') " +
                    "ORDER BY column_name")) {
                    
                    System.out.println("\nColoane găsite:");
                    while (rs.next()) {
                        System.out.println("  ✓ " + rs.getString("column_name"));
                    }
                }
                
                System.out.println();
                System.out.println("===========================================");
            }
            
        } catch (Exception e) {
            System.err.println("===========================================");
            System.err.println("✗ EROARE la migrare:");
            System.err.println("  " + e.getMessage());
            System.err.println("===========================================");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
