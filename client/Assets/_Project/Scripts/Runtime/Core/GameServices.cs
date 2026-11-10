using Shipmaster.Services.Ads;
using Shipmaster.Services.Purchases;
using Shipmaster.Services.Save;
using Shipmaster.Views;

namespace Shipmaster.Core
{
    /// <summary>
    /// Every game-wide service, created once by <see cref="GameBootstrap"/>.
    /// Gameplay code depends on the interfaces only, so fakes can be swapped
    /// for real SDKs (IAP, ads, server) in one place.
    /// </summary>
    public sealed class GameServices
    {
        public ISaveService Save { get; }
        public IAdsService Ads { get; }
        public IPurchaseService Purchases { get; }
        public IViewFactory Views { get; }

        public GameServices(ISaveService save, IAdsService ads, IPurchaseService purchases, IViewFactory views)
        {
            Save = save;
            Ads = ads;
            Purchases = purchases;
            Views = views;
        }
    }
}
