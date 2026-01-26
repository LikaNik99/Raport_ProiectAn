using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

/// <summary>
/// Manages mobile-specific touch controls and input handling
/// </summary>
public class MobileInputManager : MonoBehaviour
{
    public static MobileInputManager Instance { get; private set; }

    [Header("Touch Settings")]
    [Tooltip("Minimum swipe distance in pixels to register as a swipe")]
    public float minSwipeDistance = 100f;

    [Tooltip("Maximum time for a tap in seconds")]
    public float maxTapTime = 0.3f;

    [Header("Button Touch Areas")]
    [Tooltip("Increase button touch area by this percentage for easier mobile tapping")]
    [Range(0f, 1f)]
    public float touchAreaIncrease = 0.2f;

    [Header("Mobile Optimizations")]
    public bool enableHapticFeedback = true;
    public bool enableSwipeGestures = true;
    public bool enableDoubleTap = true;

    private Vector2 touchStartPos;
    private float touchStartTime;
    private bool isSwiping = false;

    private void Awake()
    {
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

    private void Start()
    {
        // Automatically detect mobile platform and apply optimizations
        if (IsMobilePlatform())
        {
            ApplyMobileOptimizations();
        }
    }

    private void Update()
    {
        // Handle touch input on mobile
        if (enableSwipeGestures && Input.touchCount > 0)
        {
            HandleTouch(Input.GetTouch(0));
        }
    }

    /// <summary>
    /// Check if running on a mobile platform
    /// </summary>
    public bool IsMobilePlatform()
    {
        return Application.platform == RuntimePlatform.Android ||
               Application.platform == RuntimePlatform.IPhonePlayer;
    }

    /// <summary>
    /// Apply mobile-specific UI optimizations
    /// </summary>
    private void ApplyMobileOptimizations()
    {
        Debug.Log("[MobileInputManager] Applying mobile optimizations");

        // Increase button sizes for better touch targets
        EnlargeTouchTargets();

        // Adjust canvas scaler for mobile
        AdjustCanvasScalerForMobile();

        // Set target framerate for mobile
        Application.targetFrameRate = 60;
    }

    /// <summary>
    /// Enlarge touch targets for all buttons
    /// </summary>
    private void EnlargeTouchTargets()
    {
        if (touchAreaIncrease <= 0) return;

        Button[] allButtons = FindObjectsOfType<Button>(true);
        foreach (Button button in allButtons)
        {
            RectTransform rectTransform = button.GetComponent<RectTransform>();
            if (rectTransform != null)
            {
                // Increase the button's size slightly
                Vector2 sizeDelta = rectTransform.sizeDelta;
                rectTransform.sizeDelta = sizeDelta * (1f + touchAreaIncrease);
            }
        }

        Debug.Log($"[MobileInputManager] Enlarged {allButtons.Length} button touch targets by {touchAreaIncrease * 100}%");
    }

    /// <summary>
    /// Adjust canvas scaler settings for mobile
    /// </summary>
    private void AdjustCanvasScalerForMobile()
    {
        CanvasScaler[] canvasScalers = FindObjectsOfType<CanvasScaler>(true);
        foreach (CanvasScaler scaler in canvasScalers)
        {
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1920, 1080);
            scaler.screenMatchMode = CanvasScaler.ScreenMatchMode.MatchWidthOrHeight;
            scaler.matchWidthOrHeight = 0.5f; // Balanced scaling
        }

        Debug.Log($"[MobileInputManager] Adjusted {canvasScalers.Length} canvas scalers for mobile");
    }

    /// <summary>
    /// Handle touch input for gestures
    /// </summary>
    private void HandleTouch(Touch touch)
    {
        switch (touch.phase)
        {
            case TouchPhase.Began:
                touchStartPos = touch.position;
                touchStartTime = Time.time;
                isSwiping = false;
                break;

            case TouchPhase.Moved:
                if (!isSwiping && Vector2.Distance(touchStartPos, touch.position) > minSwipeDistance)
                {
                    isSwiping = true;
                    Vector2 swipeDirection = (touch.position - touchStartPos).normalized;
                    OnSwipe(swipeDirection);
                }
                break;

            case TouchPhase.Ended:
                float touchDuration = Time.time - touchStartTime;
                if (!isSwiping && touchDuration < maxTapTime)
                {
                    OnTap(touch.position);
                }
                isSwiping = false;
                break;
        }
    }

    /// <summary>
    /// Called when a swipe gesture is detected
    /// </summary>
    private void OnSwipe(Vector2 direction)
    {
        Debug.Log($"[MobileInputManager] Swipe detected: {direction}");

        // You can add custom swipe actions here
        // For example: swipe right to hit, swipe left to stand
        if (direction.x > 0.5f)
        {
            // Swipe right - could be used for quick actions
            Debug.Log("[MobileInputManager] Swipe right");
        }
        else if (direction.x < -0.5f)
        {
            // Swipe left
            Debug.Log("[MobileInputManager] Swipe left");
        }

        PlayHapticFeedback();
    }

    /// <summary>
    /// Called when a tap is detected
    /// </summary>
    private void OnTap(Vector2 position)
    {
        Debug.Log($"[MobileInputManager] Tap detected at: {position}");
        // Taps are usually handled by the UI system automatically
    }

    /// <summary>
    /// Play haptic feedback on mobile devices
    /// </summary>
    public void PlayHapticFeedback()
    {
        if (!enableHapticFeedback) return;

#if UNITY_ANDROID || UNITY_IOS
        Handheld.Vibrate();
#endif
        Debug.Log("[MobileInputManager] Haptic feedback played");
    }

    /// <summary>
    /// Play light haptic feedback
    /// </summary>
    public void PlayLightHaptic()
    {
        if (!enableHapticFeedback) return;

#if UNITY_IOS
        // iOS light haptic
        Handheld.Vibrate();
#elif UNITY_ANDROID
        // Android light haptic
        Handheld.Vibrate();
#endif
    }

    /// <summary>
    /// Set mobile button layouts for better touch experience
    /// </summary>
    public void OptimizeButtonLayout(GameObject panel)
    {
        if (panel == null) return;

        // Increase spacing between buttons on mobile
        HorizontalLayoutGroup horizontalLayout = panel.GetComponent<HorizontalLayoutGroup>();
        if (horizontalLayout != null)
        {
            horizontalLayout.spacing = Mathf.Max(horizontalLayout.spacing, 20f);
            horizontalLayout.padding.left = Mathf.Max(horizontalLayout.padding.left, 20);
            horizontalLayout.padding.right = Mathf.Max(horizontalLayout.padding.right, 20);
        }

        VerticalLayoutGroup verticalLayout = panel.GetComponent<VerticalLayoutGroup>();
        if (verticalLayout != null)
        {
            verticalLayout.spacing = Mathf.Max(verticalLayout.spacing, 20f);
            verticalLayout.padding.top = Mathf.Max(verticalLayout.padding.top, 20);
            verticalLayout.padding.bottom = Mathf.Max(verticalLayout.padding.bottom, 20);
        }
    }
}
