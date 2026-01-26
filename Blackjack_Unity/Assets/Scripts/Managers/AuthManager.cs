using System;
using System.Collections;
using UnityEngine;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

public class AuthManager : MonoBehaviour
{
    public static AuthManager Instance { get; private set; }

    private UserData currentUser;
    private bool isAuthenticated = false;

    // Events
    public event Action<UserData> OnLoginSuccess;
    public event Action<string> OnLoginFailed;
    public event Action<UserData> OnRegisterSuccess;
    public event Action<string> OnRegisterFailed;
    public event Action OnLogout;

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
        // Subscribe to WebSocket messages
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
    /// Attempt to login with username and password
    /// </summary>
    public void Login(string username, string password)
    {
        if (string.IsNullOrEmpty(username) || string.IsNullOrEmpty(password))
        {
            OnLoginFailed?.Invoke("Username and password are required");
            return;
        }

        // Auto-reconnect if not connected
        if (!WebSocketClient.Instance.IsConnected())
        {
            Debug.Log("Not connected, reconnecting...");
            StartCoroutine(ConnectAndLogin(username, password));
            return;
        }

        var loginData = new
        {
            username = username,
            password = password
        };

        WebSocketClient.Instance.SendMessage("LOGIN", loginData);
        Debug.Log($"Attempting to login as {username}");
    }

    /// <summary>
    /// Attempt to register a new account
    /// </summary>
    public void Register(string username, string password, string confirmPassword)
    {
        if (string.IsNullOrEmpty(username) || string.IsNullOrEmpty(password))
        {
            OnRegisterFailed?.Invoke("Username and password are required");
            return;
        }

        if (password != confirmPassword)
        {
            OnRegisterFailed?.Invoke("Passwords do not match");
            return;
        }

        if (username.Length < 3 || username.Length > 20)
        {
            OnRegisterFailed?.Invoke("Username must be 3-20 characters");
            return;
        }

        if (password.Length < 6)
        {
            OnRegisterFailed?.Invoke("Password must be at least 6 characters");
            return;
        }

        // Auto-reconnect if not connected
        if (!WebSocketClient.Instance.IsConnected())
        {
            Debug.Log("Not connected, reconnecting...");
            StartCoroutine(ConnectAndRegister(username, password));
            return;
        }

        var registerData = new
        {
            username = username,
            password = password
        };

        WebSocketClient.Instance.SendMessage("REGISTER", registerData);
        Debug.Log($"Attempting to register as {username}");
    }

    /// <summary>
    /// Logout current user
    /// </summary>
    public void Logout()
    {
        currentUser = null;
        isAuthenticated = false;

        // Disconnect and reconnect WebSocket to ensure fresh session for next login
        if (WebSocketClient.Instance != null)
        {
            StartCoroutine(ReconnectAfterLogout());
        }

        OnLogout?.Invoke();
        Debug.Log("Logged out");
    }

    /// <summary>
    /// Get the current logged in user
    /// </summary>
    public UserData GetCurrentUser()
    {
        return currentUser;
    }

    /// <summary>
    /// Check if user is authenticated
    /// </summary>
    public bool IsAuthenticated()
    {
        return isAuthenticated;
    }

    /// <summary>
    /// Update current user data (e.g., after balance changes)
    /// </summary>
    public void UpdateUserData(UserData userData)
    {
        if (currentUser != null && userData.userId == currentUser.userId)
        {
            currentUser = userData;
        }
    }

    private void HandleMessage(string type, JObject json)
    {
        switch (type)
        {
            case "LOGIN_RESPONSE":
                HandleLoginResponse(json);
                break;

            case "REGISTER_RESPONSE":
                HandleRegisterResponse(json);
                break;

            case "ERROR":
                HandleError(json);
                break;
        }
    }

