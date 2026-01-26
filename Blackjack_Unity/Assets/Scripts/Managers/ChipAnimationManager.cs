using UnityEngine;
using System.Collections;
using DG.Tweening;
using UnityEngine.UI;
using TMPro;

/// <summary>
/// Manages chip and money animations for wins/losses
/// </summary>
public class ChipAnimationManager : MonoBehaviour
{
    public static ChipAnimationManager Instance { get; private set; }

    [Header("Chip Prefabs")]
    [Tooltip("Prefab for chip visual (optional)")]
    public GameObject chipPrefab;

    [Header("Animation Settings")]
    [Tooltip("Duration of chip movement animation")]
    public float chipMoveDuration = 0.8f;

    [Tooltip("Duration of money text float animation")]
    public float floatTextDuration = 1.5f;

    [Tooltip("Height that money text floats up")]
    public float floatHeight = 100f;

    [Header("Floating Text")]
    [Tooltip("Prefab for floating money text (+$50, -$20, etc.)")]
    public GameObject floatingTextPrefab;

    [Tooltip("Parent canvas for floating text")]
    public Canvas uiCanvas;

    private void Awake()
    {
        if (Instance == null)
        {
            Instance = this;
        }

        if (uiCanvas == null)
        {
            uiCanvas = FindObjectOfType<Canvas>();
        }
    }

    /// <summary>
    /// Animate winning chips moving to player
    /// </summary>
    public void AnimateWinChips(Vector3 fromPosition, Vector3 toPosition, float amount, System.Action onComplete = null)
    {
        // Show floating text for winnings
        ShowFloatingText($"+${amount:F2}", toPosition, Color.green);

        // Play win sound
        if (AudioManager.Instance != null)
        {
            AudioManager.Instance.PlayWin();
        }

        // Play win particle effect
        if (ParticleEffectsManager.Instance != null)
        {
            ParticleEffectsManager.Instance.PlayWinEffect(toPosition);
        }

        // If chip prefab exists, animate chips moving
        if (chipPrefab != null)
        {
            StartCoroutine(AnimateChipStack(fromPosition, toPosition, Mathf.Min(5, Mathf.CeilToInt(amount / 10)), onComplete));
        }
        else
        {
            onComplete?.Invoke();
        }
    }

    /// <summary>
    /// Animate losing chips moving away from player
    /// </summary>
    public void AnimateLoseChips(Vector3 fromPosition, Vector3 toPosition, float amount, System.Action onComplete = null)
    {
        // Show floating text for loss
        ShowFloatingText($"-${amount:F2}", fromPosition, Color.red);

        // Play lose sound
        if (AudioManager.Instance != null)
        {
            AudioManager.Instance.PlayLose();
        }
Debug.LogError("yuuuuuuuu");
        // Play lose particle effect
        if (ParticleEffectsManager.Instance != null)
        {
            ParticleEffectsManager.Instance.PlayLoseEffect(fromPosition);
        }

        // If chip prefab exists, animate chips moving
        if (chipPrefab != null)
        {
            StartCoroutine(AnimateChipStack(fromPosition, toPosition, Mathf.Min(3, Mathf.CeilToInt(amount / 10)), onComplete));
        }
        else
        {
            onComplete?.Invoke();
        }
    }

    /// <summary>
    /// Animate push (tie) - chips stay but show text
    /// </summary>
    public void AnimatePush(Vector3 position, float amount)
    {
        // Show floating text for push
        ShowFloatingText($"PUSH ${amount:F2}", position, Color.yellow);

        // Play push sound
        if (AudioManager.Instance != null)
        {
            AudioManager.Instance.PlayPush();
        }

        // Play push particle effect
        if (ParticleEffectsManager.Instance != null)
        {
            ParticleEffectsManager.Instance.PlayPushEffect(position);
        }
    }

    /// <summary>
    /// Animate blackjack special win
    /// </summary>
    public void AnimateBlackjackWin(Vector3 position, float amount)
    {
        // Show special floating text for blackjack
        ShowFloatingText($"BLACKJACK! +${amount:F2}", position, Color.yellow, 1.5f);

        // Play blackjack sound
        if (AudioManager.Instance != null)
        {
            AudioManager.Instance.PlayBlackjack();
        }

        // Play blackjack particle effect
        if (ParticleEffectsManager.Instance != null)
        {
            ParticleEffectsManager.Instance.PlayBlackjackEffect(position);
        }
    }

    /// <summary>
    /// Animate bust
    /// </summary>
    public void AnimateBust(Vector3 position, float amount)
    {
        // Show floating text for bust
        ShowFloatingText($"BUST! -${amount:F2}", position, Color.red);

        // Play bust sound
        if (AudioManager.Instance != null)
        {
            AudioManager.Instance.PlayBust();
        }

        // Play bust particle effect
        if (ParticleEffectsManager.Instance != null)
        {
            ParticleEffectsManager.Instance.PlayBustEffect(position);
        }
    }

    /// <summary>
    /// Animate chips being placed as a bet
    /// </summary>
    public void AnimateBetPlaced(Vector3 position, float amount, System.Action onComplete = null)
    {
        // Show floating text
        ShowFloatingText($"BET ${amount:F2}", position, Color.white, 0.8f);

        // Play bet sound
        if (AudioManager.Instance != null)
        {
            AudioManager.Instance.PlayBetPlaced();
        }

        // Play bet particle effect
        if (ParticleEffectsManager.Instance != null)
        {
            ParticleEffectsManager.Instance.PlayBetPlacedEffect(position);
        }

        onComplete?.Invoke();
    }

