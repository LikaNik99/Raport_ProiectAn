package md.uzina.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ClientApp {
    private final NetClient net = new NetClient();
    private JDialog loginDialog;
    private JTextArea logArea = new JTextArea();

    // Login dialog fields
    private final JTextField hostField = new JTextField("localhost", 12);
    private final JTextField portField = new JTextField("5000", 6);
    private final JTextField idField = new JTextField(8);
    private final JPasswordField passField = new JPasswordField(8);

    public static class ClientUser {
        public final String id;
        public final String name;
        public final String role;
        public ClientUser(String id, String name, String role) { 
            this.id = id; 
            this.name = name; 
            this.role = role; 
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientApp().show());
    }

    public void show() {
        SwingUtilities.invokeLater(this::showLoginDialog);
    }

    private void showLoginDialog() {
        loginDialog = new JDialog((JFrame) null, "Login", true);
        loginDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        loginDialog.setLayout(new BorderLayout(8, 8));

        // Create status indicator (circle) - red by default
        JPanel statusPanel = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                boolean connected = net.isConnected();
                g2.setColor(connected ? java.awt.Color.GREEN : java.awt.Color.RED);
                g2.fillOval(10, 10, 20, 20);
                g2.setColor(java.awt.Color.BLACK);
                g2.setStroke(new java.awt.BasicStroke(2));
                g2.drawOval(10, 10, 20, 20);
            }
        };
        statusPanel.setPreferredSize(new Dimension(40, 40));

        // Top: status + host/port + Connect
        JPanel top = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        top.add(statusPanel);
        top.add(new JLabel("Status: " + (net.isConnected() ? "CONNECTED" : "DISCONNECTED")));
        top.add(new JLabel("Host:"));
        top.add(hostField);
        top.add(new JLabel("Port:"));
        top.add(portField);
        JButton connect = new JButton("Connect");
        top.add(connect);
        loginDialog.add(top, BorderLayout.NORTH);

        // Center: login fields
        JPanel center = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        center.add(new JLabel("ID:"));
        center.add(idField);
        center.add(new JLabel("Password:"));
        center.add(passField);
        JButton submit = new JButton("Login");
        center.add(submit);
        loginDialog.add(center, BorderLayout.CENTER);

        // Size and position
        loginDialog.pack();
        loginDialog.setSize(new Dimension(600, 160));
        loginDialog.setLocationRelativeTo(null);

        connect.addActionListener(e -> {
            String host = hostField.getText().trim();
            int port;
            try { port = Integer.parseInt(portField.getText().trim()); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(loginDialog, "Port must be a number", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            appendLog("Attempting to connect to " + host + ":" + port + "...");
            new Thread(() -> {
                try {
                    net.connect(host, port);
                    net.setMessageHandler(this::onMessage);
                    appendLog("Connected to " + host + ":" + port);
                    SwingUtilities.invokeLater(() -> {
                        statusPanel.repaint();
                        JOptionPane.showMessageDialog(loginDialog, "Connected to server.", "Info", JOptionPane.INFORMATION_MESSAGE);
                        connect.setEnabled(false);
                        hostField.setEnabled(false);
                        portField.setEnabled(false);
                    });
                } catch (IOException ex) {
                    appendLog("Connect failed: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        statusPanel.repaint();
                        JOptionPane.showMessageDialog(loginDialog, "Connect failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }, "connect-thread").start();
        });

        submit.addActionListener(e -> {
            if (!net.isConnected()) {
                String host = hostField.getText().trim();
                int port;
                try { port = Integer.parseInt(portField.getText().trim()); }
                catch (NumberFormatException ex) { 
                    JOptionPane.showMessageDialog(loginDialog, "Port must be a number", "Error", JOptionPane.ERROR_MESSAGE); 
                    return; 
                }
                appendLog("Auto-connecting to " + host + ":" + port + "...");
                new Thread(() -> {
                    try {
                        net.connect(host, port);
                        net.setMessageHandler(this::onMessage);
                        appendLog("Auto-connected to " + host + ":" + port);
                        SwingUtilities.invokeLater(statusPanel::repaint);
                    } catch (IOException ex) {
                        appendLog("Auto-connect failed: " + ex.getMessage());
                        SwingUtilities.invokeLater(() -> {
                            statusPanel.repaint();
                            JOptionPane.showMessageDialog(loginDialog, "Connect failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        });
                        return;
                    }
                    doLogin(idField, passField, loginDialog);
                }, "auto-connect-thread").start();
                return;
            }
            doLogin(idField, passField, loginDialog);
        });

        loginDialog.setVisible(true);
    }

    private void doLogin(JTextField idField, JPasswordField passField, JDialog loginDialog) {
        String id = idField.getText().trim();
        String pass = new String(passField.getPassword());
        Map<String, String> payload = new HashMap<>();
        payload.put("id", id);
        payload.put("password", pass);
        try {
            net.send("login", payload);
            appendLog("-> login (id=" + id + ")");
        } catch (IOException ex) { 
            appendLog("Send failed: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(loginDialog, "Send failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
        }
    }

    private void openMainWindow(ClientUser u) {
        if (loginDialog != null && loginDialog.isShowing()) {
            loginDialog.dispose();
        }

        idField.setText("");
        passField.setText("");

        appendLog("Logged in as " + u.name + " (" + u.role + ")");
        
        // Check if user is administrator (ID=1)
        if ("1".equals(u.id)) {
            AdminPanel adminPanel = new AdminPanel(u, this::restartLogin, net);
            adminPanel.show();
            return;
        }
        
        switch (u.role.toUpperCase()) {
            case "WORKER" -> {
                WorkerPanel wp = new WorkerPanel(u, this::restartLogin, net);
                wp.show();
            }
            case "TEAMLEADER" -> {
                TeamLeaderPanel tlp = new TeamLeaderPanel(u, this::restartLogin, net);
                tlp.show();
            }
            case "HR" -> {
                HRPanel hrp = new HRPanel(u, this::restartLogin, net);
                hrp.show();
            }
            case "ADMIN" -> {
                AdminWindow aw = new AdminWindow(u, this::restartLogin, net);
                aw.show();
            }
            default -> JOptionPane.showMessageDialog(null, "Unknown role: " + u.role, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void restartLogin() {
        net.close();
        showLoginDialog();
    }

    private void onMessage(String type, JsonElement payload) {
        SwingUtilities.invokeLater(() -> {
            appendLog("<- " + type + ": " + (payload != null ? payload.toString() : "null"));
            try {
                if ("login_response".equals(type) && payload != null && payload.isJsonObject()) {
                    JsonObject jo = payload.getAsJsonObject();
                    boolean ok = jo.has("ok") && jo.get("ok").getAsBoolean();
                    String message = jo.has("message") ? jo.get("message").getAsString() : "";
                    if (ok && jo.has("user") && jo.get("user").isJsonObject()) {
                        JsonObject user = jo.getAsJsonObject("user");
                        String uid = user.has("id") ? user.get("id").getAsString() : "?";
                        String uname = user.has("name") ? user.get("name").getAsString() : "";
                        String urole = user.has("role") ? user.get("role").getAsString() : "";
                        ClientUser cu = new ClientUser(uid, uname, urole);
                        openMainWindow(cu);
                        JOptionPane.showMessageDialog(loginDialog, message, "Login Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(loginDialog != null ? loginDialog : null, message, "Login Failed", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (RuntimeException ex) {
                appendLog("Failed to process message: " + ex.getMessage());
            }
        });
    }

    private void appendLog(String s) {
        logArea.append(s + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}

