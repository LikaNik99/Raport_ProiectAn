using TMPro;
using UnityEngine;
using UnityEngine.UI;

/// <summary>
/// Represents a player slot at the game table
/// </summary>
public class PlayerSlot : MonoBehaviour
{
    [Header("UI Elements")]
    public TMP_Text usernameText;
    public TMP_Text balanceText;
    public TMP_Text betText;
    public TMP_Text handValueText;
    public Transform cardContainer;
    public TMP_Text splitHandValueText;
    public Transform splitCardContainer;
    public GameObject currentTurnIndicator;
    public GameObject emptySlotObject;
    public GameObject occupiedSlotObject;
    public GameObject cardPrefab;

    [Header("Animation")]
    public Transform deckPosition; // Reference to deck position
    public float cardDealInterval = 0.7f;
    private int cardCount = 0; // Track how many cards are displayed
    private int splitCardCount = 0; // Track how many split hand cards are displayed

    [Header("Status Colors")]
    public Color normalColor = Color.white;
    public Color activeColor = Color.yellow;
    public Color bustColor = Color.red;
    public Color blackjackColor = Color.green;
    public Color standColor = Color.gray;

    private PlayerData currentPlayer;

    /// <summary>
    /// Update the slot with player data
    /// </summary>
    public void UpdateSlot(PlayerData player, bool isCurrentTurn)
    {
        // Check if this is a new/different player
        bool isNewPlayer = currentPlayer == null || currentPlayer.userId != player.userId;

        if (isNewPlayer)
        {
            Debug.Log($"[PlayerSlot] New player detected: {player.username}. Resetting card count.");
            // Clear cards and reset count for new player
            ClearCards();
            cardCount = 0;
            ClearSplitCards();
            splitCardCount = 0;
        }

        currentPlayer = player;

        // Show occupied slot, hide empty slot
        if (emptySlotObject != null) emptySlotObject.SetActive(false);
        if (occupiedSlotObject != null) occupiedSlotObject.SetActive(true);

        // Update username
        if (usernameText != null)
        {
            usernameText.text = player.username;
        }

        // Update balance
        if (balanceText != null)
        {
            balanceText.text = $"${player.balance:F2}";
        }

        // Update bet
        if (betText != null)
        {
            betText.text = player.bet > 0 ? $"Bet: ${player.bet:F2}" : "";
        }

        // Update hand value
        if (handValueText != null)
        {
            if (player.hand.Count > 0)
            {
                handValueText.text = $"{player.handValue}";

                // Color based on status
                if (player.IsBusted())
                {
                    handValueText.color = bustColor;
                }
                else if (player.HasBlackjack())
                {
                    handValueText.color = blackjackColor;
                }
                else if (player.status == "standing")
                {
                    handValueText.color = standColor;
                }
                else if (isCurrentTurn)
                {
                    handValueText.color = activeColor;
                }
                else
                {
                    handValueText.color = normalColor;
                }
            }
            else
            {
                handValueText.text = "";
            }
        }

        // Update split hand display if player has split
        if (player.hasSplit && splitHandValueText != null)
        {
            splitHandValueText.gameObject.SetActive(true);
            splitHandValueText.text = $"Split: {player.splitHandValue}";

            // Color based on active hand
            if (player.activeHandIndex == 1)
            {
                splitHandValueText.color = activeColor;
                if (handValueText != null) handValueText.color = normalColor;
            }
            else
            {
                splitHandValueText.color = normalColor;
            }
        }
        else if (splitHandValueText != null)
        {
            splitHandValueText.gameObject.SetActive(false);
        }

        // Show/hide current turn indicator
        if (currentTurnIndicator != null)
        {
            currentTurnIndicator.SetActive(isCurrentTurn);
        }

        // Update cards display
        UpdateCardsDisplay(player.hand);

        // Update split hand cards display if player has split
        if (player.hasSplit && player.splitHand != null && player.splitHand.Count > 0)
        {
            UpdateSplitCardsDisplay(player.splitHand);
        }
        else
        {
            // Clear split hand cards if no split
            ClearSplitCards();
            splitCardCount = 0;
        }
    }

    /// <summary>
    /// Clear the slot (no player)
    /// </summary>
    public void ClearSlot()
    {
        currentPlayer = null;

        if (emptySlotObject != null) emptySlotObject.SetActive(true);
        if (occupiedSlotObject != null) occupiedSlotObject.SetActive(false);
        if (currentTurnIndicator != null) currentTurnIndicator.SetActive(false);

        // Clear text fields  <-- ADD THESE 4 LINES
        if (usernameText != null) usernameText.text = "";
        if (balanceText != null) balanceText.text = "";
        if (betText != null) betText.text = "";
        if (handValueText != null) handValueText.text = "";

        if (splitHandValueText != null) splitHandValueText.gameObject.SetActive(false);

        ClearCards();
        cardCount = 0;
        ClearSplitCards();
        splitCardCount = 0;
    }

