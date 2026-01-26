using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

public class ServerListPanel : MonoBehaviour
{
    [Header("UI Elements")]
    public Transform serverListContainer;
    public GameObject serverItemPrefab;
    public Button backButton;
    public Button refreshButton;

    private List<GameObject> serverItems = new List<GameObject>();

    private void Start()
    {
        // Subscribe to button clicks
        if (backButton != null)
        {
            backButton.onClick.AddListener(OnBackClicked);
        }

        if (refreshButton != null)
        {
            refreshButton.onClick.AddListener(OnRefreshClicked);
        }

        // Subscribe to lobby events
        if (LobbyManager.Instance != null)
        {
            LobbyManager.Instance.OnServerListReceived += OnServerListReceived;
        }

        // Subscribe to game events
        if (GameManager.Instance != null)
        {
            GameManager.Instance.OnRoomJoined += OnRoomJoined;
        }
    }

    private void OnDestroy()
    {
        if (LobbyManager.Instance != null)
        {
            LobbyManager.Instance.OnServerListReceived -= OnServerListReceived;
        }

        if (GameManager.Instance != null)
        {
            GameManager.Instance.OnRoomJoined -= OnRoomJoined;
        }
    }

    private void OnEnable()
    {
        // Request server list when panel is shown
        if (LobbyManager.Instance != null)
        {
            LobbyManager.Instance.RequestServerList();
        }
    }

    private void OnBackClicked()
    {
        UIManager.Instance.ShowMainMenuPanel();
    }

    private void OnRefreshClicked()
    {
        if (LobbyManager.Instance != null)
        {
            LobbyManager.Instance.RequestServerList();
            UIManager.Instance.ShowNotification("Refreshing server list...");
        }
    }

    private void OnServerListReceived(List<ServerInfo> servers)
    {
        // Clear existing items
        foreach (var item in serverItems)
        {
            Destroy(item);
        }
        serverItems.Clear();

        // Create new items
        if (servers.Count == 0)
        {
            UIManager.Instance.ShowNotification("No available servers. Try Quick Match!");
            return;
        }

        foreach (var server in servers)
        {
            CreateServerItem(server);
        }
    }

    private void CreateServerItem(ServerInfo server)
    {
        if (serverItemPrefab == null || serverListContainer == null)
        {
            Debug.LogWarning("Server item prefab or container not assigned!");
            return;
        }

        GameObject item = Instantiate(serverItemPrefab, serverListContainer);
        serverItems.Add(item);

        Debug.Log($"[ServerListPanel] Created server item for room: {server.roomId}");

        // Prepare display text
        string displayName = !string.IsNullOrEmpty(server.serverName) ? server.serverName : server.roomId;
        string passwordIndicator = server.hasPassword ? " [LOCKED]" : "";
        string fullDisplayName = displayName + passwordIndicator;

        // Try to find and set text components (supports both Text and TextMeshPro)
        SetText(item, "RoomIdText", fullDisplayName);
        SetText(item, "ServerNameText", fullDisplayName);
        SetText(item, "PlayersText", $"{server.playerCount}/{server.maxPlayers}");
        SetText(item, "MinBetText", $"${server.minBet:F2}");

        // If no specific text components found, try to set all text components in order
        TMP_Text[] tmpTexts = item.GetComponentsInChildren<TMP_Text>();
        if (tmpTexts.Length >= 3)
        {
            tmpTexts[0].text = fullDisplayName;
            tmpTexts[1].text = $"{server.playerCount}/{server.maxPlayers}";
            tmpTexts[2].text = $"${server.minBet:F2}";
            Debug.Log($"[ServerListPanel] Set texts using TMP_Text array: {tmpTexts.Length} texts found");
        }

        Text[] texts = item.GetComponentsInChildren<Text>();
        if (texts.Length >= 3)
        {
            texts[0].text = fullDisplayName;
            texts[1].text = $"{server.playerCount}/{server.maxPlayers}";
            texts[2].text = $"${server.minBet:F2}";
            Debug.Log($"[ServerListPanel] Set texts using Text array: {texts.Length} texts found");
        }

        // Setup join button
        Button joinButton = item.GetComponentInChildren<Button>();
        if (joinButton != null)
        {
            joinButton.onClick.AddListener(() => OnJoinServerClicked(server.roomId));
            joinButton.interactable = server.IsAvailable();
            Debug.Log($"[ServerListPanel] Join button configured for room: {server.roomId}");
        }
    }

    private void SetText(GameObject parent, string childName, string value)
    {
        Transform child = parent.transform.Find(childName);
        if (child != null)
        {
            TMP_Text tmpText = child.GetComponent<TMP_Text>();
            if (tmpText != null)
            {
                tmpText.text = value;
                Debug.Log($"[ServerListPanel] Set TMP_Text {childName} = {value}");
                return;
            }

            Text text = child.GetComponent<Text>();
            if (text != null)
            {
                text.text = value;
                Debug.Log($"[ServerListPanel] Set Text {childName} = {value}");
                return;
            }
        }
    }

    private void OnJoinServerClicked(string roomId)
    {
        // Find the server info to check if password is required
        ServerInfo server = LobbyManager.Instance.GetAvailableServers().Find(s => s.roomId == roomId);

        if (server != null && server.hasPassword)
        {
            // TODO: Show password input dialog
            // For now, we'll use a simple prompt (you can create a proper UI dialog later)
            UIManager.Instance.ShowNotification("Password-protected room. Password feature coming soon!", true);
            // Uncomment below when password dialog is implemented:
            // ShowPasswordDialog(roomId);
        }
        else
        {
            GameManager.Instance.JoinRoom(roomId, "");
            UIManager.Instance.ShowNotification($"Joining room {roomId}...");
        }
    }

    // TODO: Implement password dialog
    // private void ShowPasswordDialog(string roomId)
    // {
    //     // Show a dialog to enter password
    //     // On submit, call: GameManager.Instance.JoinRoom(roomId, password);
    // }

    private void OnRoomJoined(RoomAssignmentData roomData)
    {
        // Navigate to game table
        UIManager.Instance.ShowGameTablePanel();
    }
}
