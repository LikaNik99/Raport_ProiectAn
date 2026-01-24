package md.uzina.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MainServer {
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "5000"));

    // userId -> handler
    private final ConcurrentMap<Integer, ClientHandler> connectedUsers = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public void start() throws IOException {
        System.out.println("Init DB...");
        try {
            DBManager.init();
        } catch (Exception e) {
            System.err.println("DB init failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // schedule end-of-day task based on work hours config
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduleEndOfDayTask(scheduler);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);
            while (true) {
                Socket client = serverSocket.accept();
                ClientHandler handler = new ClientHandler(client, connectedUsers);
                pool.submit(handler);
            }
        } finally {
            scheduler.shutdown();
            DBManager.close();
            pool.shutdown();
        }
    }

    private void scheduleEndOfDayTask(ScheduledExecutorService scheduler) {
        try {
            WorkHoursConfigService.WorkHoursConfig cfg = WorkHoursConfigService.getConfig();
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime todayEnd = now.withHour(cfg.endHour).withMinute(cfg.endMinute).withSecond(0).withNano(0);
            if (todayEnd.isBefore(now) || todayEnd.isEqual(now)) {
                todayEnd = todayEnd.plusDays(1);
            }
            long initialDelay = java.time.Duration.between(now, todayEnd).toMillis();
            long period = java.time.Duration.ofDays(1).toMillis();

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    if (WorkHoursConfigService.isWorkingDay()) {
                        System.out.println("Running end-of-day auto-close of active sessions...");
                        WorkSessionService.endAllActiveSessions();
                    } else {
                        System.out.println("Today is not a working day; skipping auto-close.");
                    }
                } catch (Exception ex) {
                    System.err.println("Error during end-of-day task: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }, initialDelay, period, java.util.concurrent.TimeUnit.MILLISECONDS);
            System.out.println("Scheduled end-of-day task to run at " + todayEnd);
        } catch (Exception e) {
            System.err.println("Failed to schedule end-of-day task: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        MainServer s = new MainServer();
        try {
            s.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