    /// <summary>
    /// Update the visual display of cards
    /// </summary>
    private void UpdateCardsDisplay(System.Collections.Generic.List<CardData> cards)
    {
        if (cards == null) return;

        int newCardCount = cards.Count;

        // If this is a new game (fewer cards than before), reset
        if (newCardCount < cardCount)
        {
            Debug.Log($"[PlayerSlot] Card count decreased. Clearing all cards. Player: {currentPlayer?.username}");
            ClearCards();
            cardCount = 0;
        }

        // Only animate NEW cards
        if (newCardCount > cardCount)
        {
            int numNewCards = newCardCount - cardCount;
            Debug.Log($"[PlayerSlot] Player {currentPlayer?.username} has {numNewCards} new cards. Total: {newCardCount}, Previously: {cardCount}");

            // Get only the new cards
            var newCards = cards.GetRange(cardCount, numNewCards);
            StartCoroutine(DealNewCardsFromDeck(newCards));
            cardCount = newCardCount;
        }
    }

    private System.Collections.IEnumerator DealNewCardsFromDeck(System.Collections.Generic.List<CardData> newCards)
    {
        if (cardPrefab == null)
        {
            yield break;
        }

        foreach (var cardData in newCards)
        {
            GameObject cardObj = Instantiate(cardPrefab, cardContainer);
            Card cardComponent = cardObj.GetComponent<Card>();

            if (cardComponent != null)
            {
                cardComponent.SetCard(cardData);

                // If deck position is assigned, animate from deck
                if (deckPosition != null)
                {
                    cardComponent.AnimateDeal(deckPosition.position);
                }
                else
                {
                    // Fallback to simple appear animation
                    cardComponent.AnimateAppear(0f);
                }
            }

            
            
            // Wait before dealing next card
            yield return new WaitForSeconds(cardDealInterval);
        }
        cardContainer.GetComponent<HorizontalLayoutGroup>().enabled = false;
        // yield return new WaitForSeconds(0.1f);
        yield return new WaitForEndOfFrame();
        cardContainer.GetComponent<HorizontalLayoutGroup>().enabled = true;
        LayoutRebuilder.ForceRebuildLayoutImmediate(cardContainer.GetComponent<RectTransform>());
    }

    /// <summary>
    /// Clear all card visuals
    /// </summary>
    private void ClearCards()
    {
        if (cardContainer != null)
        {
            foreach (Transform child in cardContainer)
            {
                Destroy(child.gameObject);
            }
        }
    }

    /// <summary>
    /// Update the visual display of split hand cards
    /// </summary>
    private void UpdateSplitCardsDisplay(System.Collections.Generic.List<CardData> cards)
    {
        if (cards == null || splitCardContainer == null) return;

        int newCardCount = cards.Count;

        // If this is a new game (fewer cards than before), reset
        if (newCardCount < splitCardCount)
        {
            Debug.Log($"[PlayerSlot] Split card count decreased. Clearing all split cards. Player: {currentPlayer?.username}");
            ClearSplitCards();
            splitCardCount = 0;
        }

        // Only animate NEW cards
        if (newCardCount > splitCardCount)
        {
            int numNewCards = newCardCount - splitCardCount;
            Debug.Log($"[PlayerSlot] Player {currentPlayer?.username} split hand has {numNewCards} new cards. Total: {newCardCount}, Previously: {splitCardCount}");

            // Get only the new cards
            var newCards = cards.GetRange(splitCardCount, numNewCards);
            StartCoroutine(DealNewCardsFromDeckToSplit(newCards));
            splitCardCount = newCardCount;
        }
    }

    private System.Collections.IEnumerator DealNewCardsFromDeckToSplit(System.Collections.Generic.List<CardData> newCards)
    {
        if (cardPrefab == null || splitCardContainer == null)
        {
            yield break;
        }

        foreach (var cardData in newCards)
        {
            GameObject cardObj = Instantiate(cardPrefab, splitCardContainer);
            Card cardComponent = cardObj.GetComponent<Card>();

            if (cardComponent != null)
            {
                cardComponent.SetCard(cardData);

                // If deck position is assigned, animate from deck
                if (deckPosition != null)
                {
                    cardComponent.AnimateDeal(deckPosition.position);
                }
                else
                {
                    // Fallback to simple appear animation
                    cardComponent.AnimateAppear(0f);
                }
            }

            // Wait before dealing next card
            yield return new WaitForSeconds(cardDealInterval);
        }

        // Force layout rebuild for split hand
        if (splitCardContainer.GetComponent<HorizontalLayoutGroup>() != null)
        {
            splitCardContainer.GetComponent<HorizontalLayoutGroup>().enabled = false;
            yield return new WaitForEndOfFrame();
            splitCardContainer.GetComponent<HorizontalLayoutGroup>().enabled = true;
            LayoutRebuilder.ForceRebuildLayoutImmediate(splitCardContainer.GetComponent<RectTransform>());
        }
    }

    /// <summary>
    /// Clear all split hand card visuals
    /// </summary>
    private void ClearSplitCards()
    {
        if (splitCardContainer != null)
        {
            foreach (Transform child in splitCardContainer)
            {
                Destroy(child.gameObject);
            }
        }
    }

    /// <summary>
    /// Highlight this slot (e.g., for current player)
    /// </summary>
    public void SetHighlight(bool highlighted)
    {
        // You can add visual effects here like border glow, scale, etc.
        if (occupiedSlotObject != null)
        {
            var outline = occupiedSlotObject.GetComponent<Outline>();
            if (outline != null)
            {
                outline.enabled = highlighted;
            }
        }
    }
}
