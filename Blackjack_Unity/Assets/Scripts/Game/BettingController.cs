using UnityEngine;

/// <summary>
/// Controls betting logic & validation (fără să modifice UI-ul).
/// UI-ul este controlat de GameTablePanel.
/// </summary>
public class BettingController : MonoBehaviour
{
    [Header("Bet Settings")]
    [Tooltip("Bet minim permis la masă")]
    public float minBet = 10f;

    [Tooltip("Bet maxim permis la masă")]
    public float maxBet = 1000f;

    private float currentBalance = 0f;

    /// <summary>
    /// Initialize betting with user balance and table limits.
    /// Apelează asta când intră jucătorul la masă / începe runda.
    /// </summary>
    public void Initialize(float userBalance, float tableMinBet, float tableMaxBet)
    {
        currentBalance = userBalance;
        minBet = tableMinBet;
        maxBet = tableMaxBet;

        Debug.Log($"[BettingController] Initialize - balance={currentBalance}, minBet={minBet}, maxBet={maxBet}");
    }

    /// <summary>
    /// Validează dacă un bet este permis.
    /// NU schimbă slider-ul, NU schimbă UI-ul.
    /// </summary>
    public bool ValidateBet(float amount, out string errorMessage)
    {
        if (amount < minBet)
        {
            errorMessage = $"Minimum bet is ${minBet:F2}";
            return false;
        }

        if (amount > maxBet)
        {
            errorMessage = $"Maximum bet is ${maxBet:F2}";
            return false;
        }

        if (amount > currentBalance)
        {
            errorMessage = "Insufficient balance";
            return false;
        }

        errorMessage = "";
        return true;
    }

    /// <summary>
    /// Opțional: poți apela asta după ce serverul actualizează balanța.
    /// </summary>
    public void UpdateBalance(float newBalance)
    {
        currentBalance = newBalance;
        Debug.Log($"[BettingController] Balance updated: {currentBalance}");
    }
}