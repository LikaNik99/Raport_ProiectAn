using UnityEngine;
using UnityEngine.UI;
using System.Collections.Generic;
using TMPro;

public class GameTablePanel : MonoBehaviour
{
    [Header("Dealer Area")]
    public Transform dealerCardContainer;
    public TMP_Text dealerValueTMP_Text;
    public GameObject cardPrefab;
    public Transform deckPosition; // Position of the deck on table

    [Header("Card Animation Settings")]
    public float cardDealInterval = 0.7f; // Time between each card deal (0.5-1 seconds)
    private int dealerCardCount = 0; // Track how many dealer cards are displayed
    private string lastGamePhase = ""; // Track phase changes to reset cards

    [Header("Player Slots")]
    public PlayerSlot[] playerSlots; // Array of 5 player slots

    [Header("Center Display")]
    public TMP_Text potTMP_Text;
    public TMP_Text phaseTMP_Text;
    public TMP_Text currentTurnTMP_Text;

    [Header("Action Buttons")]
    public GameObject bettingPanel;
    public Slider betSlider;
    public TMP_Text betAmountTMP_Text;
    public Button placeBetButton;
    public Button[] quickBetButtons; // For $10, $20, $50, $100, $500
    public Button minBetButton;
    public Button maxBetButton;
    public GameObject gameActionsPanel;
    public Button hitButton;
    public Button standButton;
    public Button splitButton;
    public Button doubleDownButton;
    public Button insuranceButton;

    [Header("Quick Bet Settings")]
    public float[] quickBetAmounts = new float[] { 10f, 20f, 50f, 100f, 500f };
    private bool isSliderChange = false;

    // 🔹 Nou: reținem mereu suma selectată
    private float selectedBetAmount = 0f;

    [Header("Game Info")]
    public TMP_Text userBalanceTMP_Text;
    public TMP_Text userBetTMP_Text;
    public TMP_Text roomIdTMP_Text;

    [Header("Animation Managers (Optional)")]
    [Tooltip("Handles card flip and deal animations")]
    public CardAnimationManager cardAnimationManager;

    [Tooltip("Handles chip and money animations")]
    public ChipAnimationManager chipAnimationManager;

    [Tooltip("Shows whose turn it is with visual effects")]
    public TurnIndicator turnIndicator;

    [Header("Leave/Result")]
    public Button leaveTableButton;
    public GameObject resultPanel;
    public TMP_Text resultTMP_Text;
    public Button playAgainButton;
    public Button exitButton;

    private GameStateData currentState;
    
    

    private void Start()
    {
        // Subscribe to button clicks
        if (placeBetButton != null)
        {
            placeBetButton.onClick.AddListener(OnPlaceBetClicked);
        }

        if (hitButton != null)
        {
            hitButton.onClick.AddListener(OnHitClicked);
        }

        if (standButton != null)
        {
            standButton.onClick.AddListener(OnStandClicked);
        }

        if (splitButton != null)
        {
            splitButton.onClick.AddListener(OnSplitClicked);
        }

        if (doubleDownButton != null)
        {
            doubleDownButton.onClick.AddListener(OnDoubleDownClicked);
        }

        if (insuranceButton != null)
        {
            insuranceButton.onClick.AddListener(OnInsuranceClicked);
        }

        if (leaveTableButton != null)
        {
            leaveTableButton.onClick.AddListener(OnLeaveTableClicked);
        }

        if (playAgainButton != null)
        {
            playAgainButton.onClick.AddListener(OnPlayAgainClicked);
        }

        if (exitButton != null)
        {
            exitButton.onClick.AddListener(OnExitClicked);
        }

        // Subscribe to bet slider
        if (betSlider != null)
        {
            betSlider.onValueChanged.AddListener(OnBetSliderChanged);
        }

        // Setup quick bet buttons
        SetupQuickBetButtons();

        // Setup min/max bet buttons
        if (minBetButton != null)
        {
            minBetButton.onClick.AddListener(OnMinBetClicked);
        }

        if (maxBetButton != null)
        {
            maxBetButton.onClick.AddListener(OnMaxBetClicked);
        }

        // Hide panels initially
        if (gameActionsPanel != null) gameActionsPanel.SetActive(false);
        if (resultPanel != null) resultPanel.SetActive(false);
    }