    /// <summary>
    /// Show floating text that rises and fades
    /// </summary>
    public void ShowFloatingText(string text, Vector3 worldPosition, Color color, float scale = 1f)
    {
        if (floatingTextPrefab == null || uiCanvas == null)
        {
            Debug.LogWarning("[ChipAnimationManager] FloatingTextPrefab or Canvas not assigned");
            return;
        }

        // Create floating text
        GameObject textObj = Instantiate(floatingTextPrefab, uiCanvas.transform);
        RectTransform rectTransform = textObj.GetComponent<RectTransform>();

        // Convert world position to screen position
        Vector2 screenPos = RectTransformUtility.WorldToScreenPoint(Camera.main, worldPosition);
        rectTransform.position = screenPos;

        // Set text
        TMP_Text tmpText = textObj.GetComponent<TMP_Text>();
        if (tmpText != null)
        {
            tmpText.text = text;
            tmpText.color = color;
            tmpText.fontSize *= scale;
        }
        else
        {
            Text legacyText = textObj.GetComponent<Text>();
            if (legacyText != null)
            {
                legacyText.text = text;
                legacyText.color = color;
                legacyText.fontSize = Mathf.RoundToInt(legacyText.fontSize * scale);
            }
        }

        // Animate floating up and fading
        Sequence sequence = DOTween.Sequence();

        // Float up
        sequence.Append(rectTransform.DOAnchorPosY(rectTransform.anchoredPosition.y + floatHeight, floatTextDuration).SetEase(Ease.OutQuad));

        // Scale up slightly then down
        sequence.Join(rectTransform.DOScale(Vector3.one * 1.2f, floatTextDuration * 0.3f).SetEase(Ease.OutQuad));
        sequence.Append(rectTransform.DOScale(Vector3.one * 0.8f, floatTextDuration * 0.7f).SetEase(Ease.InQuad));

        // Fade out
        CanvasGroup canvasGroup = textObj.GetComponent<CanvasGroup>();
        if (canvasGroup == null)
        {
            canvasGroup = textObj.AddComponent<CanvasGroup>();
        }
        sequence.Join(canvasGroup.DOFade(0, floatTextDuration).SetEase(Ease.InQuad));

        // Destroy after animation
        sequence.OnComplete(() =>
        {
            if (textObj != null)
            {
                Destroy(textObj);
            }
        });
    }

    /// <summary>
    /// Animate a stack of chips moving
    /// </summary>
    private IEnumerator AnimateChipStack(Vector3 from, Vector3 to, int chipCount, System.Action onComplete)
    {
        for (int i = 0; i < chipCount; i++)
        {
            GameObject chip = Instantiate(chipPrefab, from, Quaternion.identity);
            chip.transform.SetParent(uiCanvas.transform);

            // Stagger the animation slightly for each chip
            float delay = i * 0.1f;
            float duration = chipMoveDuration + Random.Range(-0.1f, 0.1f);

            // Animate chip movement with slight arc
            Sequence sequence = DOTween.Sequence();
            sequence.AppendInterval(delay);
            sequence.Append(chip.transform.DOMove(to, duration).SetEase(Ease.InOutQuad));

            // Add some rotation
            sequence.Join(chip.transform.DORotate(new Vector3(0, 0, Random.Range(-180f, 180f)), duration));

            // Scale effect
            sequence.Join(chip.transform.DOScale(Vector3.one * 0.8f, duration * 0.5f).SetEase(Ease.OutQuad));
            sequence.Append(chip.transform.DOScale(Vector3.zero, duration * 0.3f).SetEase(Ease.InBack));

            // Destroy chip after animation
            sequence.OnComplete(() =>
            {
                if (chip != null)
                {
                    Destroy(chip);
                }
            });

            yield return new WaitForSeconds(0.1f);
        }

        // Wait for all animations to complete
        yield return new WaitForSeconds(chipMoveDuration);

        onComplete?.Invoke();
    }

    /// <summary>
    /// Create a simple chip stack visualization
    /// </summary>
    public GameObject CreateChipStack(Transform parent, float amount)
    {
        if (chipPrefab == null)
        {
            Debug.LogWarning("[ChipAnimationManager] Chip prefab not assigned");
            return null;
        }

        // Create container for chip stack
        GameObject stackContainer = new GameObject("ChipStack");
        stackContainer.transform.SetParent(parent);
        stackContainer.transform.localPosition = Vector3.zero;

        // Calculate number of chips based on amount (1 chip per $10)
        int chipCount = Mathf.Max(1, Mathf.CeilToInt(amount / 10f));
        chipCount = Mathf.Min(chipCount, 10); // Max 10 chips in stack

        // Stack chips vertically
        float chipHeight = 5f; // Adjust based on your chip prefab size
        for (int i = 0; i < chipCount; i++)
        {
            GameObject chip = Instantiate(chipPrefab, stackContainer.transform);
            chip.transform.localPosition = new Vector3(0, i * chipHeight, 0);

            // Slightly rotate for visual variety
            chip.transform.localRotation = Quaternion.Euler(0, 0, Random.Range(-5f, 5f));
        }

        return stackContainer;
    }

    /// <summary>
    /// Remove chip stack with animation
    /// </summary>
    public void RemoveChipStack(GameObject chipStack, System.Action onComplete = null)
    {
        if (chipStack == null)
        {
            onComplete?.Invoke();
            return;
        }

        // Animate stack disappearing
        chipStack.transform.DOScale(Vector3.zero, 0.5f).SetEase(Ease.InBack).OnComplete(() =>
        {
            if (chipStack != null)
            {
                Destroy(chipStack);
            }
            onComplete?.Invoke();
        });
    }
}
