using System;
using System.Collections;
using UnityEngine;
using UnityEngine.UI;

namespace Kamgam.UGUIParticles
{
    public class UGUIParticlesProgressBarDemo : MonoBehaviour
    {
        public Image ProgressFill;

        protected ParticleImage _particleImage;
        public ParticleImage ParticleImage
        {
            get
            {
                if (_particleImage == null)
                {
                    _particleImage = this.GetComponentInChildren<ParticleImage>();
                }
                return _particleImage;
            }
        }

        protected Coroutine _progressAnimation;

        public IEnumerator Start()
        {
            // Wait a bit before animation the progress
            yield return new WaitForSeconds(1f);

            _progressAnimation = StartCoroutine(animateProgress());
        }

        private IEnumerator animateProgress()
        {
            ParticleImage.Stop(stopBehaviour: ParticleSystemStopBehavior.StopEmittingAndClear);
            SetProgress(0f);

            // 0 - 100% progress animation
            float speed = 1f;
            while (GetProgress() < 1f)
            {
                yield return null;

                // Add some fake pauses in the middle to make it feel a bit more realistic
                if (GetProgress() > 0.3f && GetProgress() < 0.5f)
                {
                    if (UnityEngine.Random.value > 0.9f)
                    {
                        yield return new WaitForSeconds(UnityEngine.Random.value * 0.2f);
                    }
                }
                // Make it faster in the second half
                if (GetProgress() > 0.5f)
                    speed = 2f;

                SetProgress(GetProgress() + (speed * Time.deltaTime / 3f));
            }

            _progressAnimation = null;
        }

        public void OnClick()
        {
            if (_progressAnimation != null)
            {
                StopCoroutine(_progressAnimation);
            }
            _progressAnimation = StartCoroutine(animateProgress());
        }

        public void SetProgress(float value)
        {
            if (value != ProgressFill.fillAmount)
            {
                ProgressFill.fillAmount = value;
                onProgressChanged(value);
            }
        }

        public float GetProgress()
        {
            return ProgressFill.fillAmount;
        }

        private void onProgressChanged(float value)
        {
            // +2f to have the particle positioned a bit inside the end of the progress fill.
            ParticleImage.PositionXUnit = ParticlesLengthUnit.Percent;
            ParticleImage.PositionX = Mathf.Clamp((value * 100f) + 2f, 0f, 100f);

            // Stop particles once we are done.
            if (value >= 1f)
            {
                ParticleImage.PositionX = 100f;
                ParticleImage.Stop();
            }
            // Make sure particles are playing.
            else if (value > 0.01f && !ParticleImage.IsPlaying)
            {
                ParticleImage.Play();
            }
        }
    }
}
