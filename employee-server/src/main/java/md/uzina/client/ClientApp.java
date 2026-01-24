package md.uzina.client;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ClientApp {
    private final JFrame frame = new JFrame("Uzina Client");
    private final JTextArea logArea = new JTextArea(15, 50);
    private final JTextField hostField = new JTextField("localhost", 10);
    private final JTextField portField = new JTextField("5000", 5);
    private final JTextField idField = new JTextField(10);
    private final JPasswordField passField = new JPasswordField(10);
    private final JButton connectBtn = new JButton("Connect");
    private final JButton loginBtn = new JButton("Login");
    private final JButton presenceBtn = new JButton("Mark Presence");
    private final JButton logoutBtn = new JButton("Logout");

    private final NetClient net = new NetClient();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientApp().show());
    }

    public void show() {
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) { net.close(); System.exit(0);}        
        });

        JPanel top = new JPanel();
        top.add(new JLabel("Host:")); top.add(hostField);
        top.add(new JLabel("Port:")); top.add(portField);
        top.add(connectBtn);

        JPanel auth = new JPanel();
        auth.add(new JLabel("ID:")); auth.add(idField);
        auth.add(new JLabel("Password:")); auth.add(passField);
        auth.add(loginBtn);
        auth.add(presenceBtn);
        auth.add(logoutBtn);

        logArea.setEditable(false);
        JScrollPane sp = new JScrollPane(logArea);

        frame.setLayout(new BorderLayout());
        frame.add(top, BorderLayout.NORTH);
        frame.add(auth, BorderLayout.CENTER);
        frame.add(sp, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        setupActions();
        frame.setVisible(true);
    }

    private void setupActions() {
        connectBtn.addActionListener(e -> {
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            appendLog("Connecting to " + host + ":" + port + "...");
            new Thread(() -> {
                try {
                    net.connect(host, port);
                    net.setMessageHandler(this::onMessage);
                    appendLog("Connected.");
                } catch (IOException ex) {
                    appendLog("Connect failed: " + ex.getMessage());
                }
            }, "connect-thread").start();
        });

        loginBtn.addActionListener(e -> {
            String id = idField.getText().trim();
            String pass = new String(passField.getPassword());
            Map<String, String> payload = new HashMap<>();
            payload.put("id", id);
            payload.put("password", pass);
            try {
                net.send("login", payload);
                appendLog("Sent login for id=" + id);
            } catch (IOException ex) { appendLog("Send failed: " + ex.getMessage()); }
        });

        presenceBtn.addActionListener(e -> {
            try {
                net.send("mark_presence", new JsonObject());
                appendLog("Sent mark_presence");
            } catch (IOException ex) { appendLog("Send failed: " + ex.getMessage()); }
        });

        logoutBtn.addActionListener(e -> {
            try {
                net.send("logout", null);
                appendLog("Sent logout");
            } catch (IOException ex) { appendLog("Send failed: " + ex.getMessage()); }
        });
    }

    private void onMessage(String type, JsonElement payload) {
        SwingUtilities.invokeLater(() -> {
            appendLog("<-- " + type + ": " + (payload != null ? payload.toString() : "null"));
        });
    }

    private void appendLog(String s) {
        logArea.append(s + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
