using System.IO;
using Shipmaster.Gameplay;
using Shipmaster.Services.Ads;
using Shipmaster.Services.Purchases;
using Shipmaster.Services.Save;
using Shipmaster.Views;
using UnityEngine;
using UnityEngine.SceneManagement;

namespace Shipmaster.Core
{
    /// <summary>
    /// Composition root. Runs after the Bootstrap scene loads, builds the services
    /// and spawns the world from code — the scene itself only holds camera and light.
    /// </summary>
    public static class GameBootstrap
    {
        public const string BootstrapScene = "Bootstrap";

        public static GameServices Services { get; private set; }

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        static void Start()
        {
            if (SceneManager.GetActiveScene().name != BootstrapScene)
                return;

            Services = CreateServices();
            SpawnWorld(Services);
        }

        static GameServices CreateServices() => new(
            new JsonSaveService(Path.Combine(Application.persistentDataPath, "save.json")),
            new FakeAdsService(),
            new FakePurchaseService(),
            new ResourcesViewFactory());

        static void SpawnWorld(GameServices services)
        {
            services.Views.Create(ViewKeys.Ocean, null);

            var ship = new GameObject("Ship").AddComponent<Ship>();
            services.Views.Create(ViewKeys.Ship, ship.transform);
        }
    }
}
