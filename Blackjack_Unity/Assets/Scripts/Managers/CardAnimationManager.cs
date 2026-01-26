using UnityEngine;
using System.Collections;
using DG.Tweening;

/// <summary>
/// Manages card animations (flip, deal, slide)
/// Uses DOTween for smooth animations
/// </summary>
public class CardAnimationManager : MonoBehaviour
{
    public static CardAnimationManager Instance { get; private set; }

    [Header("Animation Settings")]
    [Tooltip("Duration of card flip animation")]
    public float flipDuration = 0.3f;

    [Tooltip("Duration of card deal/slide animation")]
    public float dealDuration = 0.5f;

    [Tooltip("Card movement ease type")]
    public Ease cardEase = Ease.OutQuad;

    [Tooltip("Delay between dealing multiple cards")]
    public float dealDelay = 0.2f;

    [Header("Deal Animation")]
    [Tooltip("Starting position for dealt cards (usually deck position)")]
    public Transform deckPosition;

    [Tooltip("Height arc for card trajectory")]
    public float arcHeight = 50f;

    private void Awake()
    {
        if (Instance == null)
        {
            Instance = this;
        }
    }

    /// <summary>
    /// Animate a card being dealt from deck to target position
    /// </summary>
    public void AnimateCardDeal(GameObject card, Vector3 targetPosition, System.Action onComplete = null)
    {
        if (card == null)
        {
            Debug.LogWarning("[CardAnimationManager] Card is null");
            onComplete?.Invoke();
            return;
        }

        // Start from deck position
        Vector3 startPos = deckPosition != null ? deckPosition.position : card.transform.position;
        card.transform.position = startPos;
        card.transform.localScale = Vector3.one * 0.5f; // Start smaller

        // Animate to target with arc
        Sequence sequence = DOTween.Sequence();

        // Move with arc effect
        sequence.Append(card.transform.DOMove(targetPosition, dealDuration).SetEase(cardEase));

        // Scale up to normal size
        sequence.Join(card.transform.DOScale(Vector3.one, dealDuration * 0.5f).SetEase(Ease.OutBack));

        // Add slight rotation for effect
        sequence.Join(card.transform.DORotate(new Vector3(0, 0, Random.Range(-5f, 5f)), dealDuration * 0.3f).SetEase(Ease.OutQuad));
        sequence.Append(card.transform.DORotate(Vector3.zero, dealDuration * 0.2f));

        sequence.OnComplete(() =>
        {
            // Play card deal sound
            if (AudioManager.Instance != null)
            {
                AudioManager.Instance.PlayCardDeal();
            }

            // Play card deal particle effect
            if (ParticleEffectsManager.Instance != null)
            {
                ParticleEffectsManager.Instance.PlayCardDealEffect(targetPosition);
            }

            onComplete?.Invoke();
        });
    }

    /// <summary>
    /// Animate a card flip (face up/down)
    /// </summary>
    public void AnimateCardFlip(GameObject card, bool faceUp, System.Action onComplete = null)
    {
        if (card == null)
        {
            Debug.LogWarning("[CardAnimationManager] Card is null");
            onComplete?.Invoke();
            return;
        }

        // Flip animation using rotation
        Sequence sequence = DOTween.Sequence();

        // Rotate to 90 degrees (edge view)
        sequence.Append(card.transform.DORotate(new Vector3(0, 90, 0), flipDuration * 0.5f).SetEase(Ease.InQuad));

        // Change card sprite/appearance at midpoint (when edge-on)
        sequence.AppendCallback(() =>
        {
            // Here you would swap the card sprite from back to front or vice versa
            // You can implement this by:
            // 1. Swapping Image.sprite on the card GameObject
            // 2. Enabling/disabling different child objects for front/back
            // 3. Using your custom card component's SetFaceUp method

            // Example: If you have an Image component with sprites
            var image = card.GetComponent<UnityEngine.UI.Image>();
            if (image != null)
            {
                // You would set image.sprite = faceUp ? frontSprite : backSprite;
                // This needs to be customized based on your card implementation
            }
        });

        // Rotate back to 0 degrees (show face)
        sequence.Append(card.transform.DORotate(Vector3.zero, flipDuration * 0.5f).SetEase(Ease.OutQuad));

        sequence.OnComplete(() =>
        {
            // Play card flip sound
            if (AudioManager.Instance != null)
            {
                AudioManager.Instance.PlayCardFlip();
            }

            onComplete?.Invoke();
        });
    }

    /// <summary>
    /// Animate a card sliding from one position to another
    /// </summary>
    public void AnimateCardSlide(GameObject card, Vector3 targetPosition, System.Action onComplete = null)
    {
        if (card == null)
        {
            Debug.LogWarning("[CardAnimationManager] Card is null");
            onComplete?.Invoke();
            return;
        }

        card.transform.DOMove(targetPosition, dealDuration).SetEase(cardEase).OnComplete(() =>
        {
            onComplete?.Invoke();
        });
    }

    /// <summary>
    /// Animate multiple cards being dealt in sequence
    /// </summary>
    public void AnimateMultipleCards(GameObject[] cards, Vector3[] targetPositions, System.Action onAllComplete = null)
    {
        if (cards == null || targetPositions == null || cards.Length != targetPositions.Length)
        {
            Debug.LogWarning("[CardAnimationManager] Invalid cards or positions array");
            onAllComplete?.Invoke();
            return;
        }

        StartCoroutine(DealCardsSequentially(cards, targetPositions, onAllComplete));
    }

    private IEnumerator DealCardsSequentially(GameObject[] cards, Vector3[] targetPositions, System.Action onComplete)
    {
        for (int i = 0; i < cards.Length; i++)
        {
            bool cardComplete = false;
            AnimateCardDeal(cards[i], targetPositions[i], () => cardComplete = true);

            // Wait for this card animation to complete
            yield return new WaitUntil(() => cardComplete);

            // Delay before next card
            yield return new WaitForSeconds(dealDelay);
        }

        onComplete?.Invoke();
    }

    /// <summary>
    /// Animate a card bouncing (for emphasis)
    /// </summary>
    public void AnimateCardBounce(GameObject card)
    {
        if (card == null) return;

        Sequence sequence = DOTween.Sequence();
        sequence.Append(card.transform.DOScale(Vector3.one * 1.1f, 0.1f).SetEase(Ease.OutQuad));
        sequence.Append(card.transform.DOScale(Vector3.one, 0.1f).SetEase(Ease.InQuad));
    }

    /// <summary>
    /// Animate a card being discarded/removed
    /// </summary>
    public void AnimateCardDiscard(GameObject card, System.Action onComplete = null)
    {
        if (card == null)
        {
            onComplete?.Invoke();
            return;
        }

        Sequence sequence = DOTween.Sequence();

        // Fade out and shrink
        var canvasGroup = card.GetComponent<CanvasGroup>();
        if (canvasGroup != null)
        {
            sequence.Append(canvasGroup.DOFade(0, 0.3f));
        }

        sequence.Join(card.transform.DOScale(Vector3.zero, 0.3f).SetEase(Ease.InBack));

        sequence.OnComplete(() =>
        {
            if (card != null)
            {
                Destroy(card);
            }
            onComplete?.Invoke();
        });
    }

    /// <summary>
    /// Stop all animations on a card
    /// </summary>
    public void StopCardAnimation(GameObject card)
    {
        if (card != null)
        {
            card.transform.DOKill();
        }
    }

    /// <summary>
    /// Stop all card animations
    /// </summary>
    public void StopAllAnimations()
    {
        DOTween.KillAll();
    }
}
