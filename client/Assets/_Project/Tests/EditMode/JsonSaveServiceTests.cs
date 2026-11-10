using System.IO;
using NUnit.Framework;
using Shipmaster.Services.Save;

namespace Shipmaster.Tests
{
    public sealed class JsonSaveServiceTests
    {
        string _path;

        [SetUp]
        public void SetUp() => _path = Path.Combine(Path.GetTempPath(), $"shipmaster-test-{System.Guid.NewGuid()}.json");

        [TearDown]
        public void TearDown() => File.Delete(_path);

        [Test]
        public void Load_WithoutSaveFile_ReturnsFreshData()
        {
            var data = new JsonSaveService(_path).Load();

            Assert.AreEqual(0, data.Coins);
            Assert.IsFalse(data.AdsRemoved);
        }

        [Test]
        public void Save_ThenLoad_RoundTripsData()
        {
            var service = new JsonSaveService(_path);

            service.Save(new PlayerData { Coins = 42, AdsRemoved = true });
            var loaded = service.Load();

            Assert.AreEqual(42, loaded.Coins);
            Assert.IsTrue(loaded.AdsRemoved);
        }
    }
}
