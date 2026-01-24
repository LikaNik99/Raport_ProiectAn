package md.uzina.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class HRPanel {
    private final JFrame frame;
    private final ClientApp.ClientUser user;
    private final Runnable onLogout;
    private final NetClient netClient;
    private JsonArray lastLoginHistoryData; // Store data for Excel export

    public HRPanel(ClientApp.ClientUser user, Runnable onLogout, NetClient netClient) {
        this.user = user;
        this.onLogout = onLogout;
        this.netClient = netClient;
        this.frame = new JFrame("Panou HR - " + user.name);
    }

    public void show() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        // Header: greeting + logout
        JPanel header = new JPanel();
        header.add(new JLabel("Bine ai venit, " + user.name + " (" + user.role + ")"));
        JButton logoutBtn = new JButton("Deconectare");
        logoutBtn.addActionListener(e -> {
            frame.dispose();
            onLogout.run();
        });
        header.add(logoutBtn);
        frame.add(header, BorderLayout.NORTH);

        // Main content: HR features
        JPanel content = new JPanel(new BorderLayout());
        JTextArea info = new JTextArea();
        info.setText(
            "Funcționalități HR:\n\n" +
            "1. Statistici Globale - Vezi prezența și metrici de performanță\n" +
            "2. Gestionare Cereri Concediu - Revizuiește și aprobă/respinge cererile\n" +
            "3. Absențe Nemotivate - Urmărește și gestionează absențele\n" +
            "4. Istoric Autentificări - Vezi log-urile de sistem\n" +
            "5. Editare Roluri - Modifică rolurile utilizatorilor\n" +
            "6. Rapoarte - Generează și exportă rapoarte (grafice, analize)\n\n" +
            "ID Utilizator: " + user.id + "\n" +
            "Rol: " + user.role
        );
        info.setEditable(false);
        content.add(new JScrollPane(info), BorderLayout.CENTER);
        frame.add(content, BorderLayout.CENTER);

        // Buttons for features - use grid layout for better space usage
        JPanel buttons = new JPanel(new GridLayout(0, 3, 5, 5));
        buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JButton globalStats = new JButton("Statistici Globale");
        globalStats.addActionListener(e -> showGlobalStatistics());
        buttons.add(globalStats);
        
        JButton leaveRequests = new JButton("Gestionare Concedii");
        leaveRequests.addActionListener(e -> showLeaveRequests());
        buttons.add(leaveRequests);
        
        JButton loginHistory = new JButton("Istoric Autentificări");
        loginHistory.addActionListener(e -> showLoginHistory());
        buttons.add(loginHistory);
        
        JButton empManage = new JButton("Gestionare Angajați");
        empManage.addActionListener(e -> showEmployeeManagement());
        buttons.add(empManage);
        
        JButton viewWorkStatus = new JButton("Status Lucru - Toți");
        viewWorkStatus.addActionListener(e -> viewAllWorkStatus());
        buttons.add(viewWorkStatus);
        
        JButton salaryManagement = new JButton("Gestionare Salarii");
        salaryManagement.addActionListener(e -> showSalaryManagement());
        buttons.add(salaryManagement);
        
        buttons.add(new JButton("Vezi Absențe Nemotivate"));
        buttons.add(new JButton("Generează Rapoarte"));
        
        JButton getWorkHours = new JButton("Vezi Ore Lucru");
        getWorkHours.addActionListener(e -> {
            try {
                netClient.setMessageHandler((type, payload) -> {
                    if ("work_hours_config_response".equals(type) && payload != null && payload.isJsonObject()) {
                        com.google.gson.JsonObject jo = payload.getAsJsonObject();
                        int sh = jo.has("startHour") ? jo.get("startHour").getAsInt() : 8;
                        int sm = jo.has("startMinute") ? jo.get("startMinute").getAsInt() : 0;
                        int eh = jo.has("endHour") ? jo.get("endHour").getAsInt() : 17;
                        int em = jo.has("endMinute") ? jo.get("endMinute").getAsInt() : 0;
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            javax.swing.JOptionPane.showMessageDialog(frame, "Ore de lucru: " + String.format("%02d:%02d - %02d:%02d", sh, sm, eh, em));
                        });
                    }
                });
                netClient.send("get_work_hours_config", new com.google.gson.JsonObject());
            } catch (Exception ex) { javax.swing.JOptionPane.showMessageDialog(frame, "Eroare: " + ex.getMessage()); }
        });
        buttons.add(getWorkHours);
        frame.add(buttons, BorderLayout.SOUTH);

        frame.setSize(new Dimension(800, 500));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void showGlobalStatistics() {
        JFrame statsFrame = new JFrame("Statistici Globale");
        statsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        statsFrame.setLayout(new BorderLayout(10, 10));

        // Tabbed pane for different views
        javax.swing.JTabbedPane tabbedPane = new javax.swing.JTabbedPane();
        
        // Tab 1: Statistici Generale (Text)
        JPanel textStatsPanel = createTextStatsPanel();
        tabbedPane.addTab("Date Tabelar", textStatsPanel);
        
        // Tab 2: Grafice
        JPanel chartsPanel = createChartsPanel();
        tabbedPane.addTab("Grafice", chartsPanel);
        
        statsFrame.add(tabbedPane, BorderLayout.CENTER);

        statsFrame.setSize(new Dimension(1200, 700));
        statsFrame.setLocationRelativeTo(null);
        statsFrame.setVisible(true);
    }
    
    private JPanel createTextStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Date range inputs
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        datePanel.add(new JLabel("De la:"));
        JTextField fromField = new JTextField(12);
        fromField.setText(LocalDate.now().minusDays(30).toString());
        datePanel.add(fromField);
        datePanel.add(new JLabel("Până la:"));
        JTextField toField = new JTextField(12);
        toField.setText(LocalDate.now().toString());
        datePanel.add(toField);
        panel.add(datePanel, BorderLayout.NORTH);

        // Result area
        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        // Fetch button
        JPanel actionPanel = new JPanel();
        JButton fetchBtn = new JButton("Încarcă Statistici");
        fetchBtn.addActionListener(e -> {
            try {
                String from = fromField.getText().trim();
                String to = toField.getText().trim();
                
                netClient.setMessageHandler((type, payload) -> {
                    if ("hr_daily_stats_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("error")) {
                            resultArea.setText("Eroare: " + jo.get("error").getAsString());
                        } else if (jo.has("stats")) {
                            try {
                                JsonArray stats = JsonParser.parseString(jo.get("stats").getAsString()).getAsJsonArray();
                                StringBuilder sb = new StringBuilder();
                                sb.append("Data\t\tTotal\tPrezenți\tConcediu\tAbsenți\n");
                                sb.append("==========\t=====\t========\t========\t=======\n");
                                for (int i = 0; i < stats.size(); i++) {
                                    JsonObject stat = stats.get(i).getAsJsonObject();
                                    sb.append(stat.get("date").getAsString()).append("\t")
                                      .append(stat.get("totalUsers").getAsInt()).append("\t")
                                      .append(stat.get("presentCount").getAsInt()).append("\t\t")
                                      .append(stat.get("leaveCount").getAsInt()).append("\t\t")
                                      .append(stat.get("absentExcused").getAsInt() + stat.get("absentUnexcused").getAsInt()).append("\n");
                                }
                                resultArea.setText(sb.toString());
                            } catch (Exception ex) {
                                resultArea.setText("Eroare la parsarea datelor: " + ex.getMessage());
                            }
                        }
                    }
                });
                
                JsonObject req = new JsonObject();
                req.addProperty("from", from);
                req.addProperty("to", to);
                netClient.send("get_hr_daily_stats", req);
            } catch (Exception ex) {
                resultArea.setText("Eroare: " + ex.getMessage());
            }
        });
        actionPanel.add(fetchBtn);
        panel.add(actionPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createChartsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(new JLabel("Perioada:"));
        JTextField fromField = new JTextField(12);
        fromField.setText(LocalDate.now().minusDays(30).toString());
        controlPanel.add(fromField);
        controlPanel.add(new JLabel(" - "));
        JTextField toField = new JTextField(12);
        toField.setText(LocalDate.now().toString());
        controlPanel.add(toField);
        JButton loadBtn = new JButton("Încarcă Date");
        controlPanel.add(loadBtn);
        panel.add(controlPanel, BorderLayout.NORTH);
        
        // Charts container
        JPanel chartsContainer = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.add(chartsContainer, BorderLayout.CENTER);
        
        // Initialize empty charts
        java.util.concurrent.atomic.AtomicReference<JsonArray> statsData = 
            new java.util.concurrent.atomic.AtomicReference<>(new JsonArray());
        
        loadBtn.addActionListener(e -> {
            try {
                String from = fromField.getText().trim();
                String to = toField.getText().trim();
                
                netClient.setMessageHandler((type, payload) -> {
                    if ("hr_daily_stats_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("error")) {
                            javax.swing.JOptionPane.showMessageDialog(panel, "Eroare: " + jo.get("error").getAsString());
                        } else if (jo.has("stats")) {
                            try {
                                JsonArray stats = JsonParser.parseString(jo.get("stats").getAsString()).getAsJsonArray();
                                statsData.set(stats);
                                
                                // Update charts
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    chartsContainer.removeAll();
                                    
                                    // Chart 1: Prezență vs Total
                                    chartsContainer.add(createAttendanceChart(stats));
                                    
                                    // Chart 2: Distribuție Status
                                    chartsContainer.add(createStatusPieChart(stats));
                                    
                                    // Chart 3: Tendință Prezență
                                    chartsContainer.add(createTrendChart(stats));
                                    
                                    // Chart 4: Statistici Medii
                                    chartsContainer.add(createAverageStatsPanel(stats));
                                    
                                    chartsContainer.revalidate();
                                    chartsContainer.repaint();
                                });
                            } catch (Exception ex) {
                                javax.swing.JOptionPane.showMessageDialog(panel, "Eroare: " + ex.getMessage());
                            }
                        }
                    }
                });
                
                JsonObject req = new JsonObject();
                req.addProperty("from", from);
                req.addProperty("to", to);
                netClient.send("get_hr_daily_stats", req);
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(panel, "Eroare: " + ex.getMessage());
            }
        });
        
        // Load initial data
        loadBtn.doClick();
        
        return panel;
    }
    
    private JPanel createAttendanceChart(JsonArray stats) {
        return new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                int margin = 60;
                int chartWidth = width - 2 * margin;
                int chartHeight = height - 2 * margin;
                
                // Background
                g2.setColor(java.awt.Color.WHITE);
                g2.fillRect(0, 0, width, height);
                
                // Title
                g2.setColor(java.awt.Color.BLACK);
                g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
                g2.drawString("Angajați Prezenți vs Total", 10, 20);
                
                if (stats.size() == 0) {
                    g2.drawString("Nu există date", width/2 - 50, height/2);
                    return;
                }
                
                // Find max value
                int maxTotal = 0;
                for (int i = 0; i < stats.size(); i++) {
                    JsonObject stat = stats.get(i).getAsJsonObject();
                    int total = stat.get("totalUsers").getAsInt();
                    if (total > maxTotal) maxTotal = total;
                }
                
                // Draw axes
                g2.setColor(java.awt.Color.GRAY);
                g2.drawLine(margin, margin, margin, height - margin); // Y-axis
                g2.drawLine(margin, height - margin, width - margin, height - margin); // X-axis
                
                // Draw bars
                int barWidth = Math.max(5, chartWidth / (stats.size() * 2 + 1));
                int gap = barWidth / 2;
                
                for (int i = 0; i < Math.min(stats.size(), 30); i++) {
                    JsonObject stat = stats.get(i).getAsJsonObject();
                    int total = stat.get("totalUsers").getAsInt();
                    int present = stat.get("presentCount").getAsInt();
                    
                    int x = margin + gap + i * (barWidth * 2 + gap);
                    int totalHeight = (int)((double)total / maxTotal * chartHeight);
                    int presentHeight = (int)((double)present / maxTotal * chartHeight);
                    
                    // Total bar (light blue)
                    g2.setColor(new java.awt.Color(173, 216, 230));
                    g2.fillRect(x, height - margin - totalHeight, barWidth, totalHeight);
                    
                    // Present bar (dark green)
                    g2.setColor(new java.awt.Color(34, 139, 34));
                    g2.fillRect(x + barWidth, height - margin - presentHeight, barWidth, presentHeight);
                }
                
                // Legend
                g2.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
                g2.setColor(new java.awt.Color(173, 216, 230));
                g2.fillRect(width - 150, 30, 15, 15);
                g2.setColor(java.awt.Color.BLACK);
                g2.drawString("Total Angajați", width - 130, 42);
                
                g2.setColor(new java.awt.Color(34, 139, 34));
                g2.fillRect(width - 150, 50, 15, 15);
                g2.setColor(java.awt.Color.BLACK);
                g2.drawString("Prezenți", width - 130, 62);
            }
        };
    }
    
    private JPanel createStatusPieChart(JsonArray stats) {
        return new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                
                // Background
                g2.setColor(java.awt.Color.WHITE);
                g2.fillRect(0, 0, width, height);
                
                // Title
                g2.setColor(java.awt.Color.BLACK);
                g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
                g2.drawString("Distribuție Status Angajați", 10, 20);
                
                if (stats.size() == 0) {
                    g2.drawString("Nu există date", width/2 - 50, height/2);
                    return;
                }
                
                // Calculate totals
                int totalPresent = 0, totalLeave = 0, totalAbsent = 0;
                for (int i = 0; i < stats.size(); i++) {
                    JsonObject stat = stats.get(i).getAsJsonObject();
                    totalPresent += stat.get("presentCount").getAsInt();
                    totalLeave += stat.get("leaveCount").getAsInt();
                    totalAbsent += stat.get("absentExcused").getAsInt() + stat.get("absentUnexcused").getAsInt();
                }
                
                int total = totalPresent + totalLeave + totalAbsent;
                if (total == 0) {
                    g2.drawString("Nu există date", width/2 - 50, height/2);
                    return;
                }
                
                // Draw pie chart
                int diameter = Math.min(width, height) - 100;
                int x = (width - diameter) / 2;
                int y = (height - diameter) / 2 + 20;
                
                int startAngle = 0;
                
                // Present (green)
                int presentAngle = (int)(360.0 * totalPresent / total);
                g2.setColor(new java.awt.Color(34, 139, 34));
                g2.fillArc(x, y, diameter, diameter, startAngle, presentAngle);
                startAngle += presentAngle;
                
                // Leave (yellow)
                int leaveAngle = (int)(360.0 * totalLeave / total);
                g2.setColor(new java.awt.Color(255, 215, 0));
                g2.fillArc(x, y, diameter, diameter, startAngle, leaveAngle);
                startAngle += leaveAngle;
                
                // Absent (red)
                int absentAngle = 360 - presentAngle - leaveAngle;
                g2.setColor(new java.awt.Color(220, 20, 60));
                g2.fillArc(x, y, diameter, diameter, startAngle, absentAngle);
                
                // Legend
                g2.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
                int legendX = 10;
                int legendY = height - 80;
                
                g2.setColor(new java.awt.Color(34, 139, 34));
                g2.fillRect(legendX, legendY, 15, 15);
                g2.setColor(java.awt.Color.BLACK);
                g2.drawString(String.format("Prezenți: %d (%.1f%%)", totalPresent, 100.0*totalPresent/total), legendX + 20, legendY + 12);
                
                g2.setColor(new java.awt.Color(255, 215, 0));
                g2.fillRect(legendX, legendY + 20, 15, 15);
                g2.setColor(java.awt.Color.BLACK);
                g2.drawString(String.format("Concediu: %d (%.1f%%)", totalLeave, 100.0*totalLeave/total), legendX + 20, legendY + 32);
                
                g2.setColor(new java.awt.Color(220, 20, 60));
                g2.fillRect(legendX, legendY + 40, 15, 15);
                g2.setColor(java.awt.Color.BLACK);
                g2.drawString(String.format("Absenți: %d (%.1f%%)", totalAbsent, 100.0*totalAbsent/total), legendX + 20, legendY + 52);
            }
        };
    }
    
    private JPanel createTrendChart(JsonArray stats) {
        return new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                int margin = 60;
                int chartWidth = width - 2 * margin;
                int chartHeight = height - 2 * margin;
                
                // Background
                g2.setColor(java.awt.Color.WHITE);
                g2.fillRect(0, 0, width, height);
                
                // Title
                g2.setColor(java.awt.Color.BLACK);
                g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
                g2.drawString("Tendință Prezență (%)", 10, 20);
                
                if (stats.size() < 2) {
                    g2.drawString("Insuficiente date pentru tendință", width/2 - 100, height/2);
                    return;
                }
                
                // Draw axes
                g2.setColor(java.awt.Color.GRAY);
                g2.drawLine(margin, margin, margin, height - margin); // Y-axis
                g2.drawLine(margin, height - margin, width - margin, height - margin); // X-axis
                
                // Calculate percentages
                double[] percentages = new double[stats.size()];
                for (int i = 0; i < stats.size(); i++) {
                    JsonObject stat = stats.get(i).getAsJsonObject();
                    int total = stat.get("totalUsers").getAsInt();
                    int present = stat.get("presentCount").getAsInt();
                    percentages[i] = total > 0 ? (100.0 * present / total) : 0;
                }
                
                // Draw line
                g2.setColor(new java.awt.Color(0, 102, 204));
                g2.setStroke(new java.awt.BasicStroke(2));
                
                int pointsToShow = Math.min(stats.size(), 30);
                for (int i = 0; i < pointsToShow - 1; i++) {
                    int x1 = margin + (i * chartWidth / pointsToShow);
                    int y1 = height - margin - (int)(percentages[i] * chartHeight / 100);
                    int x2 = margin + ((i + 1) * chartWidth / pointsToShow);
                    int y2 = height - margin - (int)(percentages[i + 1] * chartHeight / 100);
                    
                    g2.drawLine(x1, y1, x2, y2);
                    
                    // Draw points
                    g2.fillOval(x1 - 3, y1 - 3, 6, 6);
                }
                
                // Draw last point
                int lastIdx = pointsToShow - 1;
                int lastX = margin + (lastIdx * chartWidth / pointsToShow);
                int lastY = height - margin - (int)(percentages[lastIdx] * chartHeight / 100);
                g2.fillOval(lastX - 3, lastY - 3, 6, 6);
                
                // Y-axis labels
                g2.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 10));
                for (int i = 0; i <= 10; i++) {
                    int y = height - margin - (i * chartHeight / 10);
                    g2.drawString((i * 10) + "%", margin - 35, y + 5);
                    g2.setColor(java.awt.Color.LIGHT_GRAY);
                    g2.drawLine(margin, y, width - margin, y);
                    g2.setColor(new java.awt.Color(0, 102, 204));
                }
            }
        };
    }
    
    private JPanel createAverageStatsPanel(JsonArray stats) {
        JPanel panel = new JPanel();
        panel.setLayout(new java.awt.GridLayout(0, 1, 5, 10));
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createLineBorder(java.awt.Color.GRAY),
            "Statistici Medii",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new java.awt.Font("Arial", java.awt.Font.BOLD, 14)
        ));
        panel.setBackground(java.awt.Color.WHITE);
        
        if (stats.size() == 0) {
            panel.add(new JLabel("Nu există date"));
            return panel;
        }
        
        // Calculate averages
        int totalUsers = 0, totalPresent = 0, totalLeave = 0, totalAbsent = 0;
        for (int i = 0; i < stats.size(); i++) {
            JsonObject stat = stats.get(i).getAsJsonObject();
            totalUsers += stat.get("totalUsers").getAsInt();
            totalPresent += stat.get("presentCount").getAsInt();
            totalLeave += stat.get("leaveCount").getAsInt();
            totalAbsent += stat.get("absentExcused").getAsInt() + stat.get("absentUnexcused").getAsInt();
        }
        
        int days = stats.size();
        double avgPresent = (double)totalPresent / days;
        double avgLeave = (double)totalLeave / days;
        double avgAbsent = (double)totalAbsent / days;
        double avgAttendanceRate = totalUsers > 0 ? (100.0 * totalPresent / (totalPresent + totalAbsent)) : 0;
        
        panel.add(createStatLabel("Număr zile analizate:", String.valueOf(days)));
        panel.add(createStatLabel("Medie angajați prezenți:", String.format("%.1f", avgPresent)));
        panel.add(createStatLabel("Medie în concediu:", String.format("%.1f", avgLeave)));
        panel.add(createStatLabel("Medie absenți:", String.format("%.1f", avgAbsent)));
        panel.add(createStatLabel("Rată medie prezență:", String.format("%.1f%%", avgAttendanceRate)));
        
        return panel;
    }
    
    private JLabel createStatLabel(String label, String value) {
        JLabel lbl = new JLabel(String.format("<html><b>%s</b> %s</html>", label, value));
        lbl.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 13));
        lbl.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return lbl;
    }

    // Helper method to sort table by column
    private void sortTableByColumn(javax.swing.table.DefaultTableModel model, int column, boolean ascending) {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object[] row = new Object[model.getColumnCount()];
            for (int j = 0; j < model.getColumnCount(); j++) {
                row[j] = model.getValueAt(i, j);
            }
            rows.add(row);
        }
        
        rows.sort((row1, row2) -> {
            Object val1 = row1[column];
            Object val2 = row2[column];
            
            int comparison = 0;
            if (val1 instanceof Integer && val2 instanceof Integer) {
                comparison = ((Integer) val1).compareTo((Integer) val2);
            } else if (val1 != null && val2 != null) {
                comparison = val1.toString().compareTo(val2.toString());
            } else if (val1 == null) {
                comparison = -1;
            } else {
                comparison = 1;
            }
            
            return ascending ? comparison : -comparison;
        });
        
        model.setRowCount(0);
        for (Object[] row : rows) {
            model.addRow(row);
        }
    }

    private void showLeaveRequests() {
        JFrame leaveFrame = new JFrame("Gestionare Cereri de Concediu");
        leaveFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        leaveFrame.setLayout(new BorderLayout(8, 8));

        // Tabel pentru cereri de concediu
        String[] columnNames = {"ID", "ID Angajat", "Angajat", "De la", "Până la", "Motiv", "Status"};
        javax.swing.table.DefaultTableModel tableModel = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        javax.swing.JTable leaveTable = new javax.swing.JTable(tableModel);
        leaveTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        leaveTable.getTableHeader().setReorderingAllowed(false);
        
        // Add sorting on column header click
        leaveTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            private int lastSortColumn = -1;
            private boolean ascending = true;
            
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int column = leaveTable.columnAtPoint(e.getPoint());
                if (column >= 0) {
                    String[] options = {"Crescător", "Descrescător", "Anulează"};
                    int choice = javax.swing.JOptionPane.showOptionDialog(leaveFrame, 
                        "Sortează după " + leaveTable.getColumnName(column),
                        "Opțiuni Sortare",
                        javax.swing.JOptionPane.DEFAULT_OPTION,
                        javax.swing.JOptionPane.QUESTION_MESSAGE,
                        null, options, options[0]);
                    
                    if (choice == 0 || choice == 1) {
                        ascending = (choice == 0);
                        lastSortColumn = column;
                        sortTableByColumn(tableModel, column, ascending);
                    }
                }
            }
        });
        
        // Custom renderer for status column with colors
        leaveTable.getColumnModel().getColumn(6).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value != null ? value.toString() : "";
                if ("PENDING".equals(status) || "Asteptare".equals(status)) {
                    cell.setBackground(java.awt.Color.YELLOW);
                    cell.setForeground(java.awt.Color.BLACK);
                } else if ("APPROVED".equals(status) || "Acceptat".equals(status)) {
                    cell.setBackground(java.awt.Color.GREEN);
                    cell.setForeground(java.awt.Color.BLACK);
                } else if ("REJECTED".equals(status) || "Refuzat".equals(status)) {
                    cell.setBackground(java.awt.Color.RED);
                    cell.setForeground(java.awt.Color.WHITE);
                }
                if (isSelected) {
                    cell.setBackground(table.getSelectionBackground());
                    cell.setForeground(table.getSelectionForeground());
                }
                return cell;
            }
        });
        
        JScrollPane tableScroll = new JScrollPane(leaveTable);
        leaveFrame.add(tableScroll, BorderLayout.CENTER);

        // Store all data for filtering
        java.util.concurrent.atomic.AtomicReference<JsonArray> allRequestsData = 
            new java.util.concurrent.atomic.AtomicReference<>(new JsonArray());

        // Filter panel (top)
        JPanel filterPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        
        // Selector dată început
        filterPanel.add(new JLabel("Filtrare - De la data:"));
        javax.swing.JTextField filterDateFrom = new javax.swing.JTextField(10);
        filterDateFrom.setEditable(false);
        JButton dateFromBtn = new JButton("📅");
        dateFromBtn.addActionListener(e -> {
            javax.swing.JSpinner dateSpinner = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
            javax.swing.JSpinner.DateEditor editor = new javax.swing.JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
            dateSpinner.setEditor(editor);
            int result = javax.swing.JOptionPane.showConfirmDialog(leaveFrame, dateSpinner, "Selectează data început", 
                javax.swing.JOptionPane.OK_CANCEL_OPTION);
            if (result == javax.swing.JOptionPane.OK_OPTION) {
                java.util.Date date = (java.util.Date) dateSpinner.getValue();
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                filterDateFrom.setText(sdf.format(date));
            }
        });
        filterPanel.add(filterDateFrom);
        filterPanel.add(dateFromBtn);
        
        // Selector dată sfârșit
        filterPanel.add(new JLabel("Până la data:"));
        javax.swing.JTextField filterDateTo = new javax.swing.JTextField(10);
        filterDateTo.setEditable(false);
        JButton dateToBtn = new JButton("📅");
        dateToBtn.addActionListener(e -> {
            javax.swing.JSpinner dateSpinner = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
            javax.swing.JSpinner.DateEditor editor = new javax.swing.JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
            dateSpinner.setEditor(editor);
            int result = javax.swing.JOptionPane.showConfirmDialog(leaveFrame, dateSpinner, "Selectează data sfârșit", 
                javax.swing.JOptionPane.OK_CANCEL_OPTION);
            if (result == javax.swing.JOptionPane.OK_OPTION) {
                java.util.Date date = (java.util.Date) dateSpinner.getValue();
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                filterDateTo.setText(sdf.format(date));
            }
        });
        filterPanel.add(filterDateTo);
        filterPanel.add(dateToBtn);
        
        // Filtru ID angajat
        filterPanel.add(new JLabel("ID Angajat:"));
        javax.swing.JTextField filterWorker = new javax.swing.JTextField(10);
        filterPanel.add(filterWorker);
        
        // Dropdown status
        filterPanel.add(new JLabel("Status:"));
        String[] statusOptions = {"Toate", "Asteptare", "Acceptat", "Refuzat"};
        javax.swing.JComboBox<String> filterStatus = new javax.swing.JComboBox<>(statusOptions);
        filterPanel.add(filterStatus);
        
        JButton applyFilterBtn = new JButton("Aplică Filtre");
        filterPanel.add(applyFilterBtn);
        
        JButton clearFilterBtn = new JButton("Curăță Filtre & Sortare");
        filterPanel.add(clearFilterBtn);
        
        leaveFrame.add(filterPanel, BorderLayout.NORTH);

        // Panou acțiuni (jos)
        JPanel actionPanel = new JPanel();
        JButton fetchBtn = new JButton("Încarcă Toate Cererile");
        JButton approveBtn = new JButton("Acceptă Cererea");
        JButton rejectBtn = new JButton("Respinge Cererea");
        
        approveBtn.setEnabled(false);
        rejectBtn.setEnabled(false);
        
        // Enable/disable buttons based on selection
        leaveTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = leaveTable.getSelectedRow() >= 0;
            approveBtn.setEnabled(hasSelection);
            rejectBtn.setEnabled(hasSelection);
        });
        
        // Helper method to translate status
        java.util.function.Function<String, String> translateStatus = (status) -> {
            switch (status) {
                case "PENDING": return "Asteptare";
                case "APPROVED": return "Acceptat";
                case "REJECTED": return "Refuzat";
                default: return status;
            }
        };
        
        // Helper method to populate table
        java.util.function.Consumer<JsonArray> populateTable = (records) -> {
            tableModel.setRowCount(0);
            for (int i = 0; i < records.size(); i++) {
                JsonObject rec = records.get(i).getAsJsonObject();
                String statusOriginal = rec.get("status").getAsString();
                String statusTranslated = translateStatus.apply(statusOriginal);
                tableModel.addRow(new Object[]{
                    rec.get("id").getAsInt(),
                    rec.get("userId").getAsInt(),
                    rec.get("workerName").getAsString(),
                    rec.get("dateFrom").getAsString(),
                    rec.get("dateTo").getAsString(),
                    rec.get("reason").getAsString(),
                    statusTranslated
                });
            }
        };
        
        // Fetch button
        fetchBtn.addActionListener(e -> {
            try {
                netClient.setMessageHandler((type, payload) -> {
                    if ("all_leave_requests_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("error")) {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                javax.swing.JOptionPane.showMessageDialog(leaveFrame, "Eroare: " + jo.get("error").getAsString());
                            });
                        } else if (jo.has("requests")) {
                            try {
                                JsonArray records = JsonParser.parseString(jo.get("requests").getAsString()).getAsJsonArray();
                                allRequestsData.set(records);
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    populateTable.accept(records);
                                });
                            } catch (Exception ex) {
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    javax.swing.JOptionPane.showMessageDialog(leaveFrame, "Eroare la prelucrarea datelor: " + ex.getMessage());
                                });
                            }
                        }
                    }
                });
                
                netClient.send("get_all_leave_requests", new JsonObject());
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(leaveFrame, "Error: " + ex.getMessage());
            }
        });
        
        // Apply filter
        applyFilterBtn.addActionListener(e -> {
            try {
                JsonArray allData = allRequestsData.get();
                if (allData == null || allData.size() == 0) {
                    javax.swing.JOptionPane.showMessageDialog(leaveFrame, "Vă rugăm să încărcați mai întâi datele");
                    return;
                }
                
                String dateFromFilter = filterDateFrom.getText().trim();
                String dateToFilter = filterDateTo.getText().trim();
                String workerIdFilter = filterWorker.getText().trim();
                String statusFilter = filterStatus.getSelectedItem().toString();
                
                JsonArray filtered = new JsonArray();
                for (int i = 0; i < allData.size(); i++) {
                    JsonObject rec = allData.get(i).getAsJsonObject();
                    boolean match = true;
                    
                    if (!dateFromFilter.isEmpty() && !rec.get("dateFrom").getAsString().contains(dateFromFilter)) {
                        match = false;
                    }
                    if (!dateToFilter.isEmpty() && !rec.get("dateTo").getAsString().contains(dateToFilter)) {
                        match = false;
                    }
                    if (!workerIdFilter.isEmpty()) {
                        try {
                            int filterUserId = Integer.parseInt(workerIdFilter);
                            int recUserId = rec.get("userId").getAsInt();
                            if (filterUserId != recUserId) {
                                match = false;
                            }
                        } catch (NumberFormatException nfe) {
                            // If not a valid number, skip this filter
                        }
                    }
                    if (!"All".equals(statusFilter)) {
                        String recStatus = rec.get("status").getAsString();
                        String translatedStatus = translateStatus.apply(recStatus);
                        if (!statusFilter.equals(translatedStatus)) {
                            match = false;
                        }
                    }
                    
                    if (match) {
                        filtered.add(rec);
                    }
                }
                
                populateTable.accept(filtered);
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(leaveFrame, "Eroare la filtrare: " + ex.getMessage());
            }
        });
        
        // Clear filter
        clearFilterBtn.addActionListener(e -> {
            filterDateFrom.setText("");
            filterDateTo.setText("");
            filterWorker.setText("");
            filterStatus.setSelectedIndex(0);
            JsonArray allData = allRequestsData.get();
            if (allData != null) {
                populateTable.accept(allData);
            }
        });
        
        approveBtn.addActionListener(e -> {
            try {
                int selectedRow = leaveTable.getSelectedRow();
                if (selectedRow < 0) {
                    javax.swing.JOptionPane.showMessageDialog(leaveFrame, "Please select a request");
                    return;
                }
                
                int requestId = (Integer) tableModel.getValueAt(selectedRow, 0);
                
                netClient.setMessageHandler((type, payload) -> {
                    if ("update_leave_request_status_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        String status = jo.has("status") ? jo.get("status").getAsString() : "error";
                        String message = jo.has("message") ? jo.get("message").getAsString() : "";
                        if ("ok".equals(status)) {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                javax.swing.JOptionPane.showMessageDialog(leaveFrame, message, "Succes", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                // Auto-refresh după aprobare
                                fetchBtn.doClick();
                            });
                        } else {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                javax.swing.JOptionPane.showMessageDialog(leaveFrame, message, "Eroare", javax.swing.JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    }
                });
                
                JsonObject req = new JsonObject();
                req.addProperty("requestId", requestId);
                req.addProperty("status", "APPROVED");
                netClient.send("update_leave_request_status", req);
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(leaveFrame, "Error: " + ex.getMessage());
            }
        });
        
        rejectBtn.addActionListener(e -> {
            try {
                int selectedRow = leaveTable.getSelectedRow();
                if (selectedRow < 0) {
                    javax.swing.JOptionPane.showMessageDialog(leaveFrame, "Please select a request");
                    return;
                }
                
                int requestId = (Integer) tableModel.getValueAt(selectedRow, 0);
                
                netClient.setMessageHandler((type, payload) -> {
                    if ("update_leave_request_status_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        String status = jo.has("status") ? jo.get("status").getAsString() : "error";
                        String message = jo.has("message") ? jo.get("message").getAsString() : "";
                        if ("ok".equals(status)) {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                javax.swing.JOptionPane.showMessageDialog(leaveFrame, message, "Success", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                // Auto-refresh after rejection
                                fetchBtn.doClick();
                            });
                        } else {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                javax.swing.JOptionPane.showMessageDialog(leaveFrame, message, "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    }
                });
                
                JsonObject req = new JsonObject();
                req.addProperty("requestId", requestId);
                req.addProperty("status", "REJECTED");
                netClient.send("update_leave_request_status", req);
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(leaveFrame, "Error: " + ex.getMessage());
            }
        });
        
        JButton viewInfoBtn = new JButton("Vezi Detalii");
        viewInfoBtn.setEnabled(false);
        
        // Activează/dezactivează butonul Vezi Detalii bazat pe selecție
        leaveTable.getSelectionModel().addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                boolean hasSelection = leaveTable.getSelectedRow() >= 0;
                viewInfoBtn.setEnabled(hasSelection);
            }
        });
        
        viewInfoBtn.addActionListener(e -> {
            int selectedRow = leaveTable.getSelectedRow();
            if (selectedRow < 0) {
                javax.swing.JOptionPane.showMessageDialog(leaveFrame, "Vă rugăm să selectați o cerere");
                return;
            }
            
            int requestId = (Integer) tableModel.getValueAt(selectedRow, 0);
            int workerId = (Integer) tableModel.getValueAt(selectedRow, 1);
            String workerName = (String) tableModel.getValueAt(selectedRow, 2);
            String dateFrom = (String) tableModel.getValueAt(selectedRow, 3);
            String dateTo = (String) tableModel.getValueAt(selectedRow, 4);
            String reason = (String) tableModel.getValueAt(selectedRow, 5);
            String status = (String) tableModel.getValueAt(selectedRow, 6);
            
            showLeaveRequestInfo(requestId, workerId, workerName, dateFrom, dateTo, reason, status, true);
        });
        
        actionPanel.add(fetchBtn);
        actionPanel.add(viewInfoBtn);
        actionPanel.add(approveBtn);
        actionPanel.add(rejectBtn);
        leaveFrame.add(actionPanel, BorderLayout.SOUTH);

        leaveFrame.setSize(new Dimension(1400, 600));
        leaveFrame.setLocationRelativeTo(null);
        leaveFrame.setVisible(true);
    }

    private void showLeaveRequestInfo(int requestId, int workerId, String workerName, 
                                       String dateFrom, String dateTo, String reason, String status, boolean showPrintButton) {
        JFrame infoFrame = new JFrame("Detalii Cerere Concediu - ID: " + requestId);
        infoFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        infoFrame.setLayout(new BorderLayout(10, 10));
        
        // Create printable content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new javax.swing.BoxLayout(contentPanel, javax.swing.BoxLayout.Y_AXIS));
        contentPanel.setBackground(java.awt.Color.WHITE);
        contentPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(30, 50, 30, 50));
        
        // Page dimensions
        int pageHeight = 1000;
        int pageWidth = 650;
        int headerHeight = (int) (pageHeight * 0.15); // 15%
        int footerHeight = (int) (pageHeight * 0.10); // 10%
        
        // Load company settings
        String companyName = "COMPANY NAME";
        String contactInfo = "";
        try {
            java.nio.file.Path namePath = java.nio.file.Paths.get("company_name.txt");
            if (java.nio.file.Files.exists(namePath)) {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(namePath);
                if (!lines.isEmpty()) companyName = lines.get(0).trim();
            }
        } catch (Exception ignore) {
        }
        
        try {
            java.nio.file.Path contactPath = java.nio.file.Paths.get("company_contact.txt");
            if (java.nio.file.Files.exists(contactPath)) {
                contactInfo = java.nio.file.Files.readString(contactPath).trim();
            }
        } catch (Exception ignore) {
        }

        javax.swing.ImageIcon logoIcon = null;
        String[] logoCandidates = new String[]{"company_logo.png", "logo.png", "src/main/resources/company_logo.png", "resources/company_logo.png", "config/company_logo.png"};
        for (String lp : logoCandidates) {
            java.io.File lf = new java.io.File(lp);
            if (lf.exists()) {
                try {
                    java.awt.Image img = javax.imageio.ImageIO.read(lf);
                    if (img != null) {
                        logoIcon = new javax.swing.ImageIcon(img.getScaledInstance(70, 70, java.awt.Image.SCALE_SMOOTH));
                        break;
                    }
                } catch (Exception ex) {
                }
            }
        }

        // HEADER PANEL (15% - bordered)
        JPanel headerPanel = new JPanel(new BorderLayout(10, 5));
        headerPanel.setBackground(java.awt.Color.WHITE);
        headerPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(java.awt.Color.BLACK, 2),
            javax.swing.BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        if (logoIcon != null) {
            JLabel logoLbl = new JLabel(logoIcon);
            headerPanel.add(logoLbl, BorderLayout.WEST);
        }
        
        JLabel companyLabel = new JLabel(companyName);
        companyLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 22));
        companyLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        headerPanel.add(companyLabel, BorderLayout.CENTER);
        
        headerPanel.setPreferredSize(new java.awt.Dimension(pageWidth - 100, headerHeight));
        headerPanel.setMaximumSize(new java.awt.Dimension(pageWidth - 100, headerHeight));
        headerPanel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        contentPanel.add(headerPanel);
        contentPanel.add(javax.swing.Box.createVerticalStrut(15));

        // TITLU
        JLabel headerLabel = new JLabel("FORMULAR CERERE DE CONCEDIU");
        headerLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));
        headerLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        contentPanel.add(headerLabel);
        contentPanel.add(javax.swing.Box.createVerticalStrut(15));
        
        // DETALII CERERE
        addInfoField(contentPanel, "ID Cerere:", String.valueOf(requestId));
        addInfoField(contentPanel, "ID Angajat:", String.valueOf(workerId));
        addInfoField(contentPanel, "Nume Angajat:", workerName);
        contentPanel.add(javax.swing.Box.createVerticalStrut(12));
        
        addInfoField(contentPanel, "De la data:", dateFrom);
        addInfoField(contentPanel, "Până la data:", dateTo);
        contentPanel.add(javax.swing.Box.createVerticalStrut(12));
        
        addInfoField(contentPanel, "Status:", status);
        contentPanel.add(javax.swing.Box.createVerticalStrut(15));
        
        // SECȚIUNE MOTIV (cu spațiu flexibil)
        JLabel reasonLabel = new JLabel("Motiv:");
        reasonLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        reasonLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        contentPanel.add(reasonLabel);
        contentPanel.add(javax.swing.Box.createVerticalStrut(8));
        
        JTextArea reasonArea = new JTextArea(reason);
        reasonArea.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 13));
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.setEditable(false);
        reasonArea.setBorder(null);
        reasonArea.setBackground(java.awt.Color.WHITE);
        reasonArea.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        contentPanel.add(reasonArea);

        contentPanel.add(javax.swing.Box.createVerticalStrut(20));

        // ZONE SEMNĂTURI
        JPanel signaturesPanel = new JPanel(new java.awt.GridLayout(1, 2, 40, 0));
        signaturesPanel.setBackground(java.awt.Color.WHITE);
        signaturesPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        signaturesPanel.setMaximumSize(new java.awt.Dimension(pageWidth - 100, 80));
        
        JPanel hrSigPanel = new JPanel();
        hrSigPanel.setLayout(new javax.swing.BoxLayout(hrSigPanel, javax.swing.BoxLayout.Y_AXIS));
        hrSigPanel.setBackground(java.awt.Color.WHITE);
        JLabel hrSigLabel = new JLabel("Semnătura HR:");
        hrSigLabel.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
        hrSigLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        hrSigPanel.add(hrSigLabel);
        hrSigPanel.add(javax.swing.Box.createVerticalStrut(5));
        JLabel hrSigLine = new JLabel("_______________________");
        hrSigLine.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        hrSigPanel.add(hrSigLine);
        
        JPanel workerSigPanel = new JPanel();
        workerSigPanel.setLayout(new javax.swing.BoxLayout(workerSigPanel, javax.swing.BoxLayout.Y_AXIS));
        workerSigPanel.setBackground(java.awt.Color.WHITE);
        JLabel workerSigLabel = new JLabel("Semnătura Angajat:");
        workerSigLabel.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
        workerSigLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        workerSigPanel.add(workerSigLabel);
        workerSigPanel.add(javax.swing.Box.createVerticalStrut(5));
        JLabel workerSigLine = new JLabel("_______________________");
        workerSigLine.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        workerSigPanel.add(workerSigLine);
        
        signaturesPanel.add(hrSigPanel);
        signaturesPanel.add(workerSigPanel);
        contentPanel.add(signaturesPanel);
        
        // Add more space before footer
        contentPanel.add(javax.swing.Box.createVerticalStrut(40));
        
        // Push footer to bottom
        contentPanel.add(javax.swing.Box.createVerticalGlue());

        // FOOTER PANEL (10% - with date and contact)
        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(new javax.swing.BoxLayout(footerPanel, javax.swing.BoxLayout.Y_AXIS));
        footerPanel.setBackground(java.awt.Color.WHITE);
        footerPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        
        JLabel dateLabel = new JLabel("Tipărit la: " + java.time.LocalDate.now());
        dateLabel.setFont(new java.awt.Font("Arial", java.awt.Font.ITALIC, 11));
        dateLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        footerPanel.add(dateLabel);
        
        if (!contactInfo.isEmpty()) {
            footerPanel.add(javax.swing.Box.createVerticalStrut(5));
            JTextArea contactArea = new JTextArea(contactInfo);
            contactArea.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 10));
            contactArea.setLineWrap(true);
            contactArea.setWrapStyleWord(true);
            contactArea.setEditable(false);
            contactArea.setBorder(null);
            contactArea.setBackground(java.awt.Color.WHITE);
            contactArea.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            footerPanel.add(contactArea);
        }
        
        footerPanel.setPreferredSize(new java.awt.Dimension(pageWidth - 100, footerHeight));
        footerPanel.setMaximumSize(new java.awt.Dimension(pageWidth - 100, footerHeight));
        contentPanel.add(footerPanel);

        // Center the content panel inside the frame
        JPanel wrapper = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        wrapper.setBackground(java.awt.Color.LIGHT_GRAY);
        contentPanel.setPreferredSize(new java.awt.Dimension(pageWidth, pageHeight));
        wrapper.add(contentPanel);
        infoFrame.add(wrapper, BorderLayout.CENTER);

        // Button panel
        if (showPrintButton) {
            JPanel buttonPanel = new JPanel();
            JButton printBtn = new JButton("Tipărește");
            printBtn.addActionListener(e -> {
                try {
                    // Actualizează timestamp-ul înainte de tipărire
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    dateLabel.setText("Tipărit la: " + now.format(formatter));

                    java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
                    job.setPrintable((graphics, pageFormat, pageIndex) -> {
                        if (pageIndex > 0) {
                            return java.awt.print.Printable.NO_SUCH_PAGE;
                        }
                        java.awt.Graphics2D g2d = (java.awt.Graphics2D) graphics;
                        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                        double scaleX = pageFormat.getImageableWidth() / contentPanel.getWidth();
                        double scaleY = pageFormat.getImageableHeight() / contentPanel.getHeight();
                        double scale = Math.min(scaleX, scaleY);
                        g2d.scale(scale, scale);
                        contentPanel.printAll(graphics);
                        return java.awt.print.Printable.PAGE_EXISTS;
                    });
                    if (job.printDialog()) {
                        job.print();
                        javax.swing.JOptionPane.showMessageDialog(infoFrame, "Tipărire finalizată", "Tipărire", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(infoFrame, "Eroare la tipărire: " + ex.getMessage(), "Eroare", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            });
            buttonPanel.add(printBtn);
            JButton closeBtn = new JButton("Închide");
            closeBtn.addActionListener(e -> infoFrame.dispose());
            buttonPanel.add(closeBtn);
            infoFrame.add(buttonPanel, BorderLayout.SOUTH);
        } else {
            JPanel buttonPanel = new JPanel();
            JButton closeBtn = new JButton("Închide");
            closeBtn.addActionListener(e -> infoFrame.dispose());
            buttonPanel.add(closeBtn);
            infoFrame.add(buttonPanel, BorderLayout.SOUTH);
        }

        infoFrame.setSize(new Dimension(820, 1150));
        infoFrame.setResizable(false);
        infoFrame.setLocationRelativeTo(null);
        infoFrame.setVisible(true);
    }
    
    private void addInfoField(JPanel panel, String label, String value) {
        JPanel fieldPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        fieldPanel.setBackground(java.awt.Color.WHITE);
        fieldPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        fieldPanel.add(labelComp);
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 16));
        fieldPanel.add(valueComp);
        
        panel.add(fieldPanel);
    }

    private void showLoginHistory() {
        JFrame historyFrame = new JFrame("Login History");
        historyFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        historyFrame.setLayout(new BorderLayout(8, 8));

        // Date range inputs
        JPanel datePanel = new JPanel(new GridLayout(1, 4, 5, 5));
        datePanel.add(new JLabel("From:"));
        JTextArea fromField = new JTextArea(1, 10);
        fromField.setText(LocalDate.now().minusDays(30).toString());
        datePanel.add(fromField);
        datePanel.add(new JLabel("To:"));
        JTextArea toField = new JTextArea(1, 10);
        toField.setText(LocalDate.now().toString());
        datePanel.add(toField);
        historyFrame.add(datePanel, BorderLayout.NORTH);

        // Result area
        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        historyFrame.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        // Action buttons
        JPanel actionPanel = new JPanel();
        
        JButton fetchBtn = new JButton("Fetch Login History");
        JButton exportBtn = new JButton("Export to Excel");
        exportBtn.setEnabled(false);
        
        fetchBtn.addActionListener(e -> {
            try {
                String from = fromField.getText().trim();
                String to = toField.getText().trim();
                
                netClient.setMessageHandler((type, payload) -> {
                    if ("login_history_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("error")) {
                            resultArea.setText("Error: " + jo.get("error").getAsString());
                            exportBtn.setEnabled(false);
                        } else if (jo.has("records")) {
                            try {
                                JsonArray records = JsonParser.parseString(jo.get("records").getAsString()).getAsJsonArray();
                                StringBuilder sb = new StringBuilder();
                                sb.append("ID\tUser ID\tName\tRole\tLogin Time\t\t\tLogout Time\t\t\tDuration(sec)\n");
                                sb.append("==\t=======\t====\t====\t==========\t\t\t===========\t\t\t=============\n");
                                for (int i = 0; i < records.size(); i++) {
                                    JsonObject rec = records.get(i).getAsJsonObject();
                                    String loginTime = rec.get("loginTime").getAsString();
                                    String logoutTime = rec.has("logoutTime") && !rec.get("logoutTime").isJsonNull() 
                                        ? rec.get("logoutTime").getAsString() : "(still logged in)";
                                    int duration = rec.has("sessionDurationSeconds") ? rec.get("sessionDurationSeconds").getAsInt() : 0;
                                    sb.append(rec.get("id").getAsInt()).append("\t")
                                      .append(rec.get("userId").getAsInt()).append("\t")
                                      .append(rec.get("userName").getAsString().substring(0, Math.min(8, rec.get("userName").getAsString().length()))).append("\t")
                                      .append(rec.get("userRole").getAsString().substring(0, Math.min(4, rec.get("userRole").getAsString().length()))).append("\t")
                                      .append(loginTime).append("\t")
                                      .append(logoutTime).append("\t")
                                      .append(duration).append("\n");
                                }
                                resultArea.setText(sb.toString());
                                lastLoginHistoryData = records; // store for export
                                exportBtn.setEnabled(true);
                            } catch (Exception ex) {
                                resultArea.setText("Error parsing records: " + ex.getMessage());
                                exportBtn.setEnabled(false);
                            }
                        }
                    }
                });
                
                JsonObject req = new JsonObject();
                req.addProperty("from", from);
                req.addProperty("to", to);
                netClient.send("get_login_history", req);
            } catch (Exception ex) {
                resultArea.setText("Error: " + ex.getMessage());
                exportBtn.setEnabled(false);
            }
        });
        
        exportBtn.addActionListener(e -> {
            try {
                JsonArray records = lastLoginHistoryData;
                if (records == null || records.size() == 0) {
                    javax.swing.JOptionPane.showMessageDialog(historyFrame, "No data to export");
                    return;
                }
                
                JFileChooser fc = new JFileChooser();
                fc.setSelectedFile(new File("login_history_" + LocalDate.now() + ".xlsx"));
                int result = fc.showSaveDialog(historyFrame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    exportToExcel(records, file);
                    javax.swing.JOptionPane.showMessageDialog(historyFrame, "Exported to: " + file.getAbsolutePath());
                }
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(historyFrame, "Export failed: " + ex.getMessage());
            }
        });
        
        actionPanel.add(fetchBtn);
        actionPanel.add(exportBtn);
        historyFrame.add(actionPanel, BorderLayout.SOUTH);

        historyFrame.setSize(new Dimension(1000, 450));
        historyFrame.setLocationRelativeTo(null);
        historyFrame.setVisible(true);
    }

    private void exportToExcel(JsonArray records, File file) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Login History");
            
            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "User ID", "Name", "Role", "Login Time", "Logout Time", "Duration (seconds)"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }
            
            // Data rows
            for (int i = 0; i < records.size(); i++) {
                JsonObject rec = records.get(i).getAsJsonObject();
                Row row = sheet.createRow(i + 1);
                
                row.createCell(0).setCellValue(rec.get("id").getAsInt());
                row.createCell(1).setCellValue(rec.get("userId").getAsInt());
                row.createCell(2).setCellValue(rec.get("userName").getAsString());
                row.createCell(3).setCellValue(rec.get("userRole").getAsString());
                row.createCell(4).setCellValue(rec.get("loginTime").getAsString());
                
                if (rec.has("logoutTime") && !rec.get("logoutTime").isJsonNull()) {
                    row.createCell(5).setCellValue(rec.get("logoutTime").getAsString());
                } else {
                    row.createCell(5).setCellValue("(still logged in)");
                }
                
                if (rec.has("sessionDurationSeconds")) {
                    row.createCell(6).setCellValue(rec.get("sessionDurationSeconds").getAsInt());
                } else {
                    row.createCell(6).setCellValue(0);
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void showEmployeeManagement() {
        JFrame empFrame = new JFrame("Employee Management");
        empFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        empFrame.setLayout(new BorderLayout(8, 8));

        // Filter panel at top
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.add(new JLabel("Filter by Name:"));
        JTextField nameFilterField = new JTextField(15);
        filterPanel.add(nameFilterField);
        
        filterPanel.add(new JLabel("Role:"));
        javax.swing.JComboBox<String> roleFilterCombo = new javax.swing.JComboBox<>(new String[]{"All", "WORKER", "TEAMLEADER", "HR", "ADMIN"});
        filterPanel.add(roleFilterCombo);

        // Fetch button and action panel
        JButton fetchBtn = new JButton("Fetch All Workers");
        filterPanel.add(fetchBtn);
        
        JButton addUserBtn = new JButton("Add User");
        addUserBtn.addActionListener(e -> showAddUserDialog(empFrame, fetchBtn));
        filterPanel.add(addUserBtn);
        
        empFrame.add(filterPanel, BorderLayout.NORTH);

        // Table for workers
        String[] columnNames = {"ID", "Name", "Role", "Phone", "Address", "Job", "Team Leader"};
        javax.swing.table.DefaultTableModel tableModel = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        javax.swing.JTable workersTable = new javax.swing.JTable(tableModel);
        workersTable.setAutoCreateRowSorter(true);
        workersTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        workersTable.getTableHeader().setReorderingAllowed(false);
        
        JScrollPane tableScroll = new JScrollPane(workersTable);
        empFrame.add(tableScroll, BorderLayout.CENTER);

        // Bottom panel for actions
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        JButton editBtn = new JButton("Edit User");
        editBtn.addActionListener(e -> {
            int selectedRow = workersTable.getSelectedRow();
            if (selectedRow < 0) {
                javax.swing.JOptionPane.showMessageDialog(empFrame, "Please select a user first");
                return;
            }
            
            int userId = (Integer) tableModel.getValueAt(selectedRow, 0);
            String currentName = (String) tableModel.getValueAt(selectedRow, 1);
            String currentRole = (String) tableModel.getValueAt(selectedRow, 2);
            String currentPhone = (String) tableModel.getValueAt(selectedRow, 3);
            String currentAddress = (String) tableModel.getValueAt(selectedRow, 4);
            String currentJob = (String) tableModel.getValueAt(selectedRow, 5);
            String currentTeamLeader = (String) tableModel.getValueAt(selectedRow, 6);
            
            showEditUserDialog(empFrame, userId, currentName, currentRole, currentPhone, currentAddress, currentJob, currentTeamLeader, fetchBtn);
        });
        bottomPanel.add(editBtn);
        
        JButton deleteBtn = new JButton("Delete User");
        deleteBtn.addActionListener(e -> {
            int selectedRow = workersTable.getSelectedRow();
            if (selectedRow == -1) {
                javax.swing.JOptionPane.showMessageDialog(empFrame, "Vă rugăm selectați un utilizator", "Informație", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            int userId = (int) tableModel.getValueAt(selectedRow, 0);
            String userName = (String) tableModel.getValueAt(selectedRow, 1);
            
            int confirm = javax.swing.JOptionPane.showConfirmDialog(empFrame, 
                "Sigur doriți să ștergeți utilizatorul '" + userName + "' (ID: " + userId + ")?\nAceastă acțiune este ireversibilă!", 
                "Confirmare Ștergere", 
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE);
            
            if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                try {
                    netClient.setMessageHandler((type, payload) -> {
                        if ("delete_user_response".equals(type) && payload != null && payload.isJsonObject()) {
                            JsonObject jo = payload.getAsJsonObject();
                            if (jo.has("status") && "ok".equals(jo.get("status").getAsString())) {
                                javax.swing.JOptionPane.showMessageDialog(empFrame, "Utilizator șters cu succes!", "Succes", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                fetchBtn.doClick();
                            } else if (jo.has("status") && "error".equals(jo.get("status").getAsString())) {
                                javax.swing.JOptionPane.showMessageDialog(empFrame, "Eroare: " + jo.get("message").getAsString(), "Eroare", javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });
                    
                    JsonObject req = new JsonObject();
                    req.addProperty("userId", userId);
                    netClient.send("delete_user", req);
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(empFrame, "Error: " + ex.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        bottomPanel.add(deleteBtn);
        
        JButton editRoleBtn = new JButton("Change Role");
        editRoleBtn.addActionListener(e -> {
            int selectedRow = workersTable.getSelectedRow();
            if (selectedRow < 0) {
                javax.swing.JOptionPane.showMessageDialog(empFrame, "Please select a worker first");
                return;
            }
            
            int workerId = (Integer) tableModel.getValueAt(selectedRow, 0);
            String currentRole = (String) tableModel.getValueAt(selectedRow, 2);
            
            String[] options;
            if ("WORKER".equals(currentRole)) {
                options = new String[]{"Make Team Leader"};
            } else if ("TEAMLEADER".equals(currentRole)) {
                options = new String[]{"Change to Worker"};
            } else {
                javax.swing.JOptionPane.showMessageDialog(empFrame, "Can only change roles for WORKER and TEAMLEADER");
                return;
            }
            
            String choice = (String) javax.swing.JOptionPane.showInputDialog(empFrame, "Select action:", "Change Role",
                    javax.swing.JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            
            if (choice == null) return;
            
            if ("Make Team Leader".equals(choice)) {
                handleMakeTeamLeader(workerId, empFrame, fetchBtn);
            } else if ("Change to Worker".equals(choice)) {
                handleChangeToWorker(workerId, empFrame, fetchBtn);
            }
        });
        bottomPanel.add(editRoleBtn);
        
        JButton assignToTLBtn = new JButton("Assign to Team Leader");
        assignToTLBtn.addActionListener(e -> {
            int selectedRow = workersTable.getSelectedRow();
            if (selectedRow < 0) {
                javax.swing.JOptionPane.showMessageDialog(empFrame, "Please select a worker first");
                return;
            }
            
            int workerId = (Integer) tableModel.getValueAt(selectedRow, 0);
            String role = (String) tableModel.getValueAt(selectedRow, 2);
            
            if (!"WORKER".equals(role)) {
                javax.swing.JOptionPane.showMessageDialog(empFrame, "Can only assign WORKER to team leader");
                return;
            }
            
            String tlIdStr = javax.swing.JOptionPane.showInputDialog(empFrame, "Enter Team Leader ID:");
            if (tlIdStr == null || tlIdStr.trim().isEmpty()) return;
            
            try {
                int tlId = Integer.parseInt(tlIdStr.trim());
                handleAssignToTeamLeader(workerId, tlId, empFrame, fetchBtn);
            } catch (NumberFormatException ex) {
                javax.swing.JOptionPane.showMessageDialog(empFrame, "Invalid Team Leader ID");
            }
        });
        bottomPanel.add(assignToTLBtn);
        
        empFrame.add(bottomPanel, BorderLayout.SOUTH);

        // Fetch workers
        fetchBtn.addActionListener(e -> {
            try {
                netClient.setMessageHandler((type, payload) -> {
                    if ("all_workers_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("error")) {
                            javax.swing.JOptionPane.showMessageDialog(empFrame, "Error: " + jo.get("error").getAsString());
                        } else if (jo.has("workers")) {
                            try {
                                JsonArray workers = JsonParser.parseString(jo.get("workers").getAsString()).getAsJsonArray();
                                
                                // Clear table
                                tableModel.setRowCount(0);
                                
                                String nameFilter = nameFilterField.getText().toLowerCase().trim();
                                String roleFilter = (String) roleFilterCombo.getSelectedItem();
                                
                                // Fill table with filters
                                for (int i = 0; i < workers.size(); i++) {
                                    JsonObject worker = workers.get(i).getAsJsonObject();
                                    String name = worker.get("name").getAsString();
                                    String role = worker.get("role").getAsString();
                                    
                                    // Apply filters
                                    if (!nameFilter.isEmpty() && !name.toLowerCase().contains(nameFilter)) {
                                        continue;
                                    }
                                    if (!"All".equals(roleFilter) && !role.equals(roleFilter)) {
                                        continue;
                                    }
                                    
                                    String phone = worker.has("phone") && !worker.get("phone").isJsonNull() ? worker.get("phone").getAsString() : "";
                                    String address = worker.has("address") && !worker.get("address").isJsonNull() ? worker.get("address").getAsString() : "";
                                    String job = worker.has("job") && !worker.get("job").isJsonNull() ? worker.get("job").getAsString() : "";
                                    String teamLeader = worker.has("teamLeaderName") && !worker.get("teamLeaderName").isJsonNull() ? worker.get("teamLeaderName").getAsString() : "";
                                    
                                    tableModel.addRow(new Object[]{
                                        worker.get("id").getAsInt(),
                                        name,
                                        role,
                                        phone,
                                        address,
                                        job,
                                        teamLeader
                                    });
                                }
                            } catch (Exception ex) {
                                javax.swing.JOptionPane.showMessageDialog(empFrame, "Error parsing workers: " + ex.getMessage());
                            }
                        }
                    }
                });
                
                netClient.send("get_all_workers", new JsonObject());
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(empFrame, "Error: " + ex.getMessage());
            }
        });

        empFrame.setSize(new Dimension(900, 550));
        empFrame.setLocationRelativeTo(null);
        empFrame.setVisible(true);
        
        // Auto-fetch on open
        fetchBtn.doClick();
    }

    private void handleMakeTeamLeader(int workerId, JFrame parent, JButton refreshBtn) {
        try {
            netClient.setMessageHandler((type, payload) -> {
                if ("change_worker_to_teamleader_response".equals(type) && payload != null && payload.isJsonObject()) {
                    JsonObject jo = payload.getAsJsonObject();
                    String status = jo.has("status") ? jo.get("status").getAsString() : "error";
                    String message = jo.has("message") ? jo.get("message").getAsString() : "";
                    if ("ok".equals(status)) {
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            javax.swing.JOptionPane.showMessageDialog(parent, message, "Success", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                            refreshBtn.doClick();
                        });
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(parent, message, "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            
            JsonObject req = new JsonObject();
            req.addProperty("workerId", workerId);
            netClient.send("change_worker_to_teamleader", req);
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(parent, "Error: " + ex.getMessage());
        }
    }

    private void handleChangeToWorker(int teamLeaderId, JFrame parent, JButton refreshBtn) {
        try {
            netClient.setMessageHandler((type, payload) -> {
                if ("change_teamleader_to_worker_response".equals(type) && payload != null && payload.isJsonObject()) {
                    JsonObject jo = payload.getAsJsonObject();
                    String status = jo.has("status") ? jo.get("status").getAsString() : "error";
                    String message = jo.has("message") ? jo.get("message").getAsString() : "";
                    if ("ok".equals(status)) {
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            javax.swing.JOptionPane.showMessageDialog(parent, message, "Success", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                            refreshBtn.doClick();
                        });
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(parent, message, "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            
            JsonObject req = new JsonObject();
            req.addProperty("teamLeaderId", teamLeaderId);
            netClient.send("change_teamleader_to_worker", req);
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(parent, "Error: " + ex.getMessage());
        }
    }

    private void handleAssignToTeamLeader(int workerId, int teamLeaderId, JFrame parent, JButton refreshBtn) {
        try {
            netClient.setMessageHandler((type, payload) -> {
                if ("assign_worker_to_teamleader_response".equals(type) && payload != null && payload.isJsonObject()) {
                    JsonObject jo = payload.getAsJsonObject();
                    String status = jo.has("status") ? jo.get("status").getAsString() : "error";
                    String message = jo.has("message") ? jo.get("message").getAsString() : "";
                    if ("ok".equals(status)) {
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            javax.swing.JOptionPane.showMessageDialog(parent, message, "Success", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                            refreshBtn.doClick();
                        });
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(parent, message, "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            
            JsonObject req = new JsonObject();
            req.addProperty("workerId", workerId);
            req.addProperty("teamLeaderId", teamLeaderId);
            netClient.send("assign_worker_to_teamleader", req);
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(parent, "Error: " + ex.getMessage());
        }
    }

    private void viewAllWorkStatus() {
        JFrame statusFrame = new JFrame("All Work Status");
        statusFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        statusFrame.setLayout(new BorderLayout(8, 8));

        javax.swing.JTable table = new javax.swing.JTable();
        javax.swing.table.DefaultTableModel tableModel = new javax.swing.table.DefaultTableModel(
            new Object[]{"User ID", "Name", "Role", "Work Date", "Start Time", "End Time", "Status"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setModel(tableModel);
        table.setAutoCreateRowSorter(true);
        
        // Add mode selection panel
        JPanel modePanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        modePanel.setBorder(BorderFactory.createTitledBorder("Display Mode"));
        
        javax.swing.JRadioButton currentDayBtn = new javax.swing.JRadioButton("Current Day", true);
        javax.swing.JRadioButton fullHistoryBtn = new javax.swing.JRadioButton("Full History");
        javax.swing.JRadioButton lastPerEmployeeBtn = new javax.swing.JRadioButton("Last Start per Employee");
        
        javax.swing.ButtonGroup modeGroup = new javax.swing.ButtonGroup();
        modeGroup.add(currentDayBtn);
        modeGroup.add(fullHistoryBtn);
        modeGroup.add(lastPerEmployeeBtn);
        
        modePanel.add(currentDayBtn);
        modePanel.add(fullHistoryBtn);
        modePanel.add(lastPerEmployeeBtn);
        
        // Add filter panel with GridLayout for better organization
        JPanel filterPanel = new JPanel(new java.awt.GridLayout(2, 1, 5, 5));
        
        // First row of filters
        JPanel filterRow1 = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        filterRow1.add(new JLabel("Filter by Role:"));
        javax.swing.JComboBox<String> roleFilter = new javax.swing.JComboBox<>(new String[]{"All", "WORKER", "TEAMLEADER"});
        filterRow1.add(roleFilter);
        
        filterRow1.add(new JLabel("Filter by Status:"));
        javax.swing.JComboBox<String> statusFilter = new javax.swing.JComboBox<>(new String[]{"All", "ACTIVE", "COMPLETED", "NOT_STARTED"});
        filterRow1.add(statusFilter);
        
        filterRow1.add(new JLabel("Filter by User ID:"));
        javax.swing.JTextField userIdFilter = new javax.swing.JTextField(10);
        filterRow1.add(userIdFilter);
        
        // Second row of filters
        JPanel filterRow2 = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        filterRow2.add(new JLabel("Filter by Name:"));
        javax.swing.JTextField nameFilter = new javax.swing.JTextField(15);
        filterRow2.add(nameFilter);
        
        filterRow2.add(new JLabel("Filter by Work Date:"));
        javax.swing.JTextField workDateFilter = new javax.swing.JTextField(10);
        filterRow2.add(workDateFilter);
        
        JButton applyFilterBtn = new JButton("Apply Filter");
        filterRow2.add(applyFilterBtn);
        
        JButton clearFilterBtn = new JButton("Clear Filters");
        filterRow2.add(clearFilterBtn);
        
        filterPanel.add(filterRow1);
        filterPanel.add(filterRow2);
        
        // Combine mode and filter panels
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(modePanel, BorderLayout.NORTH);
        topPanel.add(filterPanel, BorderLayout.SOUTH);
        
        statusFrame.add(topPanel, BorderLayout.NORTH);
        statusFrame.add(new JScrollPane(table), BorderLayout.CENTER);

        // Store original data
        java.util.List<Object[]> allData = new java.util.ArrayList<>();

        applyFilterBtn.addActionListener(e -> {
            String selectedRole = (String) roleFilter.getSelectedItem();
            String selectedStatus = (String) statusFilter.getSelectedItem();
            String userIdText = userIdFilter.getText().trim();
            String nameText = nameFilter.getText().trim().toLowerCase();
            String workDateText = workDateFilter.getText().trim();
            
            tableModel.setRowCount(0);
            for (Object[] row : allData) {
                boolean roleMatch = "All".equals(selectedRole) || selectedRole.equals(row[2]);
                boolean statusMatch = "All".equals(selectedStatus) || selectedStatus.equals(row[6]);
                boolean userIdMatch = userIdText.isEmpty() || String.valueOf(row[0]).contains(userIdText);
                boolean nameMatch = nameText.isEmpty() || ((String)row[1]).toLowerCase().contains(nameText);
                boolean workDateMatch = workDateText.isEmpty() || ((String)row[3]).contains(workDateText);
                
                if (roleMatch && statusMatch && userIdMatch && nameMatch && workDateMatch) {
                    tableModel.addRow(row);
                }
            }
        });

        clearFilterBtn.addActionListener(e -> {
            roleFilter.setSelectedIndex(0);
            statusFilter.setSelectedIndex(0);
            userIdFilter.setText("");
            nameFilter.setText("");
            workDateFilter.setText("");
            tableModel.setRowCount(0);
            for (Object[] row : allData) {
                tableModel.addRow(row);
            }
        });

        JPanel buttonPanel = new JPanel();
        JButton fetchBtn = new JButton("Fetch All Work Status");
        JButton forceStopBtn = new JButton("Force Stop Work for Selected");
        JButton endAllBtn = new JButton("End All Active Sessions");
        
        // Function to fetch data with current mode
        Runnable fetchData = () -> {
            try {
                netClient.setMessageHandler((type, payload) -> {
                    if ("all_work_status_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("error")) {
                            javax.swing.JOptionPane.showMessageDialog(statusFrame, "Error: " + jo.get("error").getAsString(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                        } else if (jo.has("all_work_status")) {
                            try {
                                JsonArray statuses = JsonParser.parseString(jo.get("all_work_status").getAsString()).getAsJsonArray();
                                allData.clear();
                                tableModel.setRowCount(0);
                                for (int i = 0; i < statuses.size(); i++) {
                                    JsonObject status = statuses.get(i).getAsJsonObject();
                                    Object[] row = new Object[]{
                                        status.get("userId").getAsInt(),
                                        status.get("userName").getAsString(),
                                        status.get("userRole").getAsString(),
                                        status.has("workDate") && !status.get("workDate").isJsonNull() ? status.get("workDate").getAsString() : "N/A",
                                        status.has("startTime") && !status.get("startTime").isJsonNull() ? status.get("startTime").getAsString() : "N/A",
                                        status.has("endTime") && !status.get("endTime").isJsonNull() ? status.get("endTime").getAsString() : "N/A",
                                        status.get("status").getAsString()
                                    };
                                    allData.add(row);
                                    tableModel.addRow(row);
                                }
                            } catch (Exception ex) {
                                javax.swing.JOptionPane.showMessageDialog(statusFrame, "Error parsing data: " + ex.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                });

                // Determine mode
                String mode = "current";
                if (fullHistoryBtn.isSelected()) {
                    mode = "full_history";
                } else if (lastPerEmployeeBtn.isSelected()) {
                    mode = "last_per_employee";
                }
                
                JsonObject payload = new JsonObject();
                payload.addProperty("mode", mode);
                netClient.send("get_all_work_status", payload);
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(statusFrame, "Error: " + ex.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        };
        
        fetchBtn.addActionListener(e -> fetchData.run());
        
        // Force Stop Work button action
        forceStopBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                javax.swing.JOptionPane.showMessageDialog(statusFrame,
                        "Please select a user to force stop work.",
                        "No Selection",
                        javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int userId = ((Number) table.getValueAt(selectedRow, 0)).intValue();
            String userName = (String) table.getValueAt(selectedRow, 1);
            String status = (String) table.getValueAt(selectedRow, 6);
            
            if (!"ACTIVE".equals(status)) {
                javax.swing.JOptionPane.showMessageDialog(statusFrame,
                        "User " + userName + " is not currently working (status: " + status + ").",
                        "Invalid Status",
                        javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int confirm = javax.swing.JOptionPane.showConfirmDialog(statusFrame,
                    "Force stop work for " + userName + "?",
                    "Confirm Force Stop",
                    javax.swing.JOptionPane.YES_NO_OPTION);
            
            if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                try {
                    netClient.setMessageHandler((type, payload) -> {
                        if ("force_stop_work_response".equals(type) && payload != null && payload.isJsonObject()) {
                            JsonObject jo = payload.getAsJsonObject();
                            if (jo.has("status") && "error".equals(jo.get("status").getAsString())) {
                                javax.swing.JOptionPane.showMessageDialog(statusFrame,
                                        "Error: " + (jo.has("message") ? jo.get("message").getAsString() : "Unknown error"),
                                        "Error",
                                        javax.swing.JOptionPane.ERROR_MESSAGE);
                            } else {
                                javax.swing.JOptionPane.showMessageDialog(statusFrame,
                                        "Work stopped successfully for " + userName,
                                        "Success",
                                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                fetchData.run(); // Refresh data
                            }
                        }
                    });
                    
                    JsonObject payload = new JsonObject();
                    payload.addProperty("user_id", userId);
                    netClient.send("force_stop_work", payload);
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(statusFrame,
                            "Error: " + ex.getMessage(),
                            "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        // End All Active Sessions button action
        endAllBtn.addActionListener(e -> {
            int confirm = javax.swing.JOptionPane.showConfirmDialog(statusFrame,
                    "Are you sure you want to end ALL active work sessions?\nThis will set all ACTIVE sessions to COMPLETED.",
                    "Confirm End All Sessions",
                    javax.swing.JOptionPane.YES_NO_OPTION,
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            
            if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                try {
                    netClient.setMessageHandler((type, payload) -> {
                        if ("end_all_active_sessions_response".equals(type) && payload != null && payload.isJsonObject()) {
                            JsonObject jo = payload.getAsJsonObject();
                            if (jo.has("status") && "error".equals(jo.get("status").getAsString())) {
                                javax.swing.JOptionPane.showMessageDialog(statusFrame,
                                        "Error: " + (jo.has("message") ? jo.get("message").getAsString() : "Unknown error"),
                                        "Error",
                                        javax.swing.JOptionPane.ERROR_MESSAGE);
                            } else {
                                javax.swing.JOptionPane.showMessageDialog(statusFrame,
                                        "All active sessions ended successfully!",
                                        "Success",
                                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                fetchData.run(); // Refresh data
                            }
                        }
                    });
                    
                    netClient.send("end_all_active_sessions", new JsonObject());
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(statusFrame,
                            "Error: " + ex.getMessage(),
                            "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        // Add mode change listeners
        currentDayBtn.addActionListener(e -> fetchData.run());
        fullHistoryBtn.addActionListener(e -> fetchData.run());
        lastPerEmployeeBtn.addActionListener(e -> fetchData.run());
        
        buttonPanel.add(fetchBtn);
        buttonPanel.add(forceStopBtn);
        buttonPanel.add(endAllBtn);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> statusFrame.dispose());
        buttonPanel.add(closeBtn);

        statusFrame.add(buttonPanel, BorderLayout.SOUTH);

        statusFrame.setSize(new Dimension(1000, 600));
        statusFrame.setLocationRelativeTo(null);
        statusFrame.setVisible(true);

        // Auto-fetch on open
        fetchBtn.doClick();
    }

    private void showAddUserDialog(JFrame parent, JButton refreshBtn) {
        javax.swing.JDialog dlg = new javax.swing.JDialog(parent, "Add User", true);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(7, 2, 10, 10));
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));

        form.add(new JLabel("Name:"));
        javax.swing.JTextField nameField = new javax.swing.JTextField();
        form.add(nameField);

        form.add(new JLabel("Role:"));
        javax.swing.JComboBox<String> roleCombo = new javax.swing.JComboBox<>(new String[]{"WORKER", "TEAMLEADER", "HR", "ADMIN"});
        form.add(roleCombo);

        form.add(new JLabel("Password:"));
        javax.swing.JPasswordField passField = new javax.swing.JPasswordField();
        form.add(passField);

        form.add(new JLabel("Phone:"));
        javax.swing.JTextField phoneField = new javax.swing.JTextField();
        form.add(phoneField);

        form.add(new JLabel("Address:"));
        javax.swing.JTextField addressField = new javax.swing.JTextField();
        form.add(addressField);

        form.add(new JLabel("Job:"));
        javax.swing.JComboBox<String> jobCombo = new javax.swing.JComboBox<>();
        jobCombo.setEditable(true);
        fetchJobTitles(jobCombo, null);
        form.add(jobCombo);

        form.add(new JLabel("Team Leader:"));
        javax.swing.JComboBox<String> teamLeaderCombo = new javax.swing.JComboBox<>();
        teamLeaderCombo.addItem("--- Fără Team Leader ---");
        fetchTeamLeaders(teamLeaderCombo, null);
        form.add(teamLeaderCombo);
        
        // Enable/disable team leader combo based on role selection
        roleCombo.addActionListener(evt -> {
            String selectedRole = (String) roleCombo.getSelectedItem();
            teamLeaderCombo.setEnabled("WORKER".equals(selectedRole));
        });
        // Initial state: disable if not WORKER
        teamLeaderCombo.setEnabled("WORKER".equals(roleCombo.getSelectedItem()));

        dlg.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton createBtn = new JButton("Create");
        createBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String role = (String) roleCombo.getSelectedItem();
            String pw = new String(passField.getPassword()).trim();
            String phone = phoneField.getText().trim();
            String address = addressField.getText().trim();
            String job = jobCombo.getEditor().getItem() != null ? jobCombo.getEditor().getItem().toString().trim() : "";
            String selectedTeamLeader = (String) teamLeaderCombo.getSelectedItem();
            System.out.println("DEBUG CLIENT: Create button clicked - name: " + name + ", role: " + role);
            if (name.isEmpty() || pw.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(dlg, "Name and password are required", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Extract team leader ID from selection
            Integer teamLeaderId = null;
            if (selectedTeamLeader != null && !selectedTeamLeader.startsWith("---")) {
                String[] parts = selectedTeamLeader.split(" - ");
                if (parts.length > 0) {
                    try {
                        teamLeaderId = Integer.parseInt(parts[0]);
                    } catch (NumberFormatException ex) {
                        // ignore
                    }
                }
            }
            
            try {
                System.out.println("DEBUG CLIENT: Setting message handler for create_user_response");
                netClient.setMessageHandler((type, payload) -> {
                    System.out.println("DEBUG CLIENT: Received message type: " + type);
                    if ("create_user_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        System.out.println("DEBUG CLIENT: create_user_response payload: " + jo);
                        if (jo.has("ok")) {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                javax.swing.JOptionPane.showMessageDialog(dlg, "User created with ID: " + jo.get("userId").getAsInt(), "Success", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                dlg.dispose();
                                refreshBtn.doClick();
                            });
                        } else if (jo.has("error")) {
                            javax.swing.JOptionPane.showMessageDialog(dlg, "Error: " + jo.get("error").getAsString(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });

                JsonObject req = new JsonObject();
                req.addProperty("name", name);
                req.addProperty("role", role);
                req.addProperty("password", pw);
                req.addProperty("phone", phone);
                req.addProperty("address", address);
                req.addProperty("job", job);
                if (teamLeaderId != null) {
                    req.addProperty("teamLeaderId", teamLeaderId);
                }
                System.out.println("DEBUG CLIENT: Sending create_user request: " + req);
                netClient.send("create_user", req);
                System.out.println("DEBUG CLIENT: create_user request sent");
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(dlg, "Error: " + ex.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
        btnPanel.add(createBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dlg.dispose());
        btnPanel.add(cancelBtn);

        dlg.add(btnPanel, BorderLayout.SOUTH);

        dlg.setSize(new Dimension(400, 410));
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }

    private void showSalaryManagement() {
        JFrame salaryFrame = new JFrame("Salary Management");
        salaryFrame.setSize(1400, 700);
        salaryFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        salaryFrame.setLayout(new BorderLayout(10, 10));

        // Main panel with tabs
        javax.swing.JTabbedPane tabbedPane = new javax.swing.JTabbedPane();
        
        // Tab 1: Hourly Rates Management
        JPanel hourlyRatesPanel = createHourlyRatesPanel();
        tabbedPane.addTab("Hourly Rates", hourlyRatesPanel);
        
        // Tab 2: Salary Calculations
        JPanel salaryCalcPanel = createSalaryCalculationsPanel();
        tabbedPane.addTab("Salary Calculations", salaryCalcPanel);
        
        salaryFrame.add(tabbedPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> salaryFrame.dispose());
        bottomPanel.add(closeBtn);
        salaryFrame.add(bottomPanel, BorderLayout.SOUTH);
        
        salaryFrame.setLocationRelativeTo(frame);
        salaryFrame.setVisible(true);
    }

    private JPanel createHourlyRatesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Table for hourly rates
        String[] columns = {"User ID", "Name", "Role", "Hourly Rate (MDL)", "Valid From"};
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        javax.swing.JTable table = new javax.swing.JTable(model);
        table.setAutoCreateRowSorter(true);
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(table);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton refreshBtn = new JButton("Refresh");
        JButton setRateBtn = new JButton("Set Hourly Rate");
        JButton viewHistoryBtn = new JButton("View History");
        
        buttonPanel.add(refreshBtn);
        buttonPanel.add(setRateBtn);
        buttonPanel.add(viewHistoryBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Refresh action
        Runnable loadData = () -> {
            try {
                netClient.setMessageHandler((type, payload) -> {
                    if ("get_hourly_rates_response".equals(type) && payload != null && payload.isJsonObject()) {
                        com.google.gson.JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("status") && "error".equals(jo.get("status").getAsString())) {
                            javax.swing.JOptionPane.showMessageDialog(panel, 
                                "Error: " + jo.get("message").getAsString(), 
                                "Error", 
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        
                        if (jo.has("hourly_rates")) {
                            try {
                                com.google.gson.JsonArray rates = com.google.gson.JsonParser.parseString(
                                    jo.get("hourly_rates").getAsString()).getAsJsonArray();
                                
                                model.setRowCount(0);
                                for (int i = 0; i < rates.size(); i++) {
                                    com.google.gson.JsonObject rate = rates.get(i).getAsJsonObject();
                                    Object[] row = {
                                        rate.get("userId").getAsInt(),
                                        rate.get("userName").getAsString(),
                                        rate.get("userRole").getAsString(),
                                        rate.get("hourlyRate").getAsDouble(),
                                        rate.has("validFrom") && !rate.get("validFrom").isJsonNull() ? 
                                            rate.get("validFrom").getAsString() : "Not set"
                                    };
                                    model.addRow(row);
                                }
                            } catch (Exception ex) {
                                javax.swing.JOptionPane.showMessageDialog(panel, 
                                    "Error parsing data: " + ex.getMessage(), 
                                    "Error", 
                                    javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                });
                
                netClient.send("get_hourly_rates", new com.google.gson.JsonObject());
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(panel, 
                    "Error: " + ex.getMessage(), 
                    "Error", 
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        };
        
        refreshBtn.addActionListener(e -> loadData.run());
        
        // Set rate action
        setRateBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                javax.swing.JOptionPane.showMessageDialog(panel, 
                    "Please select an employee first.", 
                    "No Selection", 
                    javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int userId = (int) table.getValueAt(selectedRow, 0);
            String userName = (String) table.getValueAt(selectedRow, 1);
            
            String rateStr = javax.swing.JOptionPane.showInputDialog(panel, 
                "Set hourly rate for " + userName + " (MDL):", 
                "Set Hourly Rate", 
                javax.swing.JOptionPane.QUESTION_MESSAGE);
            
            if (rateStr != null && !rateStr.trim().isEmpty()) {
                try {
                    double rate = Double.parseDouble(rateStr.trim());
                    if (rate < 0) {
                        javax.swing.JOptionPane.showMessageDialog(panel, 
                            "Hourly rate cannot be negative!", 
                            "Invalid Input", 
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    netClient.setMessageHandler((type, payload) -> {
                        if ("set_hourly_rate_response".equals(type) && payload != null && payload.isJsonObject()) {
                            com.google.gson.JsonObject jo = payload.getAsJsonObject();
                            if (jo.has("status") && "error".equals(jo.get("status").getAsString())) {
                                javax.swing.JOptionPane.showMessageDialog(panel, 
                                    "Error: " + jo.get("message").getAsString(), 
                                    "Error", 
                                    javax.swing.JOptionPane.ERROR_MESSAGE);
                            } else {
                                javax.swing.JOptionPane.showMessageDialog(panel, 
                                    "Hourly rate updated successfully!", 
                                    "Success", 
                                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                loadData.run(); // Refresh
                            }
                        }
                    });
                    
                    com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                    payload.addProperty("user_id", userId);
                    payload.addProperty("hourly_rate", rate);
                    netClient.send("set_hourly_rate", payload);
                } catch (NumberFormatException ex) {
                    javax.swing.JOptionPane.showMessageDialog(panel, 
                        "Invalid number format!", 
                        "Error", 
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(panel, 
                        "Error: " + ex.getMessage(), 
                        "Error", 
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        // View history action
        viewHistoryBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                javax.swing.JOptionPane.showMessageDialog(panel, 
                    "Please select an employee first.", 
                    "No Selection", 
                    javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int userId = (int) table.getValueAt(selectedRow, 0);
            String userName = (String) table.getValueAt(selectedRow, 1);
            showHourlyRateHistory(userId, userName);
        });
        
        // Load data on panel creation
        javax.swing.SwingUtilities.invokeLater(loadData);
        
        return panel;
    }

    private JPanel createSalaryCalculationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top panel for month/year selection
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Month:"));
        javax.swing.JComboBox<String> monthCombo = new javax.swing.JComboBox<>(
            new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"});
        
        // Set to previous month
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.YearMonth prevMonth = java.time.YearMonth.from(now).minusMonths(1);
        monthCombo.setSelectedIndex(prevMonth.getMonthValue() - 1);
        topPanel.add(monthCombo);
        
        topPanel.add(new JLabel("Year:"));
        javax.swing.JComboBox<Integer> yearCombo = new javax.swing.JComboBox<>();
        for (int y = 2020; y <= 2030; y++) {
            yearCombo.addItem(y);
        }
        yearCombo.setSelectedItem(prevMonth.getYear());
        topPanel.add(yearCombo);
        
        JButton calculateBtn = new JButton("Calculate Salaries");
        JButton loadBtn = new JButton("Load Calculations");
        JButton publishBtn = new JButton("Publish Salaries");
        
        topPanel.add(calculateBtn);
        topPanel.add(loadBtn);
        topPanel.add(publishBtn);
        
        panel.add(topPanel, BorderLayout.NORTH);
        
        // Table for salary calculations
        String[] columns = {"ID", "User", "Role", "Hours", "Rate (MDL)", "Base (MDL)", "Bonuses (MDL)", "Gross (MDL)", "Tax 15% (MDL)", "Net (MDL)", "Published"};
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        javax.swing.JTable table = new javax.swing.JTable(model);
        table.setAutoCreateRowSorter(true);
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(table);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Bottom panel with action buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBonusBtn = new JButton("Add Bonus");
        JButton viewDetailsBtn = new JButton("View Details");
        
        bottomPanel.add(addBonusBtn);
        bottomPanel.add(viewDetailsBtn);
        
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        // Calculate salaries action
        calculateBtn.addActionListener(e -> {
            int confirm = javax.swing.JOptionPane.showConfirmDialog(panel,
                "Calculate salaries for previous month?\nThis will recalculate existing salaries (bonuses will be preserved).",
                "Confirm Calculation",
                javax.swing.JOptionPane.YES_NO_OPTION);
            
            if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                try {
                    netClient.setMessageHandler((type, payload) -> {
                        if ("calculate_salaries_response".equals(type) && payload != null && payload.isJsonObject()) {
                            com.google.gson.JsonObject jo = payload.getAsJsonObject();
                            if (jo.has("status") && "error".equals(jo.get("status").getAsString())) {
                                javax.swing.JOptionPane.showMessageDialog(panel,
                                    "Error: " + jo.get("message").getAsString(),
                                    "Error",
                                    javax.swing.JOptionPane.ERROR_MESSAGE);
                            } else {
                                int count = jo.has("count") ? jo.get("count").getAsInt() : 0;
                                javax.swing.JOptionPane.showMessageDialog(panel,
                                    "Calculated salaries for " + count + " employees!",
                                    "Success",
                                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                // Auto-load after calculation
                                loadBtn.doClick();
                            }
                        }
                    });
                    
                    netClient.send("calculate_salaries", new com.google.gson.JsonObject());
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(panel,
                        "Error: " + ex.getMessage(),
                        "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        // Load calculations action
        Runnable loadData = () -> {
            try {
                int month = Integer.parseInt((String) monthCombo.getSelectedItem());
                int year = (int) yearCombo.getSelectedItem();
                
                netClient.setMessageHandler((type, payload) -> {
                    if ("get_salary_calculations_response".equals(type) && payload != null && payload.isJsonObject()) {
                        com.google.gson.JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("status") && "error".equals(jo.get("status").getAsString())) {
                            javax.swing.JOptionPane.showMessageDialog(panel,
                                "Error: " + jo.get("message").getAsString(),
                                "Error",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        
                        if (jo.has("salary_calculations")) {
                            try {
                                com.google.gson.JsonArray calcs = com.google.gson.JsonParser.parseString(
                                    jo.get("salary_calculations").getAsString()).getAsJsonArray();
                                
                                model.setRowCount(0);
                                for (int i = 0; i < calcs.size(); i++) {
                                    com.google.gson.JsonObject calc = calcs.get(i).getAsJsonObject();
                                    Object[] row = {
                                        calc.get("id").getAsInt(),
                                        calc.get("userName").getAsString(),
                                        calc.get("userRole").getAsString(),
                                        String.format("%.2f", calc.get("hoursWorked").getAsDouble()),
                                        String.format("%.2f", calc.get("hourlyRate").getAsDouble()),
                                        String.format("%.2f", calc.get("baseSalary").getAsDouble()),
                                        String.format("%.2f", calc.get("bonusesTotal").getAsDouble()),
                                        String.format("%.2f", calc.get("grossSalary").getAsDouble()),
                                        String.format("%.2f", calc.get("taxAmount").getAsDouble()),
                                        String.format("%.2f", calc.get("netSalary").getAsDouble()),
                                        calc.get("isPublished").getAsBoolean() ? "Yes" : "No"
                                    };
                                    model.addRow(row);
                                }
                            } catch (Exception ex) {
                                javax.swing.JOptionPane.showMessageDialog(panel,
                                    "Error parsing data: " + ex.getMessage(),
                                    "Error",
                                    javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                });
                
                com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                payload.addProperty("month", month);
                payload.addProperty("year", year);
                netClient.send("get_salary_calculations", payload);
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(panel,
                    "Error: " + ex.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        };
        
        loadBtn.addActionListener(e -> loadData.run());
        
        // Auto-load on panel creation
        javax.swing.SwingUtilities.invokeLater(loadData);
        
        // Publish salaries action
        publishBtn.addActionListener(e -> {
            int month = Integer.parseInt((String) monthCombo.getSelectedItem());
            int year = (int) yearCombo.getSelectedItem();
            
            int confirm = javax.swing.JOptionPane.showConfirmDialog(panel,
                "Publish salaries for " + month + "/" + year + "?\nEmployees will see their salaries immediately!",
                "Confirm Publish",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE);
            
            if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                try {
                    netClient.setMessageHandler((type, payload) -> {
                        if ("publish_salaries_response".equals(type) && payload != null && payload.isJsonObject()) {
                            com.google.gson.JsonObject jo = payload.getAsJsonObject();
                            if (jo.has("status") && "error".equals(jo.get("status").getAsString())) {
                                javax.swing.JOptionPane.showMessageDialog(panel,
                                    "Error: " + jo.get("message").getAsString(),
                                    "Error",
                                    javax.swing.JOptionPane.ERROR_MESSAGE);
                            } else {
                                int count = jo.has("count") ? jo.get("count").getAsInt() : 0;
                                javax.swing.JOptionPane.showMessageDialog(panel,
                                    "Published " + count + " salaries successfully!",
                                    "Success",
                                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                loadData.run(); // Refresh
                            }
                        }
                    });
                    
                    com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                    payload.addProperty("month", month);
                    payload.addProperty("year", year);
                    netClient.send("publish_salaries", payload);
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(panel,
                        "Error: " + ex.getMessage(),
                        "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        // Add bonus action
        addBonusBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                javax.swing.JOptionPane.showMessageDialog(panel,
                    "Please select an employee first.",
                    "No Selection",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int salaryCalcId = (int) table.getValueAt(selectedRow, 0);
            String userName = (String) table.getValueAt(selectedRow, 1);
            
            showAddBonusDialog(salaryCalcId, userName, loadData);
        });
        
        // View details action
        viewDetailsBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                javax.swing.JOptionPane.showMessageDialog(panel,
                    "Please select an employee first.",
                    "No Selection",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Show detailed salary breakdown
            StringBuilder details = new StringBuilder();
            details.append("Employee: ").append(table.getValueAt(selectedRow, 1)).append("\n");
            details.append("Role: ").append(table.getValueAt(selectedRow, 2)).append("\n\n");
            details.append("Hours Worked: ").append(table.getValueAt(selectedRow, 3)).append("\n");
            details.append("Hourly Rate: ").append(table.getValueAt(selectedRow, 4)).append(" MDL\n\n");
            details.append("Base Salary: ").append(table.getValueAt(selectedRow, 5)).append(" MDL\n");
            details.append("Bonuses: ").append(table.getValueAt(selectedRow, 6)).append(" MDL\n");
            details.append("Gross Salary: ").append(table.getValueAt(selectedRow, 7)).append(" MDL\n\n");
            details.append("Tax (15%): ").append(table.getValueAt(selectedRow, 8)).append(" MDL\n\n");
            details.append("NET SALARY: ").append(table.getValueAt(selectedRow, 9)).append(" MDL\n");
            
            javax.swing.JOptionPane.showMessageDialog(panel,
                details.toString(),
                "Salary Details",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
        });
        
        return panel;
    }

    private void showHourlyRateHistory(int userId, String userName) {
        JFrame historyFrame = new JFrame("Hourly Rate History - " + userName);
        historyFrame.setSize(600, 400);
        historyFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        historyFrame.setLayout(new BorderLayout());
        
        String[] columns = {"Hourly Rate (MDL)", "Valid From", "Valid To", "Set By"};
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        javax.swing.JTable table = new javax.swing.JTable(model);
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(table);
        
        historyFrame.add(scrollPane, BorderLayout.CENTER);
        
        try {
            netClient.setMessageHandler((type, payload) -> {
                if ("get_hourly_rate_history_response".equals(type) && payload != null && payload.isJsonObject()) {
                    com.google.gson.JsonObject jo = payload.getAsJsonObject();
                    if (jo.has("status") && "error".equals(jo.get("status").getAsString())) {
                        javax.swing.JOptionPane.showMessageDialog(historyFrame,
                            "Error: " + jo.get("message").getAsString(),
                            "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    if (jo.has("hourly_rate_history")) {
                        try {
                            com.google.gson.JsonArray history = com.google.gson.JsonParser.parseString(
                                jo.get("hourly_rate_history").getAsString()).getAsJsonArray();
                            
                            model.setRowCount(0);
                            for (int i = 0; i < history.size(); i++) {
                                com.google.gson.JsonObject h = history.get(i).getAsJsonObject();
                                Object[] row = {
                                    h.get("hourlyRate").getAsDouble(),
                                    h.get("validFrom").getAsString(),
                                    h.has("validTo") && !h.get("validTo").isJsonNull() ? 
                                        h.get("validTo").getAsString() : "Current",
                                    h.has("setByName") && !h.get("setByName").isJsonNull() ? 
                                        h.get("setByName").getAsString() : "Unknown"
                                };
                                model.addRow(row);
                            }
                        } catch (Exception ex) {
                            javax.swing.JOptionPane.showMessageDialog(historyFrame,
                                "Error parsing data: " + ex.getMessage(),
                                "Error",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
            
            com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
            payload.addProperty("user_id", userId);
            netClient.send("get_hourly_rate_history", payload);
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(historyFrame,
                "Error: " + ex.getMessage(),
                "Error",
                javax.swing.JOptionPane.ERROR_MESSAGE);
        }
        
        historyFrame.setLocationRelativeTo(frame);
        historyFrame.setVisible(true);
    }

    private void showAddBonusDialog(int salaryCalcId, String userName, Runnable refreshCallback) {
        JDialog dialog = new JDialog(frame, "Add Bonus - " + userName, true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 200);
        
        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        formPanel.add(new JLabel("Amount (MDL):"));
        JTextField amountField = new JTextField();
        formPanel.add(amountField);
        
        formPanel.add(new JLabel("Description:"));
        JTextField descField = new JTextField();
        formPanel.add(descField);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addBtn = new JButton("Add Bonus");
        JButton cancelBtn = new JButton("Cancel");
        
        addBtn.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText().trim());
                String description = descField.getText().trim();
                
                if (amount < 0) {
                    javax.swing.JOptionPane.showMessageDialog(dialog,
                        "Amount cannot be negative!",
                        "Invalid Input",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (description.isEmpty()) {
                    javax.swing.JOptionPane.showMessageDialog(dialog,
                        "Please enter a description!",
                        "Invalid Input",
                        javax.swing.JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                netClient.setMessageHandler((type, payload) -> {
                    if ("add_salary_bonus_response".equals(type) && payload != null && payload.isJsonObject()) {
                        com.google.gson.JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("status") && "error".equals(jo.get("status").getAsString())) {
                            javax.swing.JOptionPane.showMessageDialog(dialog,
                                "Error: " + jo.get("message").getAsString(),
                                "Error",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                        } else {
                            javax.swing.JOptionPane.showMessageDialog(dialog,
                                "Bonus added successfully!",
                                "Success",
                                javax.swing.JOptionPane.INFORMATION_MESSAGE);
                            dialog.dispose();
                            if (refreshCallback != null) {
                                refreshCallback.run();
                            }
                        }
                    }
                });
                
                com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                payload.addProperty("salary_calculation_id", salaryCalcId);
                payload.addProperty("amount", amount);
                payload.addProperty("description", description);
                netClient.send("add_salary_bonus", payload);
            } catch (NumberFormatException ex) {
                javax.swing.JOptionPane.showMessageDialog(dialog,
                    "Invalid amount format!",
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(dialog,
                    "Error: " + ex.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(addBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void showEditUserDialog(JFrame parent, int userId, String currentName, String currentRole, String currentPhone, String currentAddress, String currentJob, String currentTeamLeader, JButton refreshBtn) {
        JDialog editDialog = new JDialog(parent, "Editare Utilizator", true);
        editDialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridLayout(8, 2, 10, 10));
        formPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        formPanel.add(new JLabel("ID Utilizator:"));
        formPanel.add(new JLabel(String.valueOf(userId)));
        
        formPanel.add(new JLabel("Nume:"));
        JTextField nameField = new JTextField(currentName);
        formPanel.add(nameField);
        
        formPanel.add(new JLabel("Rol:"));
        javax.swing.JComboBox<String> roleCombo = new javax.swing.JComboBox<>(new String[]{"WORKER", "TEAMLEADER", "HR", "ADMIN"});
        roleCombo.setSelectedItem(currentRole);
        formPanel.add(roleCombo);
        
        formPanel.add(new JLabel("Telefon:"));
        JTextField phoneField = new JTextField(currentPhone != null ? currentPhone : "");
        formPanel.add(phoneField);
        
        formPanel.add(new JLabel("Adresă:"));
        JTextField addressField = new JTextField(currentAddress != null ? currentAddress : "");
        formPanel.add(addressField);
        
        formPanel.add(new JLabel("Job:"));
        javax.swing.JComboBox<String> jobCombo = new javax.swing.JComboBox<>();
        jobCombo.setEditable(true);
        fetchJobTitles(jobCombo, currentJob);
        formPanel.add(jobCombo);
        
        formPanel.add(new JLabel("Team Leader:"));
        javax.swing.JComboBox<String> teamLeaderCombo = new javax.swing.JComboBox<>();
        teamLeaderCombo.addItem("--- Fără Team Leader ---");
        // Fetch team leaders from server
        fetchTeamLeaders(teamLeaderCombo, currentTeamLeader);
        formPanel.add(teamLeaderCombo);
        
        // Enable/disable team leader combo based on role selection
        roleCombo.addActionListener(evt -> {
            String selectedRole = (String) roleCombo.getSelectedItem();
            teamLeaderCombo.setEnabled("WORKER".equals(selectedRole));
        });
        // Initial state: disable if not WORKER
        teamLeaderCombo.setEnabled("WORKER".equals(currentRole));
        
        editDialog.add(formPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        JButton saveBtn = new JButton("Salvare");
        saveBtn.addActionListener(e -> {
            String newName = nameField.getText().trim();
            String newRole = (String) roleCombo.getSelectedItem();
            String newPhone = phoneField.getText().trim();
            String newAddress = addressField.getText().trim();
            String newJob = jobCombo.getEditor().getItem() != null ? jobCombo.getEditor().getItem().toString().trim() : "";
            String selectedTeamLeader = (String) teamLeaderCombo.getSelectedItem();
            
            if (newName.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(editDialog, "Numele nu poate fi gol", "Eroare", javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Extract team leader ID from selection
            Integer teamLeaderId = null;
            if (selectedTeamLeader != null && !selectedTeamLeader.startsWith("---")) {
                String[] parts = selectedTeamLeader.split(" - ");
                if (parts.length > 0) {
                    try {
                        teamLeaderId = Integer.parseInt(parts[0]);
                    } catch (NumberFormatException ex) {
                        // ignore
                    }
                }
            }
            
            // Disable button to prevent double-clicks
            saveBtn.setEnabled(false);
            
            try {
                System.out.println("DEBUG CLIENT EDIT: Sending update request for user " + userId);
                JsonObject req = new JsonObject();
                req.addProperty("userId", userId);
                req.addProperty("newName", newName);
                req.addProperty("newRole", newRole);
                req.addProperty("newPhone", newPhone);
                req.addProperty("newAddress", newAddress);
                req.addProperty("newJob", newJob);
                if (teamLeaderId != null) {
                    req.addProperty("teamLeaderId", teamLeaderId);
                }
                System.out.println("DEBUG CLIENT EDIT: Request payload: " + req);
                
                // Send request and wait for response
                netClient.send("update_user_info", req);
                System.out.println("DEBUG CLIENT EDIT: Request sent, waiting for response...");
                
                // Use a timer to wait for response
                javax.swing.Timer timer = new javax.swing.Timer(100, null);
                final int[] attempts = {0};
                timer.addActionListener(evt -> {
                    attempts[0]++;
                    // Try for max 5 seconds (50 attempts * 100ms)
                    if (attempts[0] > 50) {
                        timer.stop();
                        saveBtn.setEnabled(true);
                        javax.swing.JOptionPane.showMessageDialog(editDialog, 
                            "Timeout: Nu s-a primit răspuns de la server", 
                            "Eroare", 
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                        System.out.println("DEBUG CLIENT EDIT: Timeout waiting for response");
                    }
                });
                
                // Set up ONE-TIME message handler for this specific update
                netClient.setMessageHandler((type, payload) -> {
                    System.out.println("DEBUG CLIENT EDIT: Received message type: " + type);
                    if ("update_user_info_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        System.out.println("DEBUG CLIENT EDIT: Response payload: " + jo);
                        timer.stop(); // Stop timeout timer
                        
                        // Check for 'status' field
                        if (jo.has("status") && "ok".equals(jo.get("status").getAsString())) {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                System.out.println("DEBUG CLIENT EDIT: Success - closing dialog and refreshing");
                                // Close dialog first
                                editDialog.dispose();
                                // Then refresh the table
                                refreshBtn.doClick();
                                // Show success message last
                                javax.swing.JOptionPane.showMessageDialog(parent, 
                                    "Datele au fost actualizate cu succes!", 
                                    "Succes", 
                                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
                            });
                        } else if (jo.has("status") && "error".equals(jo.get("status").getAsString())) {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                System.out.println("DEBUG CLIENT EDIT: Error - " + jo.get("message").getAsString());
                                saveBtn.setEnabled(true);
                                javax.swing.JOptionPane.showMessageDialog(editDialog, 
                                    "Eroare: " + jo.get("message").getAsString(), 
                                    "Eroare", 
                                    javax.swing.JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    }
                });
                
                timer.start();
                
            } catch (Exception ex) {
                System.err.println("DEBUG CLIENT EDIT: Exception - " + ex.getMessage());
                ex.printStackTrace();
                saveBtn.setEnabled(true);
                javax.swing.JOptionPane.showMessageDialog(editDialog, "Error: " + ex.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(saveBtn);
        
        JButton cancelBtn = new JButton("Anulare");
        cancelBtn.addActionListener(e -> editDialog.dispose());
        buttonPanel.add(cancelBtn);
        
        editDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        editDialog.setSize(new Dimension(400, 420));
        editDialog.setLocationRelativeTo(parent);
        editDialog.setVisible(true);
    }

    private void fetchTeamLeaders(javax.swing.JComboBox<String> comboBox, String currentTeamLeader) {
        try {
            netClient.setMessageHandler((type, payload) -> {
                if ("all_workers_response".equals(type) && payload != null && payload.isJsonObject()) {
                    JsonObject jo = payload.getAsJsonObject();
                    if (jo.has("workers")) {
                        try {
                            JsonArray workers = JsonParser.parseString(jo.get("workers").getAsString()).getAsJsonArray();
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                for (int i = 0; i < workers.size(); i++) {
                                    JsonObject worker = workers.get(i).getAsJsonObject();
                                    String role = worker.get("role").getAsString();
                                    if ("TEAMLEADER".equals(role) || "HR".equals(role) || "ADMIN".equals(role)) {
                                        int id = worker.get("id").getAsInt();
                                        String name = worker.get("name").getAsString();
                                        String item = id + " - " + name + " (" + role + ")";
                                        comboBox.addItem(item);
                                        
                                        // Select current team leader if matches
                                        if (currentTeamLeader != null && currentTeamLeader.contains(name)) {
                                            comboBox.setSelectedItem(item);
                                        }
                                    }
                                }
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
            netClient.send("get_all_workers", new JsonObject());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void fetchJobTitles(javax.swing.JComboBox<String> comboBox, String currentJob) {
        try {
            netClient.setMessageHandler((type, payload) -> {
                if ("job_titles_response".equals(type) && payload != null && payload.isJsonObject()) {
                    JsonObject jo = payload.getAsJsonObject();
                    if (jo.has("jobTitles")) {
                        try {
                            JsonArray jobTitles = JsonParser.parseString(jo.get("jobTitles").getAsString()).getAsJsonArray();
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                for (int i = 0; i < jobTitles.size(); i++) {
                                    JsonObject jobTitle = jobTitles.get(i).getAsJsonObject();
                                    String title = jobTitle.get("title").getAsString();
                                    comboBox.addItem(title);
                                }
                                // Select current job if provided
                                if (currentJob != null && !currentJob.isEmpty()) {
                                    comboBox.setSelectedItem(currentJob);
                                }
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
            netClient.send("get_job_titles", new JsonObject());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
