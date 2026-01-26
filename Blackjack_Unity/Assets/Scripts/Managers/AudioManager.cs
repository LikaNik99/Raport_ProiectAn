using UnityEngine;

/// <summary>
/// Manages all game audio
/// Only plays sounds if audio clips are assigned
/// </summary>
public class AudioManager : MonoBehaviour
{
    public static AudioManager Instance { get; private set; }

    [Header("Audio Sources")]
    public AudioSource musicSource;
    public AudioSource sfxSource;
    public AudioSource ambienceSource;

    [Header("Music")]
    public AudioClip menuMusic;
    public AudioClip gameMusic;

    [Header("UI Sounds")]
    public AudioClip buttonClickSound;
    public AudioClip buttonHoverSound;
    public AudioClip panelOpenSound;
    public AudioClip panelCloseSound;
    public AudioClip errorSound;
    public AudioClip successSound;
    public AudioClip notificationSound;
    public AudioClip countdownTickSound;

    [Header("Betting Sounds")]
    public AudioClip betPlacedSound;
    public AudioClip chipSound;
    public AudioClip chipStackSound;
    public AudioClip sliderMoveSound;
    public AudioClip allInSound;
    public AudioClip betIncreaseSound;
    public AudioClip betDecreaseSound;

    [Header("Card Sounds")]
    public AudioClip cardDealSound;
    public AudioClip cardFlipSound;
    public AudioClip cardShuffleSound;
    public AudioClip cardSlideSound;
    public AudioClip cardPlaceSound;
    public AudioClip deckTapSound;

    [Header("Game Action Sounds")]
    public AudioClip hitSound;
    public AudioClip standSound;
    public AudioClip splitSound;
    public AudioClip doubleDownSound;
    public AudioClip insuranceSound;

    [Header("Result Sounds")]
    public AudioClip winSound;
    public AudioClip loseSound;
    public AudioClip pushSound;
    public AudioClip blackjackSound;
    public AudioClip bustSound;
    public AudioClip bigWinSound;
    public AudioClip perfectGameSound;
    public AudioClip nearMissSound;

    [Header("Multiplayer Sounds")]
    public AudioClip playerJoinSound;
    public AudioClip playerLeaveSound;
    public AudioClip yourTurnSound;
    public AudioClip chatMessageSound;
    public AudioClip opponentActionSound;

    [Header("Ambient Sounds (Loopable)")]
    public AudioClip casinoAmbienceSound;
    public AudioClip tableChatterSound;
    public AudioClip dealerVoiceSound;
    public AudioClip crowdCheerSound;

    [Header("Settings")]
    [Range(0f, 1f)] public float musicVolume = 0.5f;
    [Range(0f, 1f)] public float sfxVolume = 1f;
    [Range(0f, 1f)] public float ambienceVolume = 0.3f;

    private void Awake()
    {
        if (Instance == null)
        {
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }
        else
        {
            Destroy(gameObject);
            return;
        }

        // Setup audio sources if they don't exist
        if (musicSource == null)
        {
            musicSource = gameObject.AddComponent<AudioSource>();
            musicSource.loop = true;
            musicSource.playOnAwake = false;
        }

        if (sfxSource == null)
        {
            sfxSource = gameObject.AddComponent<AudioSource>();
            sfxSource.loop = false;
            sfxSource.playOnAwake = false;
        }

        if (ambienceSource == null)
        {
            ambienceSource = gameObject.AddComponent<AudioSource>();
            ambienceSource.loop = true;
            ambienceSource.playOnAwake = false;
        }

        UpdateVolumes();
    }

    /// <summary>
    /// Update volume settings
    /// </summary>
    public void UpdateVolumes()
    {
        if (musicSource != null) musicSource.volume = musicVolume;
        if (sfxSource != null) sfxSource.volume = sfxVolume;
        if (ambienceSource != null) ambienceSource.volume = ambienceVolume;
    }

    /// <summary>
    /// Play music (only if clip is assigned)
    /// </summary>
    public void PlayMusic(AudioClip clip)
    {
        if (clip == null || musicSource == null) return;

        if (musicSource.clip == clip && musicSource.isPlaying) return;

        musicSource.clip = clip;
        musicSource.Play();
    }

    /// <summary>
    /// Stop music
    /// </summary>
    public void StopMusic()
    {
        if (musicSource != null) musicSource.Stop();
    }

