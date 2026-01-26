using System;
using System.Collections.Generic;
using UnityEngine;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

public class GameManager : MonoBehaviour
{
    public static GameManager Instance { get; private set; }

    private GameStateData currentGameState;
    private string currentRoomId;
    private bool inGame = false;

    // Events
    public event Action<GameStateData> OnGameStateUpdated;
    public event Action<RoomAssignmentData> OnRoomJoined;
    public event Action<GameResultData> OnGameResultReceived;
    public event Action<CardData, int> OnCardDealt;
    public event Action<ChatMessage> OnChatMessageReceived;
    public event Action<string> OnError;

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
    /// Request a quick match
    /// </summary>
    public void QuickMatch()
    {
        if (!WebSocketClient.Instance.IsConnected())
        {
            OnError?.Invoke("Not connected to server");
            return;
        }

        var user = AuthManager.Instance.GetCurrentUser();
        if (user == null)
        {
            OnError?.Invoke("Not logged in");
            return;
        }

        var data = new { userId = user.userId };
        WebSocketClient.Instance.SendMessage("QUICK_MATCH", data);
        Debug.Log("Requesting quick match...");
    }

    /// <summary>
    /// Join a specific room
    /// </summary>
    public void JoinRoom(string roomId, string password = "")
    {
        if (!WebSocketClient.Instance.IsConnected())
        {
            OnError?.Invoke("Not connected to server");
            return;
        }

        var user = AuthManager.Instance.GetCurrentUser();
        if (user == null)
        {
            OnError?.Invoke("Not logged in");
            return;
        }

        var data = new
        {
            userId = user.userId,
            roomId = roomId,
            password = password
        };

        WebSocketClient.Instance.SendMessage("JOIN_ROOM", data);
        Debug.Log($"Joining room {roomId}...");
    }

    /// <summary>
    /// Place a bet
    /// </summary>
    public void PlaceBet(float amount)
    {
        if (!inGame || string.IsNullOrEmpty(currentRoomId))
        {
            OnError?.Invoke("Not in a game");
            return;
        }

        var user = AuthManager.Instance.GetCurrentUser();
        if (user == null)
        {
            OnError?.Invoke("Not logged in");
            return;
        }

        if (amount > user.balance)
        {
            OnError?.Invoke("Insufficient balance");
            return;
        }

        var data = new
        {
            userId = user.userId,
            roomId = currentRoomId,
            amount = amount
        };

        WebSocketClient.Instance.SendMessage("PLACE_BET", data);
        Debug.Log($"Placing bet: ${amount}");
    }

    /// <summary>
    /// Request a hit (draw a card)
    /// </summary>
    public void Hit()
    {
        if (!inGame || string.IsNullOrEmpty(currentRoomId))
        {
            OnError?.Invoke("Not in a game");
            return;
        }

        var user = AuthManager.Instance.GetCurrentUser();
        if (user == null)
        {
            OnError?.Invoke("Not logged in");
            return;
        }

        var data = new
        {
            userId = user.userId,
            roomId = currentRoomId
        };

        WebSocketClient.Instance.SendMessage("HIT", data);
        Debug.Log("Requesting hit...");
    }

    /// <summary>
    /// Stand (end turn)
    /// </summary>
    public void Stand()
    {
        if (!inGame || string.IsNullOrEmpty(currentRoomId))
        {
            OnError?.Invoke("Not in a game");
            return;
        }

        var user = AuthManager.Instance.GetCurrentUser();
        if (user == null)
        {
            OnError?.Invoke("Not logged in");
            return;
        }

        var data = new
        {
            userId = user.userId,
            roomId = currentRoomId
        };

        WebSocketClient.Instance.SendMessage("STAND", data);
        Debug.Log("Standing...");
    }

    /// <summary>
    /// Split hand (when player has two cards of same rank)
    /// </summary>
    public void Split()
    {
        if (!inGame || string.IsNullOrEmpty(currentRoomId))
        {
            OnError?.Invoke("Not in a game");
            return;
        }

        var user = AuthManager.Instance.GetCurrentUser();
        if (user == null)
        {
            OnError?.Invoke("Not logged in");
            return;
        }

        var data = new
        {
            userId = user.userId,
            roomId = currentRoomId
        };

        WebSocketClient.Instance.SendMessage("SPLIT", data);
        Debug.Log("Splitting hand...");
    }

    /// <summary>
    /// Double down (double bet and receive one more card)
    /// </summary>
    public void DoubleDown()
    {
        if (!inGame || string.IsNullOrEmpty(currentRoomId))
        {
            OnError?.Invoke("Not in a game");
            return;
        }

        var user = AuthManager.Instance.GetCurrentUser();
        if (user == null)
        {
            OnError?.Invoke("Not logged in");
            return;
        }

        var data = new
        {
            userId = user.userId,
            roomId = currentRoomId
        };

        WebSocketClient.Instance.SendMessage("DOUBLE_DOWN", data);
        Debug.Log("Doubling down...");
    }

    /// <summary>
    /// Buy insurance (when dealer shows Ace)
    /// </summary>
    public void BuyInsurance()
    {
        if (!inGame || string.IsNullOrEmpty(currentRoomId))
        {
            OnError?.Invoke("Not in a game");
            return;
        }

        var user = AuthManager.Instance.GetCurrentUser();
        if (user == null)
        {
            OnError?.Invoke("Not logged in");
            return;
        }

        var data = new
        {
            userId = user.userId,
            roomId = currentRoomId
        };

        WebSocketClient.Instance.SendMessage("INSURANCE", data);
        Debug.Log("Buying insurance...");
    }

