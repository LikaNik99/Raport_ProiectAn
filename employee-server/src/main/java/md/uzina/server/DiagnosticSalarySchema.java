package md.uzina.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Script de diagnostic pentru verificarea schemei bazei de date
 * Identifică ce coloane lipsesc din tabelul salary_calculations
 */
public class DiagnosticSalarySchema {
    
    private static final String[] REQUIRED_COLUMNS = {
        "id", "user_id", "month", "year", "hours_worked", "hourly_rate",
        "base_salary", "bonuses_total", "gross_salary", "tax_amount", 
        "net_salary", "is_published", "published_at", "published_by", "calculated_at"
    };
    
    private static final String[] REQUIRED_TABLES = {
        "salary_calculations", "hourly_rates", "salary_bonuses"
    };
    
    public static void main(String[] args) {
        try {
            System.out.println("════════════════════════════════════════════════════════");
            System.out.println("   DIAGNOSTIC SCHEMA BAZA DE DATE - Management Salariu");
            System.out.println("════════════════════════════════════════════════════════");
            System.out.println();
            
            // Inițializează conexiunea
            DBManager.init();
            System.out.println("✓ Conexiune la baza de date: OK");
            System.out.println();
            
            try (Connection conn = DBManager.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // 1. Verifică tabelele
                System.out.println("─────────────────────────────────────────────────────────");
                System.out.println("1. VERIFICARE TABELE");
                System.out.println("─────────────────────────────────────────────────────────");
                
                boolean allTablesExist = true;
                for (String tableName : REQUIRED_TABLES) {
                    String sql = "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                               "WHERE table_name = '" + tableName + "')";
                    try (ResultSet rs = stmt.executeQuery(sql)) {
                        if (rs.next()) {
                            boolean exists = rs.getBoolean(1);
                            if (exists) {
                                System.out.println("  ✓ Tabelul '" + tableName + "' există");
                            } else {
                                System.out.println("  ✗ LIPSĂ: Tabelul '" + tableName + "' NU există!");
                                allTablesExist = false;
                            }
                        }
                    }
                }
                System.out.println();
                
                // 2. Verifică coloanele din salary_calculations
                System.out.println("─────────────────────────────────────────────────────────");
                System.out.println("2. VERIFICARE COLOANE TABEL 'salary_calculations'");
                System.out.println("─────────────────────────────────────────────────────────");
                
                // Obține toate coloanele existente
                String columnsSql = "SELECT column_name FROM information_schema.columns " +
                                  "WHERE table_name = 'salary_calculations' " +
                                  "ORDER BY ordinal_position";
                
                java.util.Set<String> existingColumns = new java.util.HashSet<>();
                try (ResultSet rs = stmt.executeQuery(columnsSql)) {
                    System.out.println("Coloane existente:");
                    while (rs.next()) {
                        String colName = rs.getString("column_name");
                        existingColumns.add(colName);
                        System.out.println("  • " + colName);
                    }
                }
                System.out.println();
                
                // Verifică coloanele necesare
                System.out.println("Status coloane necesare:");
                boolean allColumnsExist = true;
                java.util.List<String> missingColumns = new java.util.ArrayList<>();
                
                for (String requiredCol : REQUIRED_COLUMNS) {
                    if (existingColumns.contains(requiredCol)) {
                        System.out.println("  ✓ " + requiredCol);
                    } else {
                        System.out.println("  ✗ LIPSĂ: " + requiredCol);
                        allColumnsExist = false;
                        missingColumns.add(requiredCol);
                    }
                }
                System.out.println();
                
                // 3. Rezultat final
                System.out.println("════════════════════════════════════════════════════════");
                System.out.println("   REZULTAT DIAGNOSTIC");
                System.out.println("════════════════════════════════════════════════════════");
                System.out.println();
                
                if (allTablesExist && allColumnsExist) {
                    System.out.println("✓✓✓ PERFECT! Toate tabelele și coloanele există!");
                    System.out.println();
                    System.out.println("Baza de date este configurată corect.");
                    System.out.println("Managementul salariilor ar trebui să funcționeze.");
                } else {
                    System.out.println("✗✗✗ PROBLEMA IDENTIFICATĂ!");
                    System.out.println();
                    
                    if (!allTablesExist) {
                        System.out.println("⚠ Lipsesc tabele necesare!");
                    }
                    
                    if (!allColumnsExist) {
                        System.out.println("⚠ Lipsesc coloane din tabelul 'salary_calculations':");
                        for (String col : missingColumns) {
                            System.out.println("   • " + col);
                        }
                    }
                    
                    System.out.println();
                    System.out.println("════════════════════════════════════════════════════════");
                    System.out.println("   SOLUȚIE");
                    System.out.println("════════════════════════════════════════════════════════");
                    System.out.println();
                    System.out.println("Trebuie să aplici migrarea bazei de date!");
                    System.out.println();
                    System.out.println("Rulează următoarea comandă:");
                    System.out.println();
                    System.out.println("  mvn exec:java \"-Dexec.mainClass=md.uzina.server.MigrateSalarySchema\" \"-Dexec.cleanupDaemonThreads=false\"");
                    System.out.println();
                    System.out.println("Sau folosește:");
                    System.out.println("  .\\update-server.bat");
                    System.out.println();
                }
                
                System.out.println("════════════════════════════════════════════════════════");
                
                // Exit code
                if (allTablesExist && allColumnsExist) {
                    System.exit(0);
                } else {
                    System.exit(1);
                }
            }
            
        } catch (Exception e) {
            System.err.println();
            System.err.println("════════════════════════════════════════════════════════");
            System.err.println("   EROARE LA DIAGNOSTIC");
            System.err.println("════════════════════════════════════════════════════════");
            System.err.println();
            System.err.println("Detalii eroare:");
            System.err.println("  " + e.getMessage());
            System.err.println();
            
            if (e.getMessage() != null && e.getMessage().contains("relation") && e.getMessage().contains("does not exist")) {
                System.err.println("⚠ Tabelul 'salary_calculations' nu există deloc!");
                System.err.println();
                System.err.println("Soluție:");
                System.err.println("1. Aplică schema inițială: sql/schema.sql");
                System.err.println("2. Apoi aplică migrarea: MigrateSalarySchema");
            } else if (e.getMessage() != null && e.getMessage().contains("Connection")) {
                System.err.println("⚠ Nu se poate conecta la baza de date!");
                System.err.println();
                System.err.println("Verifică:");
                System.err.println("1. PostgreSQL rulează?");
                System.err.println("2. Credențialele sunt corecte?");
                System.err.println("3. Baza de date 'logare_db' există?");
            }
            
            System.err.println();
            System.err.println("════════════════════════════════════════════════════════");
            e.printStackTrace();
            System.exit(2);
        }
    }
}
