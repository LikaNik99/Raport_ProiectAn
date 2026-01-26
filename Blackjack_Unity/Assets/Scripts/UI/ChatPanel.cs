using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;
using System;

/// <summary>
/// Chat panel for player communication in game rooms
/// </summary>
public class ChatPanel : MonoBehaviour
{
    [Header("UI Elements")]
    [Tooltip("Input field for typing messages")]
    public TMP_InputField messageInputField;

    [Tooltip("Button to send messages")]
    public Button sendButton;

    [Tooltip("Scroll view content for chat messages")]
    public Transform chatContent;

    [Tooltip("Prefab for chat message item")]
    public GameObject chatMessagePrefab;

    [Tooltip("Toggle button to show/hide chat")]
    public Button toggleButton;

    [Tooltip("Panel that contains the chat")]
    public GameObject chatContainer;

    [Header("Settings")]
    [Tooltip("Maximum number of messages to display")]
    public int maxMessages = 50;

    [Tooltip("Fade out old messages")]
    public bool fadeOldMessages = true;

    [Tooltip("Max message length")]
    public int maxMessageLength = 200;

    private List<GameObject> chatMessages = new List<GameObject>();
    private bool isVisible = true;

    private void Start()
    {
        // Subscribe to send button
        if (sendButton != null)
        {
            sendButton.onClick.AddListener(OnSendClicked);
        }

        // Subscribe to input field submit
        if (messageInputField != null)
        {
            messageInputField.onSubmit.AddListener(OnInputSubmit);
            messageInputField.characterLimit = maxMessageLength;
        }

        // Subscribe to toggle button
        if (toggleButton != null)
        {
            toggleButton.onClick.AddListener(OnToggleClicked);
        }

        // Subscribe to GameManager for chat messages
        if (GameManager.Instance != null)
        {
            GameManager.Instance.OnChatMessageReceived += OnChatMessageReceived;
        }

        // Start with chat visible
        SetChatVisibility(false);
    }

    private void OnDestroy()
    {
        if (GameManager.Instance != null)
        {
            GameManager.Instance.OnChatMessageReceived -= OnChatMessageReceived;
        }
    }

    /// <summary>
    /// Send button clicked
    /// </summary>
    private void OnSendClicked()
    {
        SendMessage();

        // Play haptic feedback
        if (MobileInputManager.Instance != null)
        {
            MobileInputManager.Instance.PlayLightHaptic();
        }
    }

    /// <summary>
    /// Input field submitted (Enter key)
    /// </summary>
    private void OnInputSubmit(string text)
    {
        SendMessage();
        messageInputField.ActivateInputField(); // Keep focus
    }

    /// <summary>
    /// Toggle chat visibility
    /// </summary>
    private void OnToggleClicked()
    {
        isVisible = !isVisible;
        SetChatVisibility(isVisible);

        // Play haptic feedback
        if (MobileInputManager.Instance != null)
        {
            MobileInputManager.Instance.PlayLightHaptic();
        }
    }

    /// <summary>
    /// Set chat panel visibility
    /// </summary>
    private void SetChatVisibility(bool visible)
    {
        if (chatContainer != null)
        {
            chatContainer.SetActive(visible);
        }

        // Update toggle button text/icon if needed
        if (toggleButton != null)
        {
            TMP_Text buttonText = toggleButton.GetComponentInChildren<TMP_Text>();
            if (buttonText != null)
            {
                buttonText.text = visible ? "Hide Chat" : "Show Chat";
            }
        }
    }

    /// <summary>
    /// Send chat message to server
    /// </summary>
    private void SendMessage()
    {
        if (messageInputField == null || string.IsNullOrWhiteSpace(messageInputField.text))
        {
            return;
        }

        string message = messageInputField.text.Trim();

        // Send message to server via GameManager
        if (GameManager.Instance != null)
        {
            GameManager.Instance.SendChatMessage(message);
        }

        // Clear input field
        messageInputField.text = "";
        // messageInputField.ActivateInputField();
    }

