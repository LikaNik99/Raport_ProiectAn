using System;
using System.Collections.Generic;

[Serializable]
public class DealerData
{
    public List<CardData> cards = new List<CardData>();
    public int handValue;
    public int visibleValue;

    public CardData GetVisibleCard()
    {
        foreach (var card in cards)
        {
            if (card.faceUp)
                return card;
        }
        return null;
    }
}

[Serializable]
public class GameStateData
{
    public string roomId;
    public string phase; // WAITING, BETTING, DEALING, PLAYER_TURNS, DEALER_TURN, RESULTS, FINISHED
    public List<PlayerData> players = new List<PlayerData>();
    public DealerData dealer = new DealerData();
    public float pot;
    public int maxPlayers;
    public float minBet;
    public int currentTurn; // userId of current player

    public PlayerData GetPlayerById(int userId)
    {
        return players.Find(p => p.userId == userId);
    }

    public bool IsMyTurn(int userId)
    {
        return currentTurn == userId && phase == "PLAYER_TURNS";
    }

    public bool IsBettingPhase()
    {
        return phase == "BETTING";
    }

    public bool IsPlayerTurns()
    {
        return phase == "PLAYER_TURNS";
    }

    public bool IsFinished()
    {
        return phase == "FINISHED" || phase == "RESULTS";
    }
}
