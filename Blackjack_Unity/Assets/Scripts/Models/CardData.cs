using System;

[Serializable]
public class CardData
{
    public string suit;  // hearts, diamonds, clubs, spades
    public string rank;  // A, 2-10, J, Q, K
    public bool faceUp = true;

    public CardData()
    {
    }

    public CardData(string suit, string rank, bool faceUp = true)
    {
        this.suit = suit;
        this.rank = rank;
        this.faceUp = faceUp;
    }

    public int GetValue()
    {
        switch (rank)
        {
            case "A":
                return 11; // Aces handled specially in hand calculation
            case "J":
            case "Q":
            case "K":
                return 10;
            default:
                int value;
                if (int.TryParse(rank, out value))
                {
                    return value;
                }
                return 0;
        }
    }

    public string GetSpriteName()
    {
        if (!faceUp)
        {
            return "card_back";
        }
        return $"{suit}_{rank}".ToLower();
    }

    public override string ToString()
    {
        return $"{rank} of {suit}";
    }
}
