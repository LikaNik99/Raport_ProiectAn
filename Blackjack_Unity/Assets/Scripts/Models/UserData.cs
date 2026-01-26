using System;

[Serializable]
public class UserData
{
    public int userId;
    public string username;
    public float balance;
    public int points;
    public int wins;
    public int totalGames;

    public UserData()
    {
    }

    public UserData(int userId, string username, float balance, int points)
    {
        this.userId = userId;
        this.username = username;
        this.balance = balance;
        this.points = points;
    }

    public float GetWinRate()
    {
        if (totalGames == 0) return 0f;
        return ((float)wins / totalGames) * 100f;
    }

    public override string ToString()
    {
        return $"User {username} - Balance: ${balance:F2}, Points: {points}";
    }
}
