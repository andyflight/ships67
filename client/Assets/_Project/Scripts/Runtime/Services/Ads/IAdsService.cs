using UnityEngine;

namespace Shipmaster.Services.Ads
{
    public interface IAdsService
    {
        bool IsRewardedReady { get; }

        /// <returns>True when the player watched the ad and earned the reward.</returns>
        Awaitable<bool> ShowRewardedAsync(string placement);

        void ShowInterstitial(string placement);
    }
}
