package md.uzina.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

public class DBDump {
    public static void main(String[] args) throws Exception {
        System.out.println("Initializing DB...");
        DBManager.init();

        System.out.println("Querying work_sessions (latest 50):");
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, user_id, work_date, start_time, end_time, status FROM work_sessions ORDER BY work_date DESC, id DESC LIMIT 50")) {
            try (ResultSet rs = stmt.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    System.out.printf("id=%d user_id=%d date=%s start=%s end=%s status=%s\n",
                            rs.getInt("id"), rs.getInt("user_id"), rs.getDate("work_date"), rs.getTimestamp("start_time"), rs.getTimestamp("end_time"), rs.getString("status"));
                    count++;
                }
                if (count == 0) System.out.println("(no work_sessions rows found)");
            }
        }

        System.out.println("\nQuerying HRStatsService.getDailyStats for last 7 days:");
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(7);
        List<HRStatsService.DailyStats> stats = HRStatsService.getDailyStats(from, to);
        if (stats == null || stats.isEmpty()) {
            System.out.println("HRStatsService returned no stats");
        } else {
            for (HRStatsService.DailyStats ds : stats) {
                System.out.printf("%s total=%d present=%d leave=%d excused=%d unexcused=%d\n",
                        ds.date, ds.totalUsers, ds.presentCount, ds.leaveCount, ds.absentExcused, ds.absentUnexcused);
            }
        }

        System.out.println("\nDone.");
        DBManager.close();
    }
}
