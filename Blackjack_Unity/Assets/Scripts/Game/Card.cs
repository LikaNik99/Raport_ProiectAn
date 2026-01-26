using UnityEngine;
using UnityEngine.UI;
#if DOTWEEN_ENABLED
using DG.Tweening;
#endif

/// <summary>
/// Visual representation of a playing card
/// </summary>
public class Card : MonoBehaviour
{
    [Header("Visual Elements")]
    public Image cardImage;
    public Sprite cardBackSprite;

    [Header("Animation Settings")]
    public float dealDuration = 0.3f;
    public float flipDuration = 0.2f;

    [Header("Card Data")]
    private CardData cardData;

    /// <summary>
    /// Set the card data and update visuals
    /// </summary>
    public void SetCard(CardData data)
    {
        cardData = data;
        UpdateVisuals();
    }

    /// <summary>
    /// Update the card's visual representation
    /// </summary>
    private void UpdateVisuals()
    {
        if (cardImage == null) return;

        if (cardData == null || !cardData.faceUp)
        {
            // Show card back
            cardImage.sprite = cardBackSprite;
        }
        else
        {
            // Load card sprite from Resources
            string spriteName = cardData.GetSpriteName();
            Sprite cardSprite = LoadCardSprite(spriteName);

            if (cardSprite != null)
            {
                cardImage.sprite = cardSprite;
            }
            else
            {
                Debug.LogWarning($"Could not load sprite for card: {spriteName}");
                cardImage.sprite = cardBackSprite;
            }
        }
    }

    /// <summary>
    /// Load a card sprite from Resources
    /// Assumes card sprites are in Resources/Cards/ folder
    /// </summary>
    private Sprite LoadCardSprite(string spriteName)
    {
        // Try to load from Resources/Cards/
        return Resources.Load<Sprite>($"Cards/{spriteName}");
    }

    /// <summary>
    /// Flip the card (show/hide face)
    /// </summary>
    public void Flip(bool faceUp)
    {
        if (cardData != null)
        {
            cardData.faceUp = faceUp;
            UpdateVisuals();
        }
    }

    /// <summary>
    /// Animate the card flip
    /// </summary>
    public void AnimateFlip(bool faceUp, float duration = 0.3f)
    {
#if DOTWEEN_ENABLED
        // Play flip sound if available
        if (AudioManager.Instance != null)
        {
            AudioManager.Instance.PlayCardFlip();
        }

        // Rotate to 90 degrees, flip sprite, then rotate back
        transform.DORotate(new Vector3(0, 90, 0), duration / 2)
            .SetEase(Ease.InOutQuad)
            .OnComplete(() =>
            {
                Flip(faceUp);
                transform.DORotate(new Vector3(0, 0, 0), duration / 2)
                    .SetEase(Ease.InOutQuad);
            });
#else
        // If DOTween not available, flip instantly
        Flip(faceUp);
#endif
    }

    /// <summary>
    /// Animate card being dealt from a position
    /// </summary>
    public void AnimateDeal(Vector3 fromPosition, System.Action onComplete = null)
    {
#if DOTWEEN_ENABLED
        // Save target position (where the card should end up)
        Vector3 targetPosition = transform.position;

        // Set initial position to deck
        transform.position = fromPosition;
        transform.localScale = Vector3.one * 0.1f; // Start very small

        // Note: Sound is played by CardAnimationManager to avoid duplicates

        // Animate to target position with scale and slight rotation
        Sequence dealSequence = DOTween.Sequence();
        dealSequence.Append(transform.DOMove(targetPosition, dealDuration).SetEase(Ease.OutQuad));
        dealSequence.Join(transform.DOScale(Vector3.one, dealDuration * 0.7f).SetEase(Ease.OutBack));
        dealSequence.Join(transform.DORotate(new Vector3(0, 0, 360), dealDuration, RotateMode.FastBeyond360));

        if (onComplete != null)
        {
            dealSequence.OnComplete(() => onComplete());
        }
#else
        // If DOTween not available, just show immediately
        transform.localScale = Vector3.one;
        onComplete?.Invoke();
#endif
    }

    /// <summary>
    /// Animate card appearing with scale
    /// </summary>
    public void AnimateAppear(float delay = 0f)
    {
#if DOTWEEN_ENABLED
        transform.localScale = Vector3.zero;
        transform.DOScale(Vector3.one, dealDuration)
            .SetDelay(delay)
            .SetEase(Ease.OutBack);
#else
        transform.localScale = Vector3.one;
#endif
    }

    /// <summary>
    /// Get the card data
    /// </summary>
    public CardData GetCardData()
    {
        return cardData;
    }
}
