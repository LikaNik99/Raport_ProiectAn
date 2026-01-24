package md.uzina.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WorkerPanel {
    private final JFrame frame;
    private final ClientApp.ClientUser user;
    private final Runnable onLogout;
    private final NetClient netClient;
    private JTextArea statusArea;
    private final AtomicReference<String> workStatus = new AtomicReference<>("INACTIVE");

    public WorkerPanel(ClientApp.ClientUser user, Runnable onLogout, NetClient netClient) {
        this.user = user;
        this.onLogout = onLogout;
        this.netClient = netClient;
        this.frame = new JFrame("Panou Angajat - " + user.name);
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

        // Main content: worker features + status
        JPanel content = new JPanel(new BorderLayout());
        statusArea = new JTextArea();
        statusArea.setText(
            "Funcționalități Angajat:\n\n" +
            "1. Începe Lucru - Marchează sosirea ta\n" +
            "2. Termină Lucru - Marchează plecarea ta\n" +
            "3. Vezi Status - Verifică sesiunea curentă de lucru\n" +
            "4. Cerere Concediu - Trimite cereri de concediu\n" +
            "5. Vezi Cererile Mele - Vizualizează cererile de concediu\n" +
            "6. Salariul Meu - Vezi informațiile despre salariu\n\n" +
            "ID Utilizator: " + user.id + "\n" +
            "Rol: " + user.role
        );
        statusArea.setEditable(false);
        content.add(new JScrollPane(statusArea), BorderLayout.CENTER);
        frame.add(content, BorderLayout.CENTER);

        // Buttons for features - use grid layout
        JPanel buttons = new JPanel(new GridLayout(0, 3, 5, 5));
        buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JButton startWorkBtn = new JButton("Începe Lucru");
        startWorkBtn.addActionListener(e -> startWork());
        buttons.add(startWorkBtn);
        
        JButton endWorkBtn = new JButton("Termină Lucru");
        endWorkBtn.addActionListener(e -> endWork());
        buttons.add(endWorkBtn);
        
        JButton statusBtn = new JButton("Vezi Status");
        statusBtn.addActionListener(e -> getWorkStatus());
        buttons.add(statusBtn);
        
        JButton requestLeaveBtn = new JButton("Cerere Concediu");
        requestLeaveBtn.addActionListener(e -> requestLeave());
        buttons.add(requestLeaveBtn);
        
        JButton viewLeaveBtn = new JButton("Vezi Cererile Mele");
        viewLeaveBtn.addActionListener(e -> viewMyLeaveRequests());
        buttons.add(viewLeaveBtn);
        
        JButton mySalaryBtn = new JButton("Salariul Meu");
        mySalaryBtn.addActionListener(e -> showMySalary());
        buttons.add(mySalaryBtn);
        frame.add(buttons, BorderLayout.SOUTH);

        frame.setSize(new Dimension(700, 450));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void startWork() {
        try {
            JsonObject payload = new JsonObject();
            netClient.setMessageHandler(this::handleStartWorkResponse);
            netClient.send("start_work", payload);
            statusArea.append("\n[...] Trimit cererea de începere lucru...");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Eroare: " + ex.getMessage(), "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleStartWorkResponse(String type, JsonElement payload) {
        SwingUtilities.invokeLater(() -> {
            if ("start_work_response".equals(type) && payload != null && payload.isJsonObject()) {
                JsonObject jo = payload.getAsJsonObject();
                String status = jo.has("status") ? jo.get("status").getAsString() : "error";
                String message = jo.has("message") ? jo.get("message").getAsString() : "";
                if ("ok".equals(status)) {
                    statusArea.append("\n[✓] Sesiunea de lucru a început cu succes");
                    JOptionPane.showMessageDialog(frame, "Lucrul a început!", "Succes", JOptionPane.INFORMATION_MESSAGE);
                    workStatus.set("ACTIVE");
                } else {
                    statusArea.append("\n[✗] Eroare: " + message);
                    JOptionPane.showMessageDialog(frame, message, "Eroare", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void endWork() {
        try {
            JsonObject payload = new JsonObject();
            netClient.setMessageHandler(this::handleEndWorkResponse);
            netClient.send("end_work", payload);
            statusArea.append("\n[...] Trimit cererea de încheiere lucru...");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Eroare: " + ex.getMessage(), "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleEndWorkResponse(String type, JsonElement payload) {
        SwingUtilities.invokeLater(() -> {
            if ("end_work_response".equals(type) && payload != null && payload.isJsonObject()) {
                JsonObject jo = payload.getAsJsonObject();
                String status = jo.has("status") ? jo.get("status").getAsString() : "error";
                String message = jo.has("message") ? jo.get("message").getAsString() : "";
                if ("ok".equals(status)) {
                    statusArea.append("\n[✓] Sesiunea de lucru s-a încheiat cu succes");
                    JOptionPane.showMessageDialog(frame, "Lucrul s-a încheiat!", "Succes", JOptionPane.INFORMATION_MESSAGE);
                    workStatus.set("INACTIVE");
                } else {
                    statusArea.append("\n[✗] Eroare: " + message);
                    JOptionPane.showMessageDialog(frame, message, "Eroare", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void getWorkStatus() {
        try {
            JsonObject payload = new JsonObject();
            netClient.setMessageHandler(this::handleGetWorkStatusResponse);
            netClient.send("get_work_status", payload);
            statusArea.append("\n[...] Verific statusul de lucru...");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Eroare: " + ex.getMessage(), "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleGetWorkStatusResponse(String type, JsonElement payload) {
        SwingUtilities.invokeLater(() -> {
            if ("work_status_response".equals(type) && payload != null && payload.isJsonObject()) {
                JsonObject jo = payload.getAsJsonObject();
                String status = jo.has("status") ? jo.get("status").getAsString() : "NECUNOSCUT";
                statusArea.append("\n[ℹ] Status curent: " + status);
                JOptionPane.showMessageDialog(frame, "Status: " + status, "Status Lucru", JOptionPane.INFORMATION_MESSAGE);
                workStatus.set(status);
            }
        });
    }

    private void requestLeave() {
        JFrame leaveFrame = new JFrame("Cerere Concediu");
        leaveFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        leaveFrame.setLayout(new BorderLayout(8, 8));

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("De la (AAAA-LL-ZZ):"));
        JTextField dateFromField = new JTextField(10);
        dateFromField.setText(LocalDate.now().toString());
        inputPanel.add(dateFromField);

        inputPanel.add(new JLabel("Până la (AAAA-LL-ZZ):"));
        JTextField dateToField = new JTextField(10);
        dateToField.setText(LocalDate.now().toString());
        inputPanel.add(dateToField);

        leaveFrame.add(inputPanel, BorderLayout.NORTH);

        // Large text area for reason in the center
        JPanel reasonPanel = new JPanel(new BorderLayout(5, 5));
        reasonPanel.add(new JLabel("Motiv (explică în detaliu de ce ai nevoie de concediu):"), BorderLayout.NORTH);
        JTextArea reasonArea = new JTextArea(10, 40);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonPanel.add(new JScrollPane(reasonArea), BorderLayout.CENTER);
        leaveFrame.add(reasonPanel, BorderLayout.CENTER);

        // Small status label at bottom
        JLabel statusLabel = new JLabel("Completează formularul și apasă Trimite Cerere");
        statusLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        
        JPanel buttonPanel = new JPanel();
        JButton submitBtn = new JButton("Trimite Cerere");
        submitBtn.addActionListener(e -> {
            try {
                String dateFrom = dateFromField.getText().trim();
                String dateTo = dateToField.getText().trim();
                String reason = reasonArea.getText().trim();

                if (dateFrom.isEmpty() || dateTo.isEmpty() || reason.isEmpty()) {
                    statusLabel.setText("Eroare: Toate câmpurile sunt obligatorii");
                    statusLabel.setForeground(java.awt.Color.RED);
                    return;
                }

                netClient.setMessageHandler((type, payload) -> {
                    if ("submit_leave_request_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        String status = jo.has("status") ? jo.get("status").getAsString() : "error";
                        String message = jo.has("message") ? jo.get("message").getAsString() : "";
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            if ("ok".equals(status)) {
                                statusLabel.setText("✓ " + message);
                                statusLabel.setForeground(new java.awt.Color(0, 128, 0));
                                JOptionPane.showMessageDialog(leaveFrame, message, "Succes", JOptionPane.INFORMATION_MESSAGE);
                                // Clear form after success
                                dateFromField.setText(LocalDate.now().toString());
                                dateToField.setText(LocalDate.now().toString());
                                reasonArea.setText("");
                            } else {
                                statusLabel.setText("✗ " + message);
                                statusLabel.setForeground(java.awt.Color.RED);
                                JOptionPane.showMessageDialog(leaveFrame, message, "Eroare", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                });

                JsonObject req = new JsonObject();
                req.addProperty("dateFrom", dateFrom);
                req.addProperty("dateTo", dateTo);
                req.addProperty("reason", reason);
                netClient.send("submit_leave_request", req);
                statusLabel.setText("Trimit cererea...");
                statusLabel.setForeground(java.awt.Color.BLUE);
            } catch (Exception ex) {
                statusLabel.setText("Eroare: " + ex.getMessage());
                statusLabel.setForeground(java.awt.Color.RED);
            }
        });
        buttonPanel.add(submitBtn);
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        leaveFrame.add(bottomPanel, BorderLayout.SOUTH);

        leaveFrame.setSize(new Dimension(700, 500));
        leaveFrame.setLocationRelativeTo(null);
        leaveFrame.setVisible(true);
    }

    private void viewMyLeaveRequests() {
        JFrame viewFrame = new JFrame("Cererile Mele de Concediu");
        viewFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        viewFrame.setLayout(new BorderLayout(8, 8));

        // Table for leave requests
        String[] columnNames = {"ID", "De la", "Până la", "Motiv", "Status"};
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
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int column = leaveTable.columnAtPoint(e.getPoint());
                if (column >= 0) {
                    String[] options = {"Crescator", "Descrescator", "Anuleaza"};
                    int choice = javax.swing.JOptionPane.showOptionDialog(viewFrame, 
                        "Sorteaza dupa " + leaveTable.getColumnName(column),
                        "Opțiuni Sortare",
                        javax.swing.JOptionPane.DEFAULT_OPTION,
                        javax.swing.JOptionPane.QUESTION_MESSAGE,
                        null, options, options[0]);
                    
                    if (choice == 0 || choice == 1) {
                        boolean ascending = (choice == 0);
                        sortTableByColumn(tableModel, column, ascending);
                    }
                }
            }
        });
        
        // Custom renderer for status column with colors
        leaveTable.getColumnModel().getColumn(4).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
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
        viewFrame.add(tableScroll, BorderLayout.CENTER);

        // Store all data for filtering
        java.util.concurrent.atomic.AtomicReference<JsonArray> allRequestsData = 
            new java.util.concurrent.atomic.AtomicReference<>(new JsonArray());

        // Filter panel (top)
        JPanel filterPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        
        // Date From picker
        filterPanel.add(new JLabel("Filtrare - Data de la:"));
        javax.swing.JTextField filterDateFrom = new javax.swing.JTextField(10);
        filterDateFrom.setEditable(false);
        JButton dateFromBtn = new JButton("📅");
        dateFromBtn.addActionListener(e -> {
            javax.swing.JSpinner dateSpinner = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
            javax.swing.JSpinner.DateEditor editor = new javax.swing.JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
            dateSpinner.setEditor(editor);
            int result = javax.swing.JOptionPane.showConfirmDialog(viewFrame, dateSpinner, "Selectț Data de la", 
                javax.swing.JOptionPane.OK_CANCEL_OPTION);
            if (result == javax.swing.JOptionPane.OK_OPTION) {
                java.util.Date date = (java.util.Date) dateSpinner.getValue();
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                filterDateFrom.setText(sdf.format(date));
            }
        });
        filterPanel.add(filterDateFrom);
        filterPanel.add(dateFromBtn);
        
        // Date To picker
        filterPanel.add(new JLabel("Data până la:"));
        javax.swing.JTextField filterDateTo = new javax.swing.JTextField(10);
        filterDateTo.setEditable(false);
        JButton dateToBtn = new JButton("📅");
        dateToBtn.addActionListener(e -> {
            javax.swing.JSpinner dateSpinner = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
            javax.swing.JSpinner.DateEditor editor = new javax.swing.JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
            dateSpinner.setEditor(editor);
            int result = javax.swing.JOptionPane.showConfirmDialog(viewFrame, dateSpinner, "Selectț Data până la", 
                javax.swing.JOptionPane.OK_CANCEL_OPTION);
            if (result == javax.swing.JOptionPane.OK_OPTION) {
                java.util.Date date = (java.util.Date) dateSpinner.getValue();
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                filterDateTo.setText(sdf.format(date));
            }
        });
        filterPanel.add(filterDateTo);
        filterPanel.add(dateToBtn);
        
        // Status dropdown
        filterPanel.add(new JLabel("Status:"));
        String[] statusOptions = {"Toate", "Asteptare", "Acceptat", "Refuzat"};
        javax.swing.JComboBox<String> filterStatus = new javax.swing.JComboBox<>(statusOptions);
        filterPanel.add(filterStatus);
        
        JButton applyFilterBtn = new JButton("Aplică Filtrare");
        filterPanel.add(applyFilterBtn);
        
        JButton clearFilterBtn = new JButton("Șterge Filtre & Sortare");
        filterPanel.add(clearFilterBtn);
        
        viewFrame.add(filterPanel, BorderLayout.NORTH);

        // Action panel (bottom)
        JPanel actionPanel = new JPanel();
        JButton fetchBtn = new JButton("Încarcă Cererile Mele");
        
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
                    if ("user_leave_requests_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("error")) {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                javax.swing.JOptionPane.showMessageDialog(viewFrame, "Eroare: " + jo.get("error").getAsString());
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
                                    javax.swing.JOptionPane.showMessageDialog(viewFrame, "Eroare la parsarea înregistrărilor: " + ex.getMessage());
                                });
                            }
                        }
                    }
                });
                
                netClient.send("get_user_leave_requests", new JsonObject());
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(viewFrame, "Eroare: " + ex.getMessage());
            }
        });
        
        // Apply filter
        applyFilterBtn.addActionListener(e -> {
            try {
                JsonArray allData = allRequestsData.get();
                if (allData == null || allData.size() == 0) {
                    javax.swing.JOptionPane.showMessageDialog(viewFrame, "Vă rugăm încărcați mai întâi datele");
                    return;
                }
                
                String dateFromFilter = filterDateFrom.getText().trim();
                String dateToFilter = filterDateTo.getText().trim();
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
                    if (!"Toate".equals(statusFilter)) {
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
                javax.swing.JOptionPane.showMessageDialog(viewFrame, "Eroare de filtrare: " + ex.getMessage());
            }
        });
        
        // Clear filter
        clearFilterBtn.addActionListener(e -> {
            filterDateFrom.setText("");
            filterDateTo.setText("");
            filterStatus.setSelectedIndex(0);
            JsonArray allData = allRequestsData.get();
            if (allData != null) {
                populateTable.accept(allData);
            }
        });
        
        JButton viewInfoBtn = new JButton("Vezi Informații");
        viewInfoBtn.setEnabled(false);
        
        // Enable/disable View Info button based on selection
        leaveTable.getSelectionModel().addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                boolean hasSelection = leaveTable.getSelectedRow() >= 0;
                viewInfoBtn.setEnabled(hasSelection);
            }
        });
        
        viewInfoBtn.addActionListener(e -> {
            int selectedRow = leaveTable.getSelectedRow();
            if (selectedRow < 0) {
                javax.swing.JOptionPane.showMessageDialog(viewFrame, "Vă rugăm selectați o cerere");
                return;
            }
            
            int requestId = (Integer) tableModel.getValueAt(selectedRow, 0);
            String dateFrom = (String) tableModel.getValueAt(selectedRow, 1);
            String dateTo = (String) tableModel.getValueAt(selectedRow, 2);
            String reason = (String) tableModel.getValueAt(selectedRow, 3);
            String status = (String) tableModel.getValueAt(selectedRow, 4);
            
            showLeaveRequestInfo(requestId, user.id, user.name, dateFrom, dateTo, reason, status, false);
        });
        
        actionPanel.add(fetchBtn);
        actionPanel.add(viewInfoBtn);
        viewFrame.add(actionPanel, BorderLayout.SOUTH);

        viewFrame.setSize(new Dimension(1200, 600));
        viewFrame.setLocationRelativeTo(null);
        viewFrame.setVisible(true);
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
    
    private void showLeaveRequestInfo(int requestId, String workerId, String workerName, 
                                       String dateFrom, String dateTo, String reason, String status, boolean showPrintButton) {
        JFrame infoFrame = new JFrame("Detalii Cerere Concediu - ID: " + requestId);
        infoFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        infoFrame.setLayout(new BorderLayout(10, 10));
        
        // Create printable content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new javax.swing.BoxLayout(contentPanel, javax.swing.BoxLayout.Y_AXIS));
        contentPanel.setBackground(java.awt.Color.WHITE);
        contentPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(40, 60, 40, 60));
        
        // Company header (logo + company name) - optional logo, company name from company_name.txt if present
        String companyName = "COMPANY NAME";
        try {
            java.nio.file.Path namePath = java.nio.file.Paths.get("company_name.txt");
            if (java.nio.file.Files.exists(namePath)) {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(namePath);
                if (!lines.isEmpty()) companyName = lines.get(0).trim();
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
                        logoIcon = new javax.swing.ImageIcon(img.getScaledInstance(80, 80, java.awt.Image.SCALE_SMOOTH));
                        break;
                    }
                } catch (Exception ex) {
                }
            }
        }

        // HEADER PANEL (15% - bordered)
        final int pageHeight = 1000;
        final int pageWidth = 650;
        int headerHeight = (int) (pageHeight * 0.15); // 15%
        
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

        // TITLE
        JLabel headerLabel = new JLabel("FORMULAR CERERE CONCEDIU");
        headerLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));
        headerLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        contentPanel.add(headerLabel);
        contentPanel.add(javax.swing.Box.createVerticalStrut(15));
        
        // REQUEST DETAILS
        addInfoField(contentPanel, "ID Cerere:", String.valueOf(requestId));
        addInfoField(contentPanel, "ID Angajat:", workerId);
        addInfoField(contentPanel, "Nume Angajat:", workerName);
        contentPanel.add(javax.swing.Box.createVerticalStrut(12));
        
        addInfoField(contentPanel, "Data de la:", dateFrom);
        addInfoField(contentPanel, "Data până la:", dateTo);
        contentPanel.add(javax.swing.Box.createVerticalStrut(12));
        
        addInfoField(contentPanel, "Status:", status);
        contentPanel.add(javax.swing.Box.createVerticalStrut(15));
        
        // REASON SECTION (with flexible space)
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

        // SIGNATURE ZONES
        JPanel signaturesPanel = new JPanel(new java.awt.GridLayout(1, 2, 40, 0));
        signaturesPanel.setBackground(java.awt.Color.WHITE);
        signaturesPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        signaturesPanel.setMaximumSize(new java.awt.Dimension(pageWidth - 100, 80));
        
        JPanel hrSigPanel = new JPanel();
        hrSigPanel.setLayout(new javax.swing.BoxLayout(hrSigPanel, javax.swing.BoxLayout.Y_AXIS));
        hrSigPanel.setBackground(java.awt.Color.WHITE);
        JLabel hrSigLabel = new JLabel("Semnătură HR:");
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
        JLabel workerSigLabel = new JLabel("Semnătură Angajat:");
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
        int footerHeight = 100;
        String contactInfo = "";
        try {
            java.nio.file.Path contactPath = java.nio.file.Paths.get("company_contact.txt");
            if (java.nio.file.Files.exists(contactPath)) {
                contactInfo = java.nio.file.Files.readString(contactPath).trim();
            }
        } catch (Exception ignore) {
        }
        
        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(new javax.swing.BoxLayout(footerPanel, javax.swing.BoxLayout.Y_AXIS));
        footerPanel.setBackground(java.awt.Color.WHITE);
        footerPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        
        JLabel dateLabel = new JLabel("Generat la: " + java.time.LocalDate.now());
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
        
        JPanel wrapper = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        wrapper.setBackground(java.awt.Color.LIGHT_GRAY);
        contentPanel.setPreferredSize(new java.awt.Dimension(pageWidth, 1000));
        wrapper.add(contentPanel);
        infoFrame.add(wrapper, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel();
        JButton closeBtn = new JButton("Închide");
        closeBtn.addActionListener(e -> infoFrame.dispose());
        buttonPanel.add(closeBtn);
        infoFrame.add(buttonPanel, BorderLayout.SOUTH);
        
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

    private void showMySalary() {
        JFrame salaryFrame = new JFrame("Salariul Meu - " + user.name);
        salaryFrame.setSize(1200, 700);
        salaryFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        salaryFrame.setLayout(new BorderLayout(10, 10));
        
        javax.swing.JTabbedPane tabbedPane = new javax.swing.JTabbedPane();
        
        // Tab 1: Hourly Rate History
        JPanel rateHistoryPanel = createRateHistoryPanel();
        tabbedPane.addTab("Istoric Tarif Orar", rateHistoryPanel);
        
        // Tab 2: Salary History
        JPanel salaryHistoryPanel = createSalaryHistoryPanel();
        tabbedPane.addTab("Istoric Salariu", salaryHistoryPanel);
        
        salaryFrame.add(tabbedPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        JButton closeBtn = new JButton("Închide");
        closeBtn.addActionListener(e -> salaryFrame.dispose());
        bottomPanel.add(closeBtn);
        salaryFrame.add(bottomPanel, BorderLayout.SOUTH);
        
        salaryFrame.setLocationRelativeTo(frame);
        salaryFrame.setVisible(true);
    }

    private JPanel createRateHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String[] columns = {"Tarif Orar (MDL)", "Valabil De la", "Valabil Până la", "Setat De"};
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
        JButton refreshBtn = new JButton("Actualizează");
        buttonPanel.add(refreshBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        Runnable loadData = () -> {
            try {
                netClient.setMessageHandler((type, payload) -> {
                    if ("get_hourly_rate_history_response".equals(type) && payload != null && payload.isJsonObject()) {
                        com.google.gson.JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("status") && "error".equals(jo.get("status").getAsString())) {
                            javax.swing.JOptionPane.showMessageDialog(panel,
                                "Eroare: " + jo.get("message").getAsString(),
                                "Eroare",
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
                                            h.get("validTo").getAsString() : "Curent",
                                        h.has("setByName") && !h.get("setByName").isJsonNull() ? 
                                            h.get("setByName").getAsString() : "Necunoscut"
                                    };
                                    model.addRow(row);
                                }
                            } catch (Exception ex) {
                                javax.swing.JOptionPane.showMessageDialog(panel,
                                    "Eroare la parsarea datelor: " + ex.getMessage(),
                                    "Eroare",
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
                    "Eroare: " + ex.getMessage(),
                    "Eroare",
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
        
        String[] columns = {"Lună/An", "Ore", "Tarif", "Salariu Bază", "Bonusuri", "Brut", "Impozit 15%", "Net", "Publicat"};
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
        JButton refreshBtn = new JButton("Actualizează");
        JButton viewDetailsBtn = new JButton("Vezi Bonusuri");
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
                                "Eroare: " + jo.get("message").getAsString(),
                                "Eroare",
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
                                    "Eroare la parsarea datelor: " + ex.getMessage(),
                                    "Eroare",
                                    javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                });
                
                netClient.send("get_my_salary_history", new com.google.gson.JsonObject());
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(panel,
                    "Eroare: " + ex.getMessage(),
                    "Eroare",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        };
        
        refreshBtn.addActionListener(e -> loadData.run());
        
        viewDetailsBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                javax.swing.JOptionPane.showMessageDialog(panel,
                    "Vă rugăm selectați o înregistrare de salariu.",
                    "Nici o selecție",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            StringBuilder details = new StringBuilder();
            details.append("Defalcare Salariu\n");
            details.append("================\n\n");
            details.append("Perioadă: ").append(table.getValueAt(selectedRow, 0)).append("\n\n");
            details.append("Ore lucrate: ").append(table.getValueAt(selectedRow, 1)).append(" ore\n");
            details.append("Tarif orar: ").append(table.getValueAt(selectedRow, 2)).append(" MDL\n\n");
            details.append("Salariu bază: ").append(table.getValueAt(selectedRow, 3)).append(" MDL\n");
            details.append("Bonusuri: ").append(table.getValueAt(selectedRow, 4)).append(" MDL\n");
            details.append("Salariu brut: ").append(table.getValueAt(selectedRow, 5)).append(" MDL\n\n");
            details.append("Impozit (15%): ").append(table.getValueAt(selectedRow, 6)).append(" MDL\n\n");
            details.append("SALARIU NET: ").append(table.getValueAt(selectedRow, 7)).append(" MDL\n\n");
            details.append("Publicat: ").append(table.getValueAt(selectedRow, 8));
            
            javax.swing.JOptionPane.showMessageDialog(panel,
                details.toString(),
                "Detalii Salariu",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
        });
        
        javax.swing.SwingUtilities.invokeLater(loadData);
        
        return panel;
    }
}
