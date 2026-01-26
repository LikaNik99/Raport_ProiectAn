using UnityEngine;

/// <summary>
/// Manages particle effects for the game with null-safe checks
/// </summary>
public class ParticleEffectsManager : MonoBehaviour
{
    public static ParticleEffectsManager Instance { get; private set; }

    [Header("Win/Loss Effects")]
    [Tooltip("Particle effect for winning a hand")]
    public ParticleSystem winParticles;
    public Transform winParticleTransform;

    [Tooltip("Particle effect for losing a hand")]
    public ParticleSystem loseParticles;
    public Transform loseParticleTransform;

    [Tooltip("Particle effect for getting blackjack")]
    public ParticleSystem blackjackParticles;
    public Transform blackjackParticleTransform;

    [Tooltip("Particle effect for busting")]
    public ParticleSystem bustParticles;
    public Transform bustParticleTransform;

    [Tooltip("Particle effect for push/tie")]
    public ParticleSystem pushParticles;
    public Transform pushParticleTransform;

    [Header("Action Effects")]
    [Tooltip("Particle effect for placing a bet")]
    public ParticleSystem betPlacedParticles;
    public Transform betPlacedParticleTransform;

    [Tooltip("Particle effect for dealing cards")]
    public ParticleSystem cardDealParticles;
    public Transform cardDealParticleTransform;

    [Tooltip("Particle effect for splitting hand")]
    public ParticleSystem splitParticles;
    public Transform splitParticleTransform;

    [Tooltip("Particle effect for doubling down")]
    public ParticleSystem doubleDownParticles;
    public Transform doubleDownParticleTransform;

    [Tooltip("Particle effect for buying insurance")]
    public ParticleSystem insuranceParticles;
    public Transform insuranceParticleTransform;

    [Header("Special Effects")]
    [Tooltip("Ambient background particles")]
    public ParticleSystem ambientParticles;
    public Transform ambientParticleTransform; // optional, only if you want a fixed location

    [Tooltip("Particle effect for critical moments")]
    public ParticleSystem specialParticles;
    public Transform specialParticleTransform;

    private void Awake()
    {
        // Singleton pattern
        if (Instance == null)
        {
            Instance = this;
            // DontDestroyOnLoad(gameObject);
        }
        // else
        // {
        //     Destroy(gameObject);
        // }
    }

    /// <summary>
    /// Play win effect
    /// </summary>
    public void PlayWinEffect(Vector3 fallbackPosition)
    {
        PlayParticle(winParticles, winParticleTransform, fallbackPosition);
    }

    /// <summary>
    /// Play lose effect
    /// </summary>
    public void PlayLoseEffect(Vector3 fallbackPosition)
    {
        PlayParticle(loseParticles, loseParticleTransform, fallbackPosition);
    }

    /// <summary>
    /// Play blackjack effect
    /// </summary>
    public void PlayBlackjackEffect(Vector3 fallbackPosition)
    {
        PlayParticle(blackjackParticles, blackjackParticleTransform, fallbackPosition);
    }

    /// <summary>
    /// Play bust effect
    /// </summary>
    public void PlayBustEffect(Vector3 fallbackPosition)
    {
        PlayParticle(bustParticles, bustParticleTransform, fallbackPosition);
    }

    /// <summary>
    /// Play push effect
    /// </summary>
    public void PlayPushEffect(Vector3 fallbackPosition)
    {
        PlayParticle(pushParticles, pushParticleTransform, fallbackPosition);
    }

    /// <summary>
    /// Play bet placed effect
    /// </summary>
    public void PlayBetPlacedEffect(Vector3 fallbackPosition)
    {
        PlayParticle(betPlacedParticles, betPlacedParticleTransform, fallbackPosition);
    }

    /// <summary>
    /// Play card deal effect
    /// </summary>
    public void PlayCardDealEffect(Vector3 fallbackPosition)
    {
        PlayParticle(cardDealParticles, cardDealParticleTransform, fallbackPosition);
    }

