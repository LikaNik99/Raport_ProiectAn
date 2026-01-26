using System;
using System.Collections;
using Cysharp.Threading.Tasks;
using UnityEngine;
using UnityEngine.UI;

/// <summary>
/// Handles dealing and animating cards
/// </summary>
public class CardDealer : MonoBehaviour
{
    public static CardDealer Instance { get; private set; }

    [Header("Card Settings")]
    public GameObject cardPrefab;
    public Transform deckPosition;
    private RectTransform deckPositionRect;
    public float dealDuration = 0.5f;
    public float dealDelay = 0.2f;

    [Header("Audio")]
    public AudioClip dealCardSound;
    public AudioClip flipCardSound;

    private AudioSource audioSource;

    private void Awake()
    {
        if (Instance == null)
        {
            Instance = this;
        }
        else
        {
            Destroy(gameObject);
        }

        audioSource = GetComponent<AudioSource>();
        if (audioSource == null)
        {
            audioSource = gameObject.AddComponent<AudioSource>();
        }

        deckPositionRect = deckPosition.GetComponent<RectTransform>();
    }

    /// <summary>
    /// Deal a card from deck to target position
    /// </summary>
    public void DealCard(CardData cardData, Transform target, bool faceUp = true, System.Action onComplete = null)
    {
        StartCoroutine(DealCardCoroutine(cardData, target, faceUp, onComplete));
    }

    /// <summary>
    /// Deal multiple cards in sequence
    /// </summary>
    public void DealCards(CardData[] cards, Transform target, bool faceUp = true, System.Action onComplete = null)
    {
        StartCoroutine(DealCardsCoroutine(cards, target, faceUp, onComplete));
    }

    private IEnumerator DealCardCoroutine(CardData cardData, Transform target, bool faceUp, System.Action onComplete)
    {
        if (cardPrefab == null || target == null)
        {
            Debug.LogWarning("Card prefab or target not set!");
            onComplete?.Invoke();
            yield break;
        }

        // Instantiate card at deck position
        GameObject cardObj = Instantiate(cardPrefab, deckPosition != null ? deckPosition.position : Vector3.zero, Quaternion.identity);
        cardObj.transform.SetParent(target, true);

        // Set card data
        Card card = cardObj.GetComponent<Card>();
        if (card != null)
        {
            CardData tempData = new CardData(cardData.suit, cardData.rank, faceUp);
            card.SetCard(tempData);
        }

        // Play deal sound
        PlaySound(dealCardSound);

        // Animate to target position
        Vector3 startPos = cardObj.transform.position;
        Vector3 targetPos = target.position;
        float elapsed = 0f;

        while (elapsed < dealDuration)
        {
            elapsed += Time.deltaTime;
            float t = elapsed / dealDuration;

            // Ease out curve
            t = 1f - Mathf.Pow(1f - t, 3f);

            cardObj.transform.position = Vector3.Lerp(startPos, targetPos, t);
            yield return null;
        }

        cardObj.transform.position = targetPos;

        // Flip to face up if needed
        if (!faceUp && card != null)
        {
            card.Flip(false);
        }

        onComplete?.Invoke();
        Rebuild();
    }

    private void Update()
    {
        LayoutRebuilder.ForceRebuildLayoutImmediate(deckPositionRect);
    }

    private async UniTask Rebuild()
    {
        deckPosition.GetComponent<HorizontalLayoutGroup>().enabled = false;
        // yield return new WaitForEndOfFrame();
        await UniTask.WaitForSeconds(0.2f);
        deckPosition.GetComponent<HorizontalLayoutGroup>().enabled = true;
        LayoutRebuilder.ForceRebuildLayoutImmediate(deckPosition.GetComponent<RectTransform>());
    }
    private IEnumerator DealCardsCoroutine(CardData[] cards, Transform target, bool faceUp, System.Action onComplete)
    {
        foreach (var cardData in cards)
        {
            bool completed = false;
            DealCard(cardData, target, faceUp, () =>
            {
                completed = true;
                Rebuild();
            });

            // Wait for deal to complete
            yield return new WaitUntil(() => completed);

            // Small delay between cards
            yield return new WaitForSeconds(dealDelay);
        }
        // deckPosition.GetComponent<HorizontalLayoutGroup>().enabled = false;
        // yield return new WaitForEndOfFrame();
        // deckPosition.GetComponent<HorizontalLayoutGroup>().enabled = true;
        // LayoutRebuilder.ForceRebuildLayoutImmediate(deckPosition.GetComponent<RectTransform>());
        onComplete?.Invoke();
        
    }

    /// <summary>
    /// Flip all cards in a container
    /// </summary>
    public void FlipCards(Transform container, bool faceUp)
    {
        PlaySound(flipCardSound);

        foreach (Transform child in container)
        {
            Card card = child.GetComponent<Card>();
            if (card != null)
            {
                card.AnimateFlip(faceUp);
            }
        }
    }

    /// <summary>
    /// Clear all cards in a container
    /// </summary>
    public void ClearCards(Transform container)
    {
        foreach (Transform child in container)
        {
            Destroy(child.gameObject);
        }
    }

    /// <summary>
    /// Play a sound effect
    /// </summary>
    private void PlaySound(AudioClip clip)
    {
        if (audioSource != null && clip != null)
        {
            audioSource.PlayOneShot(clip);
        }
    }
}
