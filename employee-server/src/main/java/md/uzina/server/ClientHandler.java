package md.uzina.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializer;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ConcurrentMap<Integer, ClientHandler> connectedUsers;
    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) -> 
            src == null ? null : context.serialize(src.toString()))
        .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> 
            src == null ? null : context.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
        .create();

    private volatile User user;
    private BufferedReader in;
    private BufferedWriter out;

    public ClientHandler(Socket socket, ConcurrentMap<Integer, ClientHandler> connectedUsers) {
        this.socket = socket;
        this.connectedUsers = connectedUsers;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                JsonObject jo = JsonParser.parseString(line).getAsJsonObject();
                String type = jo.get("type").getAsString();
                JsonObject payload = jo.has("payload") ? jo.getAsJsonObject("payload") : new JsonObject();

                switch (type) {
                    case "login":
                        handleLogin(payload);
                        break;
                    case "mark_presence":
                        handleMarkPresence(payload);
                        break;
                    case "start_work":
                        handleStartWork(payload);
                        break;
                    case "end_work":
                        handleEndWork(payload);
                        break;
                    case "get_work_status":
                        handleGetWorkStatus(payload);
                        break;
                    case "get_work_hours_config":
                        handleGetWorkHoursConfig(payload);
                        break;
                    case "update_work_hours_config":
                        handleUpdateWorkHoursConfig(payload);
                        break;
                    case "get_hr_daily_stats":
                        handleGetHRDailyStats(payload);
                        break;
                    case "get_hr_user_stats":
                        handleGetHRUserStats(payload);
                        break;
                    case "get_hr_work_sessions":
                        handleGetHRWorkSessions(payload);
                        break;
                    case "get_hr_leave_requests":
                        handleGetHRLeaveRequests(payload);
                        break;
                    case "get_login_history":
                        handleGetLoginHistory(payload);
                        break;
                    case "submit_leave_request":
                        handleSubmitLeaveRequest(payload);
                        break;
                    case "get_user_leave_requests":
                        handleGetUserLeaveRequests(payload);
                        break;
                    case "get_team_leave_requests":
                        handleGetTeamLeaveRequests(payload);
                        break;
                    case "get_all_leave_requests":
                        handleGetAllLeaveRequests(payload);
                        break;
                    case "update_leave_request_status":
                        handleUpdateLeaveRequestStatus(payload);
                        break;
                    case "get_all_workers":
                        handleGetAllWorkers(payload);
                        break;
                    case "get_job_titles":
                        handleGetJobTitles();
                        break;
                    case "change_worker_to_teamleader":
                        handleChangeWorkerToTeamLeader(payload);
                        break;
                    case "assign_worker_to_teamleader":
                        handleAssignWorkerToTeamLeader(payload);
                        break;
                    case "change_teamleader_to_worker":
                        handleChangeTeamLeaderToWorker(payload);
                        break;
                    case "get_team_work_status":
                        handleGetTeamWorkStatus(payload);
                        break;
                    case "get_all_work_status":
                        handleGetAllWorkStatus(payload);
                        break;
                    case "force_stop_work":
                        handleForceStopWork(payload);
                        break;
                    case "end_all_active_sessions":
                        handleEndAllActiveSessions(payload);
                        break;
                    case "update_user_password":
                        handleUpdateUserPassword(payload);
                        break;
                    case "update_user_info":
                        handleUpdateUserInfo(payload);
                        break;
                    case "delete_user":
                        handleDeleteUser(payload);
                        break;
                    case "create_user":
                        handleCreateUser(payload);
                        break;
                    case "set_hourly_rate":
                        handleSetHourlyRate(payload);
                        break;
                    case "get_hourly_rates":
                        handleGetHourlyRates(payload);
                        break;
                    case "calculate_salaries":
                        handleCalculateSalaries(payload);
                        break;
                    case "get_salary_calculations":
                        handleGetSalaryCalculations(payload);
                        break;
                    case "add_salary_bonus":
                        handleAddSalaryBonus(payload);
                        break;
                    case "publish_salaries":
                        handlePublishSalaries(payload);
                        break;
                    case "get_my_salary_history":
                        handleGetMySalaryHistory(payload);
                        break;
                    case "get_hourly_rate_history":
                        handleGetHourlyRateHistory(payload);
                        break;
                    case "import_users":
                        handleImportUsers(payload);
                        break;
                    case "import_work_sessions":
                        handleImportWorkSessions(payload);
                        break;
                    case "get_monthly_statistics":
                        handleGetMonthlyStatistics(payload);
                        break;
                    case "get_jobs":
                        handleGetJobs(payload);
                        break;
                    case "add_job":
                        handleAddJob(payload);
                        break;
                    case "update_job":
                        handleUpdateJob(payload);
                        break;
                    case "delete_job":
                        handleDeleteJob(payload);
                        break;
                    case "logout":
                        handleLogout();
                        return;
                    default:
                        sendMessage("error", new SimpleMsg("unknown_type", "Unknown message type: " + type));
                }
            }
        } catch (IOException ex) {
            // client disconnected
        } finally {
            cleanup();
        }
    }

    private void handleLogin(JsonObject payload) {
        String id = payload.has("id") ? payload.get("id").getAsString() : "";
        String password = payload.has("password") ? payload.get("password").getAsString() : "";
        User u = AuthService.authenticate(id, password);
        if (u != null) {
            this.user = u;
            connectedUsers.put(u.id, this);
            sendMessage("login_response", new LoginResponse(true, "Login OK", u));
            // notify HR that someone connected (optional)
            broadcastToRole("notification", new SimpleMsg("login", "User " + u.id + " s-a conectat"), "HR");
        } else {
            sendMessage("login_response", new LoginResponse(false, "Invalid credentials", null));
        }
    }

    private void handleMarkPresence(JsonObject payload) {
        if (user == null) {
            sendMessage("mark_presence_response", new SimpleMsg("error", "Not authenticated"));
            return;
        }
        boolean ok = AuthService.markPresence(user.id);
        if (ok) {
            sendMessage("mark_presence_response", new SimpleMsg("ok", "Presence marked"));
            broadcastToRole("notification", new SimpleMsg("presence", "User " + user.id + " a venit la lucru"), "HR");
        } else {
            sendMessage("mark_presence_response", new SimpleMsg("error", "DB error"));
        }
    }

    private void handleLogout() {
        if (user != null) {
            try {
                LoginHistoryService.logLogout(user.id);
            } catch (Exception ex) {
                // log but don't fail logout
            }
        }
        sendMessage("logout_response", new SimpleMsg("ok", "bye"));
        cleanup();
    }

    private void handleStartWork(JsonObject payload) {
        if (user == null) {
            sendMessage("start_work_response", new SimpleMsg("error", "Not authenticated"));
            return;
        }
        try {
            WorkSessionService.startWorkSession(user.id);
            sendMessage("start_work_response", new SimpleMsg("ok", "Work session started"));
            broadcastToRole("notification", new SimpleMsg("work_started", "User " + user.id + " a inceput munca"), "HR");
        } catch (Exception ex) {
            sendMessage("start_work_response", new SimpleMsg("error", "DB error: " + ex.getMessage()));
        }
    }

    private void handleEndWork(JsonObject payload) {
        if (user == null) {
            sendMessage("end_work_response", new SimpleMsg("error", "Not authenticated"));
            return;
        }
        try {
            WorkSessionService.endWorkSession(user.id);
            sendMessage("end_work_response", new SimpleMsg("ok", "Work session ended"));
            broadcastToRole("notification", new SimpleMsg("work_ended", "User " + user.id + " a terminat munca"), "HR");
        } catch (Exception ex) {
            sendMessage("end_work_response", new SimpleMsg("error", "DB error: " + ex.getMessage()));
        }
    }

    private void handleGetWorkStatus(JsonObject payload) {
        if (user == null) {
            sendMessage("work_status_response", new SimpleMsg("error", "Not authenticated"));
            return;
        }
        try {
            String status = WorkSessionService.getWorkStatus(user.id);
            JsonObject res = new JsonObject();
            res.addProperty("status", status);
            sendMessage("work_status_response", res);
        } catch (Exception ex) {
            sendMessage("work_status_response", new SimpleMsg("error", "DB error: " + ex.getMessage()));
        }
    }

    private void handleGetWorkHoursConfig(JsonObject payload) {
        try {
            WorkHoursConfigService.WorkHoursConfig config = WorkHoursConfigService.getConfig();
            JsonObject res = new JsonObject();
            res.addProperty("startHour", config.startHour);
            res.addProperty("startMinute", config.startMinute);
            res.addProperty("endHour", config.endHour);
            res.addProperty("endMinute", config.endMinute);
            sendMessage("work_hours_config_response", res);
        } catch (Exception ex) {
            sendMessage("work_hours_config_response", new SimpleMsg("error", "DB error: " + ex.getMessage()));
        }
    }

    private void handleUpdateWorkHoursConfig(JsonObject payload) {
        if (user == null || !"ADMIN".equalsIgnoreCase(user.role)) {
            sendMessage("update_work_hours_config_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            int startHour = payload.has("startHour") ? payload.get("startHour").getAsInt() : 8;
            int startMinute = payload.has("startMinute") ? payload.get("startMinute").getAsInt() : 0;
            int endHour = payload.has("endHour") ? payload.get("endHour").getAsInt() : 17;
            int endMinute = payload.has("endMinute") ? payload.get("endMinute").getAsInt() : 0;
            WorkHoursConfigService.updateConfig(startHour, startMinute, endHour, endMinute);
            sendMessage("update_work_hours_config_response", new SimpleMsg("ok", "Config updated"));
        } catch (Exception ex) {
            sendMessage("update_work_hours_config_response", new SimpleMsg("error", "DB error: " + ex.getMessage()));
        }
    }

    private void cleanup() {
        try {
            if (user != null) connectedUsers.remove(user.id);
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public synchronized void sendMessage(String type, Object payload) {
        try {
            Message m = new Message(type, payload);
            String s = gson.toJson(m);
            out.write(s);
            out.write("\n");
            out.flush();
        } catch (IOException ex) {
            // cannot write => disconnect
            cleanup();
        }
    }

    private void broadcastToRole(String type, Object payload, String role) {
        connectedUsers.values().forEach(ch -> {
            if (ch.user != null && role.equalsIgnoreCase(ch.user.role)) {
                ch.sendMessage(type, payload);
            }
        });
    }

    private void handleGetHRDailyStats(JsonObject payload) {
        if (user == null || !"HR".equalsIgnoreCase(user.role)) {
            sendMessage("hr_daily_stats_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            String fromStr = payload.has("from") ? payload.get("from").getAsString() : "2025-01-01";
            String toStr = payload.has("to") ? payload.get("to").getAsString() : "2025-12-31";
            java.time.LocalDate from = java.time.LocalDate.parse(fromStr);
            java.time.LocalDate to = java.time.LocalDate.parse(toStr);
            
            List<HRStatsService.DailyStats> stats = HRStatsService.getDailyStats(from, to);
            String json = new Gson().toJson(stats);
            JsonObject res = new JsonObject();
            res.addProperty("stats", json);
            sendMessage("hr_daily_stats_response", res);
        } catch (Exception ex) {
            sendMessage("hr_daily_stats_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleGetHRUserStats(JsonObject payload) {
        if (user == null || !"HR".equalsIgnoreCase(user.role)) {
            sendMessage("hr_user_stats_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            String fromStr = payload.has("from") ? payload.get("from").getAsString() : "2025-01-01";
            String toStr = payload.has("to") ? payload.get("to").getAsString() : "2025-12-31";
            String userFilter = payload.has("userFilter") ? payload.get("userFilter").getAsString() : "";
            java.time.LocalDate from = java.time.LocalDate.parse(fromStr);
            java.time.LocalDate to = java.time.LocalDate.parse(toStr);
            
            List<HRStatsService.UserStats> stats = HRStatsService.getUserStats(from, to, userFilter);
            String json = new Gson().toJson(stats);
            JsonObject res = new JsonObject();
            res.addProperty("stats", json);
            sendMessage("hr_user_stats_response", res);
        } catch (Exception ex) {
            sendMessage("hr_user_stats_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleGetHRWorkSessions(JsonObject payload) {
        if (user == null || !"HR".equalsIgnoreCase(user.role)) {
            sendMessage("hr_work_sessions_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            String fromStr = payload.has("from") ? payload.get("from").getAsString() : "2025-01-01";
            String toStr = payload.has("to") ? payload.get("to").getAsString() : "2025-12-31";
            java.time.LocalDate from = java.time.LocalDate.parse(fromStr);
            java.time.LocalDate to = java.time.LocalDate.parse(toStr);
            
            List<HRStatsService.WorkSessionRecord> records = HRStatsService.getAllWorkSessions(from, to);
            String json = new Gson().toJson(records);
            JsonObject res = new JsonObject();
            res.addProperty("records", json);
            sendMessage("hr_work_sessions_response", res);
        } catch (Exception ex) {
            sendMessage("hr_work_sessions_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleGetHRLeaveRequests(JsonObject payload) {
        if (user == null || !"HR".equalsIgnoreCase(user.role)) {
            sendMessage("hr_leave_requests_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            String fromStr = payload.has("from") ? payload.get("from").getAsString() : "2025-01-01";
            String toStr = payload.has("to") ? payload.get("to").getAsString() : "2025-12-31";
            java.time.LocalDate from = java.time.LocalDate.parse(fromStr);
            java.time.LocalDate to = java.time.LocalDate.parse(toStr);
            
            List<HRStatsService.LeaveRequestRecord> records = HRStatsService.getAllLeaveRequests(from, to);
            String json = new Gson().toJson(records);
            JsonObject res = new JsonObject();
            res.addProperty("records", json);
            sendMessage("hr_leave_requests_response", res);
        } catch (Exception ex) {
            sendMessage("hr_leave_requests_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleGetLoginHistory(JsonObject payload) {
        if (user == null || !"HR".equalsIgnoreCase(user.role)) {
            sendMessage("login_history_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            String fromStr = payload.has("from") ? payload.get("from").getAsString() : "2025-01-01";
            String toStr = payload.has("to") ? payload.get("to").getAsString() : "2025-12-31";
            java.time.LocalDate from = java.time.LocalDate.parse(fromStr);
            java.time.LocalDate to = java.time.LocalDate.parse(toStr);
            
            List<LoginHistoryService.LoginHistoryRecord> records = LoginHistoryService.getLoginHistory(from, to);
            String json = new Gson().toJson(records);
            JsonObject res = new JsonObject();
            res.addProperty("records", json);
            sendMessage("login_history_response", res);
        } catch (Exception ex) {
            sendMessage("login_history_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleSubmitLeaveRequest(JsonObject payload) {
        if (user == null) {
            sendMessage("submit_leave_request_response", new SimpleMsg("error", "Not authenticated"));
            return;
        }
        try {
            String dateFromStr = payload.has("dateFrom") ? payload.get("dateFrom").getAsString() : "";
            String dateToStr = payload.has("dateTo") ? payload.get("dateTo").getAsString() : "";
            String reason = payload.has("reason") ? payload.get("reason").getAsString() : "";
            
            java.time.LocalDate dateFrom = java.time.LocalDate.parse(dateFromStr);
            java.time.LocalDate dateTo = java.time.LocalDate.parse(dateToStr);
            
            int requestId = LeaveRequestService.submitLeaveRequest(user.id, dateFrom, dateTo, reason);
            if (requestId > 0) {
                sendMessage("submit_leave_request_response", new SimpleMsg("ok", "Leave request submitted with ID " + requestId));
                broadcastToRole("notification", new SimpleMsg("leave_request", "User " + user.id + " a trimis o cerere de concediu"), "HR");
            } else {
                sendMessage("submit_leave_request_response", new SimpleMsg("error", "Failed to submit request"));
            }
        } catch (Exception ex) {
            sendMessage("submit_leave_request_response", new SimpleMsg("error", "DB error: " + ex.getMessage()));
        }
    }

    private void handleGetUserLeaveRequests(JsonObject payload) {
        if (user == null) {
            sendMessage("user_leave_requests_response", new SimpleMsg("error", "Not authenticated"));
            return;
        }
        try {
            List<LeaveRequestService.LeaveRequest> requests = LeaveRequestService.getUserLeaveRequests(user.id);
            com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
            for (LeaveRequestService.LeaveRequest req : requests) {
                com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                obj.addProperty("id", req.id);
                obj.addProperty("userId", req.userId);
                obj.addProperty("dateFrom", req.dateFrom.toString());
                obj.addProperty("dateTo", req.dateTo.toString());
                obj.addProperty("reason", req.reason);
                obj.addProperty("status", req.status);
                obj.addProperty("createdAt", req.createdAt.toString());
                jsonArray.add(obj);
            }
            JsonObject res = new JsonObject();
            res.addProperty("requests", jsonArray.toString());
            sendMessage("user_leave_requests_response", res);
        } catch (Exception ex) {
            sendMessage("user_leave_requests_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleGetTeamLeaveRequests(JsonObject payload) {
        if (user == null || !"TEAMLEADER".equalsIgnoreCase(user.role)) {
            sendMessage("team_leave_requests_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            List<LeaveRequestService.LeaveRequestWithWorker> requests = LeaveRequestService.getTeamLeaveRequests(user.id);
            com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
            for (LeaveRequestService.LeaveRequestWithWorker req : requests) {
                com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                obj.addProperty("id", req.id);
                obj.addProperty("userId", req.userId);
                obj.addProperty("workerName", req.workerName);
                obj.addProperty("dateFrom", req.dateFrom.toString());
                obj.addProperty("dateTo", req.dateTo.toString());
                obj.addProperty("reason", req.reason);
                obj.addProperty("status", req.status);
                obj.addProperty("createdAt", req.createdAt.toString());
                jsonArray.add(obj);
            }
            JsonObject res = new JsonObject();
            res.addProperty("requests", jsonArray.toString());
            sendMessage("team_leave_requests_response", res);
        } catch (Exception ex) {
            sendMessage("team_leave_requests_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleGetAllLeaveRequests(JsonObject payload) {
        if (user == null || !"HR".equalsIgnoreCase(user.role)) {
            sendMessage("all_leave_requests_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            List<LeaveRequestService.LeaveRequestWithWorker> requests = LeaveRequestService.getAllLeaveRequests();
            com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
            for (LeaveRequestService.LeaveRequestWithWorker req : requests) {
                com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                obj.addProperty("id", req.id);
                obj.addProperty("userId", req.userId);
                obj.addProperty("workerName", req.workerName);
                obj.addProperty("dateFrom", req.dateFrom.toString());
                obj.addProperty("dateTo", req.dateTo.toString());
                obj.addProperty("reason", req.reason);
                obj.addProperty("status", req.status);
                obj.addProperty("createdAt", req.createdAt.toString());
                jsonArray.add(obj);
            }
            JsonObject res = new JsonObject();
            res.addProperty("requests", jsonArray.toString());
            sendMessage("all_leave_requests_response", res);
        } catch (Exception ex) {
            sendMessage("all_leave_requests_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleUpdateLeaveRequestStatus(JsonObject payload) {
        if (user == null || !"HR".equalsIgnoreCase(user.role)) {
            sendMessage("update_leave_request_status_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            int leaveRequestId = payload.has("requestId") ? payload.get("requestId").getAsInt() : -1;
            String newStatus = payload.has("status") ? payload.get("status").getAsString() : "PENDING";
            
            boolean success = LeaveRequestService.updateLeaveRequestStatus(leaveRequestId, newStatus);
            if (success) {
                sendMessage("update_leave_request_status_response", new SimpleMsg("ok", "Status updated to " + newStatus));
            } else {
                sendMessage("update_leave_request_status_response", new SimpleMsg("error", "Request not found"));
            }
        } catch (Exception ex) {
            sendMessage("update_leave_request_status_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleGetAllWorkers(JsonObject payload) {
        if (user == null) {
            sendMessage("all_workers_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        boolean isAuthorized = user.id == 1 || "ADMIN".equalsIgnoreCase(user.role) || "HR".equalsIgnoreCase(user.role);
        if (!isAuthorized) {
            sendMessage("all_workers_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            List<AdminService.Worker> workers = AdminService.getAllWorkers();
            String json = new Gson().toJson(workers);
            JsonObject res = new JsonObject();
            res.addProperty("workers", json);
            sendMessage("all_workers_response", res);
        } catch (Exception ex) {
            sendMessage("all_workers_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleGetJobTitles() {
        if (user == null) {
            sendMessage("job_titles_response", new SimpleMsg("error", "Not authenticated"));
            return;
        }
        try {
            List<AdminService.JobTitle> jobTitles = AdminService.getJobTitles();
            String json = new Gson().toJson(jobTitles);
            JsonObject res = new JsonObject();
            res.addProperty("jobTitles", json);
            sendMessage("job_titles_response", res);
        } catch (Exception ex) {
            sendMessage("job_titles_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleChangeWorkerToTeamLeader(JsonObject payload) {
        if (user == null) {
            sendMessage("change_worker_to_teamleader_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        boolean isAuthorized = user.id == 1 || "ADMIN".equalsIgnoreCase(user.role) || "HR".equalsIgnoreCase(user.role);
        if (!isAuthorized) {
            sendMessage("change_worker_to_teamleader_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            int workerId = payload.has("workerId") ? payload.get("workerId").getAsInt() : -1;
            boolean success = AdminService.changeWorkerToTeamLeader(workerId);
            if (success) {
                sendMessage("change_worker_to_teamleader_response", new SimpleMsg("ok", "Worker promoted to team leader"));
            } else {
                sendMessage("change_worker_to_teamleader_response", new SimpleMsg("error", "Worker not found or already team leader"));
            }
        } catch (Exception ex) {
            sendMessage("change_worker_to_teamleader_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleChangeTeamLeaderToWorker(JsonObject payload) {
        if (user == null) {
            sendMessage("change_teamleader_to_worker_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        boolean isAuthorized = user.id == 1 || "ADMIN".equalsIgnoreCase(user.role) || "HR".equalsIgnoreCase(user.role);
        if (!isAuthorized) {
            sendMessage("change_teamleader_to_worker_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            int teamLeaderId = payload.has("teamLeaderId") ? payload.get("teamLeaderId").getAsInt() : -1;
            boolean success = AdminService.changeTeamLeaderToWorker(teamLeaderId);
            if (success) {
                sendMessage("change_teamleader_to_worker_response", new SimpleMsg("ok", "Team leader demoted to worker"));
            } else {
                sendMessage("change_teamleader_to_worker_response", new SimpleMsg("error", "Failed to demote team leader"));
            }
        } catch (Exception ex) {
            sendMessage("change_teamleader_to_worker_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleAssignWorkerToTeamLeader(JsonObject payload) {
        if (user == null) {
            sendMessage("assign_worker_to_teamleader_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        boolean isAuthorized = user.id == 1 || "ADMIN".equalsIgnoreCase(user.role) || "HR".equalsIgnoreCase(user.role);
        if (!isAuthorized) {
            sendMessage("assign_worker_to_teamleader_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            int workerId = payload.has("workerId") ? payload.get("workerId").getAsInt() : -1;
            int teamLeaderId = payload.has("teamLeaderId") ? payload.get("teamLeaderId").getAsInt() : -1;
            boolean success = AdminService.assignWorkerToTeamLeader(workerId, teamLeaderId);
            if (success) {
                sendMessage("assign_worker_to_teamleader_response", new SimpleMsg("ok", "Worker assigned to team leader"));
            } else {
                sendMessage("assign_worker_to_teamleader_response", new SimpleMsg("error", "Failed to assign worker"));
            }
        } catch (Exception ex) {
            sendMessage("assign_worker_to_teamleader_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleGetTeamWorkStatus(JsonObject payload) {
        if (user == null) {
            JsonObject res = new JsonObject();
            res.addProperty("error", "Not authenticated");
            sendMessage("team_work_status_response", res);
            return;
        }
        if (!"TEAMLEADER".equalsIgnoreCase(user.role)) {
            JsonObject res = new JsonObject();
            res.addProperty("error", "Unauthorized");
            sendMessage("team_work_status_response", res);
            return;
        }
        try {
            List<WorkSessionService.WorkStatusInfo> teamStatus = WorkSessionService.getTeamWorkStatus(user.id);
            System.out.println("Fetched " + teamStatus.size() + " team work status records for team leader " + user.name);
            String json = gson.toJson(teamStatus);
            JsonObject res = new JsonObject();
            res.addProperty("team_work_status", json);
            sendMessage("team_work_status_response", res);
        } catch (Exception ex) {
            ex.printStackTrace();
            JsonObject res = new JsonObject();
            res.addProperty("error", ex.getMessage());
            sendMessage("team_work_status_response", res);
        }
    }

    private void handleGetAllWorkStatus(JsonObject payload) {
        System.out.println("=== handleGetAllWorkStatus called ===");
        if (user == null) {
            System.out.println("ERROR: User is null");
            JsonObject res = new JsonObject();
            res.addProperty("error", "Not authenticated");
            sendMessage("all_work_status_response", res);
            return;
        }
        System.out.println("User: " + user.name + " (ID: " + user.id + ", Role: " + user.role + ")");
        // Allow HR, ADMIN role, or user with ID=1
        boolean isAuthorized = "HR".equalsIgnoreCase(user.role) || "ADMIN".equalsIgnoreCase(user.role) || user.id == 1;
        System.out.println("Is authorized: " + isAuthorized);
        if (!isAuthorized) {
            System.out.println("ERROR: User not authorized");
            JsonObject res = new JsonObject();
            res.addProperty("error", "Unauthorized - Role: " + user.role + ", ID: " + user.id);
            sendMessage("all_work_status_response", res);
            return;
        }
        try {
            // Get mode from payload (default to "current" if not specified)
            String mode = "current";
            if (payload != null && payload.has("mode")) {
                mode = payload.get("mode").getAsString();
            }
            System.out.println("Calling WorkSessionService.getAllWorkStatus() with mode: " + mode);
            List<WorkSessionService.WorkStatusInfo> allStatus = WorkSessionService.getAllWorkStatus(mode);
            System.out.println("Fetched " + allStatus.size() + " work status records for user " + user.name);
            String json = gson.toJson(allStatus);
            System.out.println("JSON length: " + json.length());
            JsonObject res = new JsonObject();
            res.addProperty("all_work_status", json);
            System.out.println("Sending response...");
            sendMessage("all_work_status_response", res);
            System.out.println("Response sent successfully");
        } catch (Exception ex) {
            System.out.println("ERROR: Exception occurred: " + ex.getMessage());
            ex.printStackTrace();
            JsonObject res = new JsonObject();
            res.addProperty("error", ex.getMessage());
            sendMessage("all_work_status_response", res);
        }
    }

    private void handleForceStopWork(JsonObject payload) {
        System.out.println("=== handleForceStopWork called ===");
        if (user == null) {
            System.out.println("ERROR: User is null");
            sendMessage("force_stop_work_response", new SimpleMsg("error", "Not authenticated"));
            return;
        }
        System.out.println("User: " + user.name + " (ID: " + user.id + ", Role: " + user.role + ")");
        // Allow HR, ADMIN role, or user with ID=1
        boolean isAuthorized = "HR".equalsIgnoreCase(user.role) || "ADMIN".equalsIgnoreCase(user.role) || user.id == 1;
        System.out.println("Is authorized: " + isAuthorized);
        if (!isAuthorized) {
            System.out.println("ERROR: User not authorized");
            sendMessage("force_stop_work_response", new SimpleMsg("error", "Unauthorized - Role: " + user.role + ", ID: " + user.id));
            return;
        }
        try {
            if (!payload.has("user_id")) {
                sendMessage("force_stop_work_response", new SimpleMsg("error", "Missing user_id parameter"));
                return;
            }
            
            int targetUserId = payload.get("user_id").getAsInt();
            System.out.println("Force stopping work for user ID: " + targetUserId);
            
            WorkSessionService.endWorkSession(targetUserId);
            System.out.println("Work session ended successfully for user ID: " + targetUserId);
            
            sendMessage("force_stop_work_response", new SimpleMsg("ok", "Work stopped successfully"));
            broadcastToRole("notification", new SimpleMsg("work_forced_stop", "Admin/HR force stopped work for user " + targetUserId), "HR");
        } catch (Exception ex) {
            System.out.println("ERROR: Exception occurred: " + ex.getMessage());
            ex.printStackTrace();
            sendMessage("force_stop_work_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleEndAllActiveSessions(JsonObject payload) {
        System.out.println("=== handleEndAllActiveSessions called ===");
        if (user == null) {
            System.out.println("ERROR: User is null");
            sendMessage("end_all_active_sessions_response", new SimpleMsg("error", "Not authenticated"));
            return;
        }
        System.out.println("User: " + user.name + " (ID: " + user.id + ", Role: " + user.role + ")");
        // Allow HR, ADMIN role, or user with ID=1
        boolean isAuthorized = "HR".equalsIgnoreCase(user.role) || "ADMIN".equalsIgnoreCase(user.role) || user.id == 1;
        System.out.println("Is authorized: " + isAuthorized);
        if (!isAuthorized) {
            System.out.println("ERROR: User not authorized");
            sendMessage("end_all_active_sessions_response", new SimpleMsg("error", "Unauthorized - Role: " + user.role + ", ID: " + user.id));
            return;
        }
        try {
            System.out.println("Ending all active work sessions...");
            WorkSessionService.endAllActiveSessions();
            System.out.println("All active sessions ended successfully");
            
            sendMessage("end_all_active_sessions_response", new SimpleMsg("ok", "All active sessions ended successfully"));
            broadcastToRole("notification", new SimpleMsg("all_sessions_ended", "Admin/HR ended all active work sessions"), "HR");
        } catch (Exception ex) {
            System.out.println("ERROR: Exception occurred: " + ex.getMessage());
            ex.printStackTrace();
            sendMessage("end_all_active_sessions_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleUpdateUserPassword(JsonObject payload) {
        if (user == null) {
            sendMessage("update_user_password_response", new SimpleMsg("error", "Not authenticated"));
            return;
        }
        boolean isAuthorized = user.id == 1 || "ADMIN".equalsIgnoreCase(user.role);
        if (!isAuthorized) {
            sendMessage("update_user_password_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            int userId = payload.has("userId") ? payload.get("userId").getAsInt() : -1;
            String newPassword = payload.has("newPassword") ? payload.get("newPassword").getAsString() : "";
            if (newPassword.isEmpty()) {
                sendMessage("update_user_password_response", new SimpleMsg("error", "Password cannot be empty"));
                return;
            }
            boolean success = AdminService.updateUserPassword(userId, newPassword);
            if (success) {
                sendMessage("update_user_password_response", new SimpleMsg("ok", "Password updated successfully"));
            } else {
                sendMessage("update_user_password_response", new SimpleMsg("error", "Failed to update password"));
            }
        } catch (Exception ex) {
            sendMessage("update_user_password_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleUpdateUserInfo(JsonObject payload) {
        if (user == null) {
            sendMessage("update_user_info_response", new SimpleMsg("error", "Not authenticated"));
            return;
        }
        boolean isAuthorized = user.id == 1 || "ADMIN".equalsIgnoreCase(user.role) || "HR".equalsIgnoreCase(user.role);
        if (!isAuthorized) {
            sendMessage("update_user_info_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            int userId = payload.has("userId") ? payload.get("userId").getAsInt() : -1;
            String newName = payload.has("newName") ? payload.get("newName").getAsString() : "";
            String newRole = payload.has("newRole") ? payload.get("newRole").getAsString() : "";
            String newPhone = payload.has("newPhone") ? payload.get("newPhone").getAsString() : "";
            String newAddress = payload.has("newAddress") ? payload.get("newAddress").getAsString() : "";
            String newJob = payload.has("newJob") ? payload.get("newJob").getAsString() : null;
            Integer teamLeaderId = payload.has("teamLeaderId") ? payload.get("teamLeaderId").getAsInt() : null;
            
            if (newName.isEmpty()) {
                sendMessage("update_user_info_response", new SimpleMsg("error", "Name cannot be empty"));
                return;
            }
            boolean success = AdminService.updateUserInfo(userId, newName, newRole, newPhone, newAddress, newJob, teamLeaderId);
            if (success) {
                sendMessage("update_user_info_response", new SimpleMsg("ok", "User info updated successfully"));
            } else {
                sendMessage("update_user_info_response", new SimpleMsg("error", "Failed to update user info"));
            }
        } catch (Exception ex) {
            sendMessage("update_user_info_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleDeleteUser(JsonObject payload) {
        if (user == null) {
            sendMessage("delete_user_response", new SimpleMsg("error", "Not authenticated"));
            return;
        }
        boolean isAuthorized = user.id == 1 || "ADMIN".equalsIgnoreCase(user.role) || "HR".equalsIgnoreCase(user.role);
        if (!isAuthorized) {
            sendMessage("delete_user_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            int userId = payload.has("userId") ? payload.get("userId").getAsInt() : -1;
            if (userId <= 0) {
                sendMessage("delete_user_response", new SimpleMsg("error", "Invalid user ID"));
                return;
            }
            // Prevent deleting admin user
            if (userId == 1) {
                sendMessage("delete_user_response", new SimpleMsg("error", "Cannot delete admin user"));
                return;
            }
            boolean success = AdminService.deleteUser(userId);
            if (success) {
                sendMessage("delete_user_response", new SimpleMsg("ok", "User deleted successfully"));
            } else {
                sendMessage("delete_user_response", new SimpleMsg("error", "Failed to delete user"));
            }
        } catch (Exception ex) {
            sendMessage("delete_user_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleCreateUser(JsonObject payload) {
        if (user == null) {
            sendMessage("create_user_response", new SimpleMsg("error", "Not authenticated"));
            return;
        }
        boolean isAuthorized = user.id == 1 || "ADMIN".equalsIgnoreCase(user.role) || "HR".equalsIgnoreCase(user.role);
        if (!isAuthorized) {
            sendMessage("create_user_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            String name = payload.has("name") ? payload.get("name").getAsString() : "";
            String role = payload.has("role") ? payload.get("role").getAsString() : "WORKER";
            String password = payload.has("password") ? payload.get("password").getAsString() : "";
            String phone = payload.has("phone") ? payload.get("phone").getAsString() : "";
            String address = payload.has("address") ? payload.get("address").getAsString() : "";
            if (name.isEmpty() || password.isEmpty()) {
                sendMessage("create_user_response", new SimpleMsg("error", "Name and password are required"));
                return;
            }
            int newId = AdminService.createUser(name, role, password, phone, address);
            if (newId > 0) {
                JsonObject res = new JsonObject();
                res.addProperty("ok", true);
                res.addProperty("userId", newId);
                sendMessage("create_user_response", res);
            } else {
                sendMessage("create_user_response", new SimpleMsg("error", "Failed to create user"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            
            // Provide more user-friendly error messages
            String errorMsg = ex.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("duplicate key") || errorMsg.contains("already exists")) {
                    errorMsg = "Un utilizator cu acest ID sau nume există deja în baza de date";
                } else if (errorMsg.contains("violates not-null constraint")) {
                    errorMsg = "Lipsesc date obligatorii pentru crearea utilizatorului";
                } else if (errorMsg.contains("violates foreign key constraint")) {
                    errorMsg = "Referință invalidă către altă tabelă";
                }
            } else {
                errorMsg = "Eroare necunoscută la crearea utilizatorului";
            }
            
            sendMessage("create_user_response", new SimpleMsg("error", errorMsg));
        }
    }

    private void handleSetHourlyRate(JsonObject payload) {
        if (user == null || !("HR".equalsIgnoreCase(user.role) || "ADMIN".equalsIgnoreCase(user.role))) {
            sendMessage("set_hourly_rate_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            int userId = payload.has("user_id") ? payload.get("user_id").getAsInt() : -1;
            double rate = payload.has("hourly_rate") ? payload.get("hourly_rate").getAsDouble() : 0;
            
            if (userId <= 0 || rate < 0) {
                sendMessage("set_hourly_rate_response", new SimpleMsg("error", "Invalid user_id or hourly_rate"));
                return;
            }
            
            boolean success = SalaryService.setHourlyRate(userId, java.math.BigDecimal.valueOf(rate), user.id);
            if (success) {
                sendMessage("set_hourly_rate_response", new SimpleMsg("ok", "Hourly rate updated successfully"));
            } else {
                sendMessage("set_hourly_rate_response", new SimpleMsg("error", "Failed to update hourly rate"));
            }
        } catch (Exception ex) {
            sendMessage("set_hourly_rate_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleGetHourlyRates(JsonObject payload) {
        if (user == null || !("HR".equalsIgnoreCase(user.role) || "ADMIN".equalsIgnoreCase(user.role))) {
            sendMessage("get_hourly_rates_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            List<SalaryService.UserHourlyRate> rates = SalaryService.getAllUsersWithHourlyRates();
            String json = gson.toJson(rates);
            JsonObject res = new JsonObject();
            res.addProperty("hourly_rates", json);
            sendMessage("get_hourly_rates_response", res);
        } catch (Exception ex) {
            sendMessage("get_hourly_rates_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleCalculateSalaries(JsonObject payload) {
        if (user == null || !("HR".equalsIgnoreCase(user.role) || "ADMIN".equalsIgnoreCase(user.role))) {
            sendMessage("calculate_salaries_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            int count = SalaryService.calculateSalariesForPreviousMonth(user.id);
            JsonObject res = new JsonObject();
            res.addProperty("status", "ok");
            res.addProperty("count", count);
            res.addProperty("message", "Calculated salaries for " + count + " employees");
            sendMessage("calculate_salaries_response", res);
        } catch (Exception ex) {
            sendMessage("calculate_salaries_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleGetSalaryCalculations(JsonObject payload) {
        if (user == null || !("HR".equalsIgnoreCase(user.role) || "ADMIN".equalsIgnoreCase(user.role))) {
            sendMessage("get_salary_calculations_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            int month = payload.has("month") ? payload.get("month").getAsInt() : -1;
            int year = payload.has("year") ? payload.get("year").getAsInt() : -1;
            
            if (month < 1 || month > 12 || year < 2000) {
                sendMessage("get_salary_calculations_response", new SimpleMsg("error", "Invalid month or year"));
                return;
            }
            
            List<SalaryService.SalaryCalculation> calculations = SalaryService.getSalaryCalculationsForMonth(month, year);
            String json = gson.toJson(calculations);
            JsonObject res = new JsonObject();
            res.addProperty("salary_calculations", json);
            sendMessage("get_salary_calculations_response", res);
        } catch (Exception ex) {
            sendMessage("get_salary_calculations_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleAddSalaryBonus(JsonObject payload) {
        if (user == null || !("HR".equalsIgnoreCase(user.role) || "ADMIN".equalsIgnoreCase(user.role))) {
            sendMessage("add_salary_bonus_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            int salaryCalculationId = payload.has("salary_calculation_id") ? payload.get("salary_calculation_id").getAsInt() : -1;
            double amount = payload.has("amount") ? payload.get("amount").getAsDouble() : 0;
            String description = payload.has("description") ? payload.get("description").getAsString() : "";
            
            if (salaryCalculationId <= 0 || amount < 0 || description.isEmpty()) {
                sendMessage("add_salary_bonus_response", new SimpleMsg("error", "Invalid parameters"));
                return;
            }
            
            SalaryService.addBonus(salaryCalculationId, java.math.BigDecimal.valueOf(amount), description, user.id);
            sendMessage("add_salary_bonus_response", new SimpleMsg("ok", "Bonus added successfully"));
        } catch (Exception ex) {
            sendMessage("add_salary_bonus_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handlePublishSalaries(JsonObject payload) {
        if (user == null || !("HR".equalsIgnoreCase(user.role) || "ADMIN".equalsIgnoreCase(user.role))) {
            sendMessage("publish_salaries_response", new SimpleMsg("error", "Unauthorized"));
            return;
        }
        try {
            int month = payload.has("month") ? payload.get("month").getAsInt() : -1;
            int year = payload.has("year") ? payload.get("year").getAsInt() : -1;
            
            if (month < 1 || month > 12 || year < 2000) {
                sendMessage("publish_salaries_response", new SimpleMsg("error", "Invalid month or year"));
                return;
            }
            
            int count = SalaryService.publishSalaries(month, year, user.id);
            JsonObject res = new JsonObject();
            res.addProperty("status", "ok");
            res.addProperty("count", count);
            res.addProperty("message", "Published " + count + " salaries");
            sendMessage("publish_salaries_response", res);
            
            // Broadcast notification to all workers
            broadcastToRole("notification", new SimpleMsg("salary_published", "Salaries for " + month + "/" + year + " have been published"), "WORKER");
            broadcastToRole("notification", new SimpleMsg("salary_published", "Salaries for " + month + "/" + year + " have been published"), "TEAMLEADER");
        } catch (Exception ex) {
            sendMessage("publish_salaries_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleGetMySalaryHistory(JsonObject payload) {
        if (user == null) {
            sendMessage("get_my_salary_history_response", new SimpleMsg("error", "Not authenticated"));
            return;
        }
        try {
            List<SalaryService.SalaryCalculation> history = SalaryService.getSalaryHistoryForUser(user.id);
            String json = gson.toJson(history);
            JsonObject res = new JsonObject();
            res.addProperty("salary_history", json);
            sendMessage("get_my_salary_history_response", res);
        } catch (Exception ex) {
            sendMessage("get_my_salary_history_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleGetHourlyRateHistory(JsonObject payload) {
        if (user == null) {
            sendMessage("get_hourly_rate_history_response", new SimpleMsg("error", "Not authenticated"));
            return;
        }
        try {
            int userId = user.id;
            // HR can view history for any user
            if (("HR".equalsIgnoreCase(user.role) || "ADMIN".equalsIgnoreCase(user.role)) && payload.has("user_id")) {
                userId = payload.get("user_id").getAsInt();
            }
            
            List<SalaryService.HourlyRateHistory> history = SalaryService.getHourlyRateHistory(userId);
            String json = gson.toJson(history);
            JsonObject res = new JsonObject();
            res.addProperty("hourly_rate_history", json);
            sendMessage("get_hourly_rate_history_response", res);
        } catch (Exception ex) {
            sendMessage("get_hourly_rate_history_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleImportUsers(JsonObject payload) {
        if (user == null || !"ADMIN".equalsIgnoreCase(user.role)) {
            sendMessage("import_users_response", new SimpleMsg("error", "Not authenticated or not admin"));
            return;
        }
        try {
            JsonArray usersArray = payload.getAsJsonArray("users");
            int imported = 0;
            
            for (int i = 0; i < usersArray.size(); i++) {
                try {
                    JsonObject userObj = usersArray.get(i).getAsJsonObject();
                    
                    String name = userObj.get("name").getAsString();
                    String job = userObj.get("job").getAsString();
                    String password = userObj.get("password").getAsString();
                    boolean isWorker = userObj.get("is_worker").getAsBoolean();
                    
                    String phone = userObj.has("phone") ? userObj.get("phone").getAsString() : null;
                    String address = userObj.has("address") ? userObj.get("address").getAsString() : null;
                    Integer teamLeaderId = (isWorker && userObj.has("team_leader_id")) 
                        ? userObj.get("team_leader_id").getAsInt() 
                        : null;
                    
                    // Create user with AdminService
                    AdminService.createUser(name, password, job, phone, address, isWorker, teamLeaderId);
                    imported++;
                } catch (Exception ex) {
                    System.err.println("Error importing user at index " + i + ": " + ex.getMessage());
                    // Continue with next user
                }
            }
            
            JsonObject response = new JsonObject();
            response.addProperty("status", "ok");
            response.addProperty("imported", imported);
            sendMessage("import_users_response", response);
            
        } catch (Exception ex) {
            sendMessage("import_users_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleImportWorkSessions(JsonObject payload) {
        if (user == null || !"ADMIN".equalsIgnoreCase(user.role)) {
            sendMessage("import_work_sessions_response", new SimpleMsg("error", "Not authenticated or not admin"));
            return;
        }
        try {
            JsonArray sessionsArray = payload.getAsJsonArray("sessions");
            int imported = 0;
            
            for (int i = 0; i < sessionsArray.size(); i++) {
                try {
                    JsonObject sessionObj = sessionsArray.get(i).getAsJsonObject();
                    
                    int userId = sessionObj.get("user_id").getAsInt();
                    String startTime = sessionObj.get("start_time").getAsString();
                    String endTime = sessionObj.has("end_time") ? sessionObj.get("end_time").getAsString() : null;
                    
                    // Insert work session
                    AdminService.importWorkSession(userId, startTime, endTime);
                    imported++;
                } catch (Exception ex) {
                    System.err.println("Error importing work session at index " + i + ": " + ex.getMessage());
                    // Continue with next session
                }
            }
            
            JsonObject response = new JsonObject();
            response.addProperty("status", "ok");
            response.addProperty("imported", imported);
            sendMessage("import_work_sessions_response", response);
            
        } catch (Exception ex) {
            sendMessage("import_work_sessions_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleGetMonthlyStatistics(JsonObject payload) {
        if (user == null || !"ADMIN".equalsIgnoreCase(user.role)) {
            sendMessage("monthly_statistics_response", new SimpleMsg("error", "Not authenticated or not admin"));
            return;
        }
        try {
            int month = payload.get("month").getAsInt();
            int year = payload.get("year").getAsInt();
            
            JsonArray statistics = StatisticsService.getMonthlyStatistics(month, year);
            
            JsonObject response = new JsonObject();
            response.add("statistics", statistics);
            sendMessage("monthly_statistics_response", response);
            
        } catch (Exception ex) {
            System.err.println("Error getting monthly statistics: " + ex.getMessage());
            sendMessage("monthly_statistics_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleGetJobs(JsonObject payload) {
        try {
            List<JobTitle> jobs = JobService.getAllJobs();
            
            JsonArray jobsArray = new JsonArray();
            for (JobTitle job : jobs) {
                JsonObject jo = new JsonObject();
                jo.addProperty("id", job.id);
                jo.addProperty("title", job.title);
                if (job.description != null) {
                    jo.addProperty("description", job.description);
                }
                if (job.createdAt != null) {
                    jo.addProperty("created_at", job.createdAt.toString());
                }
                jobsArray.add(jo);
            }
            
            JsonObject response = new JsonObject();
            response.add("jobs", jobsArray);
            sendMessage("get_jobs_response", response);
            
        } catch (Exception ex) {
            sendMessage("get_jobs_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleAddJob(JsonObject payload) {
        if (user == null || !"ADMIN".equals(user.role)) {
            sendMessage("add_job_response", new SimpleMsg("error", "Acces interzis"));
            return;
        }
        
        try {
            String title = payload.get("title").getAsString();
            String description = payload.has("description") ? payload.get("description").getAsString() : null;
            
            int jobId = JobService.createJob(title, description);
            
            JsonObject response = new JsonObject();
            response.addProperty("status", "ok");
            response.addProperty("message", "Job adăugat cu succes");
            response.addProperty("id", jobId);
            sendMessage("add_job_response", response);
            
        } catch (Exception ex) {
            sendMessage("add_job_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleUpdateJob(JsonObject payload) {
        if (user == null || !"ADMIN".equals(user.role)) {
            sendMessage("update_job_response", new SimpleMsg("error", "Acces interzis"));
            return;
        }
        
        try {
            int jobId = payload.get("id").getAsInt();
            String title = payload.get("title").getAsString();
            String description = payload.has("description") ? payload.get("description").getAsString() : null;
            
            boolean success = JobService.updateJob(jobId, title, description);
            
            if (success) {
                JsonObject response = new JsonObject();
                response.addProperty("status", "ok");
                response.addProperty("message", "Job actualizat cu succes");
                sendMessage("update_job_response", response);
            } else {
                sendMessage("update_job_response", new SimpleMsg("error", "Job-ul nu a fost găsit"));
            }
            
        } catch (Exception ex) {
            sendMessage("update_job_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    private void handleDeleteJob(JsonObject payload) {
        if (user == null || !"ADMIN".equals(user.role)) {
            sendMessage("delete_job_response", new SimpleMsg("error", "Acces interzis"));
            return;
        }
        
        try {
            int jobId = payload.get("id").getAsInt();
            
            boolean success = JobService.deleteJob(jobId);
            
            if (success) {
                JsonObject response = new JsonObject();
                response.addProperty("status", "ok");
                response.addProperty("message", "Job șters cu succes");
                sendMessage("delete_job_response", response);
            } else {
                sendMessage("delete_job_response", new SimpleMsg("error", "Job-ul nu a fost găsit"));
            }
            
        } catch (Exception ex) {
            sendMessage("delete_job_response", new SimpleMsg("error", ex.getMessage()));
        }
    }

    // helper classes for payloads
    static class SimpleMsg {
        String status;
        String message;
        public SimpleMsg(String status, String message) { this.status = status; this.message = message; }
    }
    static class LoginResponse {
        boolean ok; String message; User user;
        public LoginResponse(boolean ok, String message, User user) { this.ok = ok; this.message = message; this.user = user; }
    }
}