    /// <summary>
    /// Received chat message from server
    /// </summary>
    private void OnChatMessageReceived(ChatMessage chatMessage)
    {
        AddChatMessage(chatMessage);
    }

    /// <summary>
    /// Add a chat message to the panel
    /// </summary>
    public void AddChatMessage(ChatMessage chatMessage)
    {
        Debug.Log($"[ChatPanel] AddChatMessage called: {chatMessage?.username} - {chatMessage?.message}");

        if (chatMessagePrefab == null)
        {
            Debug.LogError("[ChatPanel] chatMessagePrefab is NULL! Assign it in inspector.");
            return;
        }

        if (chatContent == null)
        {
            Debug.LogError("[ChatPanel] chatContent is NULL! Assign it in inspector.");
            return;
        }

        // Create message item
        Debug.Log($"[ChatPanel] Instantiating chat message prefab...");
        GameObject messageObj = Instantiate(chatMessagePrefab, chatContent);
        chatMessages.Add(messageObj);
        Debug.Log($"[ChatPanel] Chat message instantiated. Total messages: {chatMessages.Count}");

        // Set message text
        TMP_Text messageText = messageObj.GetComponentInChildren<TMP_Text>();
        if (messageText != null)
        {
            // Check if this is current user's message
            var currentUser = AuthManager.Instance?.GetCurrentUser();
            bool isOwnMessage = currentUser != null && currentUser.userId == chatMessage.userId;

            // Format message
            string formattedMessage;
            if (isOwnMessage)
            {
                formattedMessage = $"<color=#00FF00>You:</color> {chatMessage.message}";
            }
            else
            {
                formattedMessage = $"<color=#FFFF00>{chatMessage.username}:</color> {chatMessage.message}";
            }

            messageText.text = formattedMessage;
        }

        // Limit number of messages
        if (chatMessages.Count > maxMessages)
        {
            GameObject oldMessage = chatMessages[0];
            chatMessages.RemoveAt(0);
            Destroy(oldMessage);
        }

        // Scroll to bottom
        Canvas.ForceUpdateCanvases();
        ScrollRect scrollRect = chatContent.GetComponentInParent<ScrollRect>();
        if (scrollRect != null)
        {
            scrollRect.verticalNormalizedPosition = 0f;
        }
    }

    /// <summary>
    /// Add a system message (server notification)
    /// </summary>
    public void AddSystemMessage(string message, Color color)
    {
        if (chatMessagePrefab == null || chatContent == null)
        {
            return;
        }

        GameObject messageObj = Instantiate(chatMessagePrefab, chatContent);
        chatMessages.Add(messageObj);

        TMP_Text messageText = messageObj.GetComponentInChildren<TMP_Text>();
        if (messageText != null)
        {
            messageText.text = $"<color=#{ColorUtility.ToHtmlStringRGB(color)}>[System]</color> {message}";
        }

        // Limit number of messages
        if (chatMessages.Count > maxMessages)
        {
            GameObject oldMessage = chatMessages[0];
            chatMessages.RemoveAt(0);
            Destroy(oldMessage);
        }

        // Scroll to bottom
        Canvas.ForceUpdateCanvases();
        ScrollRect scrollRect = chatContent.GetComponentInParent<ScrollRect>();
        if (scrollRect != null)
        {
            scrollRect.verticalNormalizedPosition = 0f;
        }
    }

    /// <summary>
    /// Clear all chat messages
    /// </summary>
    public void ClearChat()
    {
        foreach (var message in chatMessages)
        {
            if (message != null)
            {
                Destroy(message);
            }
        }
        chatMessages.Clear();
    }
}

/// <summary>
/// Chat message data structure
/// </summary>
[Serializable]
public class ChatMessage
{
    public int userId;
    public string username;
    public string message;
    public long timestamp;
}
