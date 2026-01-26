using System;

[Serializable]
public class ServerInfo
{
    public string roomId;
    public int playerCount;
    public int maxPlayers;
    public float minBet;
    public string status;
    public string serverName;  // Custom server name
    public bool hasPassword;   // Whether the server is password-protected

    public bool IsFull()
    {
        return playerCount >= maxPlayers;
    }

    public bool IsAvailable()
    {
        return status == "waiting" && !IsFull();
    }

    public override string ToString()
    {
        string passwordIndicator = hasPassword ? " [LOCKED]" : "";
        string displayName = !string.IsNullOrEmpty(serverName) ? serverName : roomId;
        return $"{displayName}{passwordIndicator} - Players: {playerCount}/{maxPlayers}, Min Bet: ${minBet:F2}";
    }
}

[Serializable]
public class ServerListData
{
    public ServerInfo[] servers;
}