    private void HandleLoginResponse(JObject json)
    {
        bool success = json["success"]?.ToObject<bool>() ?? false;
        string message = json["message"]?.ToString();

        if (success)
        {
            var data = json["data"];
            if (data != null)
            {
                currentUser = data.ToObject<UserData>();
                isAuthenticated = true;
                Debug.Log($"Login successful: {currentUser.username}");
                OnLoginSuccess?.Invoke(currentUser);
            }
            else
            {
                OnLoginFailed?.Invoke("Invalid response from server");
            }
        }
        else
        {
            Debug.LogWarning($"Login failed: {message}");
            OnLoginFailed?.Invoke(message ?? "Login failed");
        }
    }

    private void HandleRegisterResponse(JObject json)
    {
        bool success = json["success"]?.ToObject<bool>() ?? false;
        string message = json["message"]?.ToString();

        if (success)
        {
            var data = json["data"];
            if (data != null)
            {
                currentUser = data.ToObject<UserData>();
                isAuthenticated = true;
                Debug.Log($"Registration successful: {currentUser.username}");
                OnRegisterSuccess?.Invoke(currentUser);
            }
            else
            {
                OnRegisterFailed?.Invoke("Invalid response from server");
            }
        }
        else
        {
            Debug.LogWarning($"Registration failed: {message}");
            OnRegisterFailed?.Invoke(message ?? "Registration failed");
        }
    }

    private void HandleError(JObject json)
    {
        string code = json["code"]?.ToString();
        string message = json["message"]?.ToString();

        if (code == "LOGIN_FAILED")
        {
            OnLoginFailed?.Invoke(message ?? "Login failed");
        }
        else if (code == "REGISTER_FAILED")
        {
            OnRegisterFailed?.Invoke(message ?? "Registration failed");
        }
    }

    /// <summary>
    /// Reconnect WebSocket after logout
    /// </summary>
    private IEnumerator ReconnectAfterLogout()
    {
        Debug.Log("Disconnecting WebSocket after logout...");
        WebSocketClient.Instance.Disconnect();

        // Wait for disconnect to complete
        yield return new WaitForSeconds(0.5f);

        // Reconnect for next login
        Debug.Log("Reconnecting WebSocket for next login...");
        WebSocketClient.Instance.Connect();

        // Wait for connection to establish
        yield return new WaitForSeconds(1f);

        Debug.Log("WebSocket reconnected and ready for next login");
    }

    /// <summary>
    /// Connect and then attempt login
    /// </summary>
    private IEnumerator ConnectAndLogin(string username, string password)
    {
        Debug.Log("Connecting WebSocket for login...");
        WebSocketClient.Instance.Connect();

        // Wait for connection to establish
        float timeout = 5f;
        float elapsed = 0f;

        while (!WebSocketClient.Instance.IsConnected() && elapsed < timeout)
        {
            yield return new WaitForSeconds(0.1f);
            elapsed += 0.1f;
        }

        if (WebSocketClient.Instance.IsConnected())
        {
            Debug.Log("Connected! Proceeding with login...");
            Login(username, password);
        }
        else
        {
            Debug.LogError("Connection timeout");
            OnLoginFailed?.Invoke("Connection timeout - could not connect to server");
        }
    }

    /// <summary>
    /// Connect and then attempt register
    /// </summary>
    private IEnumerator ConnectAndRegister(string username, string password)
    {
        Debug.Log("Connecting WebSocket for registration...");
        WebSocketClient.Instance.Connect();

        // Wait for connection to establish
        float timeout = 5f;
        float elapsed = 0f;

        while (!WebSocketClient.Instance.IsConnected() && elapsed < timeout)
        {
            yield return new WaitForSeconds(0.1f);
            elapsed += 0.1f;
        }

        if (WebSocketClient.Instance.IsConnected())
        {
            Debug.Log("Connected! Proceeding with registration...");
            Register(username, password, password); // confirmPassword same as password since we already validated
        }
        else
        {
            Debug.LogError("Connection timeout");
            OnRegisterFailed?.Invoke("Connection timeout - could not connect to server");
        }
    }
}
