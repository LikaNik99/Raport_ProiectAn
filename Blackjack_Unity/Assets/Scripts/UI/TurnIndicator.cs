using UnityEngine;
using UnityEngine.UI;
using DG.Tweening;
using TMPro;

/// <summary>
/// Enhanced turn indicator with visual feedback
/// Shows whose turn it is with animations and effects
/// </summary>
public class TurnIndicator : MonoBehaviour
{
    [Header("Visual Elements")]
    [Tooltip("Image/Panel that highlights the current player")]
    public Image highlightImage;

    [Tooltip("Text showing current player name")]
    public TMP_Text turnText;

    [Tooltip("Optional arrow or pointer")]
    public GameObject arrow;

    [Header("Colors")]
    public Color yourTurnColor = Color.green;
    public Color otherPlayerTurnColor = Color.yellow;
    public Color waitingColor = Color.gray;

    [Header("Animation Settings")]
    public float pulseDuration = 1f;
    public float pulseScale = 1.1f;
    public bool enableGlow = true;

    private Sequence pulseSequence;
    private CanvasGroup canvasGroup;

    private void Awake()
    {
        canvasGroup = GetComponent<CanvasGroup>();
        if (canvasGroup == null)
        {
            canvasGroup = gameObject.AddComponent<CanvasGroup>();
        }
    }

    /// <summary>
    /// Show turn indicator for a specific player
    /// </summary>
    public void ShowTurn(string playerName, bool isYourTurn)
    {
        // Stop any existing animations
        StopPulse();

        // Show indicator
        if (canvasGroup != null)
        {
            canvasGroup.alpha = 1f;
        }

        // Set text
        if (turnText != null)
        {
            turnText.text = isYourTurn ? "YOUR TURN" : $"{playerName}'s Turn";
        }

        // Set color
        Color targetColor = isYourTurn ? yourTurnColor : otherPlayerTurnColor;
        if (highlightImage != null)
        {
            highlightImage.color = targetColor;
        }

        // Show arrow if available
        if (arrow != null)
        {
            arrow.SetActive(true);
        }

        // Start pulsing animation if it's your turn
        if (isYourTurn)
        {
            StartPulse();

            // Play haptic feedback
            if (MobileInputManager.Instance != null)
            {
                MobileInputManager.Instance.PlayLightHaptic();
            }
        }

        // Scale in animation
        transform.localScale = Vector3.zero;
        transform.DOScale(Vector3.one, 0.3f).SetEase(Ease.OutBack);
    }

    /// <summary>
    /// Hide turn indicator
    /// </summary>
    public void Hide()
    {
        StopPulse();

        // Fade out animation
        if (canvasGroup != null)
        {
            canvasGroup.DOFade(0, 0.3f).OnComplete(() =>
            {
                gameObject.SetActive(false);
            });
        }
        else
        {
            gameObject.SetActive(false);
        }

        // Hide arrow
        if (arrow != null)
        {
            arrow.SetActive(false);
        }
    }

    /// <summary>
    /// Show waiting state (between turns)
    /// </summary>
    public void ShowWaiting(string message = "Waiting...")
    {
        StopPulse();

        if (turnText != null)
        {
            turnText.text = message;
        }

        if (highlightImage != null)
        {
            highlightImage.color = waitingColor;
        }

        if (arrow != null)
        {
            arrow.SetActive(false);
        }
    }

    /// <summary>
    /// Start pulsing animation
    /// </summary>
    private void StartPulse()
    {
        if (pulseSequence != null && pulseSequence.IsActive())
        {
            return;
        }

        pulseSequence = DOTween.Sequence();
        pulseSequence.Append(transform.DOScale(Vector3.one * pulseScale, pulseDuration * 0.5f).SetEase(Ease.InOutQuad));
        pulseSequence.Append(transform.DOScale(Vector3.one, pulseDuration * 0.5f).SetEase(Ease.InOutQuad));
        pulseSequence.SetLoops(-1, LoopType.Restart);

        // Glow effect on highlight image
        if (enableGlow && highlightImage != null)
        {
            Sequence glowSequence = DOTween.Sequence();
            Color glowColor = highlightImage.color;
            Color brightColor = glowColor * 1.5f;
            brightColor.a = glowColor.a;

            glowSequence.Append(highlightImage.DOColor(brightColor, pulseDuration * 0.5f));
            glowSequence.Append(highlightImage.DOColor(glowColor, pulseDuration * 0.5f));
            glowSequence.SetLoops(-1, LoopType.Restart);
        }

        // Rotate arrow if available
        if (arrow != null)
        {
            arrow.transform.DORotate(new Vector3(0, 0, 360), 2f, RotateMode.FastBeyond360)
                .SetLoops(-1, LoopType.Restart)
                .SetEase(Ease.Linear);
        }
    }

    /// <summary>
    /// Stop pulsing animation
    /// </summary>
    private void StopPulse()
    {
        if (pulseSequence != null && pulseSequence.IsActive())
        {
            pulseSequence.Kill();
        }

        // Reset scale
        transform.DOScale(Vector3.one, 0.2f);

        // Stop highlight glow
        if (highlightImage != null)
        {
            highlightImage.DOKill();
        }

        // Stop arrow rotation
        if (arrow != null)
        {
            arrow.transform.DOKill();
        }
    }

    private void OnDestroy()
    {
        StopPulse();
        DOTween.Kill(transform);
        if (highlightImage != null) DOTween.Kill(highlightImage);
        if (arrow != null) DOTween.Kill(arrow.transform);
    }
}