    private void SetupQuickBetButtons()
    {
        if (quickBetButtons == null) return;

        for (int i = 0; i < quickBetButtons.Length && i < quickBetAmounts.Length; i++)
        {
            int index = i; // Capture for lambda
            Button btn = quickBetButtons[i];

            if (btn != null)
            {
                // Set button text to amount
                TMP_Text btnText = btn.GetComponentInChildren<TMP_Text>();
                if (btnText != null)
                {
                    btnText.text = $"${quickBetAmounts[index]:F0}";
                }

                // 🔹 Quick Bet setează direct suma (nu mai adună)
                btn.onClick.AddListener(() => OnQuickBetClicked(quickBetAmounts[index]));
            }
        }
    }

    private void OnEnable()
    {
        // Subscribe to game events when panel is enabled
        if (GameManager.Instance != null)
        {
            GameManager.Instance.OnGameStateUpdated += OnGameStateUpdated;
            GameManager.Instance.OnGameResultReceived += OnGameResultReceived;
            Debug.Log("[GameTablePanel] Subscribed to GameManager events");
            resultPanel.SetActive(false);
        }
        else
        {
            Debug.LogError("[GameTablePanel] GameManager.Instance is NULL! Cannot subscribe to events.");
        }
    }

    private void OnDisable()
    {
        // Unsubscribe when panel is disabled
        if (GameManager.Instance != null)
        {
            GameManager.Instance.OnGameStateUpdated -= OnGameStateUpdated;
            GameManager.Instance.OnGameResultReceived -= OnGameResultReceived;
            Debug.Log("[GameTablePanel] Unsubscribed from GameManager events");
        }
    }

    private void OnGameStateUpdated(GameStateData state)
    {
        currentState = state;
        Debug.Log($"[GameTablePanel] Game state updated - Phase: {state.phase}, Players: {state.players.Count}");
        UpdateUI();
    }

    private void UpdateUI()
    {
        if (currentState == null) return;

        var currentUser = AuthManager.Instance?.GetCurrentUser();
        if (currentUser == null) return;

        // Update room info
        if (roomIdTMP_Text != null)
        {
            roomIdTMP_Text.text = $"Room: {currentState.roomId}";
        }

        // Update pot
        if (potTMP_Text != null)
        {
            potTMP_Text.text = $"Pot: ${currentState.pot:F2}";
        }

        // Update phase
        if (phaseTMP_Text != null)
        {
            phaseTMP_Text.text = $"Phase: {currentState.phase}";
        }

        // Update current turn
        if (currentTurnTMP_Text != null)
        {
            var currentPlayer = currentState.GetPlayerById(currentState.currentTurn);
            if (currentPlayer != null)
            {
                currentTurnTMP_Text.text = $"Turn: {currentPlayer.username}";
            }
            else
            {
                currentTurnTMP_Text.text = "";
            }
        }

        // Update dealer
        UpdateDealerDisplay();

        // Update player slots
        UpdatePlayerSlots();

        // Update user info
        var myPlayerData = currentState.GetPlayerById(currentUser.userId);
        if (myPlayerData != null)
        {
            if (userBalanceTMP_Text != null)
            {
                userBalanceTMP_Text.text = $"Balance: ${myPlayerData.balance:F2}";
            }

            if (userBetTMP_Text != null)
            {
                userBetTMP_Text.text = $"Bet: ${myPlayerData.bet:F2}";
            }
        }

        // Show/hide action panels based on phase and turn
        UpdateActionPanels();
    }

    private void UpdateDealerDisplay()
    {
        if (currentState.dealer == null) return;

        // Update dealer value
        if (dealerValueTMP_Text != null)
        {
            if (currentState.IsFinished())
            {
                dealerValueTMP_Text.text = $"Dealer: {currentState.dealer.handValue}";
            }
            else
            {
                dealerValueTMP_Text.text = $"Dealer: {currentState.dealer.visibleValue}";
            }
        }

        // Update dealer cards visual
        UpdateDealerCards();
    }