    /// <summary>
    /// Send a chat message
    /// </summary>
    public void SendChatMessage(string message)
    {
        if (!inGame || string.IsNullOrEmpty(currentRoomId))
        {
            OnError?.Invoke("Not in a game");
            return;
        }

        var user = AuthManager.Instance.GetCurrentUser();
        if (user == null)
        {
            OnError?.Invoke("Not logged in");
            return;
        }

        var data = new
        {
            userId = user.userId,
            roomId = currentRoomId,
            message = message
        };

        WebSocketClient.Instance.SendMessage("CHAT_MESSAGE", data);
        Debug.Log($"Sending chat message: {message}");
    }

    /// <summary>
    /// Leave current room
    /// </summary>
    public void LeaveRoom()
    {
        if (!inGame || string.IsNullOrEmpty(currentRoomId))
        {
            return;
        }

        var user = AuthManager.Instance.GetCurrentUser();
        if (user == null)
        {
            return;
        }

        var data = new { userId = user.userId };
        WebSocketClient.Instance.SendMessage("LEAVE_ROOM", data);

        currentRoomId = null;
        currentGameState = null;
        inGame = false;

        Debug.Log("Left room");
    }

    /// <summary>
    /// Signal ready for next round
    /// </summary>
    public void PlayAgain()
    {
        if (!inGame || string.IsNullOrEmpty(currentRoomId))
        {
            OnError?.Invoke("Not in a game");
            return;
        }

        var user = AuthManager.Instance.GetCurrentUser();
        if (user == null)
        {
            OnError?.Invoke("Not logged in");
            return;
        }

        var data = new
        {
            userId = user.userId,
            roomId = currentRoomId
        };

        WebSocketClient.Instance.SendMessage("PLAY_AGAIN", data);
        Debug.Log("Ready for next round");
    }

    /// <summary>
    /// Get current game state
    /// </summary>
    public GameStateData GetCurrentGameState()
    {
        return currentGameState;
    }

    /// <summary>
    /// Check if player is currently in a game
    /// </summary>
    public bool IsInGame()
    {
        return inGame;
    }

    /// <summary>
    /// Get current room ID
    /// </summary>
    public string GetCurrentRoomId()
    {
        return currentRoomId;
    }

    private void HandleMessage(string type, JObject json)
    {
        switch (type)
        {
            case "ROOM_ASSIGNED":
                HandleRoomAssigned(json);
                break;

            case "GAME_STATE":
                HandleGameState(json);
                break;

            case "CARD_DEALT":
                HandleCardDealt(json);
                break;

            case "GAME_RESULT":
                HandleGameResult(json);
                break;

            case "CHAT_MESSAGE":
                HandleChatMessage(json);
                break;

            case "ERROR":
                HandleErrorMessage(json);
                break;
        }
    }

    private void HandleRoomAssigned(JObject json)
    {
        var data = json["data"];
        if (data != null)
        {
            var roomData = data.ToObject<RoomAssignmentData>();
            currentRoomId = roomData.roomId;
            inGame = true;
            Debug.Log($"Joined room: {roomData.roomId}");
            OnRoomJoined?.Invoke(roomData);
        }
    }

    private void HandleGameState(JObject json)
    {
        var data = json["data"];
        if (data != null)
        {
            currentGameState = data.ToObject<GameStateData>();
            Debug.Log($"Game state updated - Phase: {currentGameState.phase}");
            OnGameStateUpdated?.Invoke(currentGameState);

            // Update user balance if player data is available
            var user = AuthManager.Instance.GetCurrentUser();
            if (user != null)
            {
                var playerData = currentGameState.GetPlayerById(user.userId);
                if (playerData != null)
                {
                    user.balance = playerData.balance;
                }
            }
        }
    }

    private void HandleCardDealt(JObject json)
    {
        var data = json["data"];
        if (data != null)
        {
            int userId = data["userId"]?.ToObject<int>() ?? 0;
            var card = data["card"]?.ToObject<CardData>();

            if (card != null)
            {
                Debug.Log($"Card dealt to player {userId}: {card}");
                OnCardDealt?.Invoke(card, userId);
            }
        }
    }

    private void HandleGameResult(JObject json)
    {
        var data = json["data"];
        if (data != null)
        {
            var resultData = data.ToObject<GameResultData>();
            Debug.Log("Game finished - results received");
            OnGameResultReceived?.Invoke(resultData);

            // Update user balance from results
            var user = AuthManager.Instance.GetCurrentUser();
            if (user != null && resultData.playerResults != null)
            {
                foreach (var playerResult in resultData.playerResults)
                {
                    if (playerResult.userId == user.userId)
                    {
                        user.balance = playerResult.balance;
                        break;
                    }
                }
            }
        }
    }

    private void HandleChatMessage(JObject json)
    {
        var data = json["data"];
        if (data != null)
        {
            var chatData = data["chat"];
            if (chatData != null)
            {
                var chatMessage = chatData.ToObject<ChatMessage>();
                Debug.Log($"Chat message from {chatMessage.username}: {chatMessage.message}");
                OnChatMessageReceived?.Invoke(chatMessage);
            }
        }
    }

    private void HandleErrorMessage(JObject json)
    {
        string message = json["message"]?.ToString();
        string code = json["code"]?.ToString();

        Debug.LogWarning($"Error: {code} - {message}");
        OnError?.Invoke(message ?? "An error occurred");
    }
}
