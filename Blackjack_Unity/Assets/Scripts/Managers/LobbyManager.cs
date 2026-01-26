using System;
using System.Collections.Generic;
using UnityEngine;
using Newtonsoft.Json.Linq;

public class LobbyManager : MonoBehaviour
{
    public static LobbyManager Instance { get; private set; }

    private List<ServerInfo> availableServers = new List<ServerInfo>();
    private List<LeaderboardEntry> leaderboardEntries = new List<LeaderboardEntry>();

    // Events
    public event Action<List<ServerInfo>> OnServerListReceived;
    public event Action<List<LeaderboardEntry>> OnLeaderboardReceived;
    public event Action<string> OnRoomCreated; // roomId

    private void Awake()
    {
        if (Instance == null)
        {
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }
        else
        {
            Destroy(gameObject);
        }
    }

    private void Start()
    {
        if (WebSocketClient.Instance != null)
        {
            WebSocketClient.Instance.OnMessageReceived += HandleMessage;
        }
    }

    private void OnDestroy()
    {
        if (WebSocketClient.Instance != null)
        {
            WebSocketClient.Instance.OnMessageReceived -= HandleMessage;
        }
    }

    /// <summary>
    /// Request the list of available servers
    /// </summary>
    public void RequestServerList()
    {
        if (!WebSocketClient.Instance.IsConnected())
        {
            Debug.LogWarning("Not connected to server");
            return;
        }

        WebSocketClient.Instance.SendMessage("GET_SERVERS");
        Debug.Log("Requesting server list...");
    }

    /// <summary>
    /// Request the leaderboard
    /// </summary>
    public void RequestLeaderboard(int limit = 10)
    {
        if (!WebSocketClient.Instance.IsConnected())
        {
            Debug.LogWarning("Not connected to server");
            return;
        }

        var data = new { limit = limit };
        WebSocketClient.Instance.SendMessage("GET_LEADERBOARD", data);
        Debug.Log($"Requesting leaderboard (top {limit})...");
    }

    /// <summary>
    /// Create a new room with custom name and optional password
    /// </summary>
    public void CreateRoom(string serverName, string password = "")
    {
        if (!WebSocketClient.Instance.IsConnected())
        {
            Debug.LogWarning("Not connected to server");
            return;
        }

        var user = AuthManager.Instance?.GetCurrentUser();
        if (user == null)
        {
            Debug.LogWarning("Not logged in");
            return;
        }

        var data = new
        {
            userId = user.userId,
            serverName = serverName,
            password = password
        };

        WebSocketClient.Instance.SendMessage("CREATE_ROOM", data);
        Debug.Log($"Creating room: {serverName}");
    }

    /// <summary>
    /// Get the cached list of available servers
    /// </summary>
    public List<ServerInfo> GetAvailableServers()
    {
        return new List<ServerInfo>(availableServers);
    }

    /// <summary>
    /// Get the cached leaderboard entries
    /// </summary>
    public List<LeaderboardEntry> GetLeaderboard()
    {
        return new List<LeaderboardEntry>(leaderboardEntries);
    }

    private void HandleMessage(string type, JObject json)
    {
        switch (type)
        {
            case "SERVER_LIST":
                HandleServerList(json);
                break;

            case "LEADERBOARD":
                HandleLeaderboard(json);
                break;

            case "CREATE_ROOM_RESPONSE":
                HandleCreateRoomResponse(json);
                break;
        }
    }

    private void HandleServerList(JObject json)
    {
        var data = json["data"];
        if (data != null)
        {
            var serverListData = data.ToObject<ServerListData>();
            if (serverListData != null && serverListData.servers != null)
            {
                availableServers.Clear();
                availableServers.AddRange(serverListData.servers);

                Debug.Log($"Received {availableServers.Count} available servers");
                OnServerListReceived?.Invoke(availableServers);
            }
        }
    }

    private void HandleLeaderboard(JObject json)
    {
        var data = json["data"];
        if (data != null)
        {
            var leaderboardData = data.ToObject<LeaderboardData>();
            if (leaderboardData != null && leaderboardData.leaders != null)
            {
                leaderboardEntries.Clear();
                leaderboardEntries.AddRange(leaderboardData.leaders);

                Debug.Log($"Received leaderboard with {leaderboardEntries.Count} entries");
                OnLeaderboardReceived?.Invoke(leaderboardEntries);
            }
        }
    }

    private void HandleCreateRoomResponse(JObject json)
    {
        bool success = json["success"]?.ToObject<bool>() ?? false;
        string message = json["message"]?.ToString();

        if (success)
        {
            var data = json["data"];
            if (data != null)
            {
                var roomData = data.ToObject<CreateRoomResponseData>();
                if (roomData != null)
                {
                    Debug.Log($"Room created successfully: {roomData.roomId}");
                    OnRoomCreated?.Invoke(roomData.roomId);
                }
            }
        }
        else
        {
            Debug.LogWarning($"Failed to create room: {message}");
            UIManager.Instance?.ShowNotification(message ?? "Failed to create room", true);
        }
    }
}