    private void UpdateDealerCards()
    {
        if (dealerCardContainer == null || currentState.dealer == null || currentState.dealer.cards == null)
        {
            return;
        }

        int newCardCount = currentState.dealer.cards.Count;

        // Check if we're starting a new round (phase changed to DEALING/BETTING)
        bool isNewRound = (currentState.phase == "BETTING" || currentState.phase == "DEALING") &&
                          lastGamePhase != currentState.phase;

        if (isNewRound)
        {
            Debug.Log($"[GameTablePanel] New round detected. Clearing all dealer cards. Phase: {currentState.phase}");
            // Clear all cards for new round
            foreach (Transform child in dealerCardContainer)
            {
                Destroy(child.gameObject);
            }
            dealerCardCount = 0;
        }
        // If card count decreased (shouldn't happen except in new round), reset
        else if (newCardCount < dealerCardCount)
        {
            Debug.Log($"[GameTablePanel] Card count decreased. Clearing all dealer cards.");
            foreach (Transform child in dealerCardContainer)
            {
                Destroy(child.gameObject);
            }
            dealerCardCount = 0;
        }

        // Check for cards that flipped face-up (dealer's hole card reveal)
        if (dealerCardCount > 0 && dealerCardContainer.childCount > 0)
        {
            for (int i = 0; i < Mathf.Min(dealerCardCount, currentState.dealer.cards.Count); i++)
            {
                if (i < dealerCardContainer.childCount)
                {
                    Card cardComponent = dealerCardContainer.GetChild(i).GetComponent<Card>();
                    CardData newCardData = currentState.dealer.cards[i];

                    if (cardComponent != null)
                    {
                        CardData currentCardData = cardComponent.GetCardData();

                        // Check if card flipped from face-down to face-up
                        if (currentCardData != null && !currentCardData.faceUp && newCardData.faceUp)
                        {
                            Debug.Log($"[GameTablePanel] Dealer card {i} flipping face-up!");
                            cardComponent.AnimateFlip(true, 0.3f);
                        }
                    }
                }
            }
        }

        // Only animate NEW cards
        if (newCardCount > dealerCardCount)
        {
            int numNewCards = newCardCount - dealerCardCount;
            Debug.Log($"[GameTablePanel] Dealer has {numNewCards} new cards. Total: {newCardCount}, Previously: {dealerCardCount}");

            // Get only the new cards
            var newCards = currentState.dealer.cards.GetRange(dealerCardCount, numNewCards);
            StartCoroutine(DealNewCardsFromDeck(newCards, dealerCardContainer));
            dealerCardCount = newCardCount;
        }

        lastGamePhase = currentState.phase;
    }

