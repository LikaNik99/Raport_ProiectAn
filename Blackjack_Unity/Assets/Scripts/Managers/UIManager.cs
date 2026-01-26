using System;
using TMPro;
using UnityEngine;
using UnityEngine.UI;

public class UIManager : MonoBehaviour
{
    public static UIManager Instance { get; private set; }

    [Header("Panels")]
    public GameObject loginPanel;
    public GameObject registerPanel;
    public GameObject mainMenuPanel;
    public GameObject serverListPanel;
    public GameObject createServerPanel;
    public GameObject leaderboardPanel;
    public GameObject gameTablePanel;

    [Header("Notification")]
    public GameObject notificationPanel;
    public TMP_Text notificationText;
    public float notificationDuration = 3f;

    private GameObject currentPanel;

    private void Awake()
    {
        if (Instance == null)
        {
            Instance = this;
        }
        else
        {
            Destroy(gameObject);
        }
    }

    private void Start()
    {
        // Start with login panel
        ShowLoginPanel();
    }

    /// <summary>
    /// Show the login panel
    /// </summary>
    public void ShowLoginPanel()
    {
        HideAllPanels();
        if (loginPanel != null)
        {
            loginPanel.SetActive(true);
            currentPanel = loginPanel;
        }
    }

    /// <summary>
    /// Show the register panel
    /// </summary>
    public void ShowRegisterPanel()
    {
        HideAllPanels();
        if (registerPanel != null)
        {
            registerPanel.SetActive(true);
            currentPanel = registerPanel;
        }
    }

    /// <summary>
    /// Show the main menu panel
    /// </summary>
    public void ShowMainMenuPanel()
    {
        HideAllPanels();
        if (mainMenuPanel != null)
        {
            mainMenuPanel.SetActive(true);
            currentPanel = mainMenuPanel;
        }
    }

    /// <summary>
    /// Show the server list panel
    /// </summary>
    public void ShowServerListPanel()
    {
        HideAllPanels();
        if (serverListPanel != null)
        {
            serverListPanel.SetActive(true);
            currentPanel = serverListPanel;
        }
    }

    /// <summary>
    /// Show the create server panel
    /// </summary>
    public void ShowCreateServerPanel()
    {
        HideAllPanels();
        if (createServerPanel != null)
        {
            createServerPanel.SetActive(true);
            currentPanel = createServerPanel;
        }
    }

    /// <summary>
    /// Show the leaderboard panel
    /// </summary>
    public void ShowLeaderboardPanel()
    {
        HideAllPanels();
        if (leaderboardPanel != null)
        {
            leaderboardPanel.SetActive(true);
            currentPanel = leaderboardPanel;
        }
    }

    /// <summary>
    /// Show the game table panel
    /// </summary>
    public void ShowGameTablePanel()
    {
        Debug.Log("[UIManager] ShowGameTablePanel called");
        HideAllPanels();
        if (gameTablePanel != null)
        {
            gameTablePanel.SetActive(true);
            currentPanel = gameTablePanel;
            Debug.Log("[UIManager] GameTablePanel activated");
        }
        else
        {
            Debug.LogError("[UIManager] gameTablePanel is NULL!");
        }
    }

    /// <summary>
    /// Hide all panels
    /// </summary>
    private void HideAllPanels()
    {
        if (loginPanel != null) loginPanel.SetActive(false);
        if (registerPanel != null) registerPanel.SetActive(false);
        if (mainMenuPanel != null) mainMenuPanel.SetActive(false);
        if (serverListPanel != null) serverListPanel.SetActive(false);
        if (createServerPanel != null) createServerPanel.SetActive(false);
        if (leaderboardPanel != null) leaderboardPanel.SetActive(false);
        if (gameTablePanel != null) gameTablePanel.SetActive(false);
    }

    /// <summary>
    /// Show a notification message
    /// </summary>
    public void ShowNotification(string message, bool isError = false)
    {
        if (notificationPanel != null && notificationText != null)
        {
            notificationText.text = message;
            notificationPanel.SetActive(true);

            // Change color based on error/success
            if (isError)
            {
                notificationText.color = Color.red;
            }
            else
            {
                notificationText.color = Color.white;
            }

            // Auto-hide after duration
            CancelInvoke(nameof(HideNotification));
            Invoke(nameof(HideNotification), notificationDuration);
        }

        // Also log to console
        if (isError)
        {
            Debug.LogWarning(message);
        }
        else
        {
            Debug.Log(message);
        }
    }

    /// <summary>
    /// Hide the notification
    /// </summary>
    private void HideNotification()
    {
        if (notificationPanel != null)
        {
            notificationPanel.SetActive(false);
        }
    }

    /// <summary>
    /// Get the currently active panel
    /// </summary>
    public GameObject GetCurrentPanel()
    {
        return currentPanel;
    }
}
