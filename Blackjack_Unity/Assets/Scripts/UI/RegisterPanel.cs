using TMPro;
using UnityEngine;
using UnityEngine.UI;

public class RegisterPanel : MonoBehaviour
{
    [Header("UI Elements")]
    public TMP_InputField usernameInput;
    public TMP_InputField passwordInput;
    public TMP_InputField confirmPasswordInput;
    public Button registerButton;
    public Button goToLoginButton;
    public TMP_Text errorText;

    private void Start()
    {
        // Subscribe to button clicks
        if (registerButton != null)
        {
            registerButton.onClick.AddListener(OnRegisterClicked);
        }

        if (goToLoginButton != null)
        {
            goToLoginButton.onClick.AddListener(OnGoToLoginClicked);
        }

        // Subscribe to auth events
        if (AuthManager.Instance != null)
        {
            AuthManager.Instance.OnRegisterSuccess += OnRegisterSuccess;
            AuthManager.Instance.OnRegisterFailed += OnRegisterFailed;
        }

        // Clear error text
        if (errorText != null)
        {
            errorText.text = "";
        }
    }

    private void OnDestroy()
    {
        if (AuthManager.Instance != null)
        {
            AuthManager.Instance.OnRegisterSuccess -= OnRegisterSuccess;
            AuthManager.Instance.OnRegisterFailed -= OnRegisterFailed;
        }
    }

    private void OnRegisterClicked()
    {
        string username = usernameInput.text.Trim();
        string password = passwordInput.text;
        string confirmPassword = confirmPasswordInput.text;

        // Clear previous error
        if (errorText != null)
        {
            errorText.text = "";
        }

        // Validate inputs
        if (string.IsNullOrEmpty(username) || string.IsNullOrEmpty(password) || string.IsNullOrEmpty(confirmPassword))
        {
            ShowError("Please fill in all fields");
            return;
        }

        // Disable button during registration
        if (registerButton != null)
        {
            registerButton.interactable = false;
        }

        // Attempt registration
        AuthManager.Instance.Register(username, password, confirmPassword);
    }

    private void OnGoToLoginClicked()
    {
        UIManager.Instance.ShowLoginPanel();
    }

    private void OnRegisterSuccess(UserData user)
    {
        Debug.Log($"Registration successful for {user.username}");

        // Re-enable button
        if (registerButton != null)
        {
            registerButton.interactable = true;
        }

        // Clear inputs
        if (usernameInput != null) usernameInput.text = "";
        if (passwordInput != null) passwordInput.text = "";
        if (confirmPasswordInput != null) confirmPasswordInput.text = "";

        // Navigate to main menu
        UIManager.Instance.ShowMainMenuPanel();
        UIManager.Instance.ShowNotification($"Welcome, {user.username}! You've been given $1000 to start.");
    }

    private void OnRegisterFailed(string errorMessage)
    {
        Debug.LogWarning($"Registration failed: {errorMessage}");

        // Re-enable button
        if (registerButton != null)
        {
            registerButton.interactable = true;
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
