using UnityEngine;

namespace Shipmaster.Services.Purchases
{
    /// <summary>Stand-in until Unity IAP and server-side receipt validation are integrated.</summary>
    public sealed class FakePurchaseService : IPurchaseService
    {
        public async Awaitable<PurchaseResult> PurchaseAsync(string productId)
        {
            Debug.Log($"[FakePurchases] buying '{productId}'");
            await Awaitable.WaitForSecondsAsync(0.5f);
            return PurchaseResult.Success;
        }
    }
}
