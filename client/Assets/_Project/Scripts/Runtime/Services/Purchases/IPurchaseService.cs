using UnityEngine;

namespace Shipmaster.Services.Purchases
{
    public enum PurchaseResult
    {
        Success,
        Cancelled,
        Failed,
    }

    public interface IPurchaseService
    {
        Awaitable<PurchaseResult> PurchaseAsync(string productId);
    }

    /// <summary>Store product IDs; must match Google Play Console and App Store Connect.</summary>
    public static class ProductIds
    {
        public const string RemoveAds = "com.shipmaster.remove_ads";
    }
}
