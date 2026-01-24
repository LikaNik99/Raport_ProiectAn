package md.uzina.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.function.BiConsumer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NetClient {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private final Gson gson = new Gson();
    private Thread readerThread;
    private BiConsumer<String, JsonElement> messageHandler;

    public void setMessageHandler(BiConsumer<String, JsonElement> handler) {
        this.messageHandler = handler;
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        startReader();
    }

    private void startReader() {
        readerThread = new Thread(() -> {
            try {
                String line;
                while (socket != null && !socket.isClosed() && (line = in.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    JsonObject jo = JsonParser.parseString(line).getAsJsonObject();
                    String type = jo.has("type") ? jo.get("type").getAsString() : "";
                    JsonElement payload = jo.has("payload") ? jo.get("payload") : null;
                    if (messageHandler != null) messageHandler.accept(type, payload);
                }
            } catch (IOException e) {
                // connection closed
            }
        }, "net-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public synchronized void send(String type, Object payload) throws IOException {
        JsonObject jo = new JsonObject();
        jo.addProperty("type", type);
        if (payload != null) jo.add("payload", gson.toJsonTree(payload));
        String s = gson.toJson(jo);
        out.write(s);
        out.write('\n');
        out.flush();
    }

    public void close() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
