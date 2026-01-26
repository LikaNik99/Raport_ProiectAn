using TMPro;
using UnityEngine;
using UnityEngine.UI;

public class MainMenuPanel : MonoBehaviour
{
    [Header("UI Elements")]
    public TMP_Text usernameText;
    public TMP_Text balanceText;
    public TMP_Text pointsText;
    public Button quickMatchButton;
    public Button serverListButton;
    public Button createServerButton;
    public Button leaderboardButton;
    public Button logoutButton;

    private void Start()
    {
        // Subscribe to button clicks
        if (quickMatchButton != null)
        {
            quickMatchButton.onClick.AddListener(OnQuickMatchClicked);
        }

        if (serverListButton != null)
        {
            serverListButton.onClick.AddListener(OnServerListClicked);
        }

        if (createServerButton != null)
        {
            createServerButton.onClick.AddListener(OnCreateServerClicked);
        }

        if (leaderboardButton != null)
        {
            leaderboardButton.onClick.AddListener(OnLeaderboardClicked);
        }

        if (logoutButton != null)
        {
            logoutButton.onClick.AddListener(OnLogoutClicked);
        }

        // Subscribe to game events
        if (GameManager.Instance != null)
        {
            GameManager.Instance.OnRoomJoined += OnRoomJoined;
        }

        UpdateUserInfo();
    }

    private void OnDestroy()
    {
        if (GameManager.Instance != null)
        {
            GameManager.Instance.OnRoomJoined -= OnRoomJoined;
        }
    }

    private void OnEnable()
    {
        UpdateUserInfo();
    }

    private void UpdateUserInfo()
    {
        var user = AuthManager.Instance?.GetCurrentUser();
        if (user != null)
        {
            if (usernameText != null)
            {
                usernameText.text = user.username;
            }

            if (balanceText != null)
            {
                balanceText.text = $"${user.balance:F2}";
            }

            if (pointsText != null)
            {
                pointsText.text = $"{user.points} pts";
            }
        }
    }

    private void OnQuickMatchClicked()
    {
        // Play haptic feedback on mobile
        if (MobileInputManager.Instance != null)
        {
            MobileInputManager.Instance.PlayLightHaptic();
        }

        if (quickMatchButton != null)
        {
            quickMatchButton.interactable = false;
        }

        UIManager.Instance.ShowNotification("Searching for a game...");
        GameManager.Instance.QuickMatch();
    }

    private void OnServerListClicked()
    {
        // Play haptic feedback on mobile
        if (MobileInputManager.Instance != null)
        {
            MobileInputManager.Instance.PlayLightHaptic();
        }

        UIManager.Instance.ShowServerListPanel();

        // Request server list
        if (LobbyManager.Instance != null)
        {
            LobbyManager.Instance.RequestServerList();
        }
    }

    private void OnCreateServerClicked()
    {
        // Play haptic feedback on mobile
        if (MobileInputManager.Instance != null)
        {
            MobileInputManager.Instance.PlayLightHaptic();
        }

        UIManager.Instance.ShowCreateServerPanel();
    }

    private void OnLeaderboardClicked()
    {
        // Play haptic feedback on mobile
        if (MobileInputManager.Instance != null)
        {
            MobileInputManager.Instance.PlayLightHaptic();
        }

        UIManager.Instance.ShowLeaderboardPanel();

        // Request leaderboard
        if (LobbyManager.Instance != null)
        {
            LobbyManager.Instance.RequestLeaderboard(50);
        }
    }

    private void OnLogoutClicked()
    {
        // Play haptic feedback on mobile
        if (MobileInputManager.Instance != null)
        {
            MobileInputManager.Instance.PlayLightHaptic();
        }

        AuthManager.Instance.Logout();
        WebSocketClient.Instance.Disconnect();
        UIManager.Instance.ShowLoginPanel();
        UIManager.Instance.ShowNotification("Logged out successfully");
    }

    private void OnRoomJoined(RoomAssignmentData roomData)
    {
        // Re-enable button
        if (quickMatchButton != null)
        {
            quickMatchButton.interactable = true;
        }

        // Navigate to game table
        UIManager.Instance.ShowGameTablePanel();
        UIManager.Instance.ShowNotification($"Joined room {roomData.roomId}");
    }
}