    private System.Collections.IEnumerator DealNewCardsFromDeck(System.Collections.Generic.List<CardData> newCards, Transform container)
    {
        if (cardPrefab == null)
        {
            Debug.LogError("[GameTablePanel] cardPrefab is not assigned!");
            yield break;
        }

        foreach (var cardData in newCards)
        {
            GameObject cardObj = Instantiate(cardPrefab, container);
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
    }

    private void UpdatePlayerSlots()
    {
        // Clear all slots first to handle player order changes
        var currentUser = AuthManager.Instance?.GetCurrentUser();

        // First pass: assign current user to first slot
        if (currentUser != null)
        {
            var myPlayer = currentState.GetPlayerById(currentUser.userId);
            if (myPlayer != null && playerSlots.Length > 0 && playerSlots[0] != null)
            {
                bool isCurrentTurn = currentState.currentTurn == myPlayer.userId;
                playerSlots[0].UpdateSlot(myPlayer, isCurrentTurn);
            }
        }

        // Second pass: assign other players to remaining slots
        int slotIndex = 1; // Start from slot 1 (slot 0 is for current user)
        foreach (var player in currentState.players)
        {
            // Skip current user (already in slot 0)
            if (currentUser != null && player.userId == currentUser.userId)
                continue;

            // Find next available slot
            if (slotIndex < playerSlots.Length && playerSlots[slotIndex] != null)
            {
                bool isCurrentTurn = currentState.currentTurn == player.userId;
                playerSlots[slotIndex].UpdateSlot(player, isCurrentTurn);
                slotIndex++;
            }
        }

        // Clear remaining empty slots
        for (int i = slotIndex; i < playerSlots.Length; i++)
        {
            if (playerSlots[i] != null)
            {
                playerSlots[i].ClearSlot();
            }
        }

        // Update turn indicator
        if (turnIndicator != null && currentState != null)
        {
            var currentTurnPlayer = currentState.GetPlayerById(currentState.currentTurn);
            if (currentTurnPlayer != null)
            {
                bool isMyTurn = currentUser != null && currentTurnPlayer.userId == currentUser.userId;
                turnIndicator.ShowTurn(currentTurnPlayer.username, isMyTurn);
            }
            else if (currentState.phase == "BETTING")
            {
                turnIndicator.ShowWaiting("Place your bets!");
            }
            else if (currentState.phase == "DEALER_TURN")
            {
                turnIndicator.ShowWaiting("Dealer's turn...");
            }
            else if (currentState.phase == "RESULTS" || currentState.phase == "FINISHED")
            {
                turnIndicator.Hide();
            }
        }
    }
    // Pentru a nu reseta slider-ul la fiecare UpdateUI
    private string lastPhaseForBetUI = "";
    private bool betUIInitializedThisPhase = false;


    private void UpdateActionPanels()
{
    var currentUser = AuthManager.Instance?.GetCurrentUser();
    if (currentUser == null)
    {
        Debug.LogWarning("[GameTablePanel] UpdateActionPanels: Current user is null!");
        return;
    }

    bool isMyTurn = currentState.IsMyTurn(currentUser.userId);
    bool isBettingPhase = currentState.IsBettingPhase();
    bool isPlayerTurns = currentState.IsPlayerTurns();

    Debug.Log($"[GameTablePanel] UpdateActionPanels - Phase: {currentState.phase}, IsBettingPhase: {isBettingPhase}, IsPlayerTurns: {isPlayerTurns}, IsMyTurn: {isMyTurn}");

    // Show betting panel if it's betting phase and my turn
    if (bettingPanel != null)
    {
        var myPlayer = currentState.GetPlayerById(currentUser.userId);
        bool hasBet = myPlayer != null && myPlayer.bet > 0;
        bool shouldShowBetting = isBettingPhase && !hasBet;

        bettingPanel.SetActive(shouldShowBetting);
        Debug.Log($"[GameTablePanel] Betting Panel - MyPlayer found: {myPlayer != null}, HasBet: {hasBet}, ShouldShow: {shouldShowBetting}");

        // 🔹 DOAR setăm min/max, NU și value
        if (shouldShowBetting && betSlider != null && myPlayer != null)
        {
            betSlider.minValue = currentState.minBet;
            betSlider.maxValue = Mathf.Max(currentState.minBet, myPlayer.balance);

            // dacă nu avem încă nimic selectat, punem minBet o singură dată
            if (selectedBetAmount <= 0f)
            {
                selectedBetAmount = currentState.minBet;

                isSliderChange = true;
                betSlider.value = selectedBetAmount;
                isSliderChange = false;

                if (betAmountTMP_Text != null)
                    betAmountTMP_Text.text = $"${selectedBetAmount:F2}";
            }
        }

        if (placeBetButton != null)
        {
            placeBetButton.interactable = shouldShowBetting;
        }
    }

    // Show game actions panel if it's player turns and my turn
    if (gameActionsPanel != null)
    {
        gameActionsPanel.SetActive(isPlayerTurns && isMyTurn);

        if (isPlayerTurns && isMyTurn)
        {
            var myPlayer = currentState.GetPlayerById(currentUser.userId);
            if (myPlayer != null)
            {
                if (splitButton != null)
                    splitButton.interactable = myPlayer.CanSplit();

                if (doubleDownButton != null)
                    doubleDownButton.interactable = myPlayer.CanDoubleDown();

                if (insuranceButton != null)
                {
                    bool dealerShowsAce = currentState.dealer != null &&
                                          currentState.dealer.cards != null &&
                                          currentState.dealer.cards.Count > 0 &&
                                          currentState.dealer.cards[0].rank == "ACE";
                    insuranceButton.interactable = myPlayer.CanBuyInsurance() && dealerShowsAce;
                }
            }
        }
    }
}



    private void OnBetSliderChanged(float value)
    {
        // Dacă schimbarea vine de la user, rotunjim la 5
        if (!isSliderChange)
        {
            value = Mathf.Round(value / 5f) * 5f;
            if (betSlider != null && Mathf.Abs(betSlider.value - value) > 0.1f)
            {
                isSliderChange = true;
                betSlider.value = value;
                isSliderChange = false;
            }
        }

        selectedBetAmount = value;

        Debug.Log($"[GameTablePanel] OnBetSliderChanged - value = {value}");

        if (betAmountTMP_Text != null)
        {
            betAmountTMP_Text.text = $"${value:F2}";
        }
    }


    private void OnQuickBetClicked(float amount)
    {
        if (betSlider == null) return;

        if (AudioManager.Instance != null)
            AudioManager.Instance.PlayChip();

        var currentUser = AuthManager.Instance?.GetCurrentUser();
        float newBet = amount;

        if (currentUser != null && currentState != null)
        {
            var myPlayer = currentState.GetPlayerById(currentUser.userId);
            if (myPlayer != null)
                newBet = Mathf.Clamp(newBet, currentState.minBet, Mathf.Min(1000f, myPlayer.balance));
        }

        selectedBetAmount = newBet;

        isSliderChange = true;
        betSlider.value = selectedBetAmount;
        isSliderChange = false;

        if (betAmountTMP_Text != null)
            betAmountTMP_Text.text = $"${selectedBetAmount:F2}";

        Debug.Log($"[GameTablePanel] OnQuickBetClicked - selectedBetAmount = {selectedBetAmount}");
    }


    private void OnMinBetClicked()
    {
        if (betSlider != null && currentState != null)
        {
            if (AudioManager.Instance != null)
                AudioManager.Instance.PlayButtonClick();

            selectedBetAmount = currentState.minBet;

            isSliderChange = true;
            betSlider.value = selectedBetAmount;
            isSliderChange = false;

            if (betAmountTMP_Text != null)
                betAmountTMP_Text.text = $"${selectedBetAmount:F2}";

            Debug.Log($"[GameTablePanel] OnMinBetClicked - selectedBetAmount = {selectedBetAmount}");
        }
    }


    private void OnMaxBetClicked()
    {
        if (betSlider == null || currentState == null) return;

        if (AudioManager.Instance != null)
            AudioManager.Instance.PlayButtonClick();

        var currentUser = AuthManager.Instance?.GetCurrentUser();
        if (currentUser != null)
        {
            var myPlayer = currentState.GetPlayerById(currentUser.userId);
            if (myPlayer != null)
            {
                float maxBet = Mathf.Min(1000f, myPlayer.balance);

                if (betSlider.maxValue < maxBet)
                    betSlider.maxValue = maxBet;

                selectedBetAmount = maxBet;

                isSliderChange = true;
                betSlider.value = selectedBetAmount;
                isSliderChange = false;

                if (betAmountTMP_Text != null)
                    betAmountTMP_Text.text = $"${selectedBetAmount:F2}";

                Debug.Log($"[GameTablePanel] OnMaxBetClicked - selectedBetAmount = {selectedBetAmount}");
            }
        }
    }


    private void OnPlaceBetClicked()
    {
        if (AudioManager.Instance != null)
            AudioManager.Instance.PlayButtonClick();

        float betAmount = selectedBetAmount;

        // fallback – dacă dintr-un motiv selectedBetAmount nu a fost setat
        if (betAmount <= 0f && betSlider != null)
            betAmount = betSlider.value;

        Debug.Log($"[GameTablePanel] OnPlaceBetClicked - BEFORE CLAMP - selectedBetAmount = {selectedBetAmount}, slider = {(betSlider != null ? betSlider.value : 0f)}");

        var currentUser = AuthManager.Instance?.GetCurrentUser();
        if (currentUser != null && currentState != null)
        {
            var myPlayer = currentState.GetPlayerById(currentUser.userId);
            if (myPlayer != null)
                betAmount = Mathf.Clamp(betAmount, currentState.minBet, Mathf.Min(1000f, myPlayer.balance));
        }

        Debug.Log($"[GameTablePanel] OnPlaceBetClicked - AFTER CLAMP - final betAmount = {betAmount}");

        GameManager.Instance.PlaceBet(betAmount);

        if (AudioManager.Instance != null)
            AudioManager.Instance.PlayBetPlaced();

        if (ParticleEffectsManager.Instance != null)
            ParticleEffectsManager.Instance.PlayBetPlacedEffect(transform.position);

        if (MobileInputManager.Instance != null)
            MobileInputManager.Instance.PlayLightHaptic();

        if (placeBetButton != null)
            placeBetButton.interactable = false;
    }



    private void OnHitClicked()
    {
        // Play button click sound
        if (AudioManager.Instance != null)
        {
            AudioManager.Instance.PlayButtonClick();
        }

        // Play haptic feedback on mobile
        if (MobileInputManager.Instance != null)
        {
            MobileInputManager.Instance.PlayLightHaptic();
        }

        GameManager.Instance.Hit();
    }

    private void OnStandClicked()
    {
        // Play button click sound
        if (AudioManager.Instance != null)
        {
            AudioManager.Instance.PlayButtonClick();
        }

        // Play haptic feedback on mobile
        if (MobileInputManager.Instance != null)
        {
            MobileInputManager.Instance.PlayLightHaptic();
        }

        GameManager.Instance.Stand();
    }

    private void OnSplitClicked()
    {
        // Play button click sound
        if (AudioManager.Instance != null)
        {
            AudioManager.Instance.PlayButtonClick();
        }

        // Play split particle effect
        if (ParticleEffectsManager.Instance != null)
        {
            ParticleEffectsManager.Instance.PlaySplitEffect(transform.position);
        }

        // Play haptic feedback on mobile
        if (MobileInputManager.Instance != null)
        {
            MobileInputManager.Instance.PlayLightHaptic();
        }

        GameManager.Instance.Split();
    }

    private void OnDoubleDownClicked()
    {
        // Play button click sound
        if (AudioManager.Instance != null)
        {
            AudioManager.Instance.PlayButtonClick();
        }

        // Play double down particle effect
        if (ParticleEffectsManager.Instance != null)
        {
            ParticleEffectsManager.Instance.PlayDoubleDownEffect(transform.position);
        }

        // Play haptic feedback on mobile
        if (MobileInputManager.Instance != null)
        {
            MobileInputManager.Instance.PlayLightHaptic();
        }

        GameManager.Instance.DoubleDown();
    }

    private void OnInsuranceClicked()
    {
        // Play button click sound
        if (AudioManager.Instance != null)
        {
            AudioManager.Instance.PlayButtonClick();
        }

        // Play insurance particle effect
        if (ParticleEffectsManager.Instance != null)
        {
            ParticleEffectsManager.Instance.PlayInsuranceEffect(transform.position);
        }

        // Play haptic feedback on mobile
        if (MobileInputManager.Instance != null)
        {
            MobileInputManager.Instance.PlayLightHaptic();
        }

        GameManager.Instance.BuyInsurance();
    }

    private void OnLeaveTableClicked()
    {
        // Play button click sound
        if (AudioManager.Instance != null)
        {
            AudioManager.Instance.PlayButtonClick();
        }

        // Clear all player slots
        if (playerSlots != null)
        {
            foreach (var slot in playerSlots)
            {
                if (slot != null)
                {
                    slot.ClearSlot();
                }
            }
        }

        // Clear dealer cards
        if (dealerCardContainer != null)
        {
            foreach (Transform child in dealerCardContainer)
            {
                Destroy(child.gameObject);
            }
        }

        // Reset dealer value text
        if (dealerValueTMP_Text != null)
        {
            dealerValueTMP_Text.text = "";
        }

        // Hide turn indicator
        if (turnIndicator != null)
        {
            turnIndicator.Hide();
        }

        GameManager.Instance.LeaveRoom();
        UIManager.Instance.ShowMainMenuPanel();
    }

    private void OnGameResultReceived(GameResultData result)
    {
        // Hide action panels
        if (bettingPanel != null) bettingPanel.SetActive(false);
        if (gameActionsPanel != null) gameActionsPanel.SetActive(false);

        // Show result panel
        if (resultPanel != null)
        {
            resultPanel.SetActive(true);
        }

        // Display results and play sound
        var currentUser = AuthManager.Instance?.GetCurrentUser();
        if (currentUser != null && resultTMP_Text != null)
        {
            foreach (var playerResult in result.playerResults)
            {
                if (playerResult.userId == currentUser.userId)
                {
                    string resultMessage = "";
                    switch (playerResult.result)
                    {
                        case "won":
                            resultMessage = $"YOU WON!\nHand: {playerResult.handValue}\nNew Balance: ${playerResult.balance:F2}";

                            // Play win sound (or blackjack sound if applicable)
                            if (AudioManager.Instance != null)
                            {
                                if (playerResult.handValue == 21)
                                {
                                    AudioManager.Instance.PlayBlackjack();
                                }
                                else
                                {
                                    AudioManager.Instance.PlayWin();
                                }
                            }

                            // Play win particle effect
                            if (ParticleEffectsManager.Instance != null)
                            {
                                if (playerResult.handValue == 21)
                                {
                                    ParticleEffectsManager.Instance.PlayBlackjackEffect(transform.position);
                                }
                                else
                                {
                                    ParticleEffectsManager.Instance.PlayWinEffect(transform.position);
                                }
                            }

                            // Animate winning chips (optional)
                            if (chipAnimationManager != null && playerSlots.Length > 0)
                            {
                                Vector3 playerPosition = playerSlots[0].transform.position;
                                float winAmount = playerResult.balance - currentUser.balance;
                                if (playerResult.handValue == 21)
                                {
                                    chipAnimationManager.AnimateBlackjackWin(playerPosition, winAmount);
                                }
                                else
                                {
                                    chipAnimationManager.AnimateWinChips(transform.position, playerPosition, winAmount);
                                }
                            }
                            break;
                        case "lost":
                            resultMessage = $"You Lost\nHand: {playerResult.handValue}\nNew Balance: ${playerResult.balance:F2}";

                            // Play lose or bust sound
                            if (AudioManager.Instance != null)
                            {
                                if (playerResult.handValue > 21)
                                {
                                    AudioManager.Instance.PlayBust();
                                }
                                else
                                {
                                    AudioManager.Instance.PlayLose();
                                }
                            }

                            // Play lose or bust particle effect
                            if (ParticleEffectsManager.Instance != null)
                            {
                                if (playerResult.handValue > 21)
                                {
                                    ParticleEffectsManager.Instance.PlayBustEffect(transform.position);
                                }
                                else
                                {
                                    ParticleEffectsManager.Instance.PlayLoseEffect(transform.position);
                                }
                            }

                            // Animate losing chips (optional)
                            if (chipAnimationManager != null && playerSlots.Length > 0)
                            {
                                Vector3 playerPosition = playerSlots[0].transform.position;
                                float lossAmount = currentUser.balance - playerResult.balance;
                                if (playerResult.handValue > 21)
                                {
                                    chipAnimationManager.AnimateBust(playerPosition, lossAmount);
                                }
                                else
                                {
                                    chipAnimationManager.AnimateLoseChips(playerPosition, transform.position, lossAmount);
                                }
                            }
                            break;
                        case "push":
                            resultMessage = $"PUSH (Tie)\nHand: {playerResult.handValue}\nNew Balance: ${playerResult.balance:F2}";

                            // Play push sound
                            if (AudioManager.Instance != null)
                            {
                                AudioManager.Instance.PlayPush();
                            }

                            // Play push particle effect
                            if (ParticleEffectsManager.Instance != null)
                            {
                                ParticleEffectsManager.Instance.PlayPushEffect(transform.position);
                            }

                            // Animate push (optional)
                            if (chipAnimationManager != null && playerSlots.Length > 0)
                            {
                                Vector3 playerPosition = playerSlots[0].transform.position;
                                // Get bet amount from current state or estimate
                                float betAmount = currentState != null ? currentState.pot / currentState.players.Count : 0;
                                chipAnimationManager.AnimatePush(playerPosition, betAmount);
                            }
                            break;
                    }
                    resultTMP_Text.text = resultMessage;
                    break;
                }
            }
        }
    }

    private void OnPlayAgainClicked()
    {
        if (resultPanel != null)
        {
            resultPanel.SetActive(false);
        }

        // Tell the server we're ready for the next round
        GameManager.Instance.PlayAgain();
        Debug.Log("[GameTablePanel] Signaled ready for next round");
    }

    private void OnExitClicked()
    {
        GameManager.Instance.LeaveRoom();
        UIManager.Instance.ShowMainMenuPanel();
    }
}
