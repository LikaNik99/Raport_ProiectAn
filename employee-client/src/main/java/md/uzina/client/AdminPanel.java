package md.uzina.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AdminPanel {
    private final JFrame frame;
    private final ClientApp.ClientUser user;
    private final Runnable onLogout;
    private final NetClient netClient;

    public AdminPanel(ClientApp.ClientUser user, Runnable onLogout, NetClient netClient) {
        this.user = user;
        this.onLogout = onLogout;
        this.netClient = netClient;
        this.frame = new JFrame("Admin Panel - " + user.name);
    }

    public void show() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        // Header: greeting + logout
        JPanel header = new JPanel();
        header.add(new JLabel("Bine ați venit Administrator, " + user.name));
        JButton logoutBtn = new JButton("Deconectare");
        logoutBtn.addActionListener(e -> {
            frame.dispose();
            onLogout.run();
        });
        header.add(logoutBtn);
        frame.add(header, BorderLayout.NORTH);

        // Main content: admin features
        JPanel content = new JPanel(new BorderLayout());
        JTextArea info = new JTextArea();
        info.setText(
            "Funcționalități Administrator:\n\n" +
            "1. Setări Companie - Configurați numele, logo-ul și informațiile de contact\n" +
            "2. Configurare Ore Lucru - Setați programul de lucru al companiei\n" +
            "3. Gestionare Angajați - Gestionați lucrătorii și șefii de echipă\n" +
            "4. Gestionare Cereri Concediu - Revizuiți și gestionați toate cererile de concediu\n" +
            "5. Statistici Globale - Vizualizați metricile la nivel de companie\n" +
            "6. Istoric Login - Monitorizați activitatea utilizatorilor\n" +
            "7. Setări Sistem - Configurare avansată\n\n" +
            "ID Utilizator: " + user.id + "\n" +
            "Rol: ADMINISTRATOR"
        );
        info.setEditable(false);
        content.add(new JScrollPane(info), BorderLayout.CENTER);
        frame.add(content, BorderLayout.CENTER);

        // Buttons for features - use grid layout
        JPanel buttons = new JPanel(new GridLayout(0, 3, 5, 5));
        buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JButton companySettings = new JButton("Setari Companie");
        companySettings.addActionListener(e -> showCompanySettings());
        buttons.add(companySettings);
        
        JButton workHoursBtn = new JButton("Configurare Ore Lucru");
        workHoursBtn.addActionListener(e -> showWorkHoursConfig());
        buttons.add(workHoursBtn);
        
        JButton empManage = new JButton("Gestionare Angajati");
        empManage.addActionListener(e -> showEmployeeManagement());
        buttons.add(empManage);
        
        JButton viewWorkStatus = new JButton("Vizualizare Stare Munca");
        viewWorkStatus.addActionListener(e -> viewAllWorkStatus());
        buttons.add(viewWorkStatus);
        
        JButton leaveRequests = new JButton("Cereri Concediu");
        leaveRequests.addActionListener(e -> showLeaveRequests());
        buttons.add(leaveRequests);
        
        JButton globalStats = new JButton("Statistici Globale");
        globalStats.addActionListener(e -> showGlobalStatistics());
        buttons.add(globalStats);
        
        JButton loginHistory = new JButton("Istoric Login");
        loginHistory.addActionListener(e -> showLoginHistory());
        buttons.add(loginHistory);
        
        JButton importUsers = new JButton("Import Utilizatori (Excel)");
        importUsers.addActionListener(e -> showImportUsersDialog());
        buttons.add(importUsers);
        
        JButton importWorkSessions = new JButton("Import Istoric Lucru (Excel)");
        importWorkSessions.addActionListener(e -> showImportWorkSessionsDialog());
        buttons.add(importWorkSessions);
        
        JButton monthlyStats = new JButton("Statistici Lunare Detaliate");
        monthlyStats.addActionListener(e -> showMonthlyStatistics());
        buttons.add(monthlyStats);
        
        JButton manageJobs = new JButton("Gestionare Joburi");
        manageJobs.addActionListener(e -> showJobManagement());
        buttons.add(manageJobs);
        
        frame.add(buttons, BorderLayout.SOUTH);

        frame.setSize(new Dimension(900, 550));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void showCompanySettings() {
        JFrame settingsFrame = new JFrame("Setari Companie");
        settingsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        settingsFrame.setLayout(new BorderLayout(10, 10));
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new javax.swing.BoxLayout(mainPanel, javax.swing.BoxLayout.Y_AXIS));
        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Company Name
        JPanel namePanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        namePanel.add(new JLabel("Nume Companie:"));
        JTextField nameField = new JTextField(30);
        try {
            java.nio.file.Path namePath = java.nio.file.Paths.get("company_name.txt");
            if (java.nio.file.Files.exists(namePath)) {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(namePath);
                if (!lines.isEmpty()) nameField.setText(lines.get(0).trim());
            }
        } catch (Exception ignore) {
        }
        namePanel.add(nameField);
        mainPanel.add(namePanel);
        mainPanel.add(javax.swing.Box.createVerticalStrut(10));
        
        // Contact Info (multi-line text area)
        JPanel contactPanel = new JPanel(new BorderLayout(5, 5));
        contactPanel.add(new JLabel("Informatii Contact (Adresa, Telefon):"), BorderLayout.NORTH);
        JTextArea contactArea = new JTextArea(5, 40);
        contactArea.setLineWrap(true);
        contactArea.setWrapStyleWord(true);
        try {
            java.nio.file.Path contactPath = java.nio.file.Paths.get("company_contact.txt");
            if (java.nio.file.Files.exists(contactPath)) {
                String content = java.nio.file.Files.readString(contactPath);
                contactArea.setText(content.trim());
            }
        } catch (Exception ignore) {
        }
        JScrollPane contactScroll = new JScrollPane(contactArea);
        contactPanel.add(contactScroll, BorderLayout.CENTER);
        mainPanel.add(contactPanel);
        mainPanel.add(javax.swing.Box.createVerticalStrut(10));
        
        // Logo Upload
        JPanel logoPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        logoPanel.add(new JLabel("Logo Companie:"));
        JButton uploadLogoBtn = new JButton("Incarca Logo");
        JLabel logoStatusLabel = new JLabel("Niciun logo selectat");
        
        // Check if logo exists
        String[] logoCandidates = new String[]{"company_logo.png", "logo.png"};
        for (String lp : logoCandidates) {
            if (new java.io.File(lp).exists()) {
                logoStatusLabel.setText("Logo: " + lp);
                break;
            }
        }
        
        uploadLogoBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(java.io.File f) {
                    String name = f.getName().toLowerCase();
                    return f.isDirectory() || name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
                }
                @Override
                public String getDescription() {
                    return "Image Files (*.png, *.jpg, *.jpeg)";
                }
            });
            
            int result = fc.showOpenDialog(settingsFrame);
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    java.io.File selectedFile = fc.getSelectedFile();
                    java.nio.file.Path source = selectedFile.toPath();
                    java.nio.file.Path target = java.nio.file.Paths.get("company_logo.png");
                    java.nio.file.Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    logoStatusLabel.setText("Logo: company_logo.png");
                    javax.swing.JOptionPane.showMessageDialog(settingsFrame, "Logo uploaded successfully!");
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(settingsFrame, "Error uploading logo: " + ex.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        logoPanel.add(uploadLogoBtn);
        logoPanel.add(logoStatusLabel);
        mainPanel.add(logoPanel);
        mainPanel.add(javax.swing.Box.createVerticalStrut(20));
        
        // Save button
        JPanel buttonPanel = new JPanel();
        JButton saveBtn = new JButton("Salvare Setari");
        saveBtn.addActionListener(e -> {
            try {
                // Save company name
                String companyName = nameField.getText().trim();
                if (!companyName.isEmpty()) {
                    java.nio.file.Files.write(java.nio.file.Paths.get("company_name.txt"), 
                        java.util.Collections.singletonList(companyName));
                }
                
                // Save contact info (multi-line)
                String contactInfo = contactArea.getText().trim();
                if (!contactInfo.isEmpty()) {
                    java.nio.file.Files.writeString(java.nio.file.Paths.get("company_contact.txt"), contactInfo);
                }
                
                javax.swing.JOptionPane.showMessageDialog(settingsFrame, 
                    "Setarile companiei au fost salvate cu succes!", 
                    "Success", 
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
                settingsFrame.dispose();
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(settingsFrame, 
                    "Error saving settings: " + ex.getMessage(), 
                    "Error", 
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(saveBtn);
        
        JButton cancelBtn = new JButton("Anulare");
        cancelBtn.addActionListener(e -> settingsFrame.dispose());
        buttonPanel.add(cancelBtn);
        
        mainPanel.add(buttonPanel);
        
        settingsFrame.add(new JScrollPane(mainPanel), BorderLayout.CENTER);
        settingsFrame.setSize(new Dimension(650, 450));
        settingsFrame.setLocationRelativeTo(null);
        settingsFrame.setVisible(true);
    }

    private void showWorkHoursConfig() {
        JFrame configFrame = new JFrame("Configurare Ore Lucru");
        configFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        configFrame.setLayout(new BorderLayout(10, 10));
        
        JPanel mainPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        mainPanel.add(new JLabel("Ora Start (0-23):"));
        JTextField startHourField = new JTextField("8");
        mainPanel.add(startHourField);
        
        mainPanel.add(new JLabel("Minut Start (0-59):"));
        JTextField startMinField = new JTextField("0");
        mainPanel.add(startMinField);
        
        mainPanel.add(new JLabel("Ora Sfarsit (0-23):"));
        JTextField endHourField = new JTextField("17");
        mainPanel.add(endHourField);
        
        configFrame.add(mainPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        JButton saveBtn = new JButton("Salvare Configurare");
        saveBtn.addActionListener(e -> {
            try {
                int startH = Integer.parseInt(startHourField.getText().trim());
                int startM = Integer.parseInt(startMinField.getText().trim());
                int endH = Integer.parseInt(endHourField.getText().trim());
                
                if (startH < 0 || startH > 23 || startM < 0 || startM > 59 || endH < 0 || endH > 23) {
                    javax.swing.JOptionPane.showMessageDialog(configFrame, "Valori ore invalide!", "Eroare", javax.swing.JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                javax.swing.JOptionPane.showMessageDialog(configFrame, 
                    "Work hours configured: " + String.format("%02d:%02d - %02d:00", startH, startM, endH), 
                    "Success", 
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
                configFrame.dispose();
            } catch (NumberFormatException ex) {
                javax.swing.JOptionPane.showMessageDialog(configFrame, "Va rugam introduceti numere valide!", "Eroare", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(saveBtn);
        
        JButton cancelBtn = new JButton("Anulare");
        cancelBtn.addActionListener(e -> configFrame.dispose());
        buttonPanel.add(cancelBtn);
        
        configFrame.add(buttonPanel, BorderLayout.SOUTH);
        
        configFrame.setSize(new Dimension(400, 200));
        configFrame.setLocationRelativeTo(null);
        configFrame.setVisible(true);
    }

    private void showEmployeeManagement() {
        JFrame empFrame = new JFrame("Management Angajati");
        empFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        empFrame.setLayout(new BorderLayout(8, 8));

        // Add filter panel at top
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.add(new JLabel("Filtreaza dupa Nume:"));
        JTextField nameFilterField = new JTextField(15);
        filterPanel.add(nameFilterField);
        
        filterPanel.add(new JLabel("Rol:"));
        javax.swing.JComboBox<String> roleFilterCombo = new javax.swing.JComboBox<>(new String[]{"All", "WORKER", "TEAMLEADER", "HR", "ADMIN"});
        filterPanel.add(roleFilterCombo);
        
        empFrame.add(filterPanel, BorderLayout.NORTH);

        javax.swing.JTable table = new javax.swing.JTable();
        javax.swing.table.DefaultTableModel tableModel = new javax.swing.table.DefaultTableModel(
            new Object[]{"User ID", "Name", "Role", "Phone", "Address", "Job", "Team Leader"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setModel(tableModel);
        table.setAutoCreateRowSorter(true);
        empFrame.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton fetchBtn = new JButton("Preia Totii Angajatii");
        fetchBtn.addActionListener(e -> {
            try {
                netClient.setMessageHandler((type, payload) -> {
                    if ("all_workers_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("error")) {
                            javax.swing.JOptionPane.showMessageDialog(empFrame, "Error: " + jo.get("error").getAsString(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                        } else if (jo.has("workers")) {
                            try {
                                JsonArray workers = JsonParser.parseString(jo.get("workers").getAsString()).getAsJsonArray();
                                tableModel.setRowCount(0);
                                String nameFilter = nameFilterField.getText().toLowerCase().trim();
                                String roleFilter = (String) roleFilterCombo.getSelectedItem();
                                
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
                                javax.swing.JOptionPane.showMessageDialog(empFrame, "Error parsing data: " + ex.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                });
                netClient.send("get_all_workers", new JsonObject());
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(empFrame, "Error: " + ex.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(fetchBtn);

        JButton addBtn = new JButton("Adauga Utilizator");
        addBtn.addActionListener(e -> showAddUserDialog(empFrame, fetchBtn));
        buttonPanel.add(addBtn);

        JButton editBtn = new JButton("Editeaza Selectat");
        editBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                javax.swing.JOptionPane.showMessageDialog(empFrame, "Please select a user to edit", "Info", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            int userId = (int) tableModel.getValueAt(selectedRow, 0);
            String currentName = (String) tableModel.getValueAt(selectedRow, 1);
            String currentRole = (String) tableModel.getValueAt(selectedRow, 2);
            String currentPhone = (String) tableModel.getValueAt(selectedRow, 3);
            String currentAddress = (String) tableModel.getValueAt(selectedRow, 4);
            String currentJob = (String) tableModel.getValueAt(selectedRow, 5);
            String currentTeamLeader = (String) tableModel.getValueAt(selectedRow, 6);
            
            showEditUserDialog(empFrame, userId, currentName, currentRole, currentPhone, currentAddress, currentJob, currentTeamLeader, fetchBtn);
        });
        buttonPanel.add(editBtn);

        JButton deleteBtn = new JButton("Șterge Selectat");
        deleteBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                javax.swing.JOptionPane.showMessageDialog(empFrame, "Vă rugăm selectați un utilizator", "Informație", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            int userId = (int) tableModel.getValueAt(selectedRow, 0);
            String userName = (String) tableModel.getValueAt(selectedRow, 1);
            
            int confirm = javax.swing.JOptionPane.showConfirmDialog(empFrame, 
                "Sigur doriți să ștergeți utilizatorul " + userName + "?\n\nAceastă acțiune nu poate fi anulată!", 
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
        buttonPanel.add(deleteBtn);

        JButton changePasswordBtn = new JButton("Schimba Parola");
        changePasswordBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                javax.swing.JOptionPane.showMessageDialog(empFrame, "Vă rugăm selectați un utilizator", "Informație", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            int userId = (int) tableModel.getValueAt(selectedRow, 0);
            String userName = (String) tableModel.getValueAt(selectedRow, 1);
            
            String newPassword = javax.swing.JOptionPane.showInputDialog(empFrame, "Introduceți parola nouă pentru " + userName + ":", "Schimbare Parolă", javax.swing.JOptionPane.PLAIN_MESSAGE);
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                try {
                    netClient.setMessageHandler((type, payload) -> {
                        if ("update_user_password_response".equals(type) && payload != null && payload.isJsonObject()) {
                            JsonObject jo = payload.getAsJsonObject();
                            if (jo.has("ok")) {
                                javax.swing.JOptionPane.showMessageDialog(empFrame, "Parola actualizată cu succes!", "Succes", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                            } else if (jo.has("error")) {
                                javax.swing.JOptionPane.showMessageDialog(empFrame, "Eroare: " + jo.get("error").getAsString(), "Eroare", javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });
                    
                    JsonObject req = new JsonObject();
                    req.addProperty("userId", userId);
                    req.addProperty("newPassword", newPassword.trim());
                    netClient.send("update_user_password", req);
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(empFrame, "Error: " + ex.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        buttonPanel.add(changePasswordBtn);

        JButton closeBtn = new JButton("Închide");
        closeBtn.addActionListener(e -> empFrame.dispose());
        buttonPanel.add(closeBtn);

        empFrame.add(buttonPanel, BorderLayout.SOUTH);

        empFrame.setSize(new Dimension(700, 500));
        empFrame.setLocationRelativeTo(null);
        empFrame.setVisible(true);

        // Auto-fetch on open
        fetchBtn.doClick();
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
        
        editDialog.setSize(new Dimension(400, 400));
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
                                // Add empty option first
                                comboBox.addItem("");
                                for (int i = 0; i < jobTitles.size(); i++) {
                                    JsonObject jobTitle = jobTitles.get(i).getAsJsonObject();
                                    String title = jobTitle.get("title").getAsString();
                                    comboBox.addItem(title);
                                }
                                // Set current job if provided
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

    private void showAddUserDialog(JFrame parent, JButton refreshBtn) {
        JDialog dlg = new JDialog(parent, "Adăugare Utilizator", true);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(7, 2, 10, 10));
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));

        form.add(new JLabel("Nume:"));
        JTextField nameField = new JTextField();
        form.add(nameField);

        form.add(new JLabel("Rol:"));
        javax.swing.JComboBox<String> roleCombo = new javax.swing.JComboBox<>(new String[]{"WORKER", "TEAMLEADER", "HR", "ADMIN"});
        form.add(roleCombo);

        form.add(new JLabel("Parolă:"));
        JPasswordField passField = new JPasswordField();
        form.add(passField);

        form.add(new JLabel("Telefon:"));
        JTextField phoneField = new JTextField();
        form.add(phoneField);

        form.add(new JLabel("Adresă:"));
        JTextField addressField = new JTextField();
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
        JButton createBtn = new JButton("Creează");
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
                javax.swing.JOptionPane.showMessageDialog(dlg, "Numele și parola sunt obligatorii", "Eroare", javax.swing.JOptionPane.ERROR_MESSAGE);
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
                                javax.swing.JOptionPane.showMessageDialog(dlg, "Utilizator creat cu ID: " + jo.get("userId").getAsInt(), "Succes", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                dlg.dispose();
                                refreshBtn.doClick();
                            });
                        } else if (jo.has("error")) {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                javax.swing.JOptionPane.showMessageDialog(dlg, "Eroare: " + jo.get("error").getAsString(), "Eroare la creare", javax.swing.JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    }
                });

                JsonObject req = new JsonObject();
                req.addProperty("name", name);
                req.addProperty("role", role);
                req.addProperty("password", pw);
                req.addProperty("phone", phone);
                req.addProperty("address", address);
                if (!job.isEmpty()) {
                    req.addProperty("job", job);
                }
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

        JButton cancelBtn = new JButton("Anulare");
        cancelBtn.addActionListener(e -> dlg.dispose());
        btnPanel.add(cancelBtn);

        dlg.add(btnPanel, BorderLayout.SOUTH);

        dlg.setSize(new Dimension(400, 410));
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }

    private void showLeaveRequests() {
        javax.swing.JOptionPane.showMessageDialog(frame, 
            "Gestionare Cereri Concediu\n\nAceastă funcționalitate vă permite să:\n- Vizualizați toate cererile de concediu\n- Aprobați sau respingeți cereri\n- Filtrați după lucrător sau status\n- Exportați rapoarte", 
            "Cereri Concediu", 
            javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    private void showGlobalStatistics() {
        JFrame statsFrame = new JFrame("Statistici Globale");
        statsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        statsFrame.setLayout(new BorderLayout(8, 8));

        JPanel datePanel = new JPanel(new GridLayout(1, 4, 5, 5));
        datePanel.add(new JLabel("From:"));
        JTextArea fromField = new JTextArea(1, 10);
        fromField.setText(LocalDate.now().minusDays(30).toString());
        datePanel.add(fromField);
        datePanel.add(new JLabel("To:"));
        JTextArea toField = new JTextArea(1, 10);
        toField.setText(LocalDate.now().toString());
        datePanel.add(toField);
        statsFrame.add(datePanel, BorderLayout.NORTH);

        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        statsFrame.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel();
        JButton fetchBtn = new JButton("Preia Statistici");
        fetchBtn.addActionListener(e -> {
            try {
                String from = fromField.getText().trim();
                String to = toField.getText().trim();
                
                netClient.setMessageHandler((type, payload) -> {
                    if ("hr_daily_stats_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("error")) {
                            resultArea.setText("Error: " + jo.get("error").getAsString());
                        } else if (jo.has("stats")) {
                            try {
                                JsonArray stats = JsonParser.parseString(jo.get("stats").getAsString()).getAsJsonArray();
                                StringBuilder sb = new StringBuilder();
                                sb.append("Date\t\tTotal Users\tPresent\t\tLeave\t\tAbsent\n");
                                sb.append("====\t\t===========\t=======\t\t=====\t\t======\n");
                                for (int i = 0; i < stats.size(); i++) {
                                    JsonObject stat = stats.get(i).getAsJsonObject();
                                    sb.append(stat.get("date").getAsString()).append("\t")
                                      .append(stat.get("totalUsers").getAsInt()).append("\t\t")
                                      .append(stat.get("presentCount").getAsInt()).append("\t\t")
                                      .append(stat.get("leaveCount").getAsInt()).append("\t\t")
                                      .append(stat.get("absentExcused").getAsInt() + stat.get("absentUnexcused").getAsInt()).append("\n");
                                }
                                resultArea.setText(sb.toString());
                            } catch (Exception ex) {
                                resultArea.setText("Error parsing stats: " + ex.getMessage());
                            }
                        }
                    }
                });
                
                JsonObject req = new JsonObject();
                req.addProperty("from", from);
                req.addProperty("to", to);
                netClient.send("get_hr_daily_stats", req);
            } catch (Exception ex) {
                resultArea.setText("Error: " + ex.getMessage());
            }
        });
        actionPanel.add(fetchBtn);
        statsFrame.add(actionPanel, BorderLayout.SOUTH);

        statsFrame.setSize(new Dimension(700, 400));
        statsFrame.setLocationRelativeTo(null);
        statsFrame.setVisible(true);
    }

    private void showLoginHistory() {
        JFrame historyFrame = new JFrame("Istoric Conectări");
        historyFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        historyFrame.setLayout(new BorderLayout(8, 8));

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

        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        historyFrame.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel();
        JButton fetchBtn = new JButton("Preia Istoric Conectări");
        fetchBtn.addActionListener(e -> {
            try {
                String from = fromField.getText().trim();
                String to = toField.getText().trim();
                
                netClient.setMessageHandler((type, payload) -> {
                    if ("login_history_response".equals(type) && payload != null && payload.isJsonObject()) {
                        JsonObject jo = payload.getAsJsonObject();
                        if (jo.has("error")) {
                            resultArea.setText("Error: " + jo.get("error").getAsString());
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
                            } catch (Exception ex) {
                                resultArea.setText("Error parsing records: " + ex.getMessage());
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
            }
        });
        actionPanel.add(fetchBtn);
        historyFrame.add(actionPanel, BorderLayout.SOUTH);

        historyFrame.setSize(new Dimension(1000, 450));
        historyFrame.setLocationRelativeTo(null);
        historyFrame.setVisible(true);
    }

    private void viewAllWorkStatus() {
        JFrame statusFrame = new JFrame("Status Lucru - Toți Angajații");
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
        modePanel.setBorder(BorderFactory.createTitledBorder("Mod Afișare"));
        
        javax.swing.JRadioButton currentDayBtn = new javax.swing.JRadioButton("Ziua Curentă", true);
        javax.swing.JRadioButton fullHistoryBtn = new javax.swing.JRadioButton("Istoric Complet");
        javax.swing.JRadioButton lastPerEmployeeBtn = new javax.swing.JRadioButton("Ultimul Start per Angajat");
        
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
        filterRow1.add(new JLabel("Filtrează după Rol:"));
        javax.swing.JComboBox<String> roleFilter = new javax.swing.JComboBox<>(new String[]{"Toți", "WORKER", "TEAMLEADER"});
        filterRow1.add(roleFilter);
        
        filterRow1.add(new JLabel("Filtrează după Status:"));
        javax.swing.JComboBox<String> statusFilter = new javax.swing.JComboBox<>(new String[]{"Toate", "ACTIVE", "COMPLETED", "NOT_STARTED"});
        filterRow1.add(statusFilter);
        
        filterRow1.add(new JLabel("Filtrează după ID Utilizator:"));
        javax.swing.JTextField userIdFilter = new javax.swing.JTextField(10);
        filterRow1.add(userIdFilter);
        
        // Second row of filters
        JPanel filterRow2 = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        filterRow2.add(new JLabel("Filtrează după Nume:"));
        javax.swing.JTextField nameFilter = new javax.swing.JTextField(15);
        filterRow2.add(nameFilter);
        
        filterRow2.add(new JLabel("Filtrează după Data Lucrului:"));
        javax.swing.JTextField workDateFilter = new javax.swing.JTextField(10);
        filterRow2.add(workDateFilter);
        
        JButton applyFilterBtn = new JButton("Aplică Filtru");
        filterRow2.add(applyFilterBtn);
        
        JButton clearFilterBtn = new JButton("Șterge Filtre");
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
                boolean roleMatch = "Toți".equals(selectedRole) || selectedRole.equals(row[2]);
                boolean statusMatch = "Toate".equals(selectedStatus) || selectedStatus.equals(row[6]);
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
        JButton fetchBtn = new JButton("Preia Status Lucru");
        JButton forceStopBtn = new JButton("Oprește Forțat Lucrul Selectat");
        JButton endAllBtn = new JButton("Închide Toate Sesiunile Active");
        
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
                        "Vă rugăm selectați un utilizator pentru a opri forțat lucrul.",
                        "Nicio Selecție",
                        javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int userId = ((Number) table.getValueAt(selectedRow, 0)).intValue();
            String userName = (String) table.getValueAt(selectedRow, 1);
            String status = (String) table.getValueAt(selectedRow, 6);
            
            if (!"ACTIVE".equals(status)) {
                javax.swing.JOptionPane.showMessageDialog(statusFrame,
                        "Utilizatorul " + userName + " nu lucrează în prezent (status: " + status + ").",
                        "Status Invalid",
                        javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int confirm = javax.swing.JOptionPane.showConfirmDialog(statusFrame,
                    "Opriți forțat lucrul pentru " + userName + "?",
                    "Confirmare Oprire Forțată",
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
                                        "Lucrul oprit cu succes pentru " + userName,
                                        "Succes",
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
                    "Sunteți sigur că doriți să închideți TOATE sesiunile active?\nAceasta va seta toate sesiunile ACTIVE la COMPLETED.",
                    "Confirmare Închidere Toate Sesiunile",
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
                                        "Toate sesiunile active au fost închise cu succes!",
                                        "Succes",
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

        JButton closeBtn = new JButton("Închide");
        closeBtn.addActionListener(e -> statusFrame.dispose());
        buttonPanel.add(closeBtn);

        statusFrame.add(buttonPanel, BorderLayout.SOUTH);

        statusFrame.setSize(new Dimension(1000, 600));
        statusFrame.setLocationRelativeTo(null);
        statusFrame.setVisible(true);

        // Auto-fetch on open
        fetchBtn.doClick();
    }

    private void showImportUsersDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Selectați fișierul Excel cu utilizatori");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel Files", "xlsx", "xls"));
        
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            importUsersFromExcel(selectedFile);
        }
    }

    private void importUsersFromExcel(java.io.File file) {
        try {
            org.apache.poi.ss.usermodel.Workbook workbook;
            if (file.getName().endsWith(".xlsx")) {
                workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.FileInputStream(file));
            } else {
                workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook(new java.io.FileInputStream(file));
            }
            
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            JsonArray usersArray = new JsonArray();
            
            System.out.println("DEBUG: Total rows in Excel: " + sheet.getLastRowNum());
            
            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row == null) {
                    System.out.println("DEBUG: Row " + (i + 1) + " is null, skipping");
                    continue;
                }
                
                try {
                    JsonObject userObj = new JsonObject();
                    
                    // Nume (required)
                    org.apache.poi.ss.usermodel.Cell nameCell = row.getCell(0);
                    if (nameCell == null) {
                        System.out.println("Skipping row " + (i + 1) + ": name cell is null");
                        continue;
                    }
                    String name = getCellValueAsString(nameCell);
                    if (name.trim().isEmpty()) {
                        System.out.println("Skipping row " + (i + 1) + ": name is empty");
                        continue;
                    }
                    userObj.addProperty("name", name.trim());
                    System.out.println("DEBUG: Row " + (i + 1) + " - Name: " + name);
                    
                    // Job (required)
                    org.apache.poi.ss.usermodel.Cell jobCell = row.getCell(1);
                    String job = (jobCell != null) ? getCellValueAsString(jobCell).trim() : "";
                    if (job.isEmpty()) {
                        job = "Angajat";
                    }
                    userObj.addProperty("job", job);
                    System.out.println("DEBUG: Row " + (i + 1) + " - Job: " + job);
                    
                    // Telefon (optional)
                    org.apache.poi.ss.usermodel.Cell phoneCell = row.getCell(2);
                    if (phoneCell != null) {
                        String phone = getCellValueAsString(phoneCell).trim();
                        if (!phone.isEmpty()) {
                            userObj.addProperty("phone", phone);
                            System.out.println("DEBUG: Row " + (i + 1) + " - Phone: " + phone);
                        }
                    }
                    
                    // Parola (required)
                    org.apache.poi.ss.usermodel.Cell passwordCell = row.getCell(3);
                    if (passwordCell == null) {
                        System.out.println("Skipping row " + (i + 1) + ": password cell is null");
                        continue;
                    }
                    String password = getCellValueAsString(passwordCell).trim();
                    if (password.isEmpty()) {
                        System.out.println("Skipping row " + (i + 1) + ": password is empty");
                        continue;
                    }
                    userObj.addProperty("password", password);
                    System.out.println("DEBUG: Row " + (i + 1) + " - Password: [hidden]");
                    
                    // Adresa (optional)
                    org.apache.poi.ss.usermodel.Cell addressCell = row.getCell(4);
                    if (addressCell != null) {
                        String address = getCellValueAsString(addressCell).trim();
                        if (!address.isEmpty()) {
                            userObj.addProperty("address", address);
                            System.out.println("DEBUG: Row " + (i + 1) + " - Address: " + address);
                        }
                    }
                    
                    // Is Worker (required)
                    org.apache.poi.ss.usermodel.Cell isWorkerCell = row.getCell(5);
                    boolean isWorker = false;
                    if (isWorkerCell != null) {
                        if (isWorkerCell.getCellType() == org.apache.poi.ss.usermodel.CellType.BOOLEAN) {
                            isWorker = isWorkerCell.getBooleanCellValue();
                        } else if (isWorkerCell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                            isWorker = isWorkerCell.getNumericCellValue() != 0;
                        } else {
                            String val = getCellValueAsString(isWorkerCell).trim().toLowerCase();
                            isWorker = val.equals("true") || val.equals("1") || val.equals("da") || val.equals("yes");
                        }
                    }
                    userObj.addProperty("is_worker", isWorker);
                    System.out.println("DEBUG: Row " + (i + 1) + " - Is Worker: " + isWorker);
                    
                    // Team Leader ID (optional, only if is_worker = true)
                    if (isWorker) {
                        org.apache.poi.ss.usermodel.Cell teamLeaderCell = row.getCell(6);
                        if (teamLeaderCell != null) {
                            try {
                                if (teamLeaderCell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                                    userObj.addProperty("team_leader_id", (int) teamLeaderCell.getNumericCellValue());
                                    System.out.println("DEBUG: Row " + (i + 1) + " - Team Leader ID: " + (int) teamLeaderCell.getNumericCellValue());
                                } else {
                                    String tlId = getCellValueAsString(teamLeaderCell).trim();
                                    if (!tlId.isEmpty()) {
                                        int teamLeaderId = Integer.parseInt(tlId);
                                        userObj.addProperty("team_leader_id", teamLeaderId);
                                        System.out.println("DEBUG: Row " + (i + 1) + " - Team Leader ID: " + teamLeaderId);
                                    }
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("DEBUG: Row " + (i + 1) + " - Invalid team leader ID");
                            }
                        }
                    }
                    
                    usersArray.add(userObj);
                    System.out.println("DEBUG: Row " + (i + 1) + " - Successfully added to import list");
                    
                } catch (Exception ex) {
                    System.err.println("ERROR: Row " + (i + 1) + " - Exception: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
            
            workbook.close();
            
            System.out.println("DEBUG: Total users to import: " + usersArray.size());
            
            if (usersArray.size() == 0) {
                javax.swing.JOptionPane.showMessageDialog(frame,
                    "Nu s-au găsit utilizatori valizi în fișierul Excel.",
                    "Eroare Import",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Send to server
            JsonObject request = new JsonObject();
            request.add("users", usersArray);
            
            netClient.setMessageHandler((type, payload) -> {
                if ("import_users_response".equals(type) && payload != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        try {
                            JsonObject jo = payload.getAsJsonObject();
                            if (jo.has("status") && "ok".equals(jo.get("status").getAsString())) {
                                int imported = jo.has("imported") ? jo.get("imported").getAsInt() : 0;
                                javax.swing.JOptionPane.showMessageDialog(frame,
                                    "Import reușit! " + imported + " utilizatori au fost adăugați.",
                                    "Succes",
                                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                String error = jo.has("error") ? jo.get("error").getAsString() : 
                                              jo.has("message") ? jo.get("message").getAsString() : "Eroare necunoscută";
                                javax.swing.JOptionPane.showMessageDialog(frame,
                                    "Eroare la import: " + error,
                                    "Eroare",
                                    javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception ex) {
                            javax.swing.JOptionPane.showMessageDialog(frame,
                                "Eroare la procesarea răspunsului: " + ex.getMessage(),
                                "Eroare",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            });
            
            netClient.send("import_users", request);
            
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(frame,
                "Eroare la citirea fișierului Excel: " + ex.getMessage(),
                "Eroare",
                javax.swing.JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void showImportWorkSessionsDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Selectați fișierul Excel cu istoric lucru");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel Files", "xlsx", "xls"));
        
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            importWorkSessionsFromExcel(selectedFile);
        }
    }

    private void importWorkSessionsFromExcel(java.io.File file) {
        try {
            org.apache.poi.ss.usermodel.Workbook workbook;
            if (file.getName().endsWith(".xlsx")) {
                workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.FileInputStream(file));
            } else {
                workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook(new java.io.FileInputStream(file));
            }
            
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            JsonArray sessionsArray = new JsonArray();
            
            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row == null) continue;
                
                JsonObject sessionObj = new JsonObject();
                
                // User ID (required)
                org.apache.poi.ss.usermodel.Cell userIdCell = row.getCell(0);
                if (userIdCell == null) {
                    System.out.println("Skipping row " + (i + 1) + ": missing user ID");
                    continue;
                }
                int userId = (int) (userIdCell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC 
                    ? userIdCell.getNumericCellValue()
                    : Integer.parseInt(userIdCell.getStringCellValue().trim()));
                sessionObj.addProperty("user_id", userId);
                
                // Start DateTime (required)
                org.apache.poi.ss.usermodel.Cell startCell = row.getCell(1);
                if (startCell == null) {
                    System.out.println("Skipping row " + (i + 1) + ": missing start time");
                    continue;
                }
                String startTime;
                if (startCell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC 
                    && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(startCell)) {
                    java.util.Date date = startCell.getDateCellValue();
                    startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
                } else {
                    startTime = startCell.getStringCellValue().trim();
                }
                sessionObj.addProperty("start_time", startTime);
                
                // End DateTime (optional)
                org.apache.poi.ss.usermodel.Cell endCell = row.getCell(2);
                if (endCell != null) {
                    String endTime;
                    if (endCell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC 
                        && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(endCell)) {
                        java.util.Date date = endCell.getDateCellValue();
                        endTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
                    } else {
                        endTime = endCell.getStringCellValue().trim();
                    }
                    if (!endTime.isEmpty()) {
                        sessionObj.addProperty("end_time", endTime);
                    }
                }
                
                sessionsArray.add(sessionObj);
            }
            
            workbook.close();
            
            if (sessionsArray.size() == 0) {
                javax.swing.JOptionPane.showMessageDialog(frame,
                    "Nu s-au găsit sesiuni valide în fișierul Excel.",
                    "Eroare Import",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Send to server
            JsonObject request = new JsonObject();
            request.add("sessions", sessionsArray);
            
            netClient.setMessageHandler((type, payload) -> {
                if ("import_work_sessions_response".equals(type) && payload != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        try {
                            JsonObject jo = payload.getAsJsonObject();
                            if (jo.has("status") && "ok".equals(jo.get("status").getAsString())) {
                                int imported = jo.has("imported") ? jo.get("imported").getAsInt() : 0;
                                javax.swing.JOptionPane.showMessageDialog(frame,
                                    "Import reușit! " + imported + " sesiuni de lucru au fost adăugate.",
                                    "Succes",
                                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                String error = jo.has("error") ? jo.get("error").getAsString() : 
                                              jo.has("message") ? jo.get("message").getAsString() : "Eroare necunoscută";
                                javax.swing.JOptionPane.showMessageDialog(frame,
                                    "Eroare la import: " + error,
                                    "Eroare",
                                    javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception ex) {
                            javax.swing.JOptionPane.showMessageDialog(frame,
                                "Eroare la procesarea răspunsului: " + ex.getMessage(),
                                "Eroare",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            });
            
            netClient.send("import_work_sessions", request);
            
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(frame,
                "Eroare la citirea fișierului Excel: " + ex.getMessage(),
                "Eroare",
                javax.swing.JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void showMonthlyStatistics() {
        JFrame statsFrame = new JFrame("Statistici Lunare Detaliate");
        statsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        statsFrame.setLayout(new BorderLayout(10, 10));
        
        // Top panel: month/year selector
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.add(new JLabel("Selectați luna:"));
        
        String[] months = {"Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie", 
                          "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie"};
        javax.swing.JComboBox<String> monthCombo = new javax.swing.JComboBox<>(months);
        monthCombo.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        topPanel.add(monthCombo);
        
        topPanel.add(new JLabel("An:"));
        javax.swing.JSpinner yearSpinner = new javax.swing.JSpinner(
            new javax.swing.SpinnerNumberModel(LocalDate.now().getYear(), 2020, 2100, 1));
        yearSpinner.setEditor(new javax.swing.JSpinner.NumberEditor(yearSpinner, "#"));
        topPanel.add(yearSpinner);
        
        JButton loadBtn = new JButton("Încarcă Statistici");
        topPanel.add(loadBtn);
        
        statsFrame.add(topPanel, BorderLayout.NORTH);
        
        // Center: table with statistics
        String[] columnNames = {
            "ID", "Nume", "Rol", "Zile Lucrătoare", "Zile Lucrate", 
            "Concedii Plătite", "Zile Lipsă", "Rată Prezență (%)", 
            "Ore Așteptate", "Ore Lucrate"
        };
        javax.swing.table.DefaultTableModel tableModel = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        javax.swing.JTable table = new javax.swing.JTable(tableModel);
        table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setRowHeight(25);
        
        // Make table sortable
        table.setAutoCreateRowSorter(true);
        
        JScrollPane scrollPane = new JScrollPane(table);
        statsFrame.add(scrollPane, BorderLayout.CENTER);
        
        // Bottom panel: summary
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JLabel summaryLabel = new JLabel("Total angajați: 0");
        bottomPanel.add(summaryLabel);
        statsFrame.add(bottomPanel, BorderLayout.SOUTH);
        
        // Load button action
        loadBtn.addActionListener(e -> {
            int month = monthCombo.getSelectedIndex() + 1;
            int year = (Integer) yearSpinner.getValue();
            
            JsonObject request = new JsonObject();
            request.addProperty("month", month);
            request.addProperty("year", year);
            
            netClient.setMessageHandler((type, payload) -> {
                if ("monthly_statistics_response".equals(type) && payload != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        try {
                            JsonObject jo = payload.getAsJsonObject();
                            
                            if (jo.has("error")) {
                                javax.swing.JOptionPane.showMessageDialog(statsFrame,
                                    "Eroare: " + jo.get("error").getAsString(),
                                    "Eroare",
                                    javax.swing.JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                            
                            // Clear existing data
                            tableModel.setRowCount(0);
                            
                            // Parse statistics
                            JsonArray stats = jo.getAsJsonArray("statistics");
                            if (stats == null) {
                                javax.swing.JOptionPane.showMessageDialog(statsFrame,
                                    "Nu s-au primit date de la server",
                                    "Eroare",
                                    javax.swing.JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                            
                            for (int i = 0; i < stats.size(); i++) {
                                JsonObject emp = stats.get(i).getAsJsonObject();
                                
                                Object[] row = new Object[] {
                                    emp.get("user_id").getAsInt(),
                                    emp.get("name").getAsString(),
                                    emp.get("role").getAsString(),
                                    emp.get("working_days").getAsInt(),
                                    emp.get("days_worked").getAsInt(),
                                    emp.get("paid_leaves").getAsInt(),
                                    emp.get("days_absent").getAsInt(),
                                    String.format("%.2f%%", emp.get("attendance_rate").getAsDouble()),
                                    String.format("%.1f", emp.get("expected_hours").getAsDouble()),
                                    String.format("%.1f", emp.get("actual_hours").getAsDouble())
                                };
                                tableModel.addRow(row);
                            }
                            
                            summaryLabel.setText("Total angajați: " + stats.size());
                            
                        } catch (Exception ex) {
                            javax.swing.JOptionPane.showMessageDialog(statsFrame,
                                "Eroare la procesarea datelor: " + ex.getMessage(),
                                "Eroare",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                            ex.printStackTrace();
                        }
                    });
                }
            });
            
            try {
                netClient.send("get_monthly_statistics", request);
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(statsFrame,
                    "Eroare la trimiterea cererii: " + ex.getMessage(),
                    "Eroare",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
        
        statsFrame.setSize(new Dimension(1200, 600));
        statsFrame.setLocationRelativeTo(frame);
        statsFrame.setVisible(true);
        
        // Auto-load current month
        loadBtn.doClick();
    }

    /**
     * Helper method to extract cell value as String regardless of cell type
     */
    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Check if it's a whole number
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (IllegalStateException e2) {
                        return String.valueOf(cell.getBooleanCellValue());
                    }
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    private void showJobManagement() {
        JFrame jobFrame = new JFrame("Gestionare Joburi");
        jobFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jobFrame.setLayout(new BorderLayout(10, 10));
        
        // Top panel: add new job
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBorder(BorderFactory.createTitledBorder("Adaugă Job Nou"));
        
        topPanel.add(new JLabel("Titlu Job:"));
        JTextField titleField = new JTextField(20);
        topPanel.add(titleField);
        
        topPanel.add(new JLabel("Descriere:"));
        JTextField descField = new JTextField(30);
        topPanel.add(descField);
        
        JButton addBtn = new JButton("Adaugă");
        topPanel.add(addBtn);
        
        jobFrame.add(topPanel, BorderLayout.NORTH);
        
        // Center: table with jobs
        String[] columnNames = {"ID", "Titlu Job", "Descriere", "Data Creare"};
        javax.swing.table.DefaultTableModel tableModel = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        javax.swing.JTable table = new javax.swing.JTable(tableModel);
        table.setRowHeight(25);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(table);
        jobFrame.add(scrollPane, BorderLayout.CENTER);
        
        // Bottom panel: actions
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        
        JButton editBtn = new JButton("Editează");
        JButton deleteBtn = new JButton("Șterge");
        JButton refreshBtn = new JButton("Reîncarcă");
        JButton closeBtn = new JButton("Închide");
        
        bottomPanel.add(editBtn);
        bottomPanel.add(deleteBtn);
        bottomPanel.add(refreshBtn);
        bottomPanel.add(closeBtn);
        
        jobFrame.add(bottomPanel, BorderLayout.SOUTH);
        
        // Load jobs function
        Runnable loadJobs = () -> {
            netClient.setMessageHandler((type, payload) -> {
                if ("get_jobs_response".equals(type) && payload != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        try {
                            tableModel.setRowCount(0);
                            JsonObject jo = payload.getAsJsonObject();
                            JsonArray jobs = jo.getAsJsonArray("jobs");
                            
                            for (int i = 0; i < jobs.size(); i++) {
                                JsonObject job = jobs.get(i).getAsJsonObject();
                                Object[] row = {
                                    job.get("id").getAsInt(),
                                    job.get("title").getAsString(),
                                    job.has("description") && !job.get("description").isJsonNull() 
                                        ? job.get("description").getAsString() : "",
                                    job.has("created_at") ? job.get("created_at").getAsString() : ""
                                };
                                tableModel.addRow(row);
                            }
                        } catch (Exception ex) {
                            javax.swing.JOptionPane.showMessageDialog(jobFrame,
                                "Eroare la încărcarea joburilor: " + ex.getMessage(),
                                "Eroare",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            });
            
            try {
                netClient.send("get_jobs", new JsonObject());
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(jobFrame,
                    "Eroare la trimiterea cererii: " + ex.getMessage(),
                    "Eroare",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        };
        
        // Add button action
        addBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            String desc = descField.getText().trim();
            
            if (title.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(jobFrame,
                    "Titlul jobului este obligatoriu!",
                    "Eroare",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            JsonObject request = new JsonObject();
            request.addProperty("title", title);
            if (!desc.isEmpty()) {
                request.addProperty("description", desc);
            }
            
            netClient.setMessageHandler((type, payload) -> {
                if ("add_job_response".equals(type) && payload != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        try {
                            JsonObject jo = payload.getAsJsonObject();
                            if (jo.has("status") && "ok".equals(jo.get("status").getAsString())) {
                                javax.swing.JOptionPane.showMessageDialog(jobFrame,
                                    "Job adăugat cu succes!",
                                    "Succes",
                                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                titleField.setText("");
                                descField.setText("");
                                loadJobs.run();
                            } else {
                                String error = jo.has("error") ? jo.get("error").getAsString() : 
                                              jo.has("message") ? jo.get("message").getAsString() : "Eroare necunoscută";
                                javax.swing.JOptionPane.showMessageDialog(jobFrame,
                                    "Eroare: " + error,
                                    "Eroare",
                                    javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception ex) {
                            javax.swing.JOptionPane.showMessageDialog(jobFrame,
                                "Eroare la procesare: " + ex.getMessage(),
                                "Eroare",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            });
            
            try {
                netClient.send("add_job", request);
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(jobFrame,
                    "Eroare la trimiterea cererii: " + ex.getMessage(),
                    "Eroare",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Edit button action
        editBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                javax.swing.JOptionPane.showMessageDialog(jobFrame,
                    "Selectează un job din tabel!",
                    "Eroare",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            int jobId = (Integer) tableModel.getValueAt(selectedRow, 0);
            String oldTitle = (String) tableModel.getValueAt(selectedRow, 1);
            String oldDesc = (String) tableModel.getValueAt(selectedRow, 2);
            
            JTextField editTitleField = new JTextField(oldTitle, 20);
            JTextField editDescField = new JTextField(oldDesc, 30);
            
            JPanel editPanel = new JPanel(new java.awt.GridLayout(2, 2, 5, 5));
            editPanel.add(new JLabel("Titlu Job:"));
            editPanel.add(editTitleField);
            editPanel.add(new JLabel("Descriere:"));
            editPanel.add(editDescField);
            
            int result = javax.swing.JOptionPane.showConfirmDialog(jobFrame, editPanel,
                "Editează Job", javax.swing.JOptionPane.OK_CANCEL_OPTION);
            
            if (result == javax.swing.JOptionPane.OK_OPTION) {
                String newTitle = editTitleField.getText().trim();
                String newDesc = editDescField.getText().trim();
                
                if (newTitle.isEmpty()) {
                    javax.swing.JOptionPane.showMessageDialog(jobFrame,
                        "Titlul jobului este obligatoriu!",
                        "Eroare",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                JsonObject request = new JsonObject();
                request.addProperty("id", jobId);
                request.addProperty("title", newTitle);
                request.addProperty("description", newDesc);
                
                netClient.setMessageHandler((type, payload) -> {
                    if ("update_job_response".equals(type) && payload != null) {
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            try {
                                JsonObject jo = payload.getAsJsonObject();
                                if (jo.has("status") && "ok".equals(jo.get("status").getAsString())) {
                                    javax.swing.JOptionPane.showMessageDialog(jobFrame,
                                        "Job actualizat cu succes!",
                                        "Succes",
                                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                    loadJobs.run();
                                } else {
                                    String error = jo.has("error") ? jo.get("error").getAsString() : 
                                                  jo.has("message") ? jo.get("message").getAsString() : "Eroare necunoscută";
                                    javax.swing.JOptionPane.showMessageDialog(jobFrame,
                                        "Eroare: " + error,
                                        "Eroare",
                                        javax.swing.JOptionPane.ERROR_MESSAGE);
                                }
                            } catch (Exception ex) {
                                javax.swing.JOptionPane.showMessageDialog(jobFrame,
                                    "Eroare la procesare: " + ex.getMessage(),
                                    "Eroare",
                                    javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                });
                
                try {
                    netClient.send("update_job", request);
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(jobFrame,
                        "Eroare la trimiterea cererii: " + ex.getMessage(),
                        "Eroare",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        // Delete button action
        deleteBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                javax.swing.JOptionPane.showMessageDialog(jobFrame,
                    "Selectează un job din tabel!",
                    "Eroare",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            int jobId = (Integer) tableModel.getValueAt(selectedRow, 0);
            String jobTitle = (String) tableModel.getValueAt(selectedRow, 1);
            
            int confirm = javax.swing.JOptionPane.showConfirmDialog(jobFrame,
                "Sigur vrei să ștergi jobul \"" + jobTitle + "\"?\n" +
                "ATENȚIE: Angajații cu acest job vor rămâne cu job NULL!",
                "Confirmare Ștergere",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE);
            
            if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                JsonObject request = new JsonObject();
                request.addProperty("id", jobId);
                
                netClient.setMessageHandler((type, payload) -> {
                    if ("delete_job_response".equals(type) && payload != null) {
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            try {
                                JsonObject jo = payload.getAsJsonObject();
                                if (jo.has("status") && "ok".equals(jo.get("status").getAsString())) {
                                    javax.swing.JOptionPane.showMessageDialog(jobFrame,
                                        "Job șters cu succes!",
                                        "Succes",
                                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                    loadJobs.run();
                                } else {
                                    String error = jo.has("error") ? jo.get("error").getAsString() : 
                                                  jo.has("message") ? jo.get("message").getAsString() : "Eroare necunoscută";
                                    javax.swing.JOptionPane.showMessageDialog(jobFrame,
                                        "Eroare: " + error,
                                        "Eroare",
                                        javax.swing.JOptionPane.ERROR_MESSAGE);
                                }
                            } catch (Exception ex) {
                                javax.swing.JOptionPane.showMessageDialog(jobFrame,
                                    "Eroare la procesare: " + ex.getMessage(),
                                    "Eroare",
                                    javax.swing.JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                });
                
                try {
                    netClient.send("delete_job", request);
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(jobFrame,
                        "Eroare la trimiterea cererii: " + ex.getMessage(),
                        "Eroare",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        // Refresh button
        refreshBtn.addActionListener(e -> loadJobs.run());
        
        // Close button
        closeBtn.addActionListener(e -> jobFrame.dispose());
        
        jobFrame.setSize(new Dimension(800, 500));
        jobFrame.setLocationRelativeTo(frame);
        jobFrame.setVisible(true);
        
        // Auto-load on open
        loadJobs.run();
    }
}
