using System;
using System.Collections.Generic;

// Base message structure
[Serializable]
public class Message
{
    public string type;
    public bool success;
    public string message;
}

// Generic message with data
[Serializable]
public class Message<T>
{
    public string type;
    public bool success;
    public string message;
    public T data;
}

// Login/Register Response
[Serializable]
public class AuthResponseData
{
    public int userId;
    public string username;
    public float balance;
    public int points;
    public int wins;
    public int totalGames;
}

// Room Assignment
[Serializable]
public class RoomAssignmentData
{
    public string roomId;
    public int maxPlayers;
    public float minBet;
}

// Leaderboard Entry
[Serializable]
public class LeaderboardEntry
{
    public int rank;
    public int userId;
    public string username;
    public float balance;
    public int points;
    public int wins;
    public int totalGames;
    public float winRate;
}

[Serializable]
public class LeaderboardData
{
    public List<LeaderboardEntry> leaders;
}

// Game Result
[Serializable]
public class GameResultPlayerData
{
    public int userId;
    public string username;
    public int handValue;
    public string result;
    public float balance;
}

[Serializable]
public class GameResultData
{
    public List<CardData> dealerHand;
    public int dealerValue;
    public GameResultPlayerData[] playerResults;
}

// Create Room Response
[Serializable]
public class CreateRoomResponseData
{
    public string roomId;
    public string serverName;
    public bool hasPassword;
}
