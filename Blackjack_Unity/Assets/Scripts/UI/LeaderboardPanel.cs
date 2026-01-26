using System.Collections.Generic;
using TMPro;
using UnityEngine;
using UnityEngine.UI;

public class LeaderboardPanel : MonoBehaviour
{
    [Header("UI Elements")]
    public Transform leaderboardContainer;
    public GameObject leaderboardItemPrefab;
    public Button backButton;
    public Button refreshButton;

    private List<GameObject> leaderboardItems = new List<GameObject>();

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
            LobbyManager.Instance.OnLeaderboardReceived += OnLeaderboardReceived;
        }
    }

    private void OnDestroy()
    {
        if (LobbyManager.Instance != null)
        {
            LobbyManager.Instance.OnLeaderboardReceived -= OnLeaderboardReceived;
        }
    }

    private void OnEnable()
    {
        // Request leaderboard when panel is shown
        if (LobbyManager.Instance != null)
        {
            LobbyManager.Instance.RequestLeaderboard(50);
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
            LobbyManager.Instance.RequestLeaderboard(50);
            UIManager.Instance.ShowNotification("Refreshing leaderboard...");
        }
    }

    private void OnLeaderboardReceived(List<LeaderboardEntry> entries)
    {
        // Clear existing items
        foreach (var item in leaderboardItems)
        {
            Destroy(item);
        }
        leaderboardItems.Clear();

        // Create new items
        if (entries.Count == 0)
        {
            UIManager.Instance.ShowNotification("Leaderboard is empty");
            return;
        }

        foreach (var entry in entries)
        {
            CreateLeaderboardItem(entry);
        }
    }

    private void CreateLeaderboardItem(LeaderboardEntry entry)
    {
        if (leaderboardItemPrefab == null || leaderboardContainer == null)
        {
            Debug.LogWarning("Leaderboard item prefab or container not assigned!");
            return;
        }

        GameObject item = Instantiate(leaderboardItemPrefab, leaderboardContainer);
        leaderboardItems.Add(item);

        // Set up the item (customize based on your prefab)
        TMP_Text rankText = item.transform.Find("RankText")?.GetComponent<TMP_Text>();
        if (rankText != null)
        {
            rankText.text = $"#{entry.rank}";
        }

        TMP_Text usernameText = item.transform.Find("UsernameText")?.GetComponent<TMP_Text>();
        if (usernameText != null)
        {
            usernameText.text = entry.username;

            // Highlight current user
            var currentUser = AuthManager.Instance?.GetCurrentUser();
            if (currentUser != null && entry.username == currentUser.username)
            {
                usernameText.color = Color.yellow;
                // usernameText.fontStyle = FontStyle.Bold;
            }
        }

        TMP_Text pointsText = item.transform.Find("PointsText")?.GetComponent<TMP_Text>();
        if (pointsText != null)
        {
            pointsText.text = $"{entry.points} pts";
        }

        TMP_Text balanceText = item.transform.Find("BalanceText")?.GetComponent<TMP_Text>();
        if (balanceText != null)
        {
            balanceText.text = $"${entry.balance:F0}";
        }

        TMP_Text winRateText = item.transform.Find("WinRateText")?.GetComponent<TMP_Text>();
        if (winRateText != null)
        {
            winRateText.text = $"{entry.winRate:F1}%";
        }

        TMP_Text totalGamesText = item.transform.Find("TotalGamesText")?.GetComponent<TMP_Text>();
        if (totalGamesText != null)
        {
            totalGamesText.text = $"{entry.totalGames} games";
        }
    }
}
