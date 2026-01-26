using System;
using System.Collections.Generic;

[Serializable]
public class PlayerData
{
    public int userId;
    public string username;
    public float balance;
    public float bet;
    public List<CardData> hand = new List<CardData>();
    public int handValue;
    public string status; // waiting, betting, playing, standing, busted, blackjack, won, lost, push

    // Advanced features
    public List<CardData> splitHand = new List<CardData>();
    public int splitHandValue;
    public int activeHandIndex;
    public float insuranceBet;
    public bool hasDoubledDown;
    public bool hasSplit;

    public PlayerData()
    {
    }

    public bool IsActive()
    {
        return status == "playing" || status == "betting";
    }

    public bool HasFinished()
    {
        return status == "standing" || status == "busted" || status == "blackjack";
    }

    public bool IsBusted()
    {
        return status == "busted" || handValue > 21;
    }

    public bool HasBlackjack()
    {
        return status == "blackjack" || (hand.Count == 2 && handValue == 21);
    }

    public bool CanSplit()
    {
        // Can split if: have exactly 2 cards, both same rank, haven't already split
        return !hasSplit && hand.Count == 2 && hand[0].rank == hand[1].rank;
    }

    public bool CanDoubleDown()
    {
        // Can double down if: have exactly 2 cards, haven't doubled, haven't split
        return !hasDoubledDown && !hasSplit && hand.Count == 2;
    }

    public bool CanBuyInsurance()
    {
        // Can buy insurance if: have exactly 2 cards, haven't bought insurance yet
        return insuranceBet == 0 && hand.Count == 2;
    }

    public override string ToString()
    {
        return $"Player {username} - Bet: ${bet:F2}, Hand Value: {handValue}, Status: {status}";
    }
}
