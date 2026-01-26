using System;
using System.Collections.Generic;
using UnityEngine;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

// NOTE: This class requires a WebSocket library like NativeWebSocket or WebSocket-Sharp
// You'll need to uncomment and adapt the code below based on your chosen library


using NativeWebSocket; // For NativeWebSocket library

// using WebSocketSharp; // For WebSocket-Sharp library


public class WebSocketClient : MonoBehaviour
{
    public static WebSocketClient Instance { get; private set; }

    [Header("Server Configuration")]
    public string serverUrl = "ws://192.168.100.4:8080/blackjack";


    // WebSocket connection (uncomment based on library)
    private WebSocket websocket; // For NativeWebSocket
    // private WebSocketSharp.WebSocket websocket; // For WebSocket-Sharp

    private Queue<string> messageQueue = new Queue<string>();
    private bool isConnected = false;

    // Events
    public event Action OnConnected;
    public event Action OnDisconnected;
    public event Action<string, JObject> OnMessageReceived;
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

    /// <summary>
    /// Connect to the WebSocket server
    /// </summary>
    public async void Connect()
    {
        if (isConnected)
        {
            Debug.LogWarning("Already connected to server");
            return;
        }

        try
        {
            Debug.Log($"Connecting to {serverUrl}...");

              // FOR NATIVEWEBSOCKET:
             websocket = new WebSocket(serverUrl);

             websocket.OnOpen += () =>
             {
                 Debug.Log("WebSocket Connected!");
                 isConnected = true;
                 OnConnected?.Invoke();
             };

             websocket.OnMessage += (bytes) =>
             {
                 string message = System.Text.Encoding.UTF8.GetString(bytes);
                 lock (messageQueue)
                 {
                     messageQueue.Enqueue(message);
                 }
             };

             websocket.OnError += (error) =>
             {
                 Debug.LogError($"WebSocket Error: {error}");
                 OnError?.Invoke(error);
             };

             websocket.OnClose += (code) =>
             {
                 Debug.Log($"WebSocket Closed: {code}");
                 isConnected = false;
                 OnDisconnected?.Invoke();
             };

             await websocket.Connect();

            /* FOR WEBSOCKET-SHARP:
            websocket = new WebSocketSharp.WebSocket(serverUrl);

            websocket.OnOpen += (sender, e) =>
            {
                Debug.Log("WebSocket Connected!");
                isConnected = true;
                OnConnected?.Invoke();
            };

            websocket.OnMessage += (sender, e) =>
            {
                lock (messageQueue)
                {
                    messageQueue.Enqueue(e.Data);
                }
            };

            websocket.OnError += (sender, e) =>
            {
                Debug.LogError($"WebSocket Error: {e.Message}");
                OnError?.Invoke(e.Message);
            };

            websocket.OnClose += (sender, e) =>
            {
                Debug.Log($"WebSocket Closed: {e.Code} - {e.Reason}");
                isConnected = false;
                OnDisconnected?.Invoke();
            };

            websocket.Connect();
            */

        }
        catch (Exception e)
        {
            Debug.LogError($"Connection failed: {e.Message}");
            OnError?.Invoke(e.Message);
        }
    }

    /// <summary>
    /// Disconnect from the server
    /// </summary>
    public async void Disconnect()
    {
        if (!isConnected) return;

        try
        {
             // FOR NATIVEWEBSOCKET:
            await websocket.Close();

            /* FOR WEBSOCKET-SHARP:
            websocket.Close();
            */

            isConnected = false;
        }
        catch (Exception e)
        {
            Debug.LogError($"Error disconnecting: {e.Message}");
        }
    }

    /// <summary>
    /// Send a message to the server
    /// </summary>
    public void SendMessage(string type, object data = null)
    {
        if (!isConnected)
        {
            Debug.LogWarning("Cannot send message - not connected");
            return;
        }

        try
        {
            var message = new
            {
                type = type,
                data = data ?? new { }
            };

            string json = JsonConvert.SerializeObject(message);
            Debug.Log($"Sending: {json}");

             // FOR NATIVEWEBSOCKET:
            websocket.SendText(json);

            /* FOR WEBSOCKET-SHARP:
            websocket.Send(json);
            */
        }
        catch (Exception e)
        {
            Debug.LogError($"Error sending message: {e.Message}");
        }
    }

    private void Update()
    {
        // Process message queue on main thread
        lock (messageQueue)
        {
            while (messageQueue.Count > 0)
            {
                string message = messageQueue.Dequeue();
                ProcessMessage(message);
            }
        }

         // FOR NATIVEWEBSOCKET: Dispatch events
        #if !UNITY_WEBGL || UNITY_EDITOR
        if (websocket != null)
        {
            websocket.DispatchMessageQueue();
        }
        #endif
    }

    private void ProcessMessage(string message)
    {
        try
        {
            Debug.Log($"Received: {message}");

            JObject json = JObject.Parse(message);
            string type = json["type"]?.ToString();

            if (string.IsNullOrEmpty(type))
            {
                Debug.LogWarning("Received message without type");
                return;
            }

            OnMessageReceived?.Invoke(type, json);
        }
        catch (Exception e)
        {
            Debug.LogError($"Error processing message: {e.Message}");
        }
    }

    public bool IsConnected()
    {
        return isConnected;
    }

    private void OnApplicationQuit()
    {
        Disconnect();
    }

    private void OnDestroy()
    {
        Disconnect();
    }
}