    /// <summary>
    /// Play ambience sound (looping background)
    /// </summary>
    public void PlayAmbience(AudioClip clip)
    {
        if (clip == null || ambienceSource == null) return;

        if (ambienceSource.clip == clip && ambienceSource.isPlaying) return;

        ambienceSource.clip = clip;
        ambienceSource.Play();
    }

    /// <summary>
    /// Stop ambience
    /// </summary>
    public void StopAmbience()
    {
        if (ambienceSource != null) ambienceSource.Stop();
    }

    /// <summary>
    /// Play sound effect (only if clip is assigned)
    /// </summary>
    public void PlaySFX(AudioClip clip)
    {
        if (clip == null || sfxSource == null) return;
        sfxSource.PlayOneShot(clip);
    }

    /// <summary>
    /// Play sound effect at specific volume (only if clip is assigned)
    /// </summary>
    public void PlaySFX(AudioClip clip, float volume)
    {
        if (clip == null || sfxSource == null) return;
        sfxSource.PlayOneShot(clip, volume);
    }

    // ========== CONVENIENCE METHODS FOR SPECIFIC SOUNDS ==========

    // --- Music ---
    public void PlayMenuMusic() => PlayMusic(menuMusic);
    public void PlayGameMusic() => PlayMusic(gameMusic);

    // --- UI Sounds ---
    public void PlayButtonClick() => PlaySFX(buttonClickSound);
    public void PlayButtonHover() => PlaySFX(buttonHoverSound, 0.5f); // Lower volume for hover
    public void PlayPanelOpen() => PlaySFX(panelOpenSound);
    public void PlayPanelClose() => PlaySFX(panelCloseSound);
    public void PlayError() => PlaySFX(errorSound);
    public void PlaySuccess() => PlaySFX(successSound);
    public void PlayNotification() => PlaySFX(notificationSound);
    public void PlayCountdownTick() => PlaySFX(countdownTickSound);

    // --- Betting Sounds ---
    public void PlayBetPlaced() => PlaySFX(betPlacedSound);
    public void PlayChip() => PlaySFX(chipSound);
    public void PlayChipStack() => PlaySFX(chipStackSound);
    public void PlaySliderMove() => PlaySFX(sliderMoveSound, 0.3f); // Lower volume for slider
    public void PlayAllIn() => PlaySFX(allInSound);
    public void PlayBetIncrease() => PlaySFX(betIncreaseSound);
    public void PlayBetDecrease() => PlaySFX(betDecreaseSound);

    // --- Card Sounds ---
    public void PlayCardDeal() => PlaySFX(cardDealSound);
    public void PlayCardFlip() => PlaySFX(cardFlipSound);
    public void PlayCardShuffle() => PlaySFX(cardShuffleSound);
    public void PlayCardSlide() => PlaySFX(cardSlideSound);
    public void PlayCardPlace() => PlaySFX(cardPlaceSound);
    public void PlayDeckTap() => PlaySFX(deckTapSound);

    // --- Game Action Sounds ---
    public void PlayHit() => PlaySFX(hitSound);
    public void PlayStand() => PlaySFX(standSound);
    public void PlaySplit() => PlaySFX(splitSound);
    public void PlayDoubleDown() => PlaySFX(doubleDownSound);
    public void PlayInsurance() => PlaySFX(insuranceSound);

    // --- Result Sounds ---
    public void PlayWin() => PlaySFX(winSound);
    public void PlayLose() => PlaySFX(loseSound);
    public void PlayPush() => PlaySFX(pushSound);
    public void PlayBlackjack() => PlaySFX(blackjackSound);
    public void PlayBust() => PlaySFX(bustSound);
    public void PlayBigWin() => PlaySFX(bigWinSound);
    public void PlayPerfectGame() => PlaySFX(perfectGameSound);
    public void PlayNearMiss() => PlaySFX(nearMissSound);

    // --- Multiplayer Sounds ---
    public void PlayPlayerJoin() => PlaySFX(playerJoinSound);
    public void PlayPlayerLeave() => PlaySFX(playerLeaveSound);
    public void PlayYourTurn() => PlaySFX(yourTurnSound);
    public void PlayChatMessage() => PlaySFX(chatMessageSound);
    public void PlayOpponentAction() => PlaySFX(opponentActionSound, 0.7f); // Lower volume

    // --- Ambient Sounds ---
    public void PlayCasinoAmbience() => PlayAmbience(casinoAmbienceSound);
    public void PlayTableChatter() => PlayAmbience(tableChatterSound);
    public void PlayDealerVoice() => PlaySFX(dealerVoiceSound);
    public void PlayCrowdCheer() => PlaySFX(crowdCheerSound);
}
