using TMPro;
using UnityEngine;
using UnityEngine.UI;

public class LoginPanel : MonoBehaviour
{
    [Header("UI Elements")]
    public TMP_InputField usernameInput;
    public TMP_InputField passwordInput;
    public Button loginButton;
    public Button goToRegisterButton;
    public TMP_Text errorText;

    private void Start()
    {
        // Subscribe to button clicks
        if (loginButton != null)
        {
            loginButton.onClick.AddListener(OnLoginClicked);
        }

        if (goToRegisterButton != null)
        {
            goToRegisterButton.onClick.AddListener(OnGoToRegisterClicked);
        }

        // Subscribe to auth events
        if (AuthManager.Instance != null)
        {
            AuthManager.Instance.OnLoginSuccess += OnLoginSuccess;
            AuthManager.Instance.OnLoginFailed += OnLoginFailed;
        }

        // Clear error text
        if (errorText != null)
        {
            errorText.text = "";
        }

        // Connect to server when login panel is shown
        if (WebSocketClient.Instance != null && !WebSocketClient.Instance.IsConnected())
        {
            WebSocketClient.Instance.Connect();
        }
    }

    private void OnDestroy()
    {
        if (AuthManager.Instance != null)
        {
            AuthManager.Instance.OnLoginSuccess -= OnLoginSuccess;
            AuthManager.Instance.OnLoginFailed -= OnLoginFailed;
        }
    }

    private void OnLoginClicked()
    {
        string username = usernameInput.text.Trim();
        string password = passwordInput.text;

        // Clear previous error
        if (errorText != null)
        {
            errorText.text = "";
        }

        // Validate inputs
        if (string.IsNullOrEmpty(username) || string.IsNullOrEmpty(password))
        {
            ShowError("Please enter username and password");
            return;
        }

        // Disable button during login
        if (loginButton != null)
        {
            loginButton.interactable = false;
        }

        // Attempt login
        AuthManager.Instance.Login(username, password);
    }

    private void OnGoToRegisterClicked()
    {
        UIManager.Instance.ShowRegisterPanel();
    }

    private void OnLoginSuccess(UserData user)
    {
        Debug.Log($"Login successful for {user.username}");

        // Re-enable button
        if (loginButton != null)
        {
            loginButton.interactable = true;
        }

        // Clear inputs
        if (usernameInput != null) usernameInput.text = "";
        if (passwordInput != null) passwordInput.text = "";

        // Navigate to main menu
        UIManager.Instance.ShowMainMenuPanel();
        UIManager.Instance.ShowNotification($"Welcome, {user.username}!");
    }

    private void OnLoginFailed(string errorMessage)
    {
        Debug.LogWarning($"Login failed: {errorMessage}");

        // Re-enable button
        if (loginButton != null)
        {
            loginButton.interactable = true;
        }

        // Show error
        ShowError(errorMessage);
        UIManager.Instance.ShowNotification(errorMessage, true);
    }

    private void ShowError(string message)
    {
        if (errorText != null)
        {
            errorText.text = message;
            errorText.color = Color.red;
        }
    }
}
