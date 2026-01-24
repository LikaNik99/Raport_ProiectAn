package md.uzina.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AdminWindow {
    private final JFrame frame;
    private final ClientApp.ClientUser user;
    private final Runnable onLogout;
    private final NetClient netClient;
    private JTextArea statusArea;

    public AdminWindow(ClientApp.ClientUser user, Runnable onLogout, NetClient netClient) {
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
        header.add(new JLabel("Welcome, " + user.name + " (" + user.role + ")"));
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            frame.dispose();
            onLogout.run();
        });
        header.add(logoutBtn);
        frame.add(header, BorderLayout.NORTH);

        // Main content: admin features
        JPanel content = new JPanel(new BorderLayout());
        statusArea = new JTextArea();
        statusArea.setText(
            "Admin Features:\n\n" +
            "1. Import Employees - Bulk import employee data from Excel (.xlsx) files\n" +
            "2. Export User Lists - Export all users or specific user data to Excel\n" +
            "3. Export Reports - Generate and export detailed reports for each user\n" +
            "4. Manage Passwords - Edit user passwords for account recovery\n" +
            "5. Full User Management - Create, modify, or delete user accounts\n" +
            "6. Work Hours Configuration - Set work schedule and working days\n" +
            "7. System Configuration - Configure system settings and parameters\n\n" +
            "User ID: " + user.id + "\n" +
            "Role: " + user.role
        );
        statusArea.setEditable(false);
        content.add(new JScrollPane(statusArea), BorderLayout.CENTER);
        frame.add(content, BorderLayout.CENTER);

        // Buttons for features
        JPanel buttons = new JPanel();
        buttons.add(new JButton("Import Employees"));
        buttons.add(new JButton("Export User Lists"));
        buttons.add(new JButton("Export Reports"));
        buttons.add(new JButton("Manage Passwords"));
        buttons.add(new JButton("User Management"));
        
        JButton workHoursBtn = new JButton("Work Hours Config");
        workHoursBtn.addActionListener(e -> configureWorkHours());
        buttons.add(workHoursBtn);
        
        buttons.add(new JButton("System Configuration"));
        frame.add(buttons, BorderLayout.SOUTH);

        frame.setSize(new Dimension(900, 550));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void configureWorkHours() {
        JPanel configPanel = new JPanel();
        
        JSpinner startHourSpinner = new JSpinner(new SpinnerNumberModel(8, 0, 23, 1));
        JSpinner startMinSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        JSpinner endHourSpinner = new JSpinner(new SpinnerNumberModel(17, 0, 23, 1));
        JSpinner endMinSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        
        configPanel.add(new JLabel("Start Hour:"));
        configPanel.add(startHourSpinner);
        configPanel.add(new JLabel("Start Min:"));
        configPanel.add(startMinSpinner);
        configPanel.add(new JLabel("End Hour:"));
        configPanel.add(endHourSpinner);
        configPanel.add(new JLabel("End Min:"));
        configPanel.add(endMinSpinner);
        
        int result = JOptionPane.showConfirmDialog(frame, configPanel, "Configure Work Hours", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int startH = (int) startHourSpinner.getValue();
            int startM = (int) startMinSpinner.getValue();
            int endH = (int) endHourSpinner.getValue();
            int endM = (int) endMinSpinner.getValue();
            
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("startHour", startH);
                payload.addProperty("startMinute", startM);
                payload.addProperty("endHour", endH);
                payload.addProperty("endMinute", endM);
                
                netClient.setMessageHandler(this::handleUpdateWorkHoursConfigResponse);
                netClient.send("update_work_hours_config", payload);
                statusArea.append("\n[...] Updating work hours configuration...");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleUpdateWorkHoursConfigResponse(String type, JsonElement payload) {
        SwingUtilities.invokeLater(() -> {
            if ("update_work_hours_config_response".equals(type) && payload != null && payload.isJsonObject()) {
                JsonObject jo = payload.getAsJsonObject();
                String status = jo.has("status") ? jo.get("status").getAsString() : "error";
                String message = jo.has("message") ? jo.get("message").getAsString() : "";
                if ("ok".equals(status)) {
                    statusArea.append("\n[✓] Work hours configuration updated successfully");
                    JOptionPane.showMessageDialog(frame, "Configuration saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    statusArea.append("\n[✗] Error: " + message);
                    JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
}
