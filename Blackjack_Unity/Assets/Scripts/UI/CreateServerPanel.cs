using TMPro;
using UnityEngine;
using UnityEngine.UI;

public class CreateServerPanel : MonoBehaviour
{
    [Header("UI Elements")]
    public TMP_InputField serverNameInput;
    public TMP_InputField passwordInput;
    public Button createButton;
    public Button backButton;
    public Toggle passwordToggle; // Optional: to show/hide password

    private void Start()
    {
        // Subscribe to button clicks
        if (createButton != null)
        {
            createButton.onClick.AddListener(OnCreateClicked);
        }

        if (backButton != null)
        {
            backButton.onClick.AddListener(OnBackClicked);
        }

        // Subscribe to lobby events
        if (LobbyManager.Instance != null)
        {
            LobbyManager.Instance.OnRoomCreated += OnRoomCreated;
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
            LobbyManager.Instance.OnRoomCreated -= OnRoomCreated;
        }

        if (GameManager.Instance != null)
        {
            GameManager.Instance.OnRoomJoined -= OnRoomJoined;
        }
    }

    private void OnEnable()
    {
        // Clear inputs when panel is shown
        if (serverNameInput != null)
        {
            serverNameInput.text = "";
        }

        if (passwordInput != null)
        {
            passwordInput.text = "";
        }
    }

    private void OnCreateClicked()
    {
        // Play haptic feedback on mobile
        if (MobileInputManager.Instance != null)
        {
            MobileInputManager.Instance.PlayLightHaptic();
        }

        // Validate inputs
        string serverName = serverNameInput != null ? serverNameInput.text.Trim() : "";
        string password = passwordInput != null ? passwordInput.text : "";

        if (string.IsNullOrEmpty(serverName))
        {
            UIManager.Instance.ShowNotification("Please enter a server name", true);
            return;
        }

        if (serverName.Length < 3)
        {
            UIManager.Instance.ShowNotification("Server name must be at least 3 characters", true);
            return;
        }

        if (serverName.Length > 30)
        {
            UIManager.Instance.ShowNotification("Server name must be less than 30 characters", true);
            return;
        }

        // Password is optional, but if provided, validate it
        if (!string.IsNullOrEmpty(password) && password.Length < 3)
        {
            UIManager.Instance.ShowNotification("Password must be at least 3 characters or leave empty", true);
            return;
        }

        // Disable button to prevent double-clicking
        if (createButton != null)
        {
            createButton.interactable = false;
        }

        // Request room creation
        UIManager.Instance.ShowNotification("Creating server...");
        LobbyManager.Instance.CreateRoom(serverName, password);
    }

    private void OnBackClicked()
    {
        // Play haptic feedback on mobile
        if (MobileInputManager.Instance != null)
        {
            MobileInputManager.Instance.PlayLightHaptic();
        }

        UIManager.Instance.ShowMainMenuPanel();
    }

    private void OnRoomCreated(string roomId)
    {
        // Re-enable button
        if (createButton != null)
        {
            createButton.interactable = true;
        }

        UIManager.Instance.ShowNotification($"Server created: {roomId}");
    }

    private void OnRoomJoined(RoomAssignmentData roomData)
    {
        // Re-enable button
        if (createButton != null)
        {
            createButton.interactable = true;
        }

        // Navigate to game table
        UIManager.Instance.ShowGameTablePanel();
        UIManager.Instance.ShowNotification($"Joined room {roomData.roomId}");
    }
}