    /// <summary>
    /// Play split effect
    /// </summary>
    public void PlaySplitEffect(Vector3 fallbackPosition)
    {
        PlayParticle(splitParticles, splitParticleTransform, fallbackPosition);
    }

    /// <summary>
    /// Play double down effect
    /// </summary>
    public void PlayDoubleDownEffect(Vector3 fallbackPosition)
    {
        PlayParticle(doubleDownParticles, doubleDownParticleTransform, fallbackPosition);
    }

    /// <summary>
    /// Play insurance effect
    /// </summary>
    public void PlayInsuranceEffect(Vector3 fallbackPosition)
    {
        PlayParticle(insuranceParticles, insuranceParticleTransform, fallbackPosition);
    }

    /// <summary>
    /// Play special effect
    /// </summary>
    public void PlaySpecialEffect(Vector3 fallbackPosition)
    {
        PlayParticle(specialParticles, specialParticleTransform, fallbackPosition);
    }

    /// <summary>
    /// Start or stop ambient particles
    /// </summary>
    public void SetAmbientParticles(bool active)
    {
        if (ambientParticles == null) return;

        if (active)
        {
            if (!ambientParticles.isPlaying)
            {
                // Optionally snap to transform position if provided
                if (ambientParticleTransform != null)
                    ambientParticles.transform.position = ambientParticleTransform.position;

                ambientParticles.Play();
            }
        }
        else
        {
            if (ambientParticles.isPlaying)
                ambientParticles.Stop();
        }
    }

    /// <summary>
    /// High-level helper: uses transform if available, otherwise uses fallback position
    /// </summary>
    private void PlayParticle(ParticleSystem particlePrefab, Transform spawnTransform, Vector3 fallbackPosition)
    {
        if (particlePrefab == null)
        {
            Debug.LogWarning("[ParticleEffectsManager] Attempted to play null particle system");
            return;
        }

        Vector3 spawnPos = fallbackPosition;
        if (spawnTransform != null)
        {
            spawnPos = spawnTransform.position;
        }

        PlayParticleAtPosition(particlePrefab, spawnPos);
    }

    /// <summary>
    /// Helper method to instantiate and play a particle system at a position
    /// </summary>
    private void PlayParticleAtPosition(ParticleSystem particlePrefab, Vector3 position)
    {
        if (particlePrefab == null)
        {
            Debug.LogWarning("[ParticleEffectsManager] Attempted to play null particle system");
            return;
        }

        ParticleSystem instance = Instantiate(particlePrefab, position, Quaternion.identity);
        instance.Play();

        // Auto-destroy after the particle has finished
        var main = instance.main;
        float maxLifetime = main.startLifetime.constantMax;
        float duration = main.duration + maxLifetime;

        Destroy(instance.gameObject, duration);
    }

    /// <summary>
    /// Stop all currently playing particles
    /// </summary>
    public void StopAllParticles()
    {
        if (winParticles != null && winParticles.isPlaying) winParticles.Stop();
        if (loseParticles != null && loseParticles.isPlaying) loseParticles.Stop();
        if (blackjackParticles != null && blackjackParticles.isPlaying) blackjackParticles.Stop();
        if (bustParticles != null && bustParticles.isPlaying) bustParticles.Stop();
        if (pushParticles != null && pushParticles.isPlaying) pushParticles.Stop();
        if (betPlacedParticles != null && betPlacedParticles.isPlaying) betPlacedParticles.Stop();
        if (cardDealParticles != null && cardDealParticles.isPlaying) cardDealParticles.Stop();
        if (splitParticles != null && splitParticles.isPlaying) splitParticles.Stop();
        if (doubleDownParticles != null && doubleDownParticles.isPlaying) doubleDownParticles.Stop();
        if (insuranceParticles != null && insuranceParticles.isPlaying) insuranceParticles.Stop();
        if (specialParticles != null && specialParticles.isPlaying) specialParticles.Stop();
        if (ambientParticles != null && ambientParticles.isPlaying) ambientParticles.Stop();
    }
}
