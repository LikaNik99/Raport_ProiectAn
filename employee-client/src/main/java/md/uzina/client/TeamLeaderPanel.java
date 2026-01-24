package md.uzina.client;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TeamLeaderPanel {
    private final JFrame frame;
    private final ClientApp.ClientUser user;
    private final Runnable onLogout;
    private final NetClient netClient;

    public TeamLeaderPanel(ClientApp.ClientUser user, Runnable onLogout, NetClient netClient) {
        this.user = user;
        this.onLogout = onLogout;
        this.netClient = netClient;
        this.frame = new JFrame("Team Leader Panel - " + user.name);
    }

    public void show() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        // Header: greeting + logout
        JPanel header = new JPanel();
        header.add(new JLabel("Welcome, " + user.name + " (" + user.role + ")"));
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            frame.dispose();
            onLogout.run();
        });
        header.add(logoutBtn);
        frame.add(header, BorderLayout.NORTH);

        // Main content: team leader features
        JPanel content = new JPanel(new BorderLayout());
        JTextArea info = new JTextArea();
        info.setText(
            "Team Leader Features:\n\n" +
            "1. Start/End Work - Mark your own arrival/departure\n" +
            "2. View Team Attendance - See your team's attendance records\n" +
            "3. View Team Statistics - Analyze team performance metrics\n" +
            "4. View Leave Requests - Review and manage leave requests from team members\n" +
            "5. Change Password - Update your account password\n\n" +
            "User ID: " + user.id + "\n" +
            "Role: " + user.role
        );
        info.setEditable(false);
        content.add(new JScrollPane(info), BorderLayout.CENTER);
        frame.add(content, BorderLayout.CENTER);

        // Buttons for features (include start/end/status similar to Worker)
        JPanel buttons = new JPanel();
        JButton startWorkBtn = new JButton("Start Work");
        startWorkBtn.addActionListener(e -> {
            try {
                netClient.setMessageHandler((type, payload) -> {
                    if ("start_work_response".equals(type)) {
                        // ignore details here - simple notification
                    }
                });
                netClient.send("start_work", new com.google.gson.JsonObject());
            } catch (Exception ex) { javax.swing.JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage()); }
        });
        buttons.add(startWorkBtn);

        JButton endWorkBtn = new JButton("End Work");
        endWorkBtn.addActionListener(e -> {
            try {
                netClient.setMessageHandler((type, payload) -> {
                    if ("end_work_response".equals(type)) {
                        // ignore details
                    }
                });
                netClient.send("end_work", new com.google.gson.JsonObject());
            } catch (Exception ex) { javax.swing.JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage()); }
        });
        buttons.add(endWorkBtn);

        JButton statusBtn = new JButton("View Status");
        statusBtn.addActionListener(e -> {
            try {
                netClient.setMessageHandler((type, payload) -> {
                    if ("work_status_response".equals(type) && payload != null && payload.isJsonObject()) {
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            javax.swing.JOptionPane.showMessageDialog(frame, "Status: " + payload.getAsJsonObject().get("status").getAsString());
                        });
                    }
                });
                netClient.send("get_work_status", new com.google.gson.JsonObject());
            } catch (Exception ex) { javax.swing.JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage()); }
        });
        buttons.add(statusBtn);

        JButton teamWorkStatusBtn = new JButton("View Team Work Status");
        teamWorkStatusBtn.addActionListener(e -> viewTeamWorkStatus());
        buttons.add(teamWorkStatusBtn);

        buttons.add(new JButton("View Team Attendance"));
        buttons.add(new JButton("View Team Statistics"));

        // Leave management for team leader
        JButton requestLeaveBtn = new JButton("Request Leave");
        requestLeaveBtn.addActionListener(e -> requestLeave());
        buttons.add(requestLeaveBtn);

        JButton myLeaveBtn = new JButton("View My Requests");
        myLeaveBtn.addActionListener(e -> viewMyLeaveRequests());
        buttons.add(myLeaveBtn);
        
        JButton viewLeaveBtn = new JButton("View Team Leave Requests");
        viewLeaveBtn.addActionListener(e -> viewTeamLeaveRequests());
        buttons.add(viewLeaveBtn);
        
        JButton mySalaryBtn = new JButton("My Salary");
        mySalaryBtn.addActionListener(e -> showMySalary());
        buttons.add(mySalaryBtn);
        
        buttons.add(new JButton("Change Password"));
        frame.add(buttons, BorderLayout.SOUTH);

        frame.setSize(new Dimension(700, 450));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void viewTeamLeaveRequests() {
        JFrame viewFrame = new JFrame("Team Leave Requests");
        viewFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        viewFrame.setLayout(new BorderLayout(8, 8));

        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        viewFrame.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton fetchBtn = new JButton("Fetch Team Leave Requests");
        fetchBtn.addActionListener(e -> {
            try {
                netClient.setMessageHandler((type, payload) -> {
                    if ("team_leave_requests_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("error")) {
                            resultArea.setText("Error: " + jo.get("error").getAsString());
                        } else if (jo.has("requests")) {
                            try {
                                JsonArray requests = JsonParser.parseString(jo.get("requests").getAsString()).getAsJsonArray();
                                StringBuilder sb = new StringBuilder();
                                sb.append("ID\tWorker\tFrom\t\tTo\t\tReason\t\tStatus\n");
                                sb.append("==\t======\t====\t\t==\t\t======\t\t======\n");
                                for (int i = 0; i < requests.size(); i++) {
                                    JsonObject req = requests.get(i).getAsJsonObject();
                                    sb.append(req.get("id").getAsInt()).append("\t")
                                      .append(req.get("workerName").getAsString().substring(0, Math.min(6, req.get("workerName").getAsString().length()))).append("\t")
                                      .append(req.get("dateFrom").getAsString()).append("\t")
                                      .append(req.get("dateTo").getAsString()).append("\t")
                                      .append(req.get("reason").getAsString().substring(0, Math.min(8, req.get("reason").getAsString().length()))).append("\t\t")
                                      .append(req.get("status").getAsString()).append("\n");
                                }
                                resultArea.setText(sb.toString());
                            } catch (Exception ex) {
                                resultArea.setText("Error parsing requests: " + ex.getMessage());
                            }
                        }
                    }
                });

                resultArea.setText("Fetching team leave requests...");
                JsonObject req = new JsonObject();
                netClient.send("get_team_leave_requests", req);
            } catch (Exception ex) {
                resultArea.setText("Error: " + ex.getMessage());
            }
        });
        buttonPanel.add(fetchBtn);
        viewFrame.add(buttonPanel, BorderLayout.SOUTH);

        viewFrame.setSize(new Dimension(800, 400));
        viewFrame.setLocationRelativeTo(null);
        viewFrame.setVisible(true);
    }

    // --- Leave request submission ---
    private void requestLeave() {
        JFrame dialog = new JFrame("Request Leave");
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel();
        form.add(new JLabel("From (YYYY-MM-DD):"));
        javax.swing.JTextField fromField = new javax.swing.JTextField(10);
        fromField.setText(java.time.LocalDate.now().toString());
        form.add(fromField);

        form.add(new JLabel("To:"));
        javax.swing.JTextField toField = new javax.swing.JTextField(10);
        toField.setText(java.time.LocalDate.now().toString());
        form.add(toField);

        form.add(new JLabel("Reason:"));
        javax.swing.JTextField reasonField = new javax.swing.JTextField(20);
        form.add(reasonField);

        dialog.add(form, BorderLayout.NORTH);

        JTextArea result = new JTextArea();
        result.setEditable(false);
        dialog.add(new JScrollPane(result), BorderLayout.CENTER);

        JButton submitBtn = new JButton("Submit");
        submitBtn.addActionListener(e -> {
            try {
                java.time.LocalDate from = java.time.LocalDate.parse(fromField.getText().trim());
                java.time.LocalDate to = java.time.LocalDate.parse(toField.getText().trim());
                String reason = reasonField.getText().trim();
                if (reason.isEmpty()) {
                    javax.swing.JOptionPane.showMessageDialog(dialog, "Please enter a reason");
                    return;
                }

                netClient.setMessageHandler((type, payload) -> {
                    if ("submit_leave_request_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        String status = jo.has("status") ? jo.get("status").getAsString() : "error";
                        String message = jo.has("message") ? jo.get("message").getAsString() : "";
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            result.setText(status + ": " + message);
                        });
                    }
                });

                JsonObject req = new JsonObject();
                req.addProperty("dateFrom", from.toString());
                req.addProperty("dateTo", to.toString());
                req.addProperty("reason", reason);
                netClient.send("submit_leave_request", req);
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });

        JPanel south = new JPanel();
        south.add(submitBtn);
        dialog.add(south, BorderLayout.SOUTH);

        dialog.setSize(new Dimension(500, 300));
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    // --- Personal leave requests view ---
    private void viewMyLeaveRequests() {
        JFrame viewFrame = new JFrame("My Leave Requests");
        viewFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        viewFrame.setLayout(new BorderLayout(8, 8));

        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        viewFrame.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton fetchBtn = new JButton("Fetch My Requests");
        fetchBtn.addActionListener(e -> {
            try {
                netClient.setMessageHandler((type, payload) -> {
                    if ("user_leave_requests_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("error")) {
                            resultArea.setText("Error: " + jo.get("error").getAsString());
                        } else if (jo.has("requests")) {
                            try {
                                JsonArray requests = JsonParser.parseString(jo.get("requests").getAsString()).getAsJsonArray();
                                StringBuilder sb = new StringBuilder();
                                sb.append("ID\tFrom\t\tTo\t\tReason\t\tStatus\n");
                                sb.append("==\t====\t\t==\t\t======\t\t======\n");
                                for (int i = 0; i < requests.size(); i++) {
                                    JsonObject req = requests.get(i).getAsJsonObject();
                                    sb.append(req.get("id").getAsInt()).append("\t")
                                      .append(req.get("dateFrom").getAsString()).append("\t")
                                      .append(req.get("dateTo").getAsString()).append("\t")
                                      .append(req.get("reason").getAsString().substring(0, Math.min(8, req.get("reason").getAsString().length()))).append("\t\t")
                                      .append(req.get("status").getAsString()).append("\n");
                                }
                                resultArea.setText(sb.toString());
                            } catch (Exception ex) {
                                resultArea.setText("Error parsing requests: " + ex.getMessage());
                            }
                        }
                    }
                });

                netClient.send("get_user_leave_requests", new JsonObject());
            } catch (Exception ex) {
                resultArea.setText("Error: " + ex.getMessage());
            }
        });
        buttonPanel.add(fetchBtn);
        viewFrame.add(buttonPanel, BorderLayout.SOUTH);

        viewFrame.setSize(new Dimension(700, 400));
        viewFrame.setLocationRelativeTo(null);
        viewFrame.setVisible(true);
    }

    private void viewTeamWorkStatus() {
        JFrame statusFrame = new JFrame("Team Work Status");
        statusFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        statusFrame.setLayout(new BorderLayout(8, 8));

        javax.swing.JTable table = new javax.swing.JTable();
        javax.swing.table.DefaultTableModel tableModel = new javax.swing.table.DefaultTableModel(
            new Object[]{"User ID", "Name", "Role", "Work Date", "Start Time", "End Time", "Status"}, 0
        );
        table.setModel(tableModel);
        statusFrame.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton fetchBtn = new JButton("Fetch Team Work Status");
        fetchBtn.addActionListener(e -> {
            try {
                netClient.setMessageHandler((type, payload) -> {
                    if ("team_work_status_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("error")) {
                            javax.swing.JOptionPane.showMessageDialog(statusFrame, "Error: " + jo.get("error").getAsString(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                        } else if (jo.has("team_work_status")) {
                            try {
                                JsonArray statuses = JsonParser.parseString(jo.get("team_work_status").getAsString()).getAsJsonArray();
                                tableModel.setRowCount(0);
                                for (int i = 0; i < statuses.size(); i++) {
                                    JsonObject status = statuses.get(i).getAsJsonObject();
                                    tableModel.addRow(new Object[]{
                                        status.get("userId").getAsInt(),
                                        status.get("userName").getAsString(),
                                        status.get("userRole").getAsString(),
                                        status.has("workDate") && !status.get("workDate").isJsonNull() ? status.get("workDate").getAsString() : "N/A",
                                        status.has("startTime") && !status.get("startTime").isJsonNull() ? status.get("startTime").getAsString() : "N/A",
                                        status.has("endTime") && !status.get("endTime").isJsonNull() ? status.get("endTime").getAsString() : "N/A",
                                        status.get("status").getAsString()
                                    });
                                }
                            } catch (Exception ex) {
                                javax.swing.JOptionPane.showMessageDialog(statusFrame, "Error parsing data: " + ex.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                });

                netClient.send("get_team_work_status", new JsonObject());
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(statusFrame, "Error: " + ex.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(fetchBtn);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> statusFrame.dispose());
        buttonPanel.add(closeBtn);

        statusFrame.add(buttonPanel, BorderLayout.SOUTH);

        statusFrame.setSize(new Dimension(900, 500));
        statusFrame.setLocationRelativeTo(null);
        statusFrame.setVisible(true);

        // Auto-fetch on open
        fetchBtn.doClick();
    }

    private void showMySalary() {
        JFrame salaryFrame = new JFrame("My Salary - " + user.name);
        salaryFrame.setSize(1200, 700);
        salaryFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        salaryFrame.setLayout(new BorderLayout(10, 10));
        
        javax.swing.JTabbedPane tabbedPane = new javax.swing.JTabbedPane();
        
        // Tab 1: Hourly Rate History
        JPanel rateHistoryPanel = createRateHistoryPanel();
        tabbedPane.addTab("Hourly Rate History", rateHistoryPanel);
        
        // Tab 2: Salary History
        JPanel salaryHistoryPanel = createSalaryHistoryPanel();
        tabbedPane.addTab("Salary History", salaryHistoryPanel);
        
        salaryFrame.add(tabbedPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> salaryFrame.dispose());
        bottomPanel.add(closeBtn);
        salaryFrame.add(bottomPanel, BorderLayout.SOUTH);
        
        salaryFrame.setLocationRelativeTo(frame);
        salaryFrame.setVisible(true);
    }

    private JPanel createRateHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String[] columns = {"Hourly Rate (MDL)", "Valid From", "Valid To", "Set By"};
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
        
        JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        buttonPanel.add(refreshBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        Runnable loadData = () -> {
            try {
                netClient.setMessageHandler((type, payload) -> {
                    if ("get_hourly_rate_history_response".equals(type) && payload != null && payload.isJsonObject()) {
                        com.google.gson.JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("status") && "error".equals(jo.get("status").getAsString())) {
                            javax.swing.JOptionPane.showMessageDialog(panel,
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
                                javax.swing.JOptionPane.showMessageDialog(panel,
                                    "Error parsing data: " + ex.getMessage(),
                                    "Error",
                                    javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                });
                
                com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                payload.addProperty("user_id", user.id);
                netClient.send("get_hourly_rate_history", payload);
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(panel,
                    "Error: " + ex.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        };
        
        refreshBtn.addActionListener(e -> loadData.run());
        javax.swing.SwingUtilities.invokeLater(loadData);
        
        return panel;
    }

    private JPanel createSalaryHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String[] columns = {"Month/Year", "Hours", "Rate", "Base", "Bonuses", "Gross", "Tax 15%", "Net", "Published"};
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
        
        JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        JButton viewDetailsBtn = new JButton("View Bonuses");
        buttonPanel.add(refreshBtn);
        buttonPanel.add(viewDetailsBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        Runnable loadData = () -> {
            try {
                netClient.setMessageHandler((type, payload) -> {
                    if ("get_my_salary_history_response".equals(type) && payload != null && payload.isJsonObject()) {
                        com.google.gson.JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("status") && "error".equals(jo.get("status").getAsString())) {
                            javax.swing.JOptionPane.showMessageDialog(panel,
                                "Error: " + jo.get("message").getAsString(),
                                "Error",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        
                        if (jo.has("salary_history")) {
                            try {
                                com.google.gson.JsonArray history = com.google.gson.JsonParser.parseString(
                                    jo.get("salary_history").getAsString()).getAsJsonArray();
                                
                                model.setRowCount(0);
                                for (int i = 0; i < history.size(); i++) {
                                    com.google.gson.JsonObject s = history.get(i).getAsJsonObject();
                                    Object[] row = {
                                        s.get("month").getAsInt() + "/" + s.get("year").getAsInt(),
                                        String.format("%.2f", s.get("hoursWorked").getAsDouble()),
                                        String.format("%.2f", s.get("hourlyRate").getAsDouble()),
                                        String.format("%.2f", s.get("baseSalary").getAsDouble()),
                                        String.format("%.2f", s.get("bonusesTotal").getAsDouble()),
                                        String.format("%.2f", s.get("grossSalary").getAsDouble()),
                                        String.format("%.2f", s.get("taxAmount").getAsDouble()),
                                        String.format("%.2f", s.get("netSalary").getAsDouble()),
                                        s.get("publishedAt").getAsString()
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
                
                netClient.send("get_my_salary_history", new com.google.gson.JsonObject());
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(panel,
                    "Error: " + ex.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        };
        
        refreshBtn.addActionListener(e -> loadData.run());
        
        viewDetailsBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                javax.swing.JOptionPane.showMessageDialog(panel,
                    "Please select a salary record first.",
                    "No Selection",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            StringBuilder details = new StringBuilder();
            details.append("Salary Breakdown\n");
            details.append("================\n\n");
            details.append("Period: ").append(table.getValueAt(selectedRow, 0)).append("\n\n");
            details.append("Hours Worked: ").append(table.getValueAt(selectedRow, 1)).append(" hours\n");
            details.append("Hourly Rate: ").append(table.getValueAt(selectedRow, 2)).append(" MDL\n\n");
            details.append("Base Salary: ").append(table.getValueAt(selectedRow, 3)).append(" MDL\n");
            details.append("Bonuses: ").append(table.getValueAt(selectedRow, 4)).append(" MDL\n");
            details.append("Gross Salary: ").append(table.getValueAt(selectedRow, 5)).append(" MDL\n\n");
            details.append("Tax (15%): ").append(table.getValueAt(selectedRow, 6)).append(" MDL\n\n");
            details.append("NET SALARY: ").append(table.getValueAt(selectedRow, 7)).append(" MDL\n\n");
            details.append("Published: ").append(table.getValueAt(selectedRow, 8));
            
            javax.swing.JOptionPane.showMessageDialog(panel,
                details.toString(),
                "Salary Details",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
        });
        
        javax.swing.SwingUtilities.invokeLater(loadData);
        
        return panel;
    }
}
