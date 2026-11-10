using UnityEngine;

namespace Shipmaster.Services.Ads
{
    /// <summary>Stand-in until an ad network SDK is integrated: always rewards.</summary>
    public sealed class FakeAdsService : IAdsService
    {
        public bool IsRewardedReady => true;

        public async Awaitable<bool> ShowRewardedAsync(string placement)
        {
            Debug.Log($"[FakeAds] rewarded '{placement}'");
            await Awaitable.WaitForSecondsAsync(0.5f);
            return true;
        }

        public void ShowInterstitial(string placement) =>
            Debug.Log($"[FakeAds] interstitial '{placement}'");
    }
}
